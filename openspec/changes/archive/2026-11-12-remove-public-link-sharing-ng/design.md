## Context

See `proposal.md` — Why. The relevant current state:

- `PublicLinksAdapter` is already complete for this feature. `removePublicLinkButton` is gated on a `canRemoveLinks` field, and on click it calls `listener.onRemovePublicLink(publicLink.id, publicLink.displayName)` — the display name needed for the confirmation dialog is already supplied. `setPublicLinks(...)` already tracks a `hasUserPermissionsChanged` flag so a changed flag re-renders the rows.
- `PublicLinksViewModel` already exposes `removePublicLink(linkId)` and `removeLinkResultFlow`. The flow is a `MutableSharedFlow<UIResult<Unit>>` with no `Event` wrapper, matching `GraphShareViewModel.removeShareResultFlow` rather than the `Event`-wrapped `addLinkResultFlow`. The ViewModel is constructed with `accountName`, `spaceId` and a nullable `itemId` (bound to `file?.remoteId`), so the item identifier is already in hand.
- `GraphShareFragment` resolves the delete capability in `checkPermissions()` into a private `canRemoveShares` field from `DRIVES_DELETE_PERMISSION` (`libre.graph/driveItem/permissions/delete`), the same constant the member-share delete icon uses. `onRemoveShare(...)` is the established confirmation-dialog pattern. `GraphSharesAdapter` holds a separate field of the same name, which gates only the member-share rows it renders.
- `RemoveRemoteLinkOperation` takes only `(spaceId, linkId)` and builds `graph/v1beta1/drives/{spaceId}/root/permissions/{linkId}`. `AddRemoteLinkOperation` is the only link operation that is item-aware; it takes a nullable `itemId` and branches between `root/createLink` and `items/{itemId}/createLink`.
- All four dialog and feedback strings already exist: `public_link_remove_dialog_title` (takes the link name), `public_link_remove_dialog_message`, `public_link_remove_correctly`, `public_link_remove_failed`. No string or translation work is needed.

Constraints: strict module dependency chain (`owncloudComLibrary → owncloudData → owncloudDomain ← owncloudApp`); Detekt at `maxIssues: 0`; max line length 150.

## Goals / Non-Goals

**Goals:**
- Wire the existing removal stack to the sharing panel UI with no new architectural pattern — mirror the member-share removal flow exactly.
- Make the `DELETE` request address the shared item rather than always the drive root, so removal works for files and folders inside a space.

**Non-Goals:**
- Changing `EditRemoteLinkOperation` or `EditRemotePasswordLinkOperation`, which carry the same root-only path limitation. Link editing is #4970's scope; see Risks.
- Optimistic or local removal of the row. The list is re-fetched from the server after success, consistent with the member-share flow.
- Any Room or local-cache work. Links are not persisted locally; they are derived from the Graph permissions response on each fetch.

## Decisions

### Thread a nullable `itemId` down to `RemoveRemoteLinkOperation`

The operation must address `drives/{spaceId}/items/{itemId}/permissions/{linkId}` for an item inside the drive, and keep `drives/{spaceId}/root/permissions/{linkId}` when there is no item identifier. Because `itemId` originates in the presentation layer and the chain is strictly one-directional, it has to be added as a parameter at every hop:

```
GraphShareFragment.onRemovePublicLink(id, displayName)
  → PublicLinksViewModel.removePublicLink(linkId)            // reads its own itemId field
  → RemoveLinkUseCase.Params(accountName, spaceId, itemId, linkId)
  → LinksRepository.removeLink(accountName, spaceId, itemId, linkId)
  → OCLinksRepository.removeLink(...)
  → RemoteLinksDataSource.removeLink(...) / OCRemoteLinksDataSource
  → LinksService.removeLink(spaceId, itemId, linkId) / OCLinksService
  → RemoveRemoteLinkOperation(spaceId, itemId, linkId)
```

`itemId` is nullable (`String?`) rather than required, which preserves the root case and matches `AddRemoteLinkOperation`'s signature and null-branching. The alternative — a separate `RemoveRemoteItemLinkOperation` — was rejected: it would duplicate the whole operation body to vary two path segments, and it diverges from the add-link precedent.

Making `itemId` non-null and always passing a resolved identifier was also rejected, because space-root links are a real case the existing `root/permissions` path serves and `AddRemoteLinkOperation` still supports.

