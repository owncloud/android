## Why

In the Sharing NG flow a user can see and copy the public links that already exist for a file or folder, but the `PUBLIC LINKS` section is read-only — there is no way to create a link from the Android client. Users must leave the app and use another client (e.g. the web app) to make one. This change lets a link be created in-app, so the link lifecycle can start on the device.

## What Changes

- Add a `+` affordance in the `PUBLIC LINKS` section header, shown only when the permissions response for the item grants `libre.graph/driveItem/permissions/create` (derived from data already fetched to populate the panel — no extra call).
- Add a link-creation view to configure an optional name, a mandatory permission type, an optional password, and an optional expiration date, reusing the space-link creation UX (issue #4753).
- Offer item-type-aware permission options: `Can view` and `Can edit` for files; `Can view`, `Can edit`, and `Secret file drop` for folders — each with its subtitle text.
- Reveal the password and expiration-date fields only after a permission type is selected.
- Enforce the server password policy (read from capabilities already fetched for the session):
  - When `password.enforced = true` in capabilities (`filesSharingPublicPasswordEnforced`) → password required for every permission type.
  - When `password.enforced_for.read_write` or `password.enforced_for.upload_only` is true in capabilities (`filesSharingPublicPasswordEnforcedReadWrite` / `filesSharingPublicPasswordEnforcedUploadOnly`) → password required for `Can edit` and `Secret file drop`.
- Submit creation via `POST /graph/v1beta1/drives/<drive-id>/items/<item-id>/createLink`, sending `type` (`view`/`edit`/`createOnly`), sending `displayName` as the entered name or as "Unnamed Link" (translated) when empty, sending `password` per policy, and sending `expirationDateTime` (ISO 8601) only when set. Name is capped at 255 chars.
- On success, refresh the `PUBLIC LINKS` list (by re-fetching the item's permissions) so the new link appears; the creation response is not consumed.
- On network or server error, show a message and keep the creation view open.
- Where a class built for spaces is reused for file/folder links, evaluate renaming it and moving it to a scope-neutral directory as a behavior-preserving refactor.

Out of scope: editing or deleting existing links, configuring password-protection on existing links, classic WebDAV shares.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `files-sharing/public-links` — adds requirements for creating a public link (permission-gated `+` affordance, creation view, item-type-aware permission options, field-reveal order, password-policy enforcement, submission contract, list refresh on success, and error handling). The existing read/display requirements are unchanged.

## Impact

- **owncloudComLibrary** (networking): item-targeted `createLink` operation + service method (returns success; the response is not consumed).
- **owncloudData** (data): remote datasource + repository `addLink` extended with an `itemId`.
- **owncloudDomain** (business): `AddLinkUseCase`/`LinksRepository` extended with `itemId`; shared `OCLinkType` → password-policy mapping over `OCCapability`.
- **owncloudApp** (presentation): file/folder link creation reuses the existing `PublicLinksViewModel` (renamed from `SpaceLinksViewModel`) threaded with an `itemId`, and `+`-visibility in `GraphShareFragment`; generalized creation Fragment; list refresh + success snackbar and chain-icon share; relocation of the reused space-scoped link classes to `presentation/links/`; Koin wiring in `dependecyinjection/`.
- Tests: unit tests apply only to the repositories and datasources (`OCLinksRepositoryTest`, `OCRemoteLinksDataSourceTest`); no unit tests are added for the operation, use case, ViewModel, or UI layers.
- Changelog: an `Enhancement` entry in `changelog/unreleased/`.