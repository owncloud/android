## Context

See proposal.md — Why. The read/display path for file & folder public links already exists end-to-end: `GraphShareFragment` → `GraphShareViewModel.getGraphShares()` → `GetGraphSharesAsyncUseCase` → `ShareRepository.getGraphShares(accountName, spaceId, itemId)` → `OCRemoteShareDataSource` → `GetRemoteGraphSharesForFileOperation` (GET `graph/v1beta1/drives/{spaceId}/items/{itemId}/permissions`). `PermissionsResponse.toModel()` splits each entry into a `MemberPermission` (when `grantedToV2 != null`) or an `OCLink` (when it carries a `link` object). The `OCLink` domain model already holds `id, createdDateTime, expirationDateTime, hasPassword, displayName, type: OCLinkType, webUrl`, and `OCLinkType` maps `CAN_VIEW`/`CAN_EDIT`/`CREATE_ONLY` ↔ `view`/`edit`/`createOnly`.

A parallel **space-link** creation stack already exists (issue #4753) and is the pattern to reuse:
- Domain: `LinksRepository.addLink(accountName, spaceId, displayName, type, expirationDate?, password?)`, `AddLinkUseCase`.
- Data: `OCLinksRepository` → `RemoteLinksDataSource` → `OCRemoteLinksDataSource` → `ClientManager.getLinksService()`.
- ComLibrary: `LinksService.addLink` / `OCLinksService` → `AddRemoteLinkOperation`, POSTing to `graph/v1beta1/drives/{spaceId}/root/createLink` with a `JSONObject` body (`displayName`, `expirationDateTime?`, `password?`, `type`); it returns `Unit` (does not parse the created link).
- UI: `AddPublicLinkFragment` (radio buttons → `OCLinkType`, expiration DatePicker, password via `SetPasswordDialogFragment`) + `SpaceLinksViewModel` (originally keyed on `OCSpace`; this change re-keys both on a `spaceId: String` and renames it `PublicLinksViewModel` — see D3/D5), whose `checkPasswordEnforced(permission)` maps `OCLinkType` → capability flags. Launched today only from `SpaceMembersActivity`. These UI classes originally live under `presentation/spaces/links/` and are relocated to `presentation/links/` under D5.

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
Add a nullable `itemId: String?` to `LinksRepository.addLink`, `AddLinkUseCase.Params`, `RemoteLinksDataSource.addLink`, `OCLinksService.addLink`, and `AddRemoteLinkOperation` (no `= null` default — every call site already passes it explicitly: `null` for the space flow, `OCFile.remoteId` for the file flow). When `itemId` is present, the operation targets `drives/{spaceId}/items/{itemId}/createLink`; when null, it keeps the existing `drives/{spaceId}/root/createLink` (space) behavior.
- **Why:** The only difference between space-link and file/folder-link creation is the URL path segment and the item context. A nullable parameter threaded through the existing stack keeps the space call sites working (they pass `null`); forking would duplicate four layers.
- **Alternative considered:** A separate `AddItemLinkUseCase` + operation. Rejected — near-total duplication for one path segment.

### D2: Do not consume the creation response; refresh the list on success
`AddRemoteLinkOperation` returns success (`Unit`) today, exactly as the space flow uses it. Keep it that way: the `createLink` response is not parsed. After a successful creation the ViewModel re-fetches the item's permissions (`getGraphShares()`) and the new link appears from the refreshed list.
- **Why:** The created-link object is not needed here — identity/password persistence for a future edit flow is out of scope — so parsing the response would add surface area (Moshi models, a return type threaded through four layers) for no behavior this change delivers. Re-fetching reuses the existing read path already wired into the panel.
- **Backward-compat:** The operation keeps its current `Unit` return, so space callers are entirely unaffected.
- **Alternative considered:** Parse the response into an `OCLink` and append it optimistically. Rejected — more code across four layers, and a response contract to track, for a marginal round-trip saving.

### D3: Reuse `PublicLinksViewModel` (and the generalized `AddPublicLinkFragment`) for the file/folder flow
Rather than adding a parallel create surface to `GraphShareViewModel`, the file/folder flow reuses the existing links ViewModel and its existing link methods (`createPublicLink`, `onPermissionSelected`/`onPasswordSelected`/`onExpirationDateSelected`, `checkPasswordEnforced`, and the result flows) — no new ViewModel classes or methods, avoiding duplication. That ViewModel was already re-keyed from an `OCSpace` to a mandatory `spaceId: String` (only `space.id` was ever read); it additionally takes an optional `itemId: String?` so its `createPublicLink` can target the item path (`items/{itemId}/createLink`) for files or the space root (`root/createLink`) when `itemId` is null. Because it now serves both flows, it is renamed `SpaceLinksViewModel` → `PublicLinksViewModel` and relocated under D5 (see below). The `AddPublicLinkFragment` (generalized to accept an optional `OCFile`, deriving the itemId from its `remoteId` and the file-vs-folder permission-option set from `OCFile.isFolder`) is launched from `ShareActivity`/`GraphShareFragment` and binds to this same `PublicLinksViewModel`. The space flow (`SpaceMembersActivity`) passes `space.id` with a null `itemId`; the file flow (`ShareActivity`) passes `file.spaceId` and `file.remoteId`. The `+`-visibility flag is derived separately in `GraphShareFragment.checkPermissions(...)` alongside the existing `canRemoveShares`/`canEditShares` flags. When a reused class is generalized this way, apply the naming/placement review in D5.
- **Why:** Reuses the entire existing link-creation stack (ViewModel methods, result flows, password-policy logic, UI) with a single added `itemId` parameter, instead of duplicating that surface on `GraphShareViewModel`.
- **Alternative considered:** Host a separate `createPublicLink(...)` on `GraphShareViewModel` (which already holds the `OCFile`/`OCCapability`). Rejected — duplicates the link-creation surface and the password-policy logic that `PublicLinksViewModel` already provides.

### D4: Derive gating and options from already-fetched data
The `+` affordance visibility comes from the `libre.graph/driveItem/permissions/create` entry in the space permissions list already fetched on panel open (the same `spacePermissions` source that drives the `canRemoveShares`/`canEditShares` flags — no extra call). The file-vs-folder permission-option set comes from the `OCFile` type.

### D5: Rename and relocate a reused class when its name/package no longer fit its scope
When a class originally scoped to spaces is reused here for file/folder links, evaluate whether its name and package still describe its (now broader) responsibility. If they do not, rename it and move it to the directory matching its scope — as a behavior-preserving `refactor:` within this change — updating every reference and the Koin wiring. If name and package remain accurate, leave the class where it is.
- **Applied:** all three `presentation/spaces/links/` classes served file/folder links after generalization and were relocated to the neutral `presentation/links/` package (alongside `PublicLinksAdapter`): `AddPublicLinkFragment`, `SetPasswordDialogFragment`, and the links ViewModel — which was additionally renamed `SpaceLinksViewModel` → `PublicLinksViewModel` to drop its space-specific name. All references, imports, and Koin wiring were updated; `git mv` preserved history. The domain/data/com-library links stack (`AddLinkUseCase`, `LinksRepository`, `AddRemoteLinkOperation`, `OCLinks*`) already sits in scope-neutral `links/` packages and needed no move.
- **Why:** Keeps package layout honest about what each class serves, so the next reader is not misled into thinking link creation is space-only.
- **Trade-off:** Moves/renames widen the diff and churn git history/blame; keep them strictly behavior-preserving and limited to genuinely mis-scoped classes, not a blanket sweep.
- **Alternative considered:** Leave reused classes under their original space packages. Rejected — leaves the layout lying about scope and makes the shared code harder to find.

### Koin modules to update
- `UseCaseModule.kt` — only if a new UseCase is added; the `AddLinkUseCase` signature change needs no re-registration.
- `ViewModelModule.kt` — no new ViewModel; the file flow reuses `PublicLinksViewModel` (renamed from `SpaceLinksViewModel` under D5). It MUST be registered with an explicit positional lambda — `viewModel { (accountName: String, spaceId: String, itemId: String?) -> PublicLinksViewModel(get(), get(), get(), get(), get(), accountName, spaceId, itemId, get()) }` — and NOT `viewModelOf(::PublicLinksViewModel)`: the reflective `viewModelOf` form injects the injected params by type, and with two `String` params it cannot distinguish `spaceId` from `itemId` (it feeds `itemId` the `spaceId` value), which forces the item path on the space flow (→ `400`). The explicit lambda binds the three params (`accountName`, `spaceId`, `itemId`) by position, so each resolver's `parametersOf(accountName, spaceId, itemId)` lines up.
- `RemoteDataSourceModule.kt` / `RepositoryModule.kt` — no binding changes (signatures change in place); `OCRemoteLinksDataSource` / `OCLinksRepository` already bound.
- Any class moved/renamed under D5 must have its Koin binding (and all references) updated accordingly.

### Empty name defaults to "Unnamed Link" at submission (not render)
An empty name is replaced with the translated "Unnamed Link" before the request is built, so `displayName` is always sent (never omitted) and the created link is stored with that name. The list therefore shows the stored name directly; "Unnamed Link" is not a render-time fallback. This default is already applied in `AddPublicLinkFragment` (it passes `publicLinkNameEditText.text.ifEmpty { getString(R.string.public_link_default_display_name) }` into `PublicLinksViewModel.createPublicLink(...)`), so it is inherited by the file flow with no new code.

### Data-flow path (create)
`AddPublicLinkFragment` (replaces an empty `name` with "Unnamed Link") → `PublicLinksViewModel.createPublicLink(displayName)` (using the `spaceId` + optional `itemId` it was created with) → `AddLinkUseCase(Params(accountName, spaceId, itemId, displayName, type, expirationDate?, password?))` → `LinksRepository.addLink(...)` → `OCLinksRepository` → `RemoteLinksDataSource.addLink` → `OCRemoteLinksDataSource` → `OCLinksService.addLink` → `AddRemoteLinkOperation` (POST `drives/{spaceId}/items/{itemId}/createLink`) → success. On success the fragment pops back to `GraphShareFragment`, which re-runs `getGraphShares()` (its observer, mirroring `observeAddShareResult`) so the refreshed permissions repopulate the `PUBLIC LINKS` list, and shows a success snackbar (`public_link_add_correctly`, mirroring members' `share_add_correctly`).

### Affected flavors
All three (`original` / `mdm` / `qa`) — no flavor-specific behavior.

## Risks / Trade-offs

- **[Refresh after creation adds a round trip]** → Accepted: reuses the already-wired read path and keeps the create operation's `Unit` contract unchanged; the extra `getGraphShares()` call runs only once, on success.
- **[`itemId` path change touches the space operation]** → Keep the path defaulted to `root/createLink` when `itemId` is null so the existing space behavior is preserved.
- **[Password-policy logic could be duplicated]** → Avoided by reusing `PublicLinksViewModel.checkPasswordEnforced` for both flows (D3) rather than adding a second copy on `GraphShareViewModel`, so the policy stays defined once.
- **[Create-permission key string must match the server exactly]** → Derive the `+`-gate from the exact `libre.graph/driveItem/permissions/create` key in the permissions response, confirmed against a captured fixture.
- **[D5 rename/move widens the diff and churns git blame]** → Keep moves strictly behavior-preserving, limited to genuinely mis-scoped classes; rely on the build + existing tests to confirm no behavior change.

## Migration Plan

Purely additive; no persisted-data or schema migration and no feature flag. The `+` affordance only appears when the create permission is present, and all new parameters default to the prior (space) behavior, so existing read-only behavior is unaffected. Any D5 rename/relocation is a behavior-preserving refactor. Rollback is reverting the change.

## Open Questions

- None that affect the specs, approach, or task breakdown. The exact JSON key for the create permission and the precise set of reused classes that warrant a D5 move are confirmed at implementation time, without changing the chosen design.