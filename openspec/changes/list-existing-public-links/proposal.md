## Why

In the Sharing NG flow, the sharing panel for a file or folder does not show public links that already exist for that item. Users who created a link previously — from any client — have no way to see it, so they cannot tell which links are active. Additionally, the warning text under the `PUBLIC LINKS` header incorrectly refers to "the space" even when the shared item is a file or folder.

## What Changes

- The sharing panel fetches an item's permissions from the Graph API (`GET /graph/v1beta1/drives/<drive-id>/items/<item-id>/permissions`, the same endpoint already used for shares) and lists all existing public links for that item.
- Only entries in the response `value` array that contain a `link` object are treated as public links; share entries without a `link` object are excluded.
- Each listed link shows:
  - its name — `link["@libre.graph.displayName"]`;
  - its permission label — `link.type` mapped to `Can edit` (edit), `Can view` (view), `Secret file drop` (createOnly);
  - its expiration date — only when `expirationDateTime` is present;
  - a chain icon on the right that shares `link.webUrl` through the Android share sheet when tapped.
- The warning text under `PUBLIC LINKS` changes from "Anyone with the link has access to the space" to reference the shared item, reading "Anyone with the link has access to the file" or "...the folder" depending on the item type.
- `hasPassword` is carried in the data model (not displayed) for use by a future edit flow.

Out of scope: creating, editing, or deleting public links; displaying password-protection status; classic WebDAV shares.

## Capabilities

### New Capabilities
- `files-sharing/public-links`: Retrieving and displaying the existing public links of a file or folder in the Sharing NG panel, including each link's name, permission, expiration, and share-link action, plus the item-type-aware warning text.

### Modified Capabilities
<!-- None: no existing capability specs defined yet. -->

## Impact

- **owncloudApp** (presentation): sharing panel Fragment/ViewModel render the public-links list and the item-type-aware warning; `strings.xml` warning text updated/parameterized.
- **owncloudDomain** (business): UseCase + domain model for an item's public links; repository interface for retrieving them (if not already covered by the shares flow).
- **owncloudData** (data): repository/data-source mapping of the permissions response `value` array into public-link domain models.
- **owncloudComLibrary** (networking): Moshi models for the permissions response `link` object fields, if not already present.
- No new third-party dependencies; reuses the existing Graph API permissions integration and the spaces' links UI (issue #4752) as a layout reference.