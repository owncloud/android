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