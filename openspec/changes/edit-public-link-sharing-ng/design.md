## Context

See proposal.md — Why. The read/display path and the create path for file & folder public links already exist end-to-end (issues for list + #4955), and — crucially — much of the **edit** stack was built alongside the create flow for the parallel **space-link** feature and is partially generalized:

- **UI (shared create/edit view):** `AddPublicLinkFragment` (`presentation/links/`) already supports an `editMode` + `ARG_SELECTED_PUBLIC_LINK` arg via `bindEditMode()` — it pre-fills the name, selects the permission radio (which reveals the password + expiration fields), pre-checks "has password" without exposing the clear-text password, and pre-fills the expiration date. In edit mode its submit button already reads `Save` (`R.string.share_confirm_public_link_button`), and the title is `R.string.public_link_edit_title`. On submit it calls `publicLinksViewModel.editPublicLink(id, displayName)`.
- **ViewModel:** `PublicLinksViewModel` (`presentation/links/`) already exposes `editPublicLink()` → routes to `editPasswordPublicLink()` when `wasPasswordChanged`, else `editLink()`; result flows `editLinkResultFlow` / `editPasswordLinkResultFlow`; and `checkPasswordEnforced(permission)` reuses the same capability-flag policy as create. It is constructed with `accountName`, `spaceId`, and a nullable `itemId` (already wired for the create flow to target the item).
- **Domain:** `EditLinkUseCase` (name/type/expiration) and `EditPasswordLinkUseCase` (password), plus `LinksRepository.editLink` / `editPasswordLink`.
- **Data:** `OCLinksRepository` → `RemoteLinksDataSource` / `OCRemoteLinksDataSource` → `ClientManager.getLinksService().editLink / editPasswordLink`.
- **ComLibrary:** `EditRemoteLinkOperation` (PATCH) and `EditRemotePasswordLinkOperation` (POST `setPassword`).
- **Adapter:** `PublicLinksAdapter` already renders a pencil (`editPublicLinkButton`) gated by a `canEditLinks` flag and forwards taps to `PublicLinksAdapterListener.onEditPublicLink(OCLink)`.

**The gaps that make editing a file/folder link actually work:**

1. **Edit operations target the space root, not the item.** `EditRemoteLinkOperation` and `EditRemotePasswordLinkOperation` build the path as `graph/v1beta1/drives/{spaceId}/root/permissions/{linkId}[/setPassword]`. Issue #4970 requires `.../items/{itemId}/permissions/{linkId}[/setPassword]`. The `itemId` is known in `PublicLinksViewModel` but is not threaded through `editLink`/`editPasswordLink` down to the operations.
2. **The sharing panel never enables or launches the edit flow for files/folders.** `GraphShareFragment.showLinks()` hardcodes `canEditLinks = false`; `onEditPublicLink(...)` is an empty stub; `GraphShareFragmentListener` has no edit entry point; and `ShareActivity` never launches `AddPublicLinkFragment` in edit mode. There is also no `GraphShareFragment` observer for the edit result flows (success refresh + success snackbar), and no `editLinkResultFlow`/`editPasswordLinkResultFlow` reset in `resetViewModel()` is missing one field (see D4).

The item path differs from the space path only in the segment: `items/{itemId}` vs `root`. The `canEditSharesAndLinks` flag (renamed from `canEditShares`, derived from `libre.graph/driveItem/permissions/update`, `DRIVES_UPDATE_PERMISSION`) is already computed in `GraphShareFragment.checkPermissions(...)` for the members list — it is the natural input to gate the link pencil too; the rename reflects that it now gates both affordances.

## Goals / Non-Goals

**Goals:**
- Make the already-built edit UI/ViewModel/domain/data/com-library stack actually editable from the file/folder sharing panel, by closing the three gaps above.
- Target the item permissions endpoint (`items/{itemId}/...`) for file/folder link edits while preserving the space flow's `root/...` behavior.
- Keep the edit request body identical to the space-link edit, and issue the `setPassword` request only when the password changed.
- Gate the pencil affordance on `libre.graph/driveItem/permissions/update`, re-applying it on refresh (parity with the create `+` affordance).
- Refresh the `PUBLIC LINKS` list and show a success message on a successful edit; keep the view open with an error on failure.

**Non-Goals:**
- No Room schema change — public links are derived live from the permissions response.
- No change to space-link edit behavior (keeps the `root/...` path via a null `itemId`).
- No deletion of links, no classic WebDAV (OCS) sharing changes, no new third-party dependency.
- Not re-architecting the shared create/edit fragment — it already handles both modes.

### D1: Thread an optional `itemId` through the edit stack; branch the operation path
Add a nullable `itemId: String?` to `EditLinkUseCase.Params`, `EditPasswordLinkUseCase.Params`, `LinksRepository.editLink` / `editPasswordLink`, `RemoteLinksDataSource` / `OCRemoteLinksDataSource`, `LinksService` / `OCLinksService`, and the two operations' constructors — mirroring exactly how `itemId` was threaded through the **create** stack (`addLink`). `PublicLinksViewModel.editLink` / `editPasswordPublicLink` pass the `itemId` they were constructed with. In `EditRemoteLinkOperation` / `EditRemotePasswordLinkOperation`, when `itemId` is non-null build the path `drives/{spaceId}/items/{itemId}/permissions/{linkId}[/setPassword]`; when null keep `drives/{spaceId}/root/permissions/{linkId}[/setPassword]`.
- **Why:** The only difference between space-link and file/folder-link editing is the path segment; a nullable parameter threaded through the existing stack matches the create-flow precedent and keeps space call sites working (they pass `null`). Forking would duplicate five layers.
- **Note on no default:** Follow the create-flow convention — add `itemId` with no `= null` default so every call site is forced to pass it explicitly (`PublicLinksViewModel` passes its `itemId`; the space flow passes `null`), avoiding a silent wrong-path bug.
- **Alternative considered:** Separate item-edit use cases/operations. Rejected — near-total duplication for one path segment.

### D2: Keep the edit request body identical to the space-link edit; setPassword only when changed
The file/folder edit uses the **exact same logic** as the already-shipped space-link edit — the only difference is the path segment (D1). `EditRemoteLinkOperation` keeps its current request body unchanged: it writes `expirationDateTime` (as `JSONObject.NULL` when absent) and the full `link` object (`displayName`, `type`). `EditLinkUseCase.Params`, the repository/datasource/service signatures, and `PublicLinksViewModel.editLink` keep their existing attribute shape (non-null `displayName`/`type`, nullable `expirationDate`); only `itemId` is added (D1). No diff against the original `OCLink` is computed.
Password is handled separately and unchanged: `PublicLinksViewModel.editPublicLink` only routes to `editPasswordPublicLink` when `wasPasswordChanged` is true (set by `onPasswordSelected`, and left false by `bindEditMode` when it only reflects the pre-existing password). So no `setPassword` request is issued unless the user actually changed the password.
- **Why:** The space-link edit already works end-to-end; reusing its exact body logic for file/folder links keeps the two flows consistent and avoids introducing conditional body-building. The only file/folder-specific concern is the endpoint path.
- **Trade-off:** The PATCH always sends all three attributes even when some are unchanged. Accepted: it matches the space-link edit the server already handles, and avoids added complexity.

### D3: Reuse the existing shared create/edit view and ViewModel — wire the launch from the panel
No new fragment or ViewModel. `GraphShareFragment` enables and launches the existing edit flow:
- `showLinks()` passes `canEditLinks = canEditSharesAndLinks` (the `DRIVES_UPDATE_PERMISSION` flag already computed in `checkPermissions`) instead of the hardcoded `false`.
- `GraphShareFragmentListener.addPublicLink` is parametrized with `editMode: Boolean` and `selectedPublicLink: OCLink?` (mirroring the existing `addGraphShare(... editMode, selectedShare)`) — no separate edit entry point. The existing `addPublicLinkButton` handler passes `editMode = false, selectedPublicLink = null`; `onEditPublicLink(publicLink)` calls `publicLinksViewModel.resetViewModel()` then `listener?.addPublicLink(file, accountName, editMode = true, selectedPublicLink = publicLink)`.
- `ShareActivity.addPublicLink(...)` forwards `editMode`/`selectedPublicLink` into `AddPublicLinkFragment.newInstance(accountName, spaceId, editMode, selectedPublicLink, file)` — the `newInstance` call already accepts both; only the currently-hardcoded `false`/`null` become the passed-through values.
- `GraphShareFragment` observes `publicLinksViewModel.editLinkResultFlow` and `editPasswordLinkResultFlow`: on `Success`, call `graphShareViewModel.getGraphShares()` to refresh the list and `showMessageInSnackbar(R.string.public_link_edit_correctly)`, then `publicLinksViewModel.resetViewModel()` — mirroring `observeAddLinkResult`. The `Error` branch is a deliberate no-op here because `AddPublicLinkFragment`'s own observers already show `R.string.public_link_edit_failed` and keep the view open (they pop only on success).
- **Why:** The entire edit UX (field reveal, password dialog, policy enforcement, pre-fill, Save label) already lives in `AddPublicLinkFragment`/`PublicLinksViewModel`; only the panel-side launch + result observation + permission gate are missing.

### D4: `resetViewModel()` must also clear the UI state and all edit flows
`PublicLinksViewModel.resetViewModel()` already nulls `addLinkResultFlow`, `addPublicLinkUIState`, `editLinkResultFlow`, `editPasswordLinkResultFlow`. Confirm it fully resets `wasPasswordChanged`/`selectedPermission` (it recreates `AddPublicLinkUIState()`), so reopening the edit view for a different link does not inherit stale selections. `GraphShareFragment` and `AddPublicLinkFragment` both call `resetViewModel()` before launching, matching the create flow's stale-state fix.

### D5: Pencil-affordance gating and live refresh
The pencil visibility is driven by `canEditSharesAndLinks` (`DRIVES_UPDATE_PERMISSION`), already recomputed in `checkPermissions(...)` on every `getSpacePermissions()` success — and the pull-to-refresh listener already re-fetches `getSpacePermissions()` (added in the create change). So revoking update permission mid-session and pulling to refresh hides the pencil without leaving the panel, with no extra wiring beyond passing `canEditSharesAndLinks` into `setPublicLinks`. The adapter's `setPublicLinks(...)` already recomputes `hasUserPermissionsChanged` and rebinds, so toggling `canEditLinks` repaints the rows.

### Data-flow path (edit)
`PublicLinksAdapter` pencil tap → `GraphShareFragment.onEditPublicLink(link)` → `listener.addPublicLink(file, accountName, editMode = true, selectedPublicLink = link)` → `ShareActivity` launches `AddPublicLinkFragment(editMode = true, selectedPublicLink = link, file)` → `bindEditMode()` pre-fills fields → user edits → `Save` → `PublicLinksViewModel.editPublicLink(link.id, displayName)` → (password changed? `editPasswordPublicLink` → `EditPasswordLinkUseCase` → … → `EditRemotePasswordLinkOperation` POST `items/{itemId}/permissions/{linkId}/setPassword`, then on success `editLink(...)`) and/or `editLink` → `EditLinkUseCase` → `LinksRepository.editLink` → `OCRemoteLinksDataSource` → `OCLinksService.editLink` → `EditRemoteLinkOperation` PATCH `items/{itemId}/permissions/{linkId}` → success → `AddPublicLinkFragment` pops back → `GraphShareFragment`'s edit-result observer runs `getGraphShares()` + success snackbar + `resetViewModel()`.

### Koin modules to update
- `ViewModelModule.kt` — no change: `PublicLinksViewModel` already registered with the explicit positional `(accountName, spaceId, itemId)` lambda (do **not** switch to `viewModelOf` — two `String` params can't be disambiguated by type).
- `UseCaseModule.kt`, `RepositoryModule.kt`, `RemoteDataSourceModule.kt` — no binding changes; `EditLinkUseCase`/`EditPasswordLinkUseCase`/`OCLinksRepository`/`OCRemoteLinksDataSource` signatures change in place and are already bound.

### Affected flavors
All three (`original` / `mdm` / `qa`) — no flavor-specific behavior.

## Risks / Trade-offs

- **[`itemId` path change touches the space edit operation]** → Mitigated by defaulting to `root/permissions/{linkId}` when `itemId` is null, preserving existing space behavior; covered by updating the existing space-path unit tests to pass `itemId = null` plus new item-path tests.
- **[Two sequential requests when both attributes and password change]** → `editPublicLink` chains `editPasswordPublicLink` → on success → `editLink` (existing behavior). Accepted: it mirrors the already-shipped space flow; on password failure the attribute PATCH is not sent and the view stays open with an error.
- **[Create-/update-permission gating strings must match the server exactly]** → Reuse the existing `DRIVES_UPDATE_PERMISSION` constant already validated against the permissions response for the members edit affordance.

## Migration Plan

Purely additive from a behavior standpoint; no persisted-data or schema migration and no feature flag. The pencil only appears when `libre.graph/driveItem/permissions/update` is present, and the new `itemId` parameters default the space flow to its prior `root/...` behavior. Rollback is reverting the change.

## Open Questions

- None that affect the specs, approach, or task breakdown.
