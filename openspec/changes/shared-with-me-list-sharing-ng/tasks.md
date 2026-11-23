## 1. Networking layer — `sharedWithMe` endpoint (owncloudComLibrary)

- [ ] 1.1 Add Moshi response models for the `sharedWithMe` payload under `lib/resources/shares/responses/`, carrying only the eight fields the list consumes: a wrapper holding `value`, and an entry with `id`, `name`, `size`, `file` (for `mimeType`), `createdBy` (for `user.displayName`), `remoteItem` (for its `id` alone), plus `isHidden` annotated `@Json(name = "@UI.Hidden")` and `isSynchronized` annotated `@Json(name = "@client.synchronize")` — the two boolean properties carry the domain model's names, not the raw JSON spellings. Leave `eTag`, `lastModifiedDateTime`, `parentReference`, `folder`, `remoteItem.spaceId`, `remoteItem.parentReference` and the `permissions` array unparsed. Reuse only `UserResponse` and `FileResponse` from `lib/resources/spaces/responses/SpacesResponse.kt` and do **not** modify `MemberPermissionResponse` or anything else in that file. Verify with `./gradlew :owncloudComLibrary:assembleDebug` that the generated Moshi adapters compile, and that parsing the sample payload in issue #5020 populates every modelled field, silently ignores the rest, and matches each nullable/non-null choice.
- [ ] 1.2 Add `GetRemoteSharedWithMeOperation` under `lib/resources/shares/`, targeting `graph/v1beta1/me/drive/sharedWithMe` with no query parameters and following `GetRemoteGraphSharesForFileOperation` (`GetMethod`, Moshi adapter, `RemoteOperationResult`, Timber logging on both branches). Verify `./gradlew :owncloudComLibrary:assembleDebug` passes and the endpoint path constant reads `graph/v1beta1/me/drive/sharedWithMe`.
- [ ] 1.3 Declare `getSharedWithMe(): RemoteOperationResult<SharedWithMeResponse>` on `ShareService` next to `getGraphShares` and implement it in `OCShareService` by executing the new operation. Verify `./gradlew :owncloudComLibrary:assembleDebug` passes.

## 2. Data layer — remote data source, mapper and repository (owncloudData)

*The domain model and repository contract these tasks implement are added in group 3; create `OCShareWithMe` (task 3.1) alongside 2.1 if you compile group by group.*

- [ ] 2.1 Add `getSharedWithMe(accountName: String): List<OCShareWithMe>` to `RemoteShareDataSource` and implement it in `OCRemoteShareDataSource` via `executeRemoteOperation { clientManager.getShareService(accountName).getSharedWithMe() }`, with a private response→model mapper that reads `id`, `name`, `size` and `mimeType` (from `file?.mimeType`, null for a folder) off the entry itself, `remoteItemId` from `remoteItem.id`, `sharerDisplayName` from `createdBy.user.displayName`, `isHidden` from `@UI.Hidden` (absent → `false`) and `isSynchronized` from `@client.synchronize` (absent → `false`). Verify with the unit test added in 8.2.
- [ ] 2.2 Add `getSharedWithMe(accountName: String): List<OCShareWithMe>` to `ShareRepository` and implement it in `OCShareRepository` as a straight delegation to the remote data source, following `getGraphShares`. Verify with the unit test added in 8.3.

## 3. Domain layer — model, contract and use case (owncloudDomain)

