## ADDED Requirements

### Requirement: Permission-gated create affordance

The `PUBLIC LINKS` section header SHALL present a create (`+`) affordance only when the permissions response for the item grants the current user the `libre.graph/driveItem/permissions/create` permission. When that permission is absent, the create affordance SHALL NOT be shown.

#### Scenario: User has create permission
- **WHEN** the item's permissions response grants `libre.graph/driveItem/permissions/create`
- **THEN** the `+` affordance is shown in the `PUBLIC LINKS` section header

#### Scenario: User lacks create permission
- **WHEN** the item's permissions response does not grant `libre.graph/driveItem/permissions/create`
- **THEN** the `+` affordance is not shown in the `PUBLIC LINKS` section header

### Requirement: Open the link-creation view

Tapping the `+` affordance SHALL open a link-creation view that lets the user configure a new public link with a name, a permission type, a password, and an expiration date.

#### Scenario: Open creation view
- **WHEN** the user taps the `+` affordance
- **THEN** a creation view opens with a name field and a permission-type selector
- **AND** the password and expiration-date fields are not yet shown

### Requirement: Item-type-aware permission options

The permission-type selector SHALL offer options that depend on the shared item's type. For a file, it SHALL offer "Can view" and "Can edit". For a folder, it SHALL offer "Can view", "Can edit", and "Secret file drop". Each option SHALL display a subtitle: "Can view" → "View, Download"; "Can edit" → "View, upload, edit, download, delete"; "Secret file drop" → "Upload only, existing content is not revealed".

#### Scenario: Permission options for a file
- **WHEN** the shared item is a file
- **THEN** the permission-type selector offers "Can view" and "Can edit"
- **AND** does not offer "Secret file drop"

#### Scenario: Permission options for a folder
- **WHEN** the shared item is a folder
- **THEN** the permission-type selector offers "Can view", "Can edit", and "Secret file drop"

### Requirement: Reveal password and expiration fields after permission selection

The password field and the expiration-date field SHALL be hidden until the user selects a permission type, and SHALL be revealed once a permission type is selected.

#### Scenario: Fields hidden before selection
- **WHEN** no permission type has been selected in the creation view
- **THEN** the password field and the expiration-date field are not shown

#### Scenario: Fields revealed after selection
- **WHEN** the user selects a permission type
- **THEN** the password field and the expiration-date field become visible

### Requirement: Enforce server password policy

The system SHALL require a password before the creation form can be submitted when the server capabilities mandate one for the selected permission type. When `OCIS_SHARING_PUBLIC_SHARE_MUST_HAVE_PASSWORD` is set, a password SHALL be required for every permission type. When `OCIS_SHARING_PUBLIC_WRITEABLE_SHARE_MUST_HAVE_PASSWORD` is set, a password SHALL be required for the "Can edit" and "Secret file drop" permission types. The policy SHALL be read from the capabilities already fetched for the session; the system SHALL NOT make an additional network call to determine it.

#### Scenario: Password required for all types
- **WHEN** capabilities report `password.enforced = true` (`filesSharingPublicPasswordEnforced`) and no password is entered
- **THEN** the creation form cannot be submitted for any selected permission type

#### Scenario: Password required for writeable types
- **WHEN** capabilities report `password.enforced_for.read_write = true` or `password.enforced_for.upload_only = true` (`filesSharingPublicPasswordEnforcedReadWrite` / `filesSharingPublicPasswordEnforcedUploadOnly`) and the selected permission is "Can edit" or "Secret file drop" and no password is entered
- **THEN** the creation form cannot be submitted

#### Scenario: Password optional for view type under writeable policy
- **WHEN** only `password.enforced_for.read_write` / `password.enforced_for.upload_only` are set (not `password.enforced`) and the selected permission is "Can view"
- **THEN** the creation form can be submitted without a password

### Requirement: Submit link creation request

On confirmation the system SHALL create the link on the server for the item's drive and item identifiers, sending the selected permission `type` (`view`, `edit`, or `createOnly`), the password when one is entered, the `displayName` always — set to the entered name, or to `Unnamed Link` (translated) when the name is left empty — and the `expirationDateTime` as an ISO 8601 value only when an expiration date is set. The name SHALL be limited to 255 characters.

#### Scenario: Submit with name and expiration
- **WHEN** the user confirms creation with a name and an expiration date set
- **THEN** the request includes `displayName` with the entered name and `expirationDateTime` as an ISO 8601 value

#### Scenario: Submit without name
- **WHEN** the user confirms creation with the name field left empty
- **THEN** the request includes `displayName` set to `Unnamed Link` (translated)

#### Scenario: Submit without expiration
- **WHEN** the user confirms creation with no expiration date set
- **THEN** the request omits the `expirationDateTime` field

### Requirement: Default name for unnamed links

A link created without a name SHALL be created with the name "Unnamed Link" (translated), submitted as its `displayName`; the `PUBLIC LINKS` list SHALL therefore show that created link as "Unnamed Link".

#### Scenario: Unnamed link display
- **WHEN** a link is created with no name
- **THEN** the link is created with the name "Unnamed Link"
- **AND** the list entry for it displays "Unnamed Link"

### Requirement: Refresh list on success

On a successful creation the system SHALL refresh the `PUBLIC LINKS` list so that the newly created link appears. The response returned by the creation request is not consumed; the updated list is obtained by re-fetching the item's permissions.

#### Scenario: New link appears after refresh
- **WHEN** the server confirms a successful link creation
- **THEN** the `PUBLIC LINKS` list is refreshed and the newly created link appears in it
- **AND** its chain icon shares that link's `webUrl` through the Android share sheet

### Requirement: Error handling keeps the creation view open

When link creation fails due to a network or server error, the system SHALL show an error message and SHALL keep the creation view open so the user can retry.

#### Scenario: Creation fails
- **WHEN** link creation fails with a network or server error
- **THEN** an error message is shown
- **AND** the creation view remains open