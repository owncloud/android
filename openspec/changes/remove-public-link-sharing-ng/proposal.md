## Why

The Sharing NG panel lists a file's or folder's public links but offers no way to remove one. The `PUBLIC LINKS` section is read-only: `GraphShareFragment.showLinks()` hardcodes `canRemoveLinks = false` and `onRemovePublicLink(...)` is an empty stub. A user who wants to revoke a link has to switch to another client, such as the web app.

The backend stack for link removal already exists end to end (`RemoveLinkUseCase` → `LinksRepository` → `RemoteLinksDataSource` → `LinksService` → `RemoveRemoteLinkOperation`), and `PublicLinksViewModel` already exposes `removePublicLink(linkId)` and `removeLinkResultFlow`. Only the presentation wiring is missing — plus one correctness defect in the request path, described below.

## What Changes

- A trashbin icon appears on each public link entry when the user holds the `libre.graph/driveItem/permissions/delete` permission on the item. `PublicLinksAdapter` already gates `removePublicLinkButton` on `canRemoveLinks`, so the fragment only needs to stop passing `false`.
- Tapping the icon opens a confirmation dialog, mirroring the member-share removal dialog in `onRemoveShare(...)`. Confirming calls `publicLinksViewModel.removePublicLink(...)`; cancelling dismisses without a request.
- `removeLinkResultFlow` is observed: success shows a confirmation message and refreshes the list; failure shows an error and leaves the list unchanged.
- **Request path correction.** `RemoveRemoteLinkOperation` currently targets `drives/{spaceId}/root/permissions/{linkId}`, which only addresses the space root. Deleting a link on a file or folder inside a space — the case the sharing panel is used for — would target the wrong resource. A nullable `itemId` is threaded from `RemoveLinkUseCase.Params` down to the operation, which branches between `root/permissions` and `items/{itemId}/permissions` exactly as `AddRemoteLinkOperation` already does.

No new user-facing strings are required; `public_link_remove_dialog_title`, `public_link_remove_dialog_message`, `public_link_remove_correctly` and `public_link_remove_failed` already exist.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `files-sharing/public-links`: adds requirements for a permission-gated delete affordance, a deletion confirmation step, the item-aware DELETE request path, and list refresh and error handling after a removal attempt.

## Impact

- `owncloudApp` — `GraphShareFragment` (delete flag, confirmation dialog, result observer, and its `canRemoveShares` field renamed to `canRemoveSharesAndLinks` now that it gates both sections); `PublicLinksViewModel.removePublicLink` passes `itemId`.
- `owncloudDomain` — `RemoveLinkUseCase.Params` and `LinksRepository.removeLink` gain `itemId`.
- `owncloudData` — `OCLinksRepository`, `RemoteLinksDataSource`, `OCRemoteLinksDataSource` signatures; existing `OCLinksRepositoryTest` and `OCRemoteLinksDataSourceTest` updated.
- `owncloudComLibrary` — `LinksService`, `OCLinksService`, `RemoveRemoteLinkOperation` path construction.
- No new dependencies. No translation changes. Out of scope: link creation (#4955), link editing (#4970), classic WebDAV shares.
