## Purpose

Defines the `Shared with me` list on spaces-enabled accounts: the resources other users have shared with the current user, sourced from the server's own list of incoming shares, together with the sharing context shown for each entry, the operations available on it, and the matching folder exposed to other apps through the Documents Provider.

## ADDED Requirements

### Requirement: Shared-with-me entries come from the server's incoming-shares list

On a spaces-enabled account, opening the `Shares` tab SHALL request the current account's incoming shares from the Graph endpoint `GET /graph/v1beta1/me/drive/sharedWithMe`. The request SHALL carry no query parameters, and the full list SHALL be read from the response's `value` array; the system SHALL NOT paginate. The list SHALL NOT be sourced from a WebDAV listing of the Shares jail.

#### Scenario: Opening the Shares tab on a spaces-enabled account
- **WHEN** a user selects the `Shares` tab on a spaces-enabled account
- **THEN** the system requests `GET /graph/v1beta1/me/drive/sharedWithMe` for that account
- **AND** the list is built from the entries in the response's `value` array

#### Scenario: Shares jail root has never been cached
- **WHEN** a user selects the `Shares` tab on a spaces-enabled account that has never cached the Shares jail root folder
- **THEN** the system still requests the incoming shares from the server
- **AND** the list is populated from the response rather than showing a static empty view

#### Scenario: Returning to the tab re-queries the server
- **WHEN** a user leaves the `Shares` tab and selects it again
- **THEN** the system requests the incoming shares from the server again

### Requirement: Hidden shares are excluded

An entry whose `@UI.Hidden` is `true` SHALL NOT appear in the `Shared with me` list. An entry whose `@UI.Hidden` is `false` or absent SHALL be treated as visible.

#### Scenario: Hidden entry in the response
- **WHEN** the response contains an entry whose `@UI.Hidden` is `true`
- **THEN** that entry is not listed

#### Scenario: Visible entry in the response
- **WHEN** the response contains an entry whose `@UI.Hidden` is `false`
- **THEN** that entry is listed

#### Scenario: Entry without the hidden flag
- **WHEN** the response contains an entry that carries no `@UI.Hidden` value
- **THEN** that entry is listed

### Requirement: Each entry shows its resource details and who shared it

Every listed entry SHALL show the shared resource's name, its size and the display name of the user who shared it. No role, permission or sharing-role information SHALL be rendered on a list entry.

#### Scenario: Entry shows name, size and sharer
- **WHEN** an entry for a resource shared by another user is listed
- **THEN** the entry shows the resource name
- **AND** the entry shows the resource size
- **AND** the entry shows the display name of the user who shared it

#### Scenario: No role information on entries
- **WHEN** an entry whose share grants a specific role is listed
- **THEN** the entry renders no role or permission text

### Requirement: Synchronized entries carry a read-only sync badge

An entry whose `@client.synchronize` is `true` SHALL show a sync badge, placed next to the sharing badge and to the left of the item size. An entry whose `@client.synchronize` is `false` or absent SHALL show no sync badge. The badge SHALL be purely informative: it SHALL NOT be tappable, and it SHALL NOT reflect or alter the item's available-offline state, which keeps its own indicator. The badge SHALL carry a content description so assistive technologies announce it.

#### Scenario: Entry marked as synchronized
- **WHEN** an entry whose `@client.synchronize` is `true` is listed
- **THEN** the entry shows the sync badge next to the sharing badge, to the left of the item size

#### Scenario: Entry not marked as synchronized
- **WHEN** an entry whose `@client.synchronize` is `false` is listed
- **THEN** the entry shows no sync badge

#### Scenario: Entry without the synchronize flag
- **WHEN** an entry that carries no `@client.synchronize` value is listed
- **THEN** the entry shows no sync badge

#### Scenario: Badge is not a control
- **WHEN** a user taps the sync badge on an entry
- **THEN** nothing happens
- **AND** the item's available-offline state is unchanged

#### Scenario: Badge is announced by assistive technology
- **WHEN** an entry showing the sync badge is reached with a screen reader
- **THEN** the badge is announced through its content description

#### Scenario: Available-offline state is reported separately
- **WHEN** an entry whose `@client.synchronize` is `false` has been set as available offline
- **THEN** the entry shows the available-offline indicator
- **AND** the entry shows no sync badge

### Requirement: Entries stay fully operable

A listed folder SHALL be browseable and a listed file SHALL be downloadable and previewable, exactly as from the previous Shares listing. Both files and folders SHALL remain settable as available offline. The `Share` option SHALL stay available on a listed entry so its permalink remains reachable.

#### Scenario: Browsing into a listed folder
- **WHEN** a user taps a listed folder
- **THEN** the system opens that folder's contents