**No Koin changes.** Every module binds by constructor reference (`factoryOf(::RemoveLinkUseCase)`, `factoryOf(::OCLinksRepository)`, `singleOf(::OCRemoteLinksDataSource)`), and the constructors are untouched — only method signatures and the `Params` data class change. `UseCaseModule`, `RepositoryModule` and `RemoteDataSourceModule` need no edits.

### Gate the trashbin on the already-computed permission, renamed to `canRemoveSharesAndLinks`

`showLinks()` passes the resolved delete permission as `canRemoveLinks` instead of `false`. The delete capability for links and for member shares is the same Graph permission (`libre.graph/driveItem/permissions/delete`) on the same item, already resolved once in `checkPermissions()`. Introducing a second field parsed from the same list would be duplicate state that could drift.

Because that field now gates two sections rather than one, `GraphShareFragment.canRemoveShares` is renamed to `canRemoveSharesAndLinks`. The name is otherwise misleading at its new call site: a reader of `showLinks()` would see a links section gated on something called "shares". Three sites in the fragment change — the declaration, the assignment in `checkPermissions()`, and the argument passed to `setShares(...)` — plus the new argument to `setPublicLinks(...)`.

The rename is deliberately confined to the fragment. `GraphSharesAdapter` has its own private `canRemoveShares` field and a `setShares(..., canRemoveShares: Boolean, ...)` parameter, and it only ever renders member-share rows, so the narrower name remains accurate there; it receives the value positionally and needs no edit. Renaming it too would widen the diff into member-share-only code this change does not own, and would leave the adapter claiming a scope it does not have. `PublicLinksAdapter.canRemoveLinks` is likewise correct as-is.

`canEditLinks` stays hardcoded `false`; it belongs to #4970.

### One dialog shape, shared verbatim between `GraphShareFragment` and `SpaceMembersFragment`

