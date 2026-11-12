## 1. Networking layer — item-aware DELETE path (owncloudComLibrary)

- [x] 1.1 Add a nullable `itemId: String?` parameter to `RemoveRemoteLinkOperation` (between `spaceId` and `linkId`) and branch the path construction: when `itemId` is `null` keep `root/permissions`, otherwise append `items`, the `itemId`, then `permissions`, before appending `linkId`. Mirror the constant naming and null-branching already used by `AddRemoteLinkOperation` in the same package. Verify by inspecting the built URL for both branches — `graph/v1beta1/drives/{spaceId}/items/{itemId}/permissions/{linkId}` and `graph/v1beta1/drives/{spaceId}/root/permissions/{linkId}`.
- [x] 1.2 Add `itemId: String?` to `LinksService.removeLink(spaceId, itemId, linkId)` and pass it through in `OCLinksService.removeLink` when constructing `RemoveRemoteLinkOperation`. Verify `:owncloudComLibrary:compileDebugKotlin` succeeds.

## 2. Data layer — thread itemId through (owncloudData)

- [x] 2.1 Add `itemId: String?` to `RemoteLinksDataSource.removeLink` and to `OCRemoteLinksDataSource.removeLink`, forwarding it to `getLinksService(accountName).removeLink(spaceId, itemId, linkId)`. Verify the override matches the interface and the module compiles.
- [x] 2.2 Add `itemId: String?` to `OCLinksRepository.removeLink` and forward it to `remoteLinksDataSource.removeLink(...)`. Verify `:owncloudData:compileDebugKotlin` succeeds.

## 3. Domain layer — use case and repository contract (owncloudDomain)

- [x] 3.1 Add `itemId: String?` to the `LinksRepository.removeLink` interface signature, keeping parameter order consistent with `addLink`. Verify `:owncloudDomain:compileDebugKotlin` succeeds.
- [x] 3.2 Add `itemId: String?` to `RemoveLinkUseCase.Params` and pass `params.itemId` in `run(...)`. Verify the use case compiles and no Koin module needs editing — `factoryOf(::RemoveLinkUseCase)` binds by constructor reference and the constructor is unchanged.

## 4. Presentation layer — pass itemId from the ViewModel (owncloudApp)

- [x] 4.1 In `PublicLinksViewModel.removePublicLink(linkId)`, pass the ViewModel's existing `itemId` field into `RemoveLinkUseCase.Params`. Verify no signature change is needed on `removePublicLink` itself, since `itemId` is already a constructor property bound to `file?.remoteId`.

## 5. Presentation layer — sharing panel UI wiring (owncloudApp)

