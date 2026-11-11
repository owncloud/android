## Why

In the Sharing NG panel the `PUBLIC LINKS` section is read-only: a user can list, read, and copy existing public links, and (since #4955) create new ones, but cannot change a link's name, permission type, password, or expiration date. To edit a link today a user must switch to another client (e.g. the web app). This closes that gap so link management is self-contained in the Android client.

## What Changes

- Add a per-row **pencil (edit) affordance** to each entry in the `PUBLIC LINKS` section, shown only when the item's permissions response grants `libre.graph/driveItem/permissions/update`.
- Tapping the pencil opens the **same view used for link creation**, now in an edit mode, with name, permission selector, password field, and expiration-date field pre-filled from the link's current values; the submit button reads `SAVE` (instead of `CREATE`).
- Submit the edit as a **PATCH** to the item's permissions endpoint with the link's attributes (`link.type`, `link.@libre.graph.displayName`, `expirationDateTime`), using the same request body as the existing space-link edit.
- When the password changed, send it through a **separate `setPassword` POST**.
- Enforce the existing server password policy for the selected permission type (reusing the create-flow policy logic); keep the view open with an error message on failure; refresh the list from the server on success.

## Capabilities

### New Capabilities
<!-- None — this extends the existing public-links capability. -->

### Modified Capabilities
- `files-sharing/public-links`: adds requirements for a permission-gated per-row edit affordance, opening the shared create/edit view pre-filled in edit mode, submitting the edit as a PATCH (identical body to the space-link edit) plus a separate setPassword request, the `SAVE` label, and list refresh / error-handling on edit.

## Impact

- **owncloudApp (presentation):** reuse the create view (`AddPublicLinkFragment`) and the links ViewModel (`PublicLinksViewModel`) in an edit mode; add the pencil affordance + visibility gating to the public-links list/adapter in `GraphShareFragment`; new `SAVE` string and edit-success/error snackbars.
- **owncloudDomain (business):** new/extended UseCase(s) for editing a link (PATCH) and setting a link password (POST setPassword); extended repository interface.
- **owncloudData (data):** repository impl + remote datasource methods for edit and setPassword, delegating to the links service.
- **owncloudComLibrary (networking):** new remote operations issuing `PATCH .../permissions/{link-id}` and `POST .../permissions/{link-id}/setPassword`.
- **DI:** Koin wiring for any new UseCase(s); ViewModel/datasource/repository bindings reused.
- No Room schema change (public links are derived live from the permissions response). No new third-party dependencies.
