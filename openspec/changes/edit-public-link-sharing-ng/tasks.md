## 1. ComLibrary — item-targeted edit operations (owncloudComLibrary)

- [x] 1.1 Parametrize `EditRemoteLinkOperation` with an optional `itemId: String?` (no default): when non-null build the PATCH path as `graph/v1beta1/drives/{spaceId}/items/{itemId}/permissions/{linkId}`, when null keep `drives/{spaceId}/root/permissions/{linkId}`. Keep the request body and the `Unit` return exactly as in the space-link edit. Verify both paths compile.
- [x] 1.2 Parametrize `EditRemotePasswordLinkOperation` with an optional `itemId: String?` (no default): when non-null build the POST path as `graph/v1beta1/drives/{spaceId}/items/{itemId}/permissions/{linkId}/setPassword`, when null keep `drives/{spaceId}/root/permissions/{linkId}/setPassword`. Keep the `PASSWORD_ENFORCED`/`Unit` behavior. Verify both paths compile.
- [x] 1.3 Add the optional `itemId: String?` (no default) to `LinksService.editLink` / `editPasswordLink` and forward it in `OCLinksService` to the two operations; keep `editLink`'s existing attribute params unchanged. Verify the null-itemId (space) path still resolves to `root/permissions/{linkId}`.

## 2. Data — thread `itemId` through datasource and repository (owncloudData)

- [x] 2.1 Add the optional `itemId: String?` (no default) to `RemoteLinksDataSource.editLink` / `editPasswordLink` and to `OCRemoteLinksDataSource`'s implementations, delegating to `ClientManager.getLinksService(...).editLink/editPasswordLink(...)`; keep `editLink`'s existing attribute params unchanged. Verify item-targeted and space paths compile and delegate correctly.
- [x] 2.2 Add the optional `itemId: String?` (no default) to `LinksRepository.editLink` / `editPasswordLink` (interface) and `OCLinksRepository` (impl), forwarding to the datasource; keep the existing attribute params unchanged. Verify the interface/impl compile and delegate for both paths.

## 3. Domain — extend edit use cases (owncloudDomain)

- [x] 3.1 Extend `EditLinkUseCase.Params` with the optional `itemId: String?` (no default), keeping the existing attribute params (`displayName`, `type`, `expirationDate`) unchanged; forward all to `LinksRepository.editLink`. Keep the `Unit` result.
- [x] 3.2 Extend `EditPasswordLinkUseCase.Params` with the optional `itemId: String?` (no default); forward it to `LinksRepository.editPasswordLink`. Keep the `Unit` result.

## 4. Presentation — compute changed fields and pass `itemId` (owncloudApp)

- [x] 4.1 In `PublicLinksViewModel.editLink`, pass the constructor `itemId` into `EditLinkUseCase.Params` alongside the existing attribute values (`type`, `displayName`, `expirationDate`), keeping the same logic as the space-link edit. Pass `itemId` into `EditPasswordLinkUseCase.Params` in `editPasswordPublicLink`. Verify the PATCH targets `items/{itemId}/permissions/{linkId}` with the link's attributes.
- [x] 4.2 Confirm `PublicLinksViewModel.resetViewModel()` clears `editLinkResultFlow`, `editPasswordLinkResultFlow`, and recreates `AddPublicLinkUIState()` (resetting `wasPasswordChanged`/`selectedPermission`) so reopening the edit view for another link inherits no stale selection. Add any missing reset. Verify reopening the edit view starts clean.

## 5. Presentation — enable and launch the edit flow from the panel (owncloudApp)

- [x] 5.1 In `GraphShareFragment.showLinks()`, pass `canEditLinks = canEditSharesAndLinks` (the `DRIVES_UPDATE_PERMISSION` flag, renamed from `canEditShares`, already computed in `checkPermissions`) into `graphLinksAdapter.setPublicLinks(...)` instead of the hardcoded `false`, so the pencil shows only with update permission and hides on a refresh that revokes it. Verify the pencil appears/hides with the permission.
- [x] 5.2 Parametrize `GraphShareFragmentListener.addPublicLink` with `editMode: Boolean` and `selectedPublicLink: OCLink?` (mirroring `addGraphShare`); update the existing `addPublicLinkButton` handler to pass `editMode = false, selectedPublicLink = null`, and implement `GraphShareFragment.onEditPublicLink(publicLink)` to call `publicLinksViewModel.resetViewModel()` then `listener?.addPublicLink(file, accountName, editMode = true, selectedPublicLink = publicLink)`. Verify tapping the pencil opens the edit view.
- [x] 5.3 Update `ShareActivity.addPublicLink(...)` to accept `editMode: Boolean` and `selectedPublicLink: OCLink?` and forward them into `AddPublicLinkFragment.newInstance(accountName, spaceId = file.spaceId, editMode, selectedPublicLink, file = file)` (replacing the hardcoded `false`/`null`), keeping the `R.id.members_fragment_container` back-stack launch. Verify the edit view opens pre-filled with the link's current values and the submit button reads `Save`.
- [ ] 5.4 In `GraphShareFragment`, observe `publicLinksViewModel.editLinkResultFlow` and `editPasswordLinkResultFlow`: on `Success` run `graphShareViewModel.getGraphShares()`, show `R.string.public_link_edit_correctly` via `showMessageInSnackbar`, then `publicLinksViewModel.resetViewModel()`; keep the `Error` branch a no-op (the `AddPublicLinkFragment` observers already show `R.string.public_link_edit_failed` and keep the view open). Verify the list refreshes with updated values and a success message on success, and the view stays open with an error on failure.

