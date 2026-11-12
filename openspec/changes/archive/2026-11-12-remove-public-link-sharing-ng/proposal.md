## Why

The Sharing NG panel lists a file's or folder's public links but offers no way to remove one. The `PUBLIC LINKS` section is read-only: `GraphShareFragment.showLinks()` hardcodes `canRemoveLinks = false` and `onRemovePublicLink(...)` is an empty stub. A user who wants to revoke a link has to switch to another client, such as the web app.

The backend stack for link removal already exists end to end (`RemoveLinkUseCase` → `LinksRepository` → `RemoteLinksDataSource` → `LinksService` → `RemoveRemoteLinkOperation`), and `PublicLinksViewModel` already exposes `removePublicLink(linkId)` and `removeLinkResultFlow`. Only the presentation wiring is missing — plus one correctness defect in the request path, described below.

## What Changes

- A trashbin icon appears on each public link entry when the user holds the `libre.graph/driveItem/permissions/delete` permission on the item. `PublicLinksAdapter` already gates `removePublicLinkButton` on `canRemoveLinks`, so the fragment only needs to stop passing `false`.
- Tapping the icon opens a confirmation dialog shaped like the member-share removal dialog in `onRemoveShare(...)`: a static title naming the action, and a message carrying the link's name plus the existing irreversibility notice. Confirming calls `publicLinksViewModel.removePublicLink(...)`; cancelling dismisses without a request.
- `removeLinkResultFlow` is observed: success shows a confirmation message and refreshes the list; failure shows an error and leaves the list unchanged.
- **Request path correction.** `RemoveRemoteLinkOperation` currently targets `drives/{spaceId}/root/permissions/{linkId}`, which only addresses the space root. Deleting a link on a file or folder inside a space — the case the sharing panel is used for — would target the wrong resource. A nullable `itemId` is threaded from `RemoveLinkUseCase.Params` down to the operation, which branches between `root/permissions` and `items/{itemId}/permissions` exactly as `AddRemoteLinkOperation` already does.
- **Dialog parity with `SpaceMembersFragment`.** The Spaces member-management screen already has a working public-link removal dialog, built differently (via the shared `Fragment.showAlertDialog` extension, no icon, opposite title/message split). At the user's request, both screens are brought to the exact same dialog — same strings, same manual `AlertDialog.Builder` construction, same icon — rather than having the Sharing NG panel introduce a third wording. This reaches past issue #4973's own scope (the Sharing NG panel); it's included for consistency, not because the panel's acceptance criteria require it.

No new string resources are created. `public_link_remove_dialog_title` and `public_link_remove_dialog_message` are both reused and modified in place: the title drops its `%1$s` placeholder and becomes the static "Remove public link"; the message gains a `%1$s` placeholder for the link's name, keeping its existing irreversibility wording. Both fragments call them the same way. Since the English source text of both strings changes, their existing human translations (`public_link_remove_dialog_title` in `values-de`/`values-es`; `public_link_remove_dialog_message` in `values-de`/`values-de-rDE`/`values-es`) are deleted rather than left stale — the app falls back to English until Transifex supplies new translations. `public_link_remove_correctly` and `public_link_remove_failed` already exist, are untouched, and are reused as-is for the result flow in both fragments.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `files-sharing/public-links`: adds requirements for a permission-gated delete affordance, a deletion confirmation step, the item-aware DELETE request path, and list refresh and error handling after a removal attempt.

## Impact

- `owncloudApp` — `GraphShareFragment` (delete flag, confirmation dialog, result observer, and its `canRemoveShares` field renamed to `canRemoveSharesAndLinks` now that it gates both sections); `SpaceMembersFragment` (`onRemovePublicLink` switched from the shared `showAlertDialog` extension to a manual `AlertDialog.Builder`, unused import removed); `PublicLinksViewModel.removePublicLink` passes `itemId`; `public_link_remove_dialog_title` and `public_link_remove_dialog_message` modified in place in `strings.xml` (no new strings), with their stale translations in `values-de`, `values-de-rDE`, and `values-es` removed.
- `owncloudDomain` — `RemoveLinkUseCase.Params` and `LinksRepository.removeLink` gain `itemId`.
- `owncloudData` — `OCLinksRepository`, `RemoteLinksDataSource`, `OCRemoteLinksDataSource` signatures; existing `OCLinksRepositoryTest` and `OCRemoteLinksDataSourceTest` updated.
- `owncloudComLibrary` — `LinksService`, `OCLinksService`, `RemoveRemoteLinkOperation` path construction.
- No new dependencies. No translations are authored here — the Transifex-only rule still holds; the locale edits are deletions of entries whose English source changed, not new translations. Out of scope: link creation (#4955), link editing (#4970), classic WebDAV shares.
