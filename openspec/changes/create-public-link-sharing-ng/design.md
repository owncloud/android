## Context

See proposal.md — Why. The read/display path for file & folder public links already exists end-to-end: `GraphShareFragment` → `GraphShareViewModel.getGraphShares()` → `GetGraphSharesAsyncUseCase` → `ShareRepository.getGraphShares(accountName, spaceId, itemId)` → `OCRemoteShareDataSource` → `GetRemoteGraphSharesForFileOperation` (GET `graph/v1beta1/drives/{spaceId}/items/{itemId}/permissions`). `PermissionsResponse.toModel()` splits each entry into a `MemberPermission` (when `grantedToV2 != null`) or an `OCLink` (when it carries a `link` object). The `OCLink` domain model already holds `id, createdDateTime, expirationDateTime, hasPassword, displayName, type: OCLinkType, webUrl`, and `OCLinkType` maps `CAN_VIEW`/`CAN_EDIT`/`CREATE_ONLY` ↔ `view`/`edit`/`createOnly`.

A parallel **space-link** creation stack already exists (issue #4753) and is the pattern to reuse:
- Domain: `LinksRepository.addLink(accountName, spaceId, displayName, type, expirationDate?, password?)`, `AddLinkUseCase`.
- Data: `OCLinksRepository` → `RemoteLinksDataSource` → `OCRemoteLinksDataSource` → `ClientManager.getLinksService()`.
- ComLibrary: `LinksService.addLink` / `OCLinksService` → `AddRemoteLinkOperation`, POSTing to `graph/v1beta1/drives/{spaceId}/root/createLink` with a `JSONObject` body (`displayName`, `expirationDateTime?`, `password?`, `type`); it returns `Unit` (does not parse the created link).
- UI: `AddPublicLinkFragment` (radio buttons → `OCLinkType`, expiration DatePicker, password via `SetPasswordDialogFragment`) + `SpaceLinksViewModel` (originally keyed on `OCSpace`; this change re-keys both on a `spaceId: String` — see D3), whose `checkPasswordEnforced(permission)` maps `OCLinkType` → capability flags. Launched today only from `SpaceMembersActivity`. These UI classes live under `presentation/spaces/links/`.

Password policy lives in `OCCapability` (`filesSharingPublicPasswordEnforced`, `…EnforcedReadOnly`, `…EnforcedReadWrite`, `…EnforcedUploadOnly`, plus `passwordPolicy`), read via `GetStoredCapabilitiesUseCase`. The issue's `OCIS_*` env vars surface as these fields; the spec's two rules map to: `…Enforced` → all types; `…EnforcedReadWrite`/`…EnforcedUploadOnly` → `Can edit`/`Secret file drop`. `GraphShareViewModel` already holds the item's `OCFile` and the account `OCCapability`.

The item create endpoint differs from the space-root one only in the path segment: `items/{itemId}/createLink` vs. `root/createLink`.

## Goals / Non-Goals

**Goals:**
- Reuse the existing links stack by parametrizing it with an optional `itemId`, rather than forking a parallel create stack.
- Keep the creation UI consistent with the space-link creation UX (field reveal order, password dialog, permission radios).
- Refresh the `PUBLIC LINKS` list after a successful creation by reusing the existing permissions read path.
- Keep package layout honest: when a reused class outgrows its space-only scope, rename/relocate it.

**Non-Goals:**
- No Room schema change — public links are derived live from the permissions response, not cached.
- Do not consume the `createLink` response — the operation keeps its `Unit` return; no parsing of the created link, and no persistence of `id`/`hasPassword` (that only served an out-of-scope edit flow).
- No new network dependency; reuse OkHttp/Moshi/`org.json` already in `owncloudComLibrary`.
- No change to space-link creation behavior (it keeps the `root/createLink` path).

## Decisions

### D1: Extend the existing links stack with an optional `itemId`
Add `itemId: String? = null` to `LinksRepository.addLink`, `AddLinkUseCase.Params`, `RemoteLinksDataSource.addLink`, `OCLinksService.addLink`, and `AddRemoteLinkOperation`. When `itemId` is present, the operation targets `drives/{spaceId}/items/{itemId}/createLink`; when null, it keeps the existing `drives/{spaceId}/root/createLink` (space) behavior.
- **Why:** The only difference between space-link and file/folder-link creation is the URL path segment and the item context. A defaulted nullable parameter is backward-compatible with every current space call site; forking would duplicate four layers.
- **Alternative considered:** A separate `AddItemLinkUseCase` + operation. Rejected — near-total duplication for one path segment.

### D2: Do not consume the creation response; refresh the list on success
`AddRemoteLinkOperation` returns success (`Unit`) today, exactly as the space flow uses it. Keep it that way: the `createLink` response is not parsed. After a successful creation the ViewModel re-fetches the item's permissions (`getGraphShares()`) and the new link appears from the refreshed list.
- **Why:** The created-link object is not needed here — identity/password persistence for a future edit flow is out of scope — so parsing the response would add surface area (Moshi models, a return type threaded through four layers) for no behavior this change delivers. Re-fetching reuses the existing read path already wired into the panel.
- **Backward-compat:** The operation keeps its current `Unit` return, so space callers are entirely unaffected.
- **Alternative considered:** Parse the response into an `OCLink` and append it optimistically. Rejected — more code across four layers, and a response contract to track, for a marginal round-trip saving.

### D3: Host file/folder create logic in `GraphShareViewModel`, reuse a generalized `AddPublicLinkFragment`
`SpaceLinksViewModel` posts to the space root; files carry `OCFile` (`spaceId` + `remoteId` as itemId), and `GraphShareViewModel` already holds both the `OCFile` and `OCCapability`. Add `createPublicLink(...)` to `GraphShareViewModel`, and derive the `+`-visibility flag in `GraphShareFragment.checkPermissions(...)` alongside the existing `canRemoveShares`/`canEditShares` flags; generalize `AddPublicLinkFragment` to accept an optional `OCFile` (deriving the itemId from its `remoteId` and the file-vs-folder permission-option set from `OCFile.isFolder`) so it can be launched from `ShareActivity`/`GraphShareFragment`. Because only `space.id` was ever read, re-key `AddPublicLinkFragment` and `SpaceLinksViewModel` from an `OCSpace` to a mandatory `spaceId: String` — the space flow (`SpaceMembersActivity`) passes `space.id` and the file flow (`ShareActivity`) passes `file.spaceId`. Lift the reusable `checkPasswordEnforced(permission)` logic so both ViewModels share one definition. When a class is generalized this way, apply the naming/placement review in D5.
- **Why:** Keeps `OCFile`-centric state in the ViewModel that already owns it, and maximizes UI reuse.
- **Alternative considered:** Make `SpaceLinksViewModel` accept either `OCSpace` or `OCFile`. Rejected — overloads a space-scoped ViewModel with file concerns.

### D4: Derive gating and options from already-fetched data
The `+` affordance visibility comes from the `libre.graph/driveItem/permissions/create` entry in the space permissions list already fetched on panel open (the same `spacePermissions` source that drives the `canRemoveShares`/`canEditShares` flags — no extra call). The file-vs-folder permission-option set comes from the `OCFile` type.

### D5: Rename and relocate a reused class when its name/package no longer fit its scope
When a class originally scoped to spaces is reused here for file/folder links, evaluate whether its name and package still describe its (now broader) responsibility. If they do not, rename it and move it to the directory matching its scope — as a behavior-preserving `refactor:` within this change — updating every reference and the Koin wiring. If name and package remain accurate, leave the class where it is.
- **Concrete candidate:** `AddPublicLinkFragment` (and `SetPasswordDialogFragment` if it moves with it) currently lives under `presentation/spaces/links/`, which reads as space-only; once it also serves file/folder links, relocate it to a neutral shared package (e.g. `presentation/links/`) and drop any space-specific naming. The domain/data/com-library links stack (`AddLinkUseCase`, `LinksRepository`, `AddRemoteLinkOperation`, `OCLinks*`) already sits in scope-neutral `links/` packages, so it likely needs no move — confirm per class rather than assume.
- **Why:** Keeps package layout honest about what each class serves, so the next reader is not misled into thinking link creation is space-only.
- **Trade-off:** Moves/renames widen the diff and churn git history/blame; keep them strictly behavior-preserving and limited to genuinely mis-scoped classes, not a blanket sweep.
- **Alternative considered:** Leave reused classes under their original space packages. Rejected — leaves the layout lying about scope and makes the shared code harder to find.

### Koin modules to update
- `UseCaseModule.kt` — only if a new UseCase is added; the `AddLinkUseCase` signature change needs no re-registration.
- `ViewModelModule.kt` — no new ViewModel when reusing `GraphShareViewModel`; update only if a dedicated create ViewModel is introduced.
- `RemoteDataSourceModule.kt` / `RepositoryModule.kt` — no binding changes (signatures change in place); `OCRemoteLinksDataSource` / `OCLinksRepository` already bound.
- Any class moved/renamed under D5 must have its Koin binding (and all references) updated accordingly.

### Empty name defaults to "Unnamed Link" at submission (not render)
An empty name is replaced with the translated "Unnamed Link" before the request is built, so `displayName` is always sent (never omitted) and the created link is stored with that name. The list therefore shows the stored name directly; "Unnamed Link" is not a render-time fallback. Apply the default in `GraphShareViewModel.createPublicLink(...)` before calling `AddLinkUseCase`, so the single source of the default lives in the ViewModel.

### Data-flow path (create)
`AddPublicLinkFragment` → `GraphShareViewModel.createPublicLink(name?, type, password?, expiration?)` (replaces an empty `name` with "Unnamed Link") → `AddLinkUseCase(Params(accountName, spaceId, itemId, displayName, type, expirationDate?, password?))` → `LinksRepository.addLink(...)` → `OCLinksRepository` → `RemoteLinksDataSource.addLink` → `OCRemoteLinksDataSource` → `OCLinksService.addLink` → `AddRemoteLinkOperation` (POST `drives/{spaceId}/items/{itemId}/createLink`) → success. On success the ViewModel re-runs `getGraphShares()` and the refreshed permissions repopulate the `PUBLIC LINKS` list.

### Affected flavors
All three (`original` / `mdm` / `qa`) — no flavor-specific behavior.

## Risks / Trade-offs

- **[Refresh after creation adds a round trip]** → Accepted: reuses the already-wired read path and keeps the create operation's `Unit` contract unchanged; the extra `getGraphShares()` call runs only once, on success.
- **[`itemId` path change touches the space operation]** → Keep the path defaulted to `root/createLink` when `itemId` is null so the existing space behavior is preserved.
- **[`checkPasswordEnforced` duplicated across two ViewModels]** → Extract the `OCLinkType` → capability-flag mapping to one shared place so the policy is defined once.
- **[Create-permission key string must match the server exactly]** → Derive the `+`-gate from the exact `libre.graph/driveItem/permissions/create` key in the permissions response, confirmed against a captured fixture.
- **[D5 rename/move widens the diff and churns git blame]** → Keep moves strictly behavior-preserving, limited to genuinely mis-scoped classes; rely on the build + existing tests to confirm no behavior change.

## Migration Plan

Purely additive; no persisted-data or schema migration and no feature flag. The `+` affordance only appears when the create permission is present, and all new parameters default to the prior (space) behavior, so existing read-only behavior is unaffected. Any D5 rename/relocation is a behavior-preserving refactor. Rollback is reverting the change.

## Open Questions

- None that affect the specs, approach, or task breakdown. The exact JSON key for the create permission and the precise set of reused classes that warrant a D5 move are confirmed at implementation time, without changing the chosen design.