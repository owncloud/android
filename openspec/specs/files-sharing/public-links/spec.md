## Purpose

Defines how the Sharing NG panel retrieves and displays the public links that already exist for a file or folder, so a user can see which links are active, read each link's details, and copy a link's URL.

## Requirements

### Requirement: Fetch existing public links when opening the sharing panel

When the sharing panel is opened for a file or folder, the system SHALL request that item's permissions from the Graph API permissions endpoint and derive the item's public links from the response. The system SHALL treat only entries in the response `value` array that contain a `link` object as public links; entries without a `link` object SHALL be excluded.

#### Scenario: Permissions fetched on panel open
- **WHEN** a user opens the sharing panel for a file or folder
- **THEN** the system requests the item's permissions from the Graph API permissions endpoint for that item's drive and item identifiers

#### Scenario: Only entries with a link object are listed
- **WHEN** the permissions response contains a mix of entries, some with a `link` object and some without
- **THEN** only the entries that contain a `link` object are shown in the `PUBLIC LINKS` section
- **AND** entries without a `link` object are excluded from the public-links list

#### Scenario: No public links exist
- **WHEN** the permissions response contains no entry with a `link` object
- **THEN** the `PUBLIC LINKS` section shows no link entries

### Requirement: Display each public link's name

Each public link entry SHALL display the name taken from `link["@libre.graph.displayName"]`.

#### Scenario: Named link
- **WHEN** a public link has a `link["@libre.graph.displayName"]`
- **THEN** the entry displays that value as its name

### Requirement: Display each public link's permission

Each public link entry SHALL display a permission label derived from `link.type`: `edit` SHALL be shown as "Can edit", `view` SHALL be shown as "Can view", and `createOnly` SHALL be shown as "Secret file drop".

#### Scenario: Edit permission
- **WHEN** a public link has `link.type` equal to `edit`
- **THEN** the entry displays "Can edit" as its permission

#### Scenario: View permission
- **WHEN** a public link has `link.type` equal to `view`
- **THEN** the entry displays "Can view" as its permission

#### Scenario: Create-only permission
- **WHEN** a public link has `link.type` equal to `createOnly`
- **THEN** the entry displays "Secret file drop" as its permission

### Requirement: Display each public link's expiration date

Each public link entry SHALL display an expiration date only when the response provides `expirationDateTime` for that link. When `expirationDateTime` is absent, the entry SHALL NOT display an expiration date row.

#### Scenario: Link with expiration
- **WHEN** a public link has an `expirationDateTime` value
- **THEN** the entry displays that expiration date

#### Scenario: Link without expiration
- **WHEN** a public link has no `expirationDateTime` value
- **THEN** the entry displays no expiration date row

### Requirement: Share a public link's URL

Each public link entry SHALL present a chain icon on the right side of the entry. When the user taps the chain icon, the system SHALL share that link's `link.webUrl` through the Android share sheet.

#### Scenario: Share link URL
- **WHEN** a user taps the chain icon on a public link entry
- **THEN** the system opens the Android share sheet offering that link's `link.webUrl`

### Requirement: Item-type-aware public-links warning text

The warning text under the `PUBLIC LINKS` header SHALL reference the type of the shared item. When the shared item is a file, the text SHALL read "Anyone with the link has access to the file". When the shared item is a folder, the text SHALL read "Anyone with the link has access to the folder".

#### Scenario: Warning for a file
- **WHEN** the shared item is a file
- **THEN** the warning text reads "Anyone with the link has access to the file"

#### Scenario: Warning for a folder
- **WHEN** the shared item is a folder
- **THEN** the warning text reads "Anyone with the link has access to the folder"

### Requirement: Permission-gated create affordance

The `PUBLIC LINKS` section header SHALL present a create (`+`) affordance only when the permissions response for the item grants the current user the `libre.graph/driveItem/permissions/create` permission. When that permission is absent, the create affordance SHALL NOT be shown.

#### Scenario: User has create permission
- **WHEN** the item's permissions response grants `libre.graph/driveItem/permissions/create`
- **THEN** the `+` affordance is shown in the `PUBLIC LINKS` section header

#### Scenario: User lacks create permission
- **WHEN** the item's permissions response does not grant `libre.graph/driveItem/permissions/create`
- **THEN** the `+` affordance is not shown in the `PUBLIC LINKS` section header

#### Scenario: Create permission revoked during the session
- **WHEN** the `+` affordance is shown and the item's permissions are re-fetched (e.g. via pull-to-refresh) and no longer grant `libre.graph/driveItem/permissions/create`
- **THEN** the `+` affordance is hidden without the user leaving the sharing panel

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

#### Scenario: Fields hidden again when the creation view is reopened
- **WHEN** the user selects a permission type, leaves the creation view without creating a link, and reopens it
- **THEN** the password field and the expiration-date field are hidden again until a permission type is selected anew

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

On a successful creation the system SHALL refresh the `PUBLIC LINKS` list so that the newly created link appears, and SHALL show a success message confirming the link was created. The response returned by the creation request is not consumed; the updated list is obtained by re-fetching the item's permissions.

#### Scenario: New link appears after refresh
- **WHEN** the server confirms a successful link creation
- **THEN** the `PUBLIC LINKS` list is refreshed and the newly created link appears in it
- **AND** a success message is shown confirming the creation
- **AND** its chain icon shares that link's `webUrl` through the Android share sheet

### Requirement: Error handling keeps the creation view open

When link creation fails due to a network or server error, the system SHALL show an error message and SHALL keep the creation view open so the user can retry.

#### Scenario: Creation fails
- **WHEN** link creation fails with a network or server error
- **THEN** an error message is shown
- **AND** the creation view remains open