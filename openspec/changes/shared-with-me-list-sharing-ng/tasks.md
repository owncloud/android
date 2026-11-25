## 1. Networking layer — `sharedWithMe` endpoint (owncloudComLibrary)

- [x] 1.1 Add Moshi response models for the `sharedWithMe` payload under `lib/resources/shares/responses/`, carrying only the eight fields the list consumes: a wrapper holding `value`, and an entry with `id`, `remoteItem` (for its `id` alone, placed as the entry's second property), `name`, `size`, `file` (for `mimeType`), `createdBy` (for `user.displayName`), plus `isHidden` annotated `@Json(name = "@UI.Hidden")` and `isSynchronized` annotated `@Json(name = "@client.synchronize")` — the two boolean properties carry the domain model's names, not the raw JSON spellings; `OCSharedWithMe` (task 3.1) follows this same property order. Leave `eTag`, `lastModifiedDateTime`, `parentReference`, `folder`, `remoteItem.spaceId`, `remoteItem.parentReference` and the `permissions` array unparsed. Reuse only `UserResponse` and `FileResponse` from `lib/resources/spaces/responses/SpacesResponse.kt` and do **not** modify `MemberPermissionResponse` or anything else in that file. Verify with `./gradlew :owncloudComLibrary:assembleDebug` that the generated Moshi adapters compile, and that parsing the sample payload in issue #5020 populates every modelled field, silently ignores the rest, and matches each nullable/non-null choice.
- [x] 1.2 Add `GetRemoteSharedWithMeOperation` under `lib/resources/shares/`, targeting `graph/v1beta1/me/drive/sharedWithMe` with no query parameters and following `GetRemoteGraphSharesForFileOperation` (`GetMethod`, Moshi adapter, `RemoteOperationResult`, Timber logging on both branches). Verify `./gradlew :owncloudComLibrary:assembleDebug` passes and the endpoint path constant reads `graph/v1beta1/me/drive/sharedWithMe`.
- [x] 1.3 Declare `getSharedWithMe(): RemoteOperationResult<SharedWithMeResponse>` on `ShareService` next to `getGraphShares` and implement it in `OCShareService` by executing the new operation. Verify `./gradlew :owncloudComLibrary:assembleDebug` passes.

## 2. Data layer — remote data source, mapper and repository (owncloudData)

*The domain model and repository contract these tasks implement are added in group 3; create `OCSharedWithMe` (task 3.1) alongside 2.1 if you compile group by group.*

- [x] 2.1 Add `getSharedWithMe(accountName: String): List<OCSharedWithMe>` to `RemoteShareDataSource` and implement it in `OCRemoteShareDataSource` via `executeRemoteOperation { clientManager.getShareService(accountName).getSharedWithMe() }`, with a response→model mapper (not `private`, matching `PermissionsResponse.toModel()` in the same companion object — task 8.2 imports it directly to build its expected value, following the existing `getGraphShares` test's use of `OCRemoteShareDataSource.Companion.toModel`) that reads `id`, `name`, `size` and `mimeType` (from `file?.mimeType`, null for a folder) off the entry itself, `remoteItemId` from `remoteItem.id`, `sharerDisplayName` from `createdBy.user.displayName`, `isHidden` from `@UI.Hidden` (absent → `false`) and `isSynchronized` from `@client.synchronize` (absent → `false`). Verify with the unit test added in 8.2.
- [x] 2.2 Add `getSharedWithMe(accountName: String): List<OCSharedWithMe>` to `ShareRepository` and implement it in `OCShareRepository` as a straight delegation to the remote data source, following `getGraphShares`. Verify with the unit test added in 8.3.

## 3. Domain layer — model, contract, use case and row aggregate (owncloudDomain)

- [x] 3.1 Add `OCSharedWithMe` under `domain/sharing/shares/model/` with exactly eight fields, in this order — matching the response's property order (task 1.1): `id` (the entry's own id in the Shares jail, used as the match key), `remoteItemId` (the real resource id), `name`, `size`, `mimeType` (null for a folder), `sharerDisplayName`, `isHidden` and `isSynchronized`. No folder flag and no space id — see design.md D6. Verify `./gradlew :owncloudDomain:assembleDebug` passes and the model carries no Android framework types.
- [x] 3.2 Add `GetSharedWithMeAsyncUseCase` extending `BaseUseCaseWithResult<List<OCSharedWithMe>, Params>` with an `accountName` param, returning only entries whose `isHidden` is `false` so the in-app list and the Documents Provider cannot diverge. Verify `./gradlew :owncloudDomain:assembleDebug` passes and the filter is applied in the use case, not in a caller.
- [x] 3.3 Register `GetSharedWithMeAsyncUseCase` with `factoryOf(::GetSharedWithMeAsyncUseCase)` in `UseCaseModule`. Verify the app starts without a Koin `NoBeanDefFoundException` once a consumer injects it.
- [x] 3.4 Add `val sharedWithMe: OCSharedWithMe? = null` as the last property of `OCFileWithSyncInfo` (design.md D5). It must stay optional and defaulted so no existing construction site changes, and it must not be read from or written to Room — nothing in `owncloudData`'s mappers may touch it. Verify `./gradlew assembleDebug` compiles with no change to any existing `OCFileWithSyncInfo` construction, and that `./gradlew :owncloudData:testDebugUnitTest` still passes.

## 4. Presentation layer — the filtered and enriched flow (owncloudApp)

- [x] 4.1 In `MainFileListViewModel`, inject `GetSharedWithMeAsyncUseCase`, add a `private val sharedWithMeEntries: MutableStateFlow<List<OCSharedWithMe>?>` initialised to `null` plus a public `refreshSharedWithMe()` that populates it, and add a branch to `retrieveFlowForShareByLink()` for `remotePath == ROOT_PATH && spaceId == SPACE_ID_SHARES` that combines `retrieveFlowForAllFiles(currentFolderDisplayed, accountName)` with `sharedWithMeEntries`, keeping only the children matching a visible entry and copying that entry onto the row's `sharedWithMe` field. Resolve the matching entry **once** and only then act on it, rather than repeating the action in each branch: `val entry = entries.firstOrNull { it.id == file.remoteId } ?: entries.firstOrNull { it.name == file.fileName }`, then enrich the row when `entry` is non-null and drop it otherwise. Task 7.1 must resolve the entry with this identical two-step shape and only then keep the child. The flow must be nullable, because it carries three distinct states and a non-null `List` collapses two of them: `null` means the shares are **unavailable** (never requested, or the request failed) and the jail content passes through unfiltered and un-enriched (design.md D2), whereas an empty list means the server genuinely reported no visible share and the list must come out empty so the `No shares` view is correct. Accordingly `refreshSharedWithMe()` stores `result.getDataOrNull()`, leaving the flow `null` on failure. Do **not** add an error state to `FileListUiState`. The existing `spaceId == null` branch must be left exactly as it is. Because `MainFileListViewModel` is constructed with positional `get()` calls at `ViewModelModule.kt:103`, add the matching `get()` at the right position. Do **not** call `refreshSharedWithMe()` from `init`: this ViewModel is shared by all five file lists and by `FolderPickerActivity`, and it is not recreated when the tab changes (`updateFileListOption()` only updates a flow), so an `init` call would both request on legacy accounts — which `Requirement: Legacy accounts are unaffected` forbids — and never retry on re-entering the tab. Task 6.1 owns the trigger. Verify the `Shares` tab lists only visible shares and that `All files` / `Available offline` / spaces are unchanged.
- [x] 4.2 Call `refreshSharedWithMe()` from the existing pull-to-refresh listener in `MainFileListFragment` (`setOnRefreshListener`, `:436`) so a refresh re-queries Graph as well as the jail. The call must be guarded by the full list condition from design.md D5 — `fileListOption == SHARED_BY_LINK && remotePath == ROOT_PATH && spaceId == SPACE_ID_SHARES` — because this listener is shared by all five lists and by `FolderPickerActivity`: without the option and space tests the endpoint would be hit on other lists and on legacy accounts, which `Requirement: Legacy accounts are unaffected` forbids, and without `remotePath == ROOT_PATH` pulling down inside a shared subfolder would request shares that nobody reads, since jail children inherit `SPACE_ID_SHARES`. The folder refresh itself must keep running unguarded for every list. Verify that sharing a new resource from the web client and pulling down makes it appear, that hiding one makes it disappear, and that pulling down inside a shared subfolder logs no `sharedWithMe` request.

## 5. Presentation layer — rendering the sharer and the sync badge (owncloudApp)

- [ ] 5.1 Add the sync badge vector drawable and its content-description string. Do not reuse `ic_available_offline` or `sync_pin` — both already mean the app's own available-offline state. Verify the drawable renders at 15dp tinted with `@color/list_item_lastmod_and_filesize_text` and the string is declared only in the default `strings.xml` (no translations — Transifex owns those).
- [ ] 5.2 Add a third 15dp `ImageView` with id `sync_badge_icon` to the `share_icons_layout` row of **both** `item_file_list.xml` (`:69`) and `grid_item.xml` (`:45`), using the same id in both so the generic bind in task 5.3 covers list and grid alike — 2dp spacing, same tint, and in the list layout anchored so the row still ends at the start of `file_list_size`. Verify in the layout preview that the badge sits next to the sharing badge and to the left of the size in list mode, that grid is not visually broken, and that both files keep a trailing newline.
- [ ] 5.3 In `FileListAdapter.onBindViewHolder()` (`:200-205`), make `share_icons_layout` visible when the row has a `sharedWithMe` context in addition to its current conditions, and make `sync_badge_icon` visible only when `sharedWithMe?.isSynchronized == true`. The badge must get no click listener and must not be derived from available-offline state. Verify that a synchronized share shows the badge in both list and grid mode, a non-synchronized one shows none, tapping the badge only triggers the normal row click, and that `All files`, `Available offline` and a space show unchanged badge rows.
- [ ] 5.4 In the `LIST_ITEM` branch of `FileListAdapter.setSpecificViewHolder()` (`:293`), extend the `spacePathLine` condition with the shares case and render `sharedWithMe.sharerDisplayName` there instead of the parent path. Verify a shared-with-me row shows the sharer, that the available-offline and legacy shared-by-link lists still show the parent path and space exactly as before, and that rows in `All files` still hide that line.

## 6. Presentation layer — routing, cleanup and share panel (owncloudApp)

- [x] 6.1 Trigger `refreshSharedWithMe()` when the `Shares` tab is opened, so the shares are requested once per tab open and re-requested on every re-entry (task 4.1 deliberately leaves the trigger here). The trigger must fire only for the list condition from design.md D5 — `fileListOption == SHARED_BY_LINK && remotePath == ROOT_PATH && spaceId == SPACE_ID_SHARES` — never when browsing deeper into a shared folder, never on another list and never on a legacy account. `navigateTo(FileListOption.SHARED_BY_LINK)` (`FileDisplayActivity.kt:1833-1848`) keeps its `getRootSharesFolder()` fork, and `initAndShowListOfShares()` (`:431`), its `SharesFragment` import (`:112`), `SharesFragment.kt` and `shares_fragment.xml` are all kept: the static placeholder stays the tab's answer while the Shares space row is still unknown to the client (design.md D4). Note that `mainFileListFragment` resolves through `findFragmentByTag()` while `initAndShowListOfFiles()` commits asynchronously, so a call on that getter right after a first mount is silently dropped; pick a trigger point that covers both the first mount and later re-entries, bearing in mind that the fragment is not recreated when the tab changes. Decide explicitly what the existing `if (previousFileListOption != newFileListOption || initialState)` guard should do when the tab is reselected while already active. Verify the tab requests the shares on the first open and on every re-entry, that no `sharedWithMe` request is logged from any other list or on a legacy account, and that `./gradlew assembleDebug` passes.
- [x] 6.2 Regression-check back navigation and folder taps, which design.md D4 expects to need no new code: back from a folder opened out of the shares list returns to the jail-root list via `onBrowseUp()`, back at the jail root exits the tab exactly as today (`FileDisplayActivity.kt:782-792`), the toolbar keeps reading `bottom_nav_shares`, and tapping a shared folder navigates in place. Verify each by hand; if any of them misbehaves, the fix belongs in this task.
- [ ] 6.3 Hide `members_section` and `public_links_section` in `GraphShareFragment` when `file.spaceId == SPACE_ID_SHARES || file.isSharedWithMe`, leaving the header permalink untouched. Verify the share panel for a shared-with-me item shows only the permalink, and the panel for an own item still shows both sections.

## 7. Documents Provider — keep the `Shares` folder in agreement (owncloudApp)

- [ ] 7.1 In `DocumentsStorageProvider.queryChildDocuments()`, when the requested folder is the Shares jail root (`spaceId == SPACE_ID_SHARES` and `remotePath == ROOT_PATH`), filter the children to those matching an entry returned by `GetSharedWithMeAsyncUseCase`, using the identical two-step shape as the in-app filter in 4.1: resolve the matching entry **once** with `val entry = entries.firstOrNull { it.id == file.remoteId } ?: entries.firstOrNull { it.name == file.fileName }`, then keep the child when `entry` is non-null and drop it otherwise — same two rules, same order, same shape, so the two surfaces cannot drift apart — and, on failure, list the unfiltered children rather than failing the query, which is the same degradation the app applies. Verify from a file-picker that a share hidden via `@UI.Hidden` does not appear while the others do, that the visible children are the same set the `Shares` tab shows, and that the folder still opens with the network off.
- [ ] 7.2 Confirm no regression on the `Shares` row itself: it still appears at the account root and opens correctly with and without a prior visit to the `Shares` tab, and open/download/upload inside it plus the existing create/rename/delete gating in `FileCursor.addFile()` are unchanged. Verify by browsing the account root from a third-party app on a freshly added account.

## 8. Unit tests (owncloudData)

- [ ] 8.1 Add `OCSharedWithMe` and `sharedWithMe` response factories to `owncloudTestUtil`, covering a visible entry, a hidden entry, a synchronized entry and a folder entry. Verify they compile and are consumed by 8.2 and 8.3.
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
| Fresh account before the spaces sync lands | 1. Add a spaces-enabled account. 2. Without opening any other tab, tap `Shares` immediately. | The tab behaves as it does today: the static `No shares` placeholder is shown and no `sharedWithMe` request is made. Leaving the tab and selecting it again, once the spaces are known, lists the shares. | | |
| Hidden share excluded | 1. In the web client, hide one of the shares received by the test user. 2. Open the `Shares` tab in the app and pull to refresh. | The hidden share is not listed; the remaining shares still are. | | |
| No role information on entries | 1. Receive shares with different roles (viewer, editor). 2. Open the `Shares` tab. | No entry shows any role or permission text. | | |
| Sync badge shown for synchronized shares | 1. In the web client, enable sync on one received share and leave another without it. 2. Open the `Shares` tab. | The synchronized share shows the sync badge next to the sharing badge and to the left of its size; the other share shows no sync badge. | | |
| Sync badge is not a control | 1. Open the `Shares` tab on an entry showing the sync badge. 2. Tap directly on the badge. | Nothing happens beyond the normal row behaviour; the item's available-offline state is unchanged. | | |
| Sync badge announced by TalkBack | 1. Enable TalkBack. 2. Open the `Shares` tab and move focus onto an entry showing the sync badge. | The badge is announced through its content description. | | |
| Sync badge is independent of available offline | 1. Set a non-synchronized share as available offline. 2. Look at its entry. | The available-offline indicator is shown and no sync badge appears. | | |
| Browse into a shared folder | 1. Open the `Shares` tab. 2. Tap a shared folder. | The folder's contents are listed, as before this change. | | |
| Back from a shared folder | 1. Browse into a shared folder from the `Shares` tab. 2. Press back. | The filtered `Shared with me` list is shown again, still on the `Shares` tab, with the toolbar titled `Shares`. | | |
| Back at the jail root | 1. Open the `Shares` tab. 2. Press back without entering any folder. | The app behaves exactly as it does today on that tab (it exits), with no detour into personal files. | | |
| Download a shared file | 1. Open the `Shares` tab. 2. Tap a shared file. | The file is downloaded and previewed as it is elsewhere in the app. | | |
| Set a shared file as available offline | 1. Open the `Shares` tab. 2. Open a shared file's three-dot menu. 3. Choose `Set as available offline`. | The file is downloaded, kept in sync and shows the available-offline indicator. | | |
| Set a shared folder as available offline | 1. Open the `Shares` tab. 2. Open a shared folder's three-dot menu. 3. Choose `Set as available offline`. | The folder is marked available offline and its contents are synchronized. | | |
| Share panel offers only the permalink | 1. Open the `Shares` tab. 2. Open an entry's three-dot menu and choose `Share`. | The panel shows the permalink button in the header; the `Share with people` and `Public links` sections are absent. | | |
| Share panel unchanged for own items | 1. Open the personal space. 2. Open the share panel for an own file. | Both the `Share with people` and `Public links` sections are shown as before. | | |
| Empty state only when there is nothing to show | 1. Log in with an account that has no shares received. 2. Open the `Shares` tab. | The `No shares` / `You are not collaborating on other people's resources.` view is shown. | | |
| Pull to refresh re-queries the endpoint | 1. Open the `Shares` tab. 2. From the web client, share a new resource with the test user. 3. Pull down on the list. | The new share appears in the list. | | |
| Graph failure degrades to the unfiltered jail | 1. Open the `Shares` tab and let it load. 2. Enable airplane mode. 3. Pull down to refresh. | The jail contents stay listed without sharer or sync badge; the tab is not blocked by an error and the empty-dataset view is not shown. | | |
| Documents Provider folder opens without a prior tab visit | 1. Add a spaces-enabled account. 2. From a third-party app, open the Android file picker and browse the account root. 3. Open `Shares`. | The `Shares` folder is listed and opens, showing the shares received by the user. | | |
| Documents Provider hides hidden shares | 1. Hide one received share in the web client. 2. From a third-party app, open the account's `Shares` folder in the picker. | The hidden share is not listed; the others are. | | |
| Documents Provider operations unchanged | 1. From a third-party app, open a file inside the `Shares` folder, then download one and upload into a writable folder. 2. Attempt to create, rename or delete inside `Shares`. | Open, download and upload behave as before; create, rename and delete remain unavailable inside `Shares`, exactly as before this change. | | |
| Legacy account untouched | 1. Log in to an oC10 (non-spaces) account. 2. Open the `Shares` tab and, separately, browse the account through a file picker. | The tab shows the same shared-by-link list as before and no `sharedWithMe` request is made; the picker exposes the same folders as before. | | |
| Other file lists unaffected | 1. Open `All files`, `Spaces` and `Available offline`, in list and in grid mode. 2. Check badge rows, the path/space line, sorting, search, selection and the three-dot menus. | All of them behave exactly as before; no row anywhere shows the sync badge and no row gains or loses its path/space line. | | |

### Gherkin scenarios (E2E suite proposal)

Feature: Shared with me list filtered and enriched by the Graph sharedWithMe endpoint

#### Scenario 1: User sees who shared each resource in the Shares tab

Covers the core promise of the feature: the `Shares` tab carries the sharing context the plain WebDAV listing could not. The distinguishing precondition is that the account is spaces-enabled and has never cached the Shares jail root, which is exactly the case that previously showed a static empty placeholder. The user is expected to see the shared resource listed together with the name of the person who shared it.

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

Verifies that filtering the list did not cost the user any of the operations the previous listing supported, and that the share panel is trimmed to what actually applies to a resource shared with them. The distinguishing precondition is that the entry is a folder, so browsing, back navigation, available-offline and the share panel are all exercised on the same item. The folder must browse and sync normally while its share panel exposes the permalink alone.

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