## 6. Unit Tests (owncloudData)

- [ ] 6.1 In `OCRemoteLinksDataSourceTest`, extend the `editLink` and `editPasswordLink` tests to cover the item path (non-null `itemId` = `OC_FILE.remoteId`) and keep the space path (null `itemId`), asserting delegation to `getLinksService(...).editLink/editPasswordLink(...)` with the right arguments. Use `createRemoteOperationResultMock`.
- [ ] 6.2 In `OCLinksRepositoryTest`, extend the `editLink` and `editPasswordLink` tests to assert delegation to the datasource for the item path (non-null `itemId`) and the space path (null `itemId`).

## 7. Changelog

- [ ] 7.1 Add `changelog/unreleased/XXXX` (placeholder number until the PR is opened) as an `Enhancement` following the project changelog format, with a title under 80 chars, a present-perfect-passive description, and links to issue #4970 and the PR. Verify the file matches the required format and ends with a trailing newline.

## 8. Verification

- [ ] 8.1 Run `./gradlew detekt` and `./gradlew ktlintCheck` (and `lintDebug` if applicable); fix any issues. Verify all pass (detekt `maxIssues: 0`).

## QA

### Test plan

| Test name | Steps to execute | Expected result | Result | Comments |
|-----------|------------------|-----------------|--------|----------|
| **Affordance visible** | | | | |
| Pencil visible — file | As a user with `libre.graph/driveItem/permissions/update`, open the sharing panel for a file that has a public link. Check both orientations | A pencil icon appears on each public link entry | | |
| Pencil visible — folder | As a user with `libre.graph/driveItem/permissions/update`, open the sharing panel for a folder that has a public link | A pencil icon appears on each public link entry | | |
| Pencil hidden — no permission | As a user without `libre.graph/driveItem/permissions/update`, open the sharing panel for an item with a public link | No pencil icon on any public link entry | | |
| Pencil hidden after live downgrade | 1. As a space manager, open Share (pencil visible)<br>2. Another admin removes your update permission<br>3. Pull to refresh | The pencil is hidden after the refresh, without leaving the panel | | |
| **Open edit view pre-filled** | | | | |
| Pre-filled fields | Tap the pencil on a link with a name, permission, password, and expiration date | Edit view opens with the name, permission radio, and expiration date set to the link's current values; password and expiration fields visible immediately; submit button reads `Save` | | |
| Password not shown in clear | Tap the pencil on a link that has a password | The password is not displayed in clear text; the field reflects "has password" | | |
| Permission options — file | Tap the pencil on a file link and open the permission selector | Only `Can view` and `Can edit` offered (no `Secret file drop`) | | |
| Permission options — folder | Tap the pencil on a folder link and open the permission selector | `Can view`, `Can edit`, and `Secret file drop` offered | | |
| Reopen is clean | 1. Edit link A, change permission, back out without saving<br>2. Tap the pencil on link B | Link B's edit view shows link B's values; no state leaked from A | | |
| **Edit submission** | | | | |
| Change name only | 1. Tap pencil<br>2. Change only the name<br>3. `Save` | PATCH sends the link's attributes; list shows the new name. Check in web | | |
| Change permission only | 1. Tap pencil<br>2. Change only the permission<br>3. `Save` | PATCH sends the link's attributes; list shows the new permission. Check in web | | |
| Set expiration date | 1. Tap pencil on a link with no expiration<br>2. Set a future date<br>3. `Save` | PATCH sends `expirationDateTime` as ISO 8601; list shows the expiration. Check in web | | |
| Change expiration date | 1. Tap pencil on a link with an expiration<br>2. Pick a different date<br>3. `Save` | PATCH sends the new `expirationDateTime`; list shows the updated date. Check in web | | |
| Remove expiration date | 1. Tap pencil on a link with an expiration<br>2. Switch off the expiration<br>3. `Save` | Expiration cleared server-side (`expirationDateTime` sent as null); list shows no expiration. Check in web | | |
| No-op save | 1. Tap pencil<br>2. Change nothing<br>3. `Save` | PATCH sends the link's unchanged attributes; list unchanged | | |
| Change all attributes | 1. Tap pencil<br>2. Change name, permission, and expiration<br>3. `Save` | All attributes sent; list reflects all updates. Check in web | | |
| Long name | Attempt to enter a name exceeding 255 characters | Input is restricted to 255 characters maximum | | |
| **Password** | | | | |
| Change password | 1. Tap pencil<br>2. Set a new policy-compliant password<br>3. `Save` | A separate `setPassword` request is sent; link requires the new password. Check in web | | |
| Keep password unchanged | 1. Tap pencil on a link with a password<br>2. Change only the name, leave password untouched<br>3. `Save` | No `setPassword` request is sent; the existing password still works. Check in web | | |
| Password mandatory on switch to edit | In a server with `OCIS_SHARING_PUBLIC_WRITEABLE_SHARE_MUST_HAVE_PASSWORD`: 1. Tap pencil on a password-less `Can view` link<br>2. Switch permission to `Can edit` | `Save` is disabled until a password is entered | | |
| Password mandatory all types | In a server with `OCIS_SHARING_PUBLIC_SHARE_MUST_HAVE_PASSWORD`, tap pencil on a password-less link and select any permission | `Save` disabled until a password is entered | | |
| Password not compliant with policy | 1. In a server with a password policy, tap pencil and select a permission<br>2. Enter a non-compliant password | `Save` disabled | | |
| **After edit** | | | | |
| List refresh | Edit a link successfully | The `PUBLIC LINKS` list refreshes and shows the link's updated values; a success message is shown | | |
| Chain icon still works | After editing, tap the chain icon on the edited link | Android share sheet opens with the link's `webUrl` | | |
| **Errors** | | | | |
| No network | 1. Open the edit view<br>2. Disable network<br>3. `Save` | Error message shown; edit view stays open | | |
| Server down | 1. Open the edit view<br>2. Turn the server off<br>3. `Save` | Error message shown; edit view stays open | | |
| Permission revoked during edit | 1. Open the edit view<br>2. Have update permission removed server-side<br>3. `Save` | Error message shown; edit view stays open for retry | | |
| **Regression** | | | | |
| Space link edit unchanged | Edit a public link from space management as before | Space link still edited via `root/permissions/{linkId}`; behavior unchanged. Check in web | | |
| Create flow unchanged | Create a public link for a file/folder | Creation still works via `items/{itemId}/createLink`; new link appears | | |
| Existing links readable | Open a sharing panel with existing links | Links listed correctly; chain icons share the correct URL | | |
| **Accessibility** | One device | | | |
| Talkback | Enable Talkback and navigate the edit and password views | Every field is correctly labelled and navigable | | |
| Keyboard navigation | Connect a BT keyboard and navigate the edit and password views | Every field selectable; no dead paths | | |

