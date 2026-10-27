## 1. The `+` affordance

- [x] 1.1 Derive and expose a `+`-affordance visibility flag in `GraphShareFragment.checkPermissions(...)` from the `libre.graph/driveItem/permissions/create` entry in the fetched space permissions list. _Implemented in `GraphShareFragment.checkPermissions(spacePermissions)` (where sibling `canRemoveShares`/`canEditShares` are derived) via a new `canCreatePublicLinks` flag + `DRIVES_CREATE_PERMISSION` constant, rather than in the ViewModel — matching the existing permission-check pattern._
- [x] 1.2 Show the `+` affordance in the `PUBLIC LINKS` header of `GraphShareFragment` bound to the visibility flag (1.1). Verify the button appears only when create permission is granted. _Set `addPublicLinkButton.isVisible = canCreatePublicLinks` in `checkPermissions`; the `add_public_link_button` already existed in `members_fragment.xml` (default `gone`). No click action yet (task 2.3)._

## 2. Instantiate the creation screen

- [x] 2.1 Generalize `AddPublicLinkFragment` to accept an optional `itemId` and an is-folder flag so it can be launched for a file/folder; show `Can view`/`Can edit` for files and additionally `Secret file drop` for folders, each with its subtitle. Verify by launching for a file (two options) and a folder (three options). _Takes a single optional `OCFile` arg; Secret-file-drop row (and its trailing divider) hidden for files via `file?.isFolder ?: true`; subtitles already in `public_link_permissions.xml`. The actual file-flow launch + `GraphShareViewModel` binding (and reading `OCFile.remoteId` as the itemId) is deferred to 2.3/phase 3._
- [x] 2.2 Keep password and expiration-date fields hidden until a permission type is selected, then reveal them, matching the space-link pattern. Verify both fields are gone before selection and visible after. _No new code: inherited from the generalized `AddPublicLinkFragment`. `options_layout` (wrapping both the password and expiration-date includes) defaults to `visibility="gone"` and is set visible only inside `selectedPermission?.let { optionsLayout.isVisible = true }`._
- [x] 2.3 Launch `AddPublicLinkFragment` for the current item from the `+` affordance (from `ShareActivity`/`GraphShareFragment`). Verify tapping the `+` opens the creation view. _Added `addPublicLink(file, accountName)` to `GraphShareFragmentListener` + `+` click handler (mirrors `addGraphShare`, create-only — no edit-mode params per out-of-scope); `ShareActivity.addPublicLink` instantiates the fragment with `editMode = false`. `AddPublicLinkFragment`/`SpaceLinksViewModel` re-keyed from `OCSpace` to a mandatory `spaceId: String` (only `space.id` was ever used); file flow passes `file.spaceId`, space flow (`SpaceMembersActivity`) passes `space.id`. Fragment still binds `SpaceLinksViewModel`; file-flow `GraphShareViewModel` create surface is phase 3._
- [x] 2.4 Add the "Unnamed Link" string (and any new permission/subtitle strings not already present) to `strings.xml`. Verify the app builds and the default name renders for an unnamed link. _No new strings needed: `public_link_default_display_name` ("Unnamed Link") and the permission titles/subtitles (`public_link_view`/`_description`, `public_link_edit`/`_description`, `public_link_create_only`/`_description`) already exist with the spec's values, inherited from the reused space-link UX._

## 3. Create-link logic (presentation → networking)