Both screens offer public-link removal: `GraphShareFragment` (the Sharing NG panel, this change's primary scope) and `SpaceMembersFragment` (Spaces member management, already shipped and working). The user asked for the two dialogs to read identically rather than let the Sharing NG panel introduce a third, divergent wording. Both are rebuilt around the same shape used by every other removal dialog in the app — `onRemoveShare(...)` and, in `SpaceMembersFragment` itself, `onRemoveMember(...)`: a static title naming the action, and a message carrying the specific target's name.

No brand-new string resource is created for either part of the dialog. Both the existing `public_link_remove_dialog_title` and `public_link_remove_dialog_message` are modified in place instead:

- `public_link_remove_dialog_title` changes from "Do you really want to remove the link: %1$s?" (name in the title) to a static "Remove public link" — the `%1$s` placeholder is dropped entirely, since the title no longer carries the name.
- `public_link_remove_dialog_message` changes from "Recreating the same link again is not possible" to "Do you want to remove the public link: %1$s? Recreating the same link again is not possible." — gaining a `%1$s` placeholder for the name, while keeping the existing irreversibility wording verbatim so the requirement that the dialog "warns that the deletion cannot be undone" still holds.

Both resource names are unchanged; only their English values change, and both fragments now call them the same way: title with no argument, message formatted with the link's display name. `public_link_remove_correctly` and `public_link_remove_failed` are unaffected and keep being reused as-is for the result flow in both fragments.

**Translations of the two modified strings are deleted, not updated.** `public_link_remove_dialog_title` currently has human translations in `values-de` and `values-es`; `public_link_remove_dialog_message` in `values-de`, `values-de-rDE`, and `values-es`. Since the English source text changes, those translated entries no longer match what they're translating and are removed from each of those locale files — the app falls back to the English string until Transifex produces new translations for the updated text. The base `values/strings.xml` entries are the only ones edited with new content; no other locale file is touched, and no translation is authored here (this isn't a translation PR, it's clearing stale ones).

**Sequencing risk:** because both strings are shared resources modified in place, the moment `public_link_remove_dialog_message` gains the `%1$s` placeholder, every caller must pass the name — a caller still invoking `getString(R.string.public_link_remove_dialog_message)` with no argument compiles fine (plain resource lookups aren't checked against the format string at compile time) but throws `MissingFormatArgumentException` at runtime when that dialog is shown. `GraphShareFragment` and `SpaceMembersFragment` must therefore be updated together, not independently — whichever one moves to the new template first temporarily leaves the other on a stale call shape; the task breakdown reflects this by having one task modify both strings and update `GraphShareFragment`, and the very next task fix `SpaceMembersFragment`'s call to match before any build could plausibly ship between them.

**`GraphShareFragment.onRemovePublicLink(...)`** builds the dialog with a manual `AlertDialog.Builder`, identical in structure to `onRemoveShare(...)` in the same file: `ic_warning` icon, `public_link_remove_dialog_title` as the title (no argument), `public_link_remove_dialog_message` formatted with the link's display name as the message, `common_yes` invoking `publicLinksViewModel.removePublicLink(publicLinkId)`, `common_no` dismissing, and `avoidScreenshotsIfNeeded()` on the shown dialog.

**`SpaceMembersFragment.onRemovePublicLink(...)`** currently builds its dialog through the shared `Fragment.showAlertDialog(...)` extension in `FragmentExt.kt`, which has no icon parameter — `onRemoveShare`-style dialogs need one, `showAlertDialog` doesn't offer one. Rather than add an icon parameter to that shared helper (it also backs unrelated dialogs in automatic-upload settings and the spaces list, so widening its signature would ripple past this change), `SpaceMembersFragment.onRemovePublicLink(...)` switches to the same manual `AlertDialog.Builder` construction, mirroring its own `onRemoveMember(...)` a few lines above — that method already builds its dialog exactly this way, so this isn't a new pattern for the file. It passes `public_link_remove_dialog_title` (no argument) and the now-two-argument `public_link_remove_dialog_message` the same way `GraphShareFragment` does. The `showAlertDialog` import becomes unused once this is the only call site in the file and is removed. `FragmentExt.kt` itself, and every other caller of the shared helper, are untouched.

This reaches outside the Sharing NG panel that issue #4973 scopes, since `SpaceMembersFragment` belongs to the Spaces feature area and already has a fully working removal flow. It's included because the user explicitly asked for dialog parity between the two screens, not because the panel's own acceptance criteria require it.

### Observe `removeLinkResultFlow` like `removeShareResultFlow`

A new `observeRemoveLinkResult()` is registered in `subscribeToViewModels()`. Because the flow emits bare `UIResult` values (not `Event`-wrapped), the collector takes the `uiResult` directly — identical in shape to `observeRemoveShareResult()`:

- `Success` → `showMessageInSnackbar(getString(R.string.public_link_remove_correctly))` then `graphShareViewModel.getGraphShares()` to re-fetch and re-render.
- `Error` → `showErrorInSnackbar(R.string.public_link_remove_failed, uiResult.error)` and a `Timber.e`. No refresh, so the link remains listed.
- `Loading` → no-op, matching the sibling observers.

Refreshing via `getGraphShares()` rather than mutating the local `links` list keeps the panel's single source of truth on the server response and reuses the existing render path, which also covers the empty-state message when the last link is removed.

## Risks / Trade-offs

- **`EditRemoteLinkOperation` and `EditRemotePasswordLinkOperation` keep the same root-only path defect this change fixes for removal.** → Out of scope here; left to #4970, which owns link editing. Worth flagging on that issue so the fix lands with the same nullable-`itemId` shape rather than a divergent one.
- **Changing `LinksRepository.removeLink` and `RemoteLinksDataSource.removeLink` signatures breaks the two existing data-layer tests** (`OCLinksRepositoryTest`, `OCRemoteLinksDataSourceTest`), whose `removeLink` cases pass named arguments. → Both are updated in the same change; the compiler surfaces every call site, so there is no silent breakage.
- **Full re-fetch after each deletion costs an extra round trip and briefly shows the refresh spinner.** → Accepted: it is the established behavior for member-share removal and for link creation, and consistency beats saving one request on a low-frequency action.
- **A stale `linkId` (link already deleted on another client) will fail server-side rather than being detected up front.** → The error path already handles it: the user sees the removal-failed message and the subsequent refresh reconciles the list.

## Migration Plan

Not applicable — no persisted data, no schema change, no API contract owned by this client. The change is additive UI behavior plus a corrected request path; reverting the commit fully restores the prior read-only section. All three flavors (`original`, `mdm`, `qa`) are affected equally, since nothing here is flavor-conditional.