### Gherkin scenarios (E2E suite proposal)

**Scenario: User changes the permission type of an existing link** —
This verifies the edit PATCH targets the item permissions endpoint: when a user
edits the permission level of a link that already has a name and expiration, the
link's attributes are sent to the item permissions endpoint and the permission
is updated on the server after the edit succeeds.

```gherkin
Feature: Edit an existing public link for a file or folder

  Scenario: User changes the permission type of an existing link
    Given a signed-in user with update permission on "report.pdf" that has a "Can view" public link named "Report"
    When the user opens the sharing panel for "report.pdf"
    And taps the pencil icon on the "Report" link
    And changes the permission to "Can edit"
    And taps "SAVE"
    Then the PUBLIC LINKS list refreshes and the "Report" link shows "Can edit"
    And the edit is sent to the item permissions endpoint
```

**Scenario: Server enforces a password when upgrading a folder link to edit** —
This verifies the password policy is re-evaluated on permission change during
an edit: a password-less view link on a folder, edited to a writeable type on
a server that mandates passwords for writeable shares, must block SAVE until a
compliant password is entered and then be sent via a separate setPassword request.

```gherkin
Feature: Edit an existing public link for a file or folder

  Scenario: Server enforces a password when upgrading a folder link to edit
    Given a signed-in user with update permission on the folder "Team" holding a password-less "Can view" link
    And the server enforces a password for writeable public links
    When the user opens the sharing panel for "Team"
    And taps the pencil icon on the link and selects "Can edit"
    Then SAVE is disabled until a valid password is entered
    And on saving, the new password is sent through a separate setPassword request
```