- [x] 3.1 Thread an optional `itemId: String?` through `SpaceLinksViewModel` so its existing `createPublicLink` targets the item path for files: add `itemId` as a nullable constructor param (no default — always passed explicitly) and pass it into `AddLinkUseCase.Params`; `AddPublicLinkFragment` supplies it from the launched `OCFile.remoteId` (null for the space flow). No new ViewModel or create method — reuse the existing `SpaceLinksViewModel` surface. (Revert the `createPublicLink`/`addLinkResultFlow`/`addLinkUseCase` additions made to `GraphShareViewModel` in a prior apply — that approach is dropped per D3.) _Done: `itemId` added to `SpaceLinksViewModel` ctor + passed to `AddLinkUseCase.Params`; both `activityViewModel` resolvers updated (`AddPublicLinkFragment` → `file?.remoteId`; `SpaceMembersFragment` → null). `GraphShareViewModel` was already clean (no revert needed). Also fixed a latent 2.3 regression: `SpaceMembersFragment` was still passing a parcelable `OCSpace` to the re-keyed `spaceId: String` param — now passes `space.id`. App compiles end-to-end with the full `itemId` slice (3.4–3.8)._
- [x] 3.2 Before submission, replace an empty name with the translated "Unnamed Link" so it is sent as the `displayName` (already done in `AddPublicLinkFragment`, inherited by the file flow — confirm it applies); after creation success the fragment pops back and `GraphShareFragment` re-runs `getGraphShares()` (observer) so the links repopulate and shows a success snackbar (`public_link_add_correctly`, mirroring members' `share_add_correctly`); render the links from the refreshed list in `PublicLinksAdapter` and wire each chain icon to share its `webUrl` via the Android share sheet. Verify the new row appears after refresh showing "Unnamed Link" for an empty name, a success message is shown, and the chain icon opens the share sheet. _Done: empty-name default inherited from `AddPublicLinkFragment`; list render (`showLinks`) + chain-icon share (`onCopyOrSendPublicLink`) already existed. `GraphShareFragment` resolves the shared `SpaceLinksViewModel` (params from its `OCFile`: accountName/spaceId/remoteId, matching the create fragment to avoid the `activityViewModel` first-resolver mismatch) and observes `addLinkResultFlow` → on success `getGraphShares()` + `resetViewModel()`, mirroring `observeAddShareResult`. Success snackbar `showMessageInSnackbar(public_link_add_correctly)` added to that success branch._
- [x] 3.3 On creation error, show an error message and keep the creation view open (already handled by `AddPublicLinkFragment`'s `addLinkResultFlow` observer, inherited by the file flow — confirm it applies). Verify by forcing a failure and confirming the view stays open with a visible error. _No new code: `AddPublicLinkFragment`'s `addLinkResultFlow` observer already shows `public_link_add_failed` via `showErrorInSnackbar` on `Error` and does NOT pop the back stack (only `Success` pops), so the creation view stays open. The `GraphShareFragment`-side observer added in 3.2 keeps its `Error` branch a deliberate no-op to avoid double-handling._
- [x] 3.4 Extend `AddLinkUseCase.Params` with the optional `itemId`, keeping the success/`Unit` result. _Done: nullable `Params.itemId: String?` added (no default) and `run()` forwards it to `linksRepository.addLink(...)`._
- [x] 3.5 Extend `RemoteLinksDataSource.addLink` and `OCRemoteLinksDataSource.addLink` with the optional `itemId`, delegating to `ClientManager.getLinksService(...).addLink(...)`. Verify the item-targeted and space paths compile and delegate correctly. _Done: nullable `itemId` (no default) on the interface, forwarded through the impl to `getLinksService(...).addLink(...)`._
- [x] 3.6 Extend `LinksRepository.addLink` (interface) and `OCLinksRepository.addLink` (impl) with the optional `itemId`. Verify the interface/impl compile and delegate for both paths. _Done: nullable `itemId` (no default) on the interface, forwarded through the impl to the datasource._
- [x] 3.7 Parametrize `AddRemoteLinkOperation` with an optional `itemId`: when present build the POST path as `graph/v1beta1/drives/{spaceId}/items/{itemId}/createLink`, when null keep `root/createLink`. The operation keeps its `Unit` return — the response is not consumed. _Done: `itemId` ctor param; path branches to `items/{itemId}/createLink` when present, else `root/createLink`._
- [x] 3.8 Add an `itemId`-aware `addLink` to `LinksService` and `OCLinksService`, keeping the `Unit`/success return. Verify the interface/impl compile and the null-itemId (space) path still resolves to `root/createLink`. _Done: nullable `itemId` (no default) on the interface, forwarded through the impl to `AddRemoteLinkOperation`._

## 4. D5 rename/relocate review

- [ ] 4.1 For each class reused beyond its original space scope (e.g. `AddPublicLinkFragment`, `SetPasswordDialogFragment`), apply the D5 review: if its name/package no longer fit its broadened scope, rename it and move it to the correct directory (e.g. out of `presentation/spaces/links/` into a shared `presentation/links/`) as a behavior-preserving `refactor:`, updating all references and Koin wiring; leave it in place if name and package still fit. Verify `./gradlew assembleOriginalDebug` compiles and existing tests stay green with no behavior change.

## 5. Dependency injection

- [ ] 5.1 Update Koin wiring only where signatures, new components, or D5 moves require it (`UseCaseModule`, `ViewModelModule`, `RemoteDataSourceModule`, `RepositoryModule`) and confirm the graph resolves. Verify `./gradlew assembleOriginalDebug` compiles and the modules resolve at startup.

## 6. Unit Tests

- [ ] 6.1 In `OCRemoteLinksDataSourceTest`, extend tests for `addLink` to assert delegation to the item-targeted path (non-null `itemId`) and that the null-`itemId` (space) path still resolves correctly. Use `createRemoteOperationResultMock`.
- [ ] 6.2 In `OCLinksRepositoryTest`, extend tests for `addLink` to assert delegation to the datasource for the item path (`itemId` non-null) and the space path (`itemId` null).

## 7. Changelog

- [ ] 7.1 Add `changelog/unreleased/XXXX` (placeholder number until the PR is opened) as an `Enhancement` following the project changelog format, linking issue #4955 and the PR. Verify the file matches the required format.

## 8. Verification

- [ ] 8.1 Run `./gradlew testDebugUnitTest testMdmDebugUnitTest --continue`, `./gradlew detekt`, and `./gradlew ktlintCheck`; fix any issues. Verify all pass (detekt maxIssues: 0).

## QA

### Test plan

| Test name | Steps to execute | Expected result | Result | Comments |
|-----------|------------------|-----------------|--------|----------|
| **Operation available** | | | | |
| Create button visible — file | As a user with `libre.graph/driveItem/permissions/create`, open the sharing panel for a file. Check both orientations | `+` appears in the `PUBLIC LINKS` header | | |
| Create button visible — folder | As a user with `libre.graph/driveItem/permissions/create`, open the sharing panel for a folder. Check both orientations | `+` appears in the `PUBLIC LINKS` header | | |
| Create button hidden — no permission | As a user without `libre.graph/driveItem/permissions/create`, open the sharing panel for a file | No `+` in the `PUBLIC LINKS` header | | |
| Permission granted dynamically | 1. Open sharing panel without create permission<br>2. In web, grant the create permission<br>3. Reopen sharing panel | 1. No `+` in the `PUBLIC LINKS` header<br>3. `+` appears in the `PUBLIC LINKS` header | | |
| Permission removed dynamically | 1. Open sharing panel with create permission<br>2. In web, remove the create permission<br>3. Reopen sharing panel | 1. `+` appears in the `PUBLIC LINKS` header<br>3. No `+` in the `PUBLIC LINKS` header | | |
| **Set up link** | User with create permission, on `Add Link` | | | |
| No permission selected | Open the creation view without selecting a permission | `Create` button disabled; password and expiration date fields not visible | | |
| Password & expiration visible | Select any permission type | Password and expiration date fields become visible | | |
| Add Name | 1. Enter a name<br>2. Select any permission<br>3. Click `Create` | Link created and listed with the correct name. Check in web | | |
| Add Empty Name | 1. Leave name empty<br>2. Select any permission<br>3. Click `Create` | Link created and listed as "Unnamed Link". Check in web | | |
| Add Long Name | Attempt to enter a name exceeding 255 characters | Input is restricted to 255 characters maximum | | |
| Can View — file | 1. Tap `+` on a file<br>2. Select `Can view`<br>3. Click `Create` | Only `Can view` and `Can edit` are offered (no `Secret file drop`); link created with `Can view`. Subtitle: "View, Download". Check in web | | |
| Can Edit — file | 1. Tap `+` on a file<br>2. Select `Can edit`<br>3. Click `Create` | Link created with `Can edit`. Subtitle: "View, upload, edit, download, delete". Check in web | | |
| Can View — folder | 1. Tap `+` on a folder<br>2. Select `Can view`<br>3. Click `Create` | All three options offered; link created with `Can view`. Check in web | | |
| Can Edit — folder | 1. Tap `+` on a folder<br>2. Select `Can edit`<br>3. Click `Create` | Link created with `Can edit`. Check in web | | |
| Secret file drop — folder | 1. Tap `+` on a folder<br>2. Select `Secret file drop`<br>3. Click `Create` | `Secret file drop` option offered with subtitle "Upload only, existing content is not revealed"; link created. Check in web | | |
| Secret file drop not available for file | Tap `+` on a file and open permission selector | Only `Can view` and `Can edit` are offered; `Secret file drop` is not shown | | |
| All fields together | 1. Enter a valid name<br>2. Select any permission<br>3. Set a password<br>4. Set an expiration date<br>5. Click `Create` | Link created with the correct name, permission, password, and expiration date. Check in web that all four match | | |
| **Password** | | | | |
| Password mandatory | In a server with mandatory password (`OCIS_SHARING_PUBLIC_SHARE_MUST_HAVE_PASSWORD`), select any permission | Password field visible and mandatory for every permission level; `Create` locked until a password is set | | |
| Password not mandatory | In a server without mandatory password, select any permission | Password field visible and not mandatory; `Create` enabled without a password | | |
| Password not compliant with policy | 1. In a server with a password policy, select any permission<br>2. Enter a password that does not comply with the policy | `Create` button disabled | | |
| Password not mandatory for view (writeable policy) | In a server with `OCIS_SHARING_PUBLIC_WRITEABLE_SHARE_MUST_HAVE_PASSWORD`:<br>1. Select `Can view`<br>2. Click `Create` without entering a password | Password is not mandatory; link created without password. Check in web that the link is accessible without a password | | |
| Password mandatory for edit (writeable policy) | In a server with `OCIS_SHARING_PUBLIC_WRITEABLE_SHARE_MUST_HAVE_PASSWORD`, select `Can edit` | Password is mandatory; `Create` disabled until a password is set | | |
| Password mandatory for secret drop (writeable policy) | In a server with `OCIS_SHARING_PUBLIC_WRITEABLE_SHARE_MUST_HAVE_PASSWORD`, select `Secret file drop` | Password is mandatory; `Create` disabled until a password is set | | |
| Password enforcement switches with permission | In a server with `OCIS_SHARING_PUBLIC_WRITEABLE_SHARE_MUST_HAVE_PASSWORD`:<br>1. Select `Can view`<br>2. Select `Can edit`<br>3. Select `Secret file drop`<br>4. Select `Can view` again | 1. Password not mandatory, `Create` enabled<br>2. Password mandatory, `Create` disabled<br>3. Password mandatory, `Create` disabled<br>4. Password not mandatory, `Create` enabled | | |
| Password removed (mandatory server) | 1. In a server with mandatory password, select any permission<br>2. Enter and confirm a password<br>3. Remove the password | `Create` disabled; password field shows as empty | | |
| Password removed (non-mandatory server) | 1. In a server without mandatory password, select any permission<br>2. Enter and confirm a password<br>3. Remove the password<br>4. Click `Create` | 3. `Create` still enabled<br>4. Link created without password. Check in web that the link is accessible without a password | | |
| Password generated | 1. Select any permission<br>2. In the password dialog, click `Generate`<br>3. Click `Copy`<br>4. Click `Set`<br>5. Click `Create` | 4. Password set (shown as `****`)<br>5. Link created and listed. Check in web that the link requires the generated password (available in clipboard) | | |
| Password custom | 1. Select any permission<br>2. In the password dialog, enter a policy-compliant password<br>3. Click `Copy`<br>4. Click `Set`<br>5. Click `Create` | 4. Password set (shown as `****`)<br>5. Link created and listed. Check in web that the link requires the typed password | | |
| No password policy | 1. In a server with no password policy, select any permission<br>2. In the password dialog, enter any password<br>3. Click `Set`<br>4. Click `Create` | 3. Password set (shown as `****`)<br>4. Link created and listed. Check in web | | |
| Permission selected & no mandatory password | 1. Select a permission<br>2. Do not enter the mandatory password | `Create` button disabled | | |
| No permission & no mandatory password | Do not select any permission; do not enter a mandatory password | `Create` button disabled | | |
| **Expiration date** | | | | |
| Add expiration date | 1. Select any permission<br>2. Open the expiration date picker and choose a valid future date<br>3. Click `Create` | Link created and listed with the correct expiration date. Check in web | | |
| Remove expiration date | 1. Select any permission<br>2. Open the expiration date picker and choose a valid date<br>3. Disable (switch off) the expiration date<br>4. Click `Create` | 2. Expiration date displayed<br>4. Link created with no expiration date. Check in web | | |
| **After creation** | | | | |
| New link appears in list | Create a link successfully | The `PUBLIC LINKS` list refreshes and the newly created link appears | | |
| Unnamed link label | Create a link with an empty name | The list entry shows "Unnamed Link" as the display name | | |
| Share via chain icon | Tap the chain icon on the newly created link | Android share sheet opens offering the link's `webUrl` | | |
| **Errors** | | | | |
| Permission removed during creation | 1. Tap `+` and configure a new link<br>2. Before submitting, have the create permission removed server-side<br>3. Click `Create` | Error "Link could not be created" shown; creation view stays open | | |
| No network connection | 1. Open the creation view<br>2. Disable network on the device<br>3. Fill in required fields and click `Create` | Error "not connected to a network" shown; no results | | |
| Server down | 1. Open the creation view<br>2. Turn the server off<br>3. Fill in required fields and click `Create` | Error "connecting to the server" shown; no results | | |
| Error keeps view open for retry | Force any creation failure (network or server error) | Error message shown; creation view remains open so the user can retry | | |
| **Regression** | | | | |
| Space link creation | Create a public link from space management as before | Space link still created via `root/createLink`; behavior unchanged. Check in web | | |
| Existing links readable | Open sharing panel for an item that already has public links | Existing links listed correctly; chain icons open the share sheet with the correct URL | | |
| **Accessibility** | One device | | | |
| Talkback | 1. Enable Talkback<br>2. Navigate through the `Add Link` and password views | Every field is correctly labelled and navigable via Talkback | | |
| Keyboard navigation | 1. Connect a physical keyboard via BT<br>2. Navigate through the `Add Link` and password views | Every field is selectable; no dead paths in navigation | | |

### Gherkin scenario (E2E suite proposal)

```gherkin
Feature: Create a public link for a file or folder

  Scenario: User creates a password-less view link for a file
    Given a signed-in user on an oCIS server with permission to create links on "report.pdf"
    And the server does not enforce a password for public links
    When the user opens the sharing panel for "report.pdf"
    And taps the "+" button in the PUBLIC LINKS section
    And selects the "Can view" permission
    And confirms creation without a name, password, or expiration date
    Then a new public link appears in the PUBLIC LINKS list labelled "Unnamed Link"
    And tapping its chain icon opens the Android share sheet with the link URL

  Scenario: Server enforces a password for an edit link on a folder
    Given a signed-in user with permission to create links on the folder "Team"
    And the server enforces a password for writeable public links
    When the user opens the sharing panel for "Team"
    And taps the "+" button and selects the "Can edit" permission
    And attempts to confirm creation without entering a password
    Then creation is blocked until a valid password is entered
```