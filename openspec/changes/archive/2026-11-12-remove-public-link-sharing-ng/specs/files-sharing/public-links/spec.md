## ADDED Requirements

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