#### Scenario: Opening a listed file
- **WHEN** a user taps a listed file
- **THEN** the system downloads and previews the file as it does elsewhere in the app

#### Scenario: Setting a listed file as available offline
- **WHEN** a user sets a listed file as available offline
- **THEN** the file is marked available offline and kept in sync

#### Scenario: Setting a listed folder as available offline
- **WHEN** a user sets a listed folder as available offline
- **THEN** the folder is marked available offline and kept in sync

#### Scenario: Share option reachable from the list
- **WHEN** a user opens the options for a listed entry
- **THEN** the `Share` option is offered

### Requirement: Empty, loading and error states

The `No shares` empty-dataset view SHALL be shown only when the server's response contains no visible share. Pull to refresh SHALL re-query the endpoint. When the request fails because of a network or server error, the system SHALL show an error message and SHALL keep the previously loaded list rather than clearing it or showing the empty-dataset view.

#### Scenario: Server returns no visible share
- **WHEN** the response contains no entry, or only entries whose `@UI.Hidden` is `true`
- **THEN** the `No shares` empty-dataset view is shown

#### Scenario: Server returns visible shares
- **WHEN** the response contains at least one visible entry
- **THEN** the entries are listed
- **AND** the empty-dataset view is not shown

#### Scenario: Pull to refresh
- **WHEN** a user pulls down on the list
- **THEN** the system requests the incoming shares from the server again
- **AND** the list is rebuilt from the new response

#### Scenario: Request fails with a list already on screen
- **WHEN** the request fails with a network or server error and entries are already listed
- **THEN** an error message is shown
- **AND** the previously listed entries remain on screen

#### Scenario: Request fails with no list yet
- **WHEN** the request fails with a network or server error and no entry has ever been listed
- **THEN** an error message is shown
- **AND** the empty-dataset view is not presented as the server's answer

### Requirement: The share panel offers only the permalink for an item shared with the user

When the share panel is opened for an item that was shared with the current user, the `Share with people` and `Public links` sections SHALL both be hidden and the permalink affordance in the panel header SHALL remain visible and functional. For an item not shared with the current user, both sections SHALL keep their existing visibility rules.

#### Scenario: Share panel for an item shared with the user
- **WHEN** a user opens the share panel for an item shared with them
- **THEN** the `Share with people` section is hidden
- **AND** the `Public links` section is hidden
- **AND** the permalink affordance in the panel header is shown and usable

#### Scenario: Share panel for the user's own item
- **WHEN** a user opens the share panel for an item of their own
- **THEN** the `Share with people` and `Public links` sections are shown according to their existing rules

### Requirement: The Documents Provider Shares folder lists the same resources

The `Shares` folder SHALL keep appearing at the account root in the Documents Provider for a spaces-enabled account and SHALL open without error whether or not the user has previously visited the `Shares` tab. Its children SHALL be the same visible shares as the in-app `Shared with me` list: a share whose `@UI.Hidden` is `true` SHALL NOT be listed there either. Opening, downloading and uploading inside that folder SHALL keep working as before, and create, rename and delete SHALL stay gated exactly as before.

#### Scenario: Shares folder at the account root without a prior tab visit
- **WHEN** another app browses the account root through the Documents Provider on a spaces-enabled account whose `Shares` tab has never been opened
- **THEN** the `Shares` folder is listed
- **AND** opening it succeeds and lists its children

#### Scenario: Hidden shares excluded from the provider folder
- **WHEN** another app lists the children of the `Shares` folder and the account has a share whose `@UI.Hidden` is `true`
- **THEN** that share is not among the listed children

#### Scenario: Provider folder children match the in-app list
- **WHEN** another app lists the children of the `Shares` folder
- **THEN** the children are the same set of resources as the in-app `Shared with me` list

#### Scenario: Operations inside the provider folder unchanged
- **WHEN** another app opens, downloads or uploads a resource inside the `Shares` folder
- **THEN** the operation behaves as it did before this change
- **AND** create, rename and delete remain gated as they were before

### Requirement: Legacy accounts are unaffected

On an account without spaces support the incoming-shares endpoint SHALL NOT be requested, and both the `Shares` tab and the Documents Provider SHALL keep their current behaviour.

#### Scenario: Shares tab on a legacy account
- **WHEN** a user selects the `Shares` tab on an account without spaces support
- **THEN** no request is made to the incoming-shares endpoint
- **AND** the tab shows the same list it showed before this change

#### Scenario: Documents Provider on a legacy account
- **WHEN** another app browses an account without spaces support through the Documents Provider
- **THEN** the account root exposes the same folders it did before this change
