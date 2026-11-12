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
### Requirement: Permission-gated delete affordance

Each public link entry SHALL present a delete (trashbin) affordance only when the permissions response for the shared item grants the current user the `libre.graph/driveItem/permissions/delete` permission. When that permission is absent, the delete affordance SHALL NOT be shown on any public link entry.

#### Scenario: User has delete permission
- **WHEN** the item's permissions response grants `libre.graph/driveItem/permissions/delete`
- **AND** the `PUBLIC LINKS` section lists at least one link
- **THEN** each public link entry shows a trashbin affordance

#### Scenario: User lacks delete permission
- **WHEN** the item's permissions response does not grant `libre.graph/driveItem/permissions/delete`
- **THEN** no public link entry shows a trashbin affordance

#### Scenario: Delete permission revoked during the session
- **WHEN** the trashbin affordance is shown and the item's permissions are re-fetched (e.g. via pull-to-refresh) and no longer grant `libre.graph/driveItem/permissions/delete`
- **THEN** the trashbin affordance is hidden without the user leaving the sharing panel

### Requirement: Confirm before deleting a public link

Tapping a public link entry's trashbin affordance SHALL open a confirmation dialog that identifies the link by its displayed name and warns that the deletion cannot be undone. The dialog SHALL offer an affirmative and a dismissive choice. No deletion request SHALL be sent until the user makes the affirmative choice.

#### Scenario: Confirmation dialog identifies the link
- **WHEN** a user taps the trashbin affordance on a public link entry
- **THEN** a confirmation dialog opens asking the user to confirm removal of that link
- **AND** the dialog text includes the name displayed for that link

#### Scenario: User cancels the confirmation
- **WHEN** the confirmation dialog is open and the user chooses the dismissive option
- **THEN** the dialog is dismissed
- **AND** no deletion request is sent to the server
- **AND** the `PUBLIC LINKS` list is left unchanged

#### Scenario: User confirms the deletion
- **WHEN** the confirmation dialog is open and the user chooses the affirmative option
- **THEN** a deletion request is sent to the server for that link

### Requirement: Deletion request targets the selected link on the shared item

On confirmation, the system SHALL send a `DELETE` request with an empty body for the selected link, addressed to the permission resource of the shared item within the item's drive. When the shared item is an item inside the drive, the request SHALL address that item's permissions resource. When the shared item is the drive root, the request SHALL address the root's permissions resource. A response carrying no content SHALL be treated as success.

#### Scenario: Deleting a link on a file or folder inside a drive
- **WHEN** the user confirms deletion of a link on a file or folder identified by an item identifier within a drive
- **THEN** the system sends `DELETE /graph/v1beta1/drives/<drive-id>/items/<item-id>/permissions/<link-id>` with an empty body

#### Scenario: Deleting a link on the drive root
- **WHEN** the user confirms deletion of a link on a shared item that has no item identifier because it is the drive root
- **THEN** the system sends `DELETE /graph/v1beta1/drives/<drive-id>/root/permissions/<link-id>` with an empty body

#### Scenario: No-content response is a success
- **WHEN** the server answers the deletion request with a no-content response
- **THEN** the system treats the deletion as successful

### Requirement: Refresh the public links list after a successful deletion

After a deletion succeeds, the system SHALL re-fetch the shared item's permissions and re-render the `PUBLIC LINKS` section so the removed link no longer appears, and SHALL inform the user that the link was removed. The deletion SHALL affect only the targeted link: every other public link and every member share on the item SHALL remain listed.

#### Scenario: Removed link disappears from the list
- **WHEN** a deletion request for a link succeeds
- **THEN** the `PUBLIC LINKS` section is re-rendered from freshly fetched data
- **AND** the removed link is no longer listed
- **AND** the user is shown a confirmation that the public link was removed

#### Scenario: Other links and member shares are untouched
- **WHEN** an item has several public links and at least one member share, and one public link is deleted successfully
- **THEN** the remaining public links are still listed
- **AND** the member shares are still listed unchanged

#### Scenario: Last link removed
- **WHEN** the only public link on the item is deleted successfully
- **THEN** the `PUBLIC LINKS` section shows no link entries and presents its empty-state message

### Requirement: Failed deletion keeps the link in the list

When a deletion request fails because of a network or server error, the system SHALL show the user an error message indicating that the public link could not be removed, and the targeted link SHALL remain listed in the `PUBLIC LINKS` section.

#### Scenario: Network error during deletion
- **WHEN** the user confirms a deletion and the request fails because the server is unreachable
- **THEN** an error message stating that the public link could not be removed is shown
- **AND** the targeted link is still listed in the `PUBLIC LINKS` section

#### Scenario: Server rejects the deletion
- **WHEN** the user confirms a deletion and the server answers with an error response
- **THEN** an error message stating that the public link could not be removed is shown
- **AND** the targeted link is still listed in the `PUBLIC LINKS` section