- [x] 5.1 Rename the private `canRemoveShares` field in `GraphShareFragment` to `canRemoveSharesAndLinks`, since it will now gate both the member-share and the public-link delete affordances. Three sites change: the field declaration, the assignment in `checkPermissions()`, and the argument passed to `graphSharesAdapter.setShares(...)`. Do **not** rename `GraphSharesAdapter`'s own `canRemoveShares` field or its `setShares(...)` parameter — that adapter renders only member-share rows, so the narrower name stays accurate and the value is passed positionally. Verify `:owncloudApp:compileOriginalDebugKotlin` succeeds and that no `canRemoveShares` reference remains in `GraphShareFragment`.
- [x] 5.2 In `GraphShareFragment.showLinks()`, replace `canRemoveLinks = false` with `canRemoveLinks = canRemoveSharesAndLinks`, leaving `canEditLinks = false` untouched (link editing belongs to #4970). Verify by opening the sharing panel on an item where the user holds `libre.graph/driveItem/permissions/delete` and confirming the trashbin icon appears on each public link entry, and is absent on an item where the user lacks it.
- [x] 5.3 In `strings.xml`, modify `public_link_remove_dialog_title` in place — drop its `%1$s` placeholder, changing its value to the static "Remove public link" — and modify `public_link_remove_dialog_message` in place to gain a `%1$s` placeholder, changing its value to "Do you want to remove the public link: %1$s? Recreating the same link again is not possible." (reuse both existing resource names; do not create any new string). Then, in `values-de/strings.xml` and `values-es/strings.xml`, delete the translated `public_link_remove_dialog_title` entry, and in `values-de/strings.xml`, `values-de-rDE/strings.xml`, and `values-es/strings.xml`, delete the translated `public_link_remove_dialog_message` entry — their English source changed, so the existing translations no longer match and must not be left stale; the app falls back to English until Transifex retranslates. Then implement `GraphShareFragment.onRemovePublicLink(publicLinkId, publicLinkDisplayName)` to show an `AlertDialog` shaped like `onRemoveShare(...)`: `ic_warning` icon, `public_link_remove_dialog_title` as the title (no argument), `public_link_remove_dialog_message` formatted with `publicLinkDisplayName` as the message, `common_yes` calling `publicLinksViewModel.removePublicLink(publicLinkId)`, `common_no` dismissing, and `avoidScreenshotsIfNeeded()` on the shown dialog. This task alone is **not independently shippable**: `SpaceMembersFragment` still calls `getString(R.string.public_link_remove_dialog_title, publicLinkDisplayName)` with an argument the string no longer accepts, and `getString(R.string.public_link_remove_dialog_message)` with no argument when the string now requires one — both throw at runtime (`IllegalFormatConversionException` / `MissingFormatArgumentException`) the moment its dialog is shown. 5.4 must land in the same session before any build leaves this state. Verify tapping the trashbin opens a dialog titled "Remove public link" naming the link in its message, and that cancelling issues no request.
- [x] 5.4 Fix `SpaceMembersFragment.onRemovePublicLink(publicLinkId, publicLinkDisplayName)`, which 5.3 left calling both modified strings the old way. Bring it to the same dialog as 5.3, for parity between the two screens that offer public-link removal: replace its `showAlertDialog(...)` call (the shared `Fragment.showAlertDialog` extension in `FragmentExt.kt`) with a manual `AlertDialog.Builder`, mirroring this same file's own `onRemoveMember(...)`, which already builds its dialog this way: `ic_warning` icon, `public_link_remove_dialog_title` as the title (no argument), `public_link_remove_dialog_message` formatted with `publicLinkDisplayName` as the message, `common_yes` calling `publicLinksViewModel.removePublicLink(publicLinkId)`, `common_no` dismissing, `avoidScreenshotsIfNeeded()`. Remove the now-unused `com.owncloud.android.extensions.showAlertDialog` import from this file. Do **not** modify `FragmentExt.kt` or any of its other call sites (automatic-upload settings, spaces list), and do not touch `strings.xml` again — 5.3 already updated both strings and their translations. Verify `:owncloudApp:compileOriginalDebugKotlin` succeeds and that opening this dialog no longer crashes.
- [x] 5.5 Add a private `observeRemoveLinkResult()` to `GraphShareFragment` collecting `publicLinksViewModel.removeLinkResultFlow` via `collectLatestLifecycleFlow`, taking the bare `UIResult` (the flow is a `MutableSharedFlow` with no `Event` wrapper, like `removeShareResultFlow`): on `Success` show `public_link_remove_correctly` in a snackbar and call `graphShareViewModel.getGraphShares()`; on `Error` show `public_link_remove_failed` via `showErrorInSnackbar` plus a `Timber.e`; `Loading` is a no-op. Verify no new string resources are required for this task — `public_link_remove_correctly` and `public_link_remove_failed` already exist in `strings.xml` (the dialog's own new strings are added in 5.3).
- [x] 5.6 Register `observeRemoveLinkResult()` in `subscribeToViewModels()`. Verify end to end that confirming a deletion removes the link from the `PUBLIC LINKS` list, leaves other links and member shares listed, and that a failed deletion shows the error while the link stays in the list.

## 6. Unit tests (owncloudData)

- [x] 6.1 Update the `removeLink` test in `OCLinksRepositoryTest` for the new `itemId` parameter, and add a case covering a `null` `itemId`, asserting the value is forwarded to `remoteLinksDataSource.removeLink(...)` in both cases. Verify `./gradlew :owncloudData:testDebugUnitTest --tests "*OCLinksRepositoryTest*"` passes.
- [x] 6.2 Update the `removeLink` test in `OCRemoteLinksDataSourceTest` for the new `itemId` parameter, and add a case covering a `null` `itemId`, asserting `ocLinksService.removeLink(spaceId, itemId, linkId)` is invoked with the expected arguments. Verify `./gradlew :owncloudData:testDebugUnitTest --tests "*OCRemoteLinksDataSourceTest*"` passes.

## 7. Changelog

- [x] 7.1 Create `changelog/unreleased/5048` (named after PR #5048) with type `Enhancement`, a title under 80 characters, a description in present perfect passive tense, the issue link `https://github.com/owncloud/android/issues/4973` and the PR link. Verify the file ends with a trailing newline and matches the format of the other files in `changelog/unreleased/`.

## 8. Verification

- [ ] 8.1 Run `./gradlew detekt` and confirm zero issues (`maxIssues: 0`).
- [ ] 8.2 Run `./gradlew ktlintCheck` and confirm it passes; use `./gradlew ktlintFormat` to fix formatting if needed.
- [ ] 8.3 Run `./gradlew lintDebug` and confirm no new lint warnings were introduced.

## QA

### Test plan

| Test name | Steps to execute | Expected result | Result | Comments |
|---|---|---|---|---|
| Trashbin visible with delete permission | 1. Log in to an oCIS account. 2. Open a space where the user holds `libre.graph/driveItem/permissions/delete`. 3. Create or locate a file that already has a public link. 4. Open the sharing panel. | A trashbin icon is shown on each entry in the `PUBLIC LINKS` section. | | |
| Trashbin hidden without delete permission | 1. Log in as a user with only viewer access to a space. 2. Open a file in that space that has a public link. 3. Open the sharing panel. | No trashbin icon is shown on any public link entry; the links are still listed and readable. | | |
| Confirmation dialog names the link | 1. Open the sharing panel on a file with a public link named e.g. "Quarterly report". 2. Tap the trashbin icon on that entry. | A dialog titled "Remove public link" appears, with a message naming "Quarterly report" and warning that the same link cannot be recreated. | | |
| Dialog matches the Spaces member-management screen | 1. Open the sharing panel on a file with a public link named "Quarterly report" and tap its trashbin icon. 2. Separately, open the Spaces member-management screen for a space with a public link of the same name and tap its trashbin icon. | Both dialogs show the exact same title ("Remove public link") and the same message text, differing only in the link name interpolated. | | |
| Cancel leaves the link intact | 1. Tap the trashbin icon on a public link entry. 2. Tap the negative button in the dialog. | The dialog closes, no request is sent, and the link is still listed in the `PUBLIC LINKS` section. | | |
| Delete a link on a file inside a space | 1. Open the sharing panel on a file inside a project space that has a public link. 2. Tap the trashbin icon. 3. Confirm. | The request targets the file's own item path, a success message is shown, and the link disappears from the list. | | |
| Delete a link on a space root | 1. Open the sharing panel on a space root that has a public link. 2. Tap the trashbin icon. 3. Confirm. | The request targets the root permissions path, a success message is shown, and the link disappears from the list. | | |
| Only the targeted link is removed | 1. Create three public links and at least one member share on the same folder. 2. Open the sharing panel. 3. Delete the second link and confirm. | Only the second link disappears; the other two links and the member share remain listed unchanged. | | |
| Last link removed shows empty state | 1. Open the sharing panel on an item with exactly one public link. 2. Delete it and confirm. | The `PUBLIC LINKS` section shows no entries and displays its empty-state message. | | |
| Deletion fails without network | 1. Open the sharing panel on an item with a public link. 2. Enable airplane mode. 3. Tap the trashbin icon and confirm. | An error message stating the public link could not be removed is shown, and the link is still listed. | | |
| Member share removal still works | 1. Open the sharing panel on an item with a member share. 2. Remove the member share and confirm. | The member share is removed as before, with its own confirmation dialog and success message; public links are unaffected. | | |
| Link creation and sharing still work | 1. Open the sharing panel. 2. Create a new public link. 3. Tap the chain icon on an existing link. | Creation succeeds and the list refreshes; the chain icon still opens the Android share sheet with the link URL. | | |

### Gherkin scenarios (E2E suite proposal)

Feature: Remove a public link from the Sharing NG panel

#### Scenario 1: User removes a public link from a file inside a space

Confirms the happy path for the item-scoped deletion, which is the case the sharing panel is actually used for. The distinguishing precondition is that the link belongs to a file inside a space rather than to the space root, so the request must address the item path. The link is expected to vanish from the list right away.

```gherkin
Scenario: User removes a public link from a file inside a space
  Given user "Alice" has logged in to the Android client
  And user "Alice" has a file "report.pdf" inside the project space "Marketing"
  And user "Alice" has created a public link named "Quarterly report" for "report.pdf"
  And user "Alice" holds the "libre.graph/driveItem/permissions/delete" permission on "report.pdf"
  When user "Alice" opens the sharing panel for "report.pdf"
  Then the public link "Quarterly report" should be listed in the "PUBLIC LINKS" section
  And a trashbin icon should be displayed on the public link "Quarterly report"
  When user "Alice" taps the trashbin icon on the public link "Quarterly report"
  Then a confirmation dialog mentioning "Quarterly report" should be displayed
  When user "Alice" confirms the removal
  Then user "Alice" should see the message "Public link removed correctly"
  And the public link "Quarterly report" should not be listed in the "PUBLIC LINKS" section
```

#### Scenario 2: User cancels the removal of a public link

Guards the confirmation step so an accidental tap cannot destroy a link that cannot be recreated. What sets this apart is that the user reaches the dialog and then backs out, so no request may reach the server at all. The link must survive completely untouched.

```gherkin
Scenario: User cancels the removal of a public link
  Given user "Alice" has logged in to the Android client
  And user "Alice" has a file "report.pdf" inside the project space "Marketing"
  And user "Alice" has created a public link named "Quarterly report" for "report.pdf"
  And user "Alice" holds the "libre.graph/driveItem/permissions/delete" permission on "report.pdf"
  When user "Alice" opens the sharing panel for "report.pdf"
  And user "Alice" taps the trashbin icon on the public link "Quarterly report"
  Then a confirmation dialog mentioning "Quarterly report" should be displayed
  When user "Alice" dismisses the confirmation dialog
  Then the public link "Quarterly report" should still be listed in the "PUBLIC LINKS" section
```
