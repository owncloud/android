## ADDED Requirements

### Requirement: Permission-gated edit affordance

Each public link entry SHALL present an edit (pencil) affordance only when the permissions response for the item grants the current user the `libre.graph/driveItem/permissions/update` permission. When that permission is absent, the edit affordance SHALL NOT be shown on any public link entry.

#### Scenario: User has update permission
- **WHEN** the item's permissions response grants `libre.graph/driveItem/permissions/update`
- **THEN** a pencil (edit) icon is shown on each public link entry

#### Scenario: User lacks update permission
- **WHEN** the item's permissions response does not grant `libre.graph/driveItem/permissions/update`
- **THEN** no pencil (edit) icon is shown on any public link entry

#### Scenario: Update permission revoked during the session
- **WHEN** the pencil icon is shown and the item's permissions are re-fetched (e.g. via pull-to-refresh) and no longer grant `libre.graph/driveItem/permissions/update`
- **THEN** the pencil icon is hidden on the public link entries without the user leaving the sharing panel

### Requirement: Open the link-edit view pre-filled

Tapping the pencil affordance on a public link entry SHALL open the same view used for link creation, in an edit mode, with the name field, permission-type selector, password field, and expiration-date field pre-filled with that link's current values. Because a permission type is already selected, the password field and the expiration-date field SHALL be visible immediately.

#### Scenario: Open edit view with current values
- **WHEN** the user taps the pencil icon on a public link entry
- **THEN** the edit view opens with the name field set to the link's current `link["@libre.graph.displayName"]`
- **AND** the permission-type selector set to the link's current `link.type`
- **AND** the expiration-date field set to the link's current `expirationDateTime` when it has one, and empty when it has none

#### Scenario: Password and expiration fields visible on open
- **WHEN** the edit view opens for an existing link
- **THEN** the password field and the expiration-date field are visible without the user re-selecting a permission type

#### Scenario: Password is not pre-filled in clear text
- **WHEN** the edit view opens for a link that already has a password
- **THEN** the password field does not display the existing password in clear text

### Requirement: Item-type-aware permission options in the edit view

The permission-type selector in the edit view SHALL offer the same options as the creation view, depending on the shared item's type. For a file it SHALL offer "Can view" and "Can edit". For a folder it SHALL offer "Can view", "Can edit", and "Secret file drop".

#### Scenario: Edit permission options for a file
- **WHEN** the edit view is opened for a public link on a file
- **THEN** the permission-type selector offers "Can view" and "Can edit"
- **AND** does not offer "Secret file drop"

#### Scenario: Edit permission options for a folder
- **WHEN** the edit view is opened for a public link on a folder
- **THEN** the permission-type selector offers "Can view", "Can edit", and "Secret file drop"

### Requirement: Submit button labelled SAVE in edit mode

When the shared create/edit view is opened in edit mode, its submit button SHALL be labelled `SAVE`.

#### Scenario: Save label in edit mode
- **WHEN** the edit view is opened for an existing public link
- **THEN** the submit button is labelled `SAVE`

### Requirement: Enforce server password policy in the edit view

The system SHALL require a password before the edit form can be submitted when the server capabilities mandate one for the selected permission type, following the same policy as link creation. When `OCIS_SHARING_PUBLIC_SHARE_MUST_HAVE_PASSWORD` is set, a password SHALL be required for every permission type. When `OCIS_SHARING_PUBLIC_WRITEABLE_SHARE_MUST_HAVE_PASSWORD` is set, a password SHALL be required for the "Can edit" and "Secret file drop" permission types. The policy SHALL be read from the capabilities already fetched for the session; the system SHALL NOT make an additional network call to determine it.

#### Scenario: Password required for all types in edit
- **WHEN** capabilities report `password.enforced = true` (`filesSharingPublicPasswordEnforced`) and the link has no password set
- **THEN** the edit form cannot be submitted for any selected permission type until a password is entered

#### Scenario: Password required when switching to a writeable type in edit
- **WHEN** capabilities report `password.enforced_for.read_write = true` or `password.enforced_for.upload_only = true` (`filesSharingPublicPasswordEnforcedReadWrite` / `filesSharingPublicPasswordEnforcedUploadOnly`) and the user selects "Can edit" or "Secret file drop" and the link has no password set
- **THEN** the edit form cannot be submitted until a password is entered

#### Scenario: Password optional for view type under writeable policy in edit
- **WHEN** only `password.enforced_for.read_write` / `password.enforced_for.upload_only` are set (not `password.enforced`) and the selected permission is "Can view"
- **THEN** the edit form can be submitted without entering a password

### Requirement: Submit link edit

On confirmation (`SAVE`) the system SHALL send the link's attributes — the permission `type` (`view`, `edit`, or `createOnly`), the `displayName`, and the `expirationDateTime` — to the item's permissions endpoint for that link, using the same request body as the existing space-link edit. The name SHALL be limited to 255 characters. When an expiration date is set it SHALL be sent as an ISO 8601 value, and when no expiration date is set `expirationDateTime` SHALL be sent as null.

#### Scenario: Edited permission type is sent
- **WHEN** the user changes the permission type and confirms with `SAVE`
- **THEN** the request includes the new `type`

#### Scenario: Edited name is sent
- **WHEN** the user changes the link name and confirms with `SAVE`
- **THEN** the request includes `displayName` with the new name

#### Scenario: Expiration date is sent as ISO 8601
- **WHEN** the link has an expiration date and the user confirms with `SAVE`
- **THEN** the request includes `expirationDateTime` as an ISO 8601 value

#### Scenario: Absent expiration date is sent as null
- **WHEN** the link has no expiration date and the user confirms with `SAVE`
- **THEN** the request sends `expirationDateTime` as null

### Requirement: Submit a changed password through a separate request

When the user changes the link's password, the system SHALL send the new password to the item's `setPassword` endpoint for that link in a request separate from the attribute edit. When the password is not changed, the system SHALL NOT issue a `setPassword` request.

#### Scenario: Changed password is sent via setPassword
- **WHEN** the user enters or changes the password and confirms with `SAVE`
- **THEN** the new password is sent to the link's `setPassword` endpoint in a separate request

#### Scenario: Unchanged password issues no setPassword request
- **WHEN** the user confirms with `SAVE` without changing the password
- **THEN** no `setPassword` request is issued

### Requirement: Refresh list on successful edit

On a successful edit the system SHALL refresh the `PUBLIC LINKS` list so the edited link's updated values appear, and SHALL show a success message confirming the link was edited. The updated list SHALL be obtained by re-fetching the item's permissions.

#### Scenario: Edited link reflected after refresh
- **WHEN** the server confirms a successful edit
- **THEN** the `PUBLIC LINKS` list is refreshed and the edited link shows its updated values
- **AND** a success message is shown confirming the edit

### Requirement: Error handling keeps the edit view open

When a link edit fails due to a network or server error, the system SHALL show an error message and SHALL keep the edit view open so the user can retry.

#### Scenario: Edit fails
- **WHEN** a link edit fails with a network or server error
- **THEN** an error message is shown
- **AND** the edit view remains open