- [ ] 3.1 Add `OCShareWithMe` under `domain/sharing/shares/model/` with exactly eight fields: `id` (the entry's own id in the Shares jail, used as the join key), `remoteItemId` (the real resource id), `name`, `size`, `mimeType` (null for a folder), `isHidden`, `sharerDisplayName` and `isSynchronized`. No folder flag and no space id — see design.md D6. Verify `./gradlew :owncloudDomain:assembleDebug` passes and the model carries no Android framework types.
- [ ] 3.2 Add `GetSharedWithMeAsyncUseCase` extending `BaseUseCaseWithResult<List<OCShareWithMe>, Params>` with an `accountName` param, returning only entries whose `isHidden` is `false` so the in-app list and the Documents Provider cannot diverge. Verify `./gradlew :owncloudDomain:assembleDebug` passes and the filter is applied in the use case, not in a caller.
- [ ] 3.3 Register `GetSharedWithMeAsyncUseCase` with `factoryOf(::GetSharedWithMeAsyncUseCase)` in `UseCaseModule`. Verify the app starts without a Koin `NoBeanDefFoundException` once a consumer injects it.

## 4. Presentation layer — shares list state (owncloudApp)

- [ ] 4.1 Add `SharesViewModel` (constructor param `accountName`) exposing a `StateFlow` UI state with `entries`, `refreshing` and `error`. On load and on refresh it: resolves the jail root through `GetFileByRemotePathUseCase(accountName, ROOT_PATH, SPACE_ID_SHARES)`; runs `RefreshSpacesFromServerAsyncUseCase` first if `getWebDavUrlForSpace(SPACE_ID_SHARES)` is still unresolved; refreshes the jail root with `SynchronizeFolderUseCase(ROOT_PATH, accountName, SPACE_ID_SHARES, REFRESH_FOLDER)`; calls `GetSharedWithMeAsyncUseCase`; and joins each entry to an `OCFileWithSyncInfo` on `remoteId == entry.id`, falling back to `fileName == entry.name`. It then collects `GetFolderContentAsStreamUseCase(jailRoot.id)` and re-joins on every emission so available-offline and transfer state stay live. On a Graph failure it sets `error` and leaves `entries` untouched. Verify by launching the `Shares` tab and observing one `sharedWithMe` request per open and per pull-to-refresh in the app log.
- [ ] 4.2 Register `SharesViewModel` in `ViewModelModule` with an `accountName` parameter, following the `SpacesListViewModel` registration. Verify the `Shares` tab opens without a Koin resolution error.

## 5. Presentation layer — shares list UI (owncloudApp)

- [ ] 5.1 Add the sync badge vector drawable and its content-description string. Do not reuse `ic_available_offline` or `sync_pin` — both already mean the app's own available-offline state. Verify the drawable renders at 15dp tinted with `@color/list_item_lastmod_and_filesize_text` and the string is declared only in the default `strings.xml` (no translations — Transifex owns those).
- [ ] 5.2 Add `item_shared_with_me.xml`, cloned from `item_file_list.xml`, with a third 15dp `ImageView` for the sync badge inside `share_icons_layout` (2dp spacing, same tint, anchored to the start of `file_list_size`) and the sharer rendered in the `space_path_line` row. Verify in the layout preview that the badge sits next to the sharing badge and to the left of the size, and that the file ends with a trailing newline.
- [ ] 5.3 Add `SharedWithMeAdapter` over a `SharedWithMeListItem` (entry plus optional `OCFileWithSyncInfo`), rendering name, size, sharer, the sharing badge, the sync badge when `isSynchronized` is `true`, the local-state pin, a thumbnail from `MimetypeIconUtil`/`ThumbnailsCacheManager` (the folder icon when `mimeType` is null) and a three-dot affordance. The sync badge must be non-clickable and must not be derived from available-offline state. Verify an entry with `isSynchronized = false` shows no badge and that tapping the badge area does not consume the row click.
- [ ] 5.4 Rework `shares_fragment.xml` into a `SwipeRefreshLayout` wrapping a `RecyclerView`, keeping the existing `item_empty_dataset` include. Verify the empty include still centres and the file ends with a trailing newline.
- [ ] 5.5 Turn `SharesFragment` into the real list: collect the ViewModel state, feed the adapter, drive swipe-to-refresh, show the `No shares` / `shares_list_empty_subtitle` empty view only when a successful response yielded no visible entry, and surface errors in a snackbar without clearing the list. Verify by opening the tab on an account with shares, with none, and with the network off after a successful load.
- [ ] 5.6 Wire per-item actions: the three-dot opens a bottom sheet built from `file_options_bottom_sheet_fragment.xml` whose option set comes from `FilterFileMenuOptionsUseCase` with `onlySharedByLinkFiles = true`; available-offline goes through `FileOperationsViewModel.performOperation(SetFilesAsAvailableOffline / UnsetFilesAsAvailableOffline)` (plus `SynchronizeFolderOperation` for a folder, as `MainFileListFragment.onSingleFileActionChosen` does); `Share`, `Open with`, `Send` and `Details` delegate to `MainFileListFragment.FileActions`. Verify a file and a folder can both be set and unset as available offline from the list and that `Share` opens the share panel.
- [ ] 5.7 Wire row taps: a file goes to `FileActions.onFileClicked`; a folder asks `FileDisplayActivity` to mount `MainFileListFragment` rooted at that folder with `FileListOption.SHARED_BY_LINK`. An entry with no matching `OCFile` yet must not crash — it reports that the item is not available and triggers a refresh. Verify by tapping a shared folder, a shared file, and by forcing an unmatched entry.

## 6. Presentation layer — routing and share panel (owncloudApp)

- [ ] 6.1 Replace the `getRootSharesFolder()` fork in `navigateTo(FileListOption.SHARED_BY_LINK)` (`FileDisplayActivity.kt:1833-1848`): on a spaces-enabled account always `initAndShowListOfShares()`; otherwise keep the existing legacy branch untouched. Give `initAndShowListOfShares()` a `TAG_LIST_OF_SHARES` fragment tag and set `fileActions = this@FileDisplayActivity`. Verify the tab shows the Graph list on a spaces account that has never cached the jail root, and the unchanged list on a legacy account.
- [ ] 6.2 Make back navigation from a folder opened out of the shares list return to `SharesFragment` instead of leaving the tab, and keep the toolbar title at `bottom_nav_shares` while the list is shown. Verify by browsing into a shared folder and pressing back.
- [ ] 6.3 Hide `members_section` and `public_links_section` in `GraphShareFragment` when `file.spaceId == SPACE_ID_SHARES || file.isSharedWithMe`, leaving the header permalink untouched. Verify the share panel for a shared-with-me item shows only the permalink, and the panel for an own item still shows both sections.

## 7. Documents Provider — keep the `Shares` folder in agreement (owncloudApp)

- [ ] 7.1 In `DocumentsStorageProvider.queryChildDocuments()`, when the requested folder is the Shares jail root (`spaceId == SPACE_ID_SHARES` and `remotePath == ROOT_PATH`), filter the children to those matching an entry returned by `GetSharedWithMeAsyncUseCase`, using the same two rules as the in-app join in 4.1 — `remoteId == entry.id`, falling back to `fileName == entry.name` — so the two surfaces cannot diverge. On failure list the unfiltered children rather than failing the query. Verify from a file-picker that a share hidden via `@UI.Hidden` does not appear while the others do, that the visible children are the same set the `Shares` tab shows, and that the folder still opens with the network off.
- [ ] 7.2 Confirm no regression on the `Shares` row itself: it still appears at the account root and opens correctly with and without a prior visit to the `Shares` tab, and open/download/upload inside it plus the existing create/rename/delete gating in `FileCursor.addFile()` are unchanged. Verify by browsing the account root from a third-party app on a freshly added account.

## 8. Unit tests (owncloudData)

- [ ] 8.1 Add `OCShareWithMe` and `sharedWithMe` response factories to `owncloudTestUtil`, covering a visible entry, a hidden entry, a synchronized entry and a folder entry. Verify they compile and are consumed by 8.2 and 8.3.
- [ ] 8.2 Add `getSharedWithMe` cases to `OCRemoteShareDataSourceTest` following the existing `getGraphShares` cases: success maps the payload (including sharer, hidden and synchronized flags, and the absent-flag defaults), and a failing remote operation propagates its exception. Verify with `./gradlew :owncloudData:testDebugUnitTest`.
- [ ] 8.3 Add `getSharedWithMe` cases to `OCShareRepositoryTest`: the repository returns what the remote data source returns, and propagates its exception. Verify with `./gradlew :owncloudData:testDebugUnitTest`.

## 9. Changelog

- [ ] 9.1 Add `changelog/unreleased/XXXX` (placeholder until the PR number is known) with type `Enhancement`, a title under 80 chars, a description in present perfect passive tense, the issue link `https://github.com/owncloud/android/issues/5020` and the PR link. Verify the file matches the format of the existing entries in `changelog/unreleased/`.

## 10. Verification

- [ ] 10.1 Run `./gradlew detekt` and confirm zero issues (`maxIssues: 0`).
- [ ] 10.2 Run `./gradlew ktlintCheck` and confirm it passes; use `./gradlew ktlintFormat` to fix formatting if needed.
- [ ] 10.3 Run `./gradlew lintDebug` and confirm no new lint warnings were introduced.

## QA

### Test plan

| Test name | Steps to execute | Expected result | Result | Comments |
|---|---|---|---|---|
| Shares tab lists incoming shares | 1. Log in to a spaces-enabled oCIS account that has resources shared with it. 2. Tap the `Shares` tab. | The resources shared with the user are listed, each showing its name, size and the display name of the user who shared it. | | |
| Fresh account with no prior jail cache | 1. Add a spaces-enabled account. 2. Without opening any other tab, tap `Shares`. | The list is populated from the server; the `No shares` placeholder is not shown. | | |
| Hidden share excluded | 1. In the web client, hide one of the shares received by the test user. 2. Open the `Shares` tab in the app and pull to refresh. | The hidden share is not listed; the remaining shares still are. | | |
| No role information on entries | 1. Receive shares with different roles (viewer, editor). 2. Open the `Shares` tab. | No entry shows any role or permission text. | | |
| Sync badge shown for synchronized shares | 1. In the web client, enable sync on one received share and leave another without it. 2. Open the `Shares` tab. | The synchronized share shows the sync badge next to the sharing badge and to the left of its size; the other share shows no sync badge. | | |
| Sync badge is not a control | 1. Open the `Shares` tab on an entry showing the sync badge. 2. Tap directly on the badge. | Nothing happens beyond the normal row behaviour; the item's available-offline state is unchanged. | | |
| Sync badge announced by TalkBack | 1. Enable TalkBack. 2. Open the `Shares` tab and move focus onto an entry showing the sync badge. | The badge is announced through its content description. | | |
| Sync badge is independent of available offline | 1. Set a non-synchronized share as available offline. 2. Look at its entry. | The available-offline indicator is shown and no sync badge appears. | | |
| Browse into a shared folder | 1. Open the `Shares` tab. 2. Tap a shared folder. | The folder's contents are listed, as before this change. | | |
| Back from a shared folder | 1. Browse into a shared folder from the `Shares` tab. 2. Press back. | The `Shared with me` list is shown again, still on the `Shares` tab, with the toolbar titled `Shares`. | | |
| Download a shared file | 1. Open the `Shares` tab. 2. Tap a shared file. | The file is downloaded and previewed as it is elsewhere in the app. | | |
| Set a shared file as available offline | 1. Open the `Shares` tab. 2. Open a shared file's three-dot menu. 3. Choose `Set as available offline`. | The file is downloaded, kept in sync and shows the available-offline indicator. | | |
| Set a shared folder as available offline | 1. Open the `Shares` tab. 2. Open a shared folder's three-dot menu. 3. Choose `Set as available offline`. | The folder is marked available offline and its contents are synchronized. | | |
| Share panel offers only the permalink | 1. Open the `Shares` tab. 2. Open an entry's three-dot menu and choose `Share`. | The panel shows the permalink button in the header; the `Share with people` and `Public links` sections are absent. | | |
| Share panel unchanged for own items | 1. Open the personal space. 2. Open the share panel for an own file. | Both the `Share with people` and `Public links` sections are shown as before. | | |
| Empty state only when the server says so | 1. Log in with an account that has no shares received. 2. Open the `Shares` tab. | The `No shares` / `You are not collaborating on other people's resources.` view is shown. | | |
| Pull to refresh re-queries the endpoint | 1. Open the `Shares` tab. 2. From the web client, share a new resource with the test user. 3. Pull down on the list. | The new share appears in the list. | | |
| Error keeps the previous list | 1. Open the `Shares` tab and let it load. 2. Enable airplane mode. 3. Pull down to refresh. | An error message is shown and the previously loaded entries remain on screen; the empty-dataset view is not shown. | | |
| Documents Provider folder opens without a prior tab visit | 1. Add a spaces-enabled account. 2. From a third-party app, open the Android file picker and browse the account root. 3. Open `Shares`. | The `Shares` folder is listed and opens, showing the shares received by the user. | | |
| Documents Provider hides hidden shares | 1. Hide one received share in the web client. 2. From a third-party app, open the account's `Shares` folder in the picker. | The hidden share is not listed; the others are. | | |
| Documents Provider operations unchanged | 1. From a third-party app, open a file inside the `Shares` folder, then download one and upload into a writable folder. 2. Attempt to create, rename or delete inside `Shares`. | Open, download and upload behave as before; create, rename and delete remain unavailable inside `Shares`, exactly as before this change. | | |
| Legacy account untouched | 1. Log in to an oC10 (non-spaces) account. 2. Open the `Shares` tab and, separately, browse the account through a file picker. | The tab shows the same shared-by-link list as before and no `sharedWithMe` request is made; the picker exposes the same folders as before. | | |
| Other tabs unaffected | 1. Open `All files`, `Spaces` and `Available offline`. | All three behave exactly as before, with unchanged badges, menus and toolbar titles. | | |

### Gherkin scenarios (E2E suite proposal)

Feature: Shared with me list sourced from the Graph sharedWithMe endpoint

#### Scenario 1: User sees who shared each resource in the Shares tab

Covers the core promise of the feature: the `Shares` tab is populated from the server's own list of incoming shares and carries the sharing context the old WebDAV listing could not. The distinguishing precondition is that the account is spaces-enabled and has never cached the Shares jail root, which is exactly the case that previously showed a static empty placeholder. The user is expected to see the shared resource listed together with the name of the person who shared it.

```gherkin
Scenario: User sees who shared each resource in the Shares tab
  Given user "Mark" has shared the file "600_0985.JPG" with user "Alice"
  And user "Alice" has logged in to the Android client on a spaces-enabled account
  And user "Alice" has not opened the "Shares" tab before
  When user "Alice" taps the "Shares" tab
  Then the resource "600_0985.JPG" should be listed in the "Shared with me" list
  And the entry "600_0985.JPG" should display the sharer "Mark"
  And the entry "600_0985.JPG" should display its size
  And the entry "600_0985.JPG" should display no role information
```

#### Scenario 2: Shared resource keeps its operations and offers only the permalink

Verifies that moving the list onto the Graph endpoint did not cost the user any of the operations the old listing supported, and that the share panel is trimmed to what actually applies to a resource shared with them. The distinguishing precondition is that the entry is a folder, so browsing, available-offline and the share panel are all exercised on the same item. The folder must browse and sync normally while its share panel exposes the permalink alone.

```gherkin
Scenario: Shared resource keeps its operations and offers only the permalink
  Given user "Mark" has shared the folder "Images" with user "Alice"
  And user "Alice" has logged in to the Android client on a spaces-enabled account
  When user "Alice" taps the "Shares" tab
  And user "Alice" taps the folder "Images"
  Then the contents of the folder "Images" should be listed
  When user "Alice" navigates back
  Then the resource "Images" should be listed in the "Shared with me" list
  When user "Alice" sets the resource "Images" as available offline
  Then the resource "Images" should display the available offline indicator
  When user "Alice" opens the sharing panel for the resource "Images"
  Then the permalink button should be displayed
  And the "Share with people" section should not be displayed
  And the "Public links" section should not be displayed
```
