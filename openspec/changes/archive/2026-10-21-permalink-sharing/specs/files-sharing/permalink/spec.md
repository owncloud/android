## Purpose

Defines when the `Share` screen is reachable on oCIS and how the permalink (private link) button in the oCIS Share header is shown and acted upon, so that a user can always reach and copy an item's permalink while oC10 behavior stays unchanged.

## ADDED Requirements

### Requirement: Share screen reachable for a single item on oCIS

On an oCIS account, the `Share` menu option SHALL be offered for a single selected item regardless of the `resharing`/`sharing` capability values, the item's `R` (reshare) permission, and the `share_via_link_feature`/`share_with_users_feature` setup flags. Reachability of the `Share` screen SHALL NOT depend on any of those values on oCIS.

An account is treated as oCIS when its stored capabilities report the `spaces` capability as enabled; otherwise the account is treated as oC10.

#### Scenario: Single item in an oCIS personal space with resharing disabled
- **WHEN** an oCIS account has a single item selected in its personal space and the `resharing` capability is `false`
- **THEN** the `Share` menu option is offered

#### Scenario: Single item in an oCIS custom space with no reshare permission
- **WHEN** an oCIS account has a single item selected in a custom space, the item's permissions carry no `R` flag, and the `resharing` capability is `false`
- **THEN** the `Share` menu option is offered

#### Scenario: oCIS with both share feature flags disabled
- **WHEN** an oCIS account has a single item selected and both `share_via_link_feature` and `share_with_users_feature` are `false`
- **THEN** the `Share` menu option is offered

### Requirement: Share screen stays restricted in available-offline and multiselection on oCIS

On an oCIS account, the `Share` menu option SHALL NOT be offered in the available-offline shortcut, and SHALL NOT be offered when more than one item is selected.

#### Scenario: Available-offline shortcut on oCIS
- **WHEN** an oCIS account is in the available-offline shortcut
- **THEN** the `Share` menu option is not offered

#### Scenario: Multiselection on oCIS
- **WHEN** an oCIS account has more than one item selected
- **THEN** the `Share` menu option is not offered

### Requirement: oC10 Share reachability unchanged

On an oC10 account, the conditions under which the `Share` menu option is offered SHALL remain exactly as they were before this change: it is offered only when the selection is not the available-offline shortcut, at least one of the share-via-link / share-with-users features is enabled, resharing is allowed for the selection, and the single selected item carries the reshare permission.

#### Scenario: oC10 gate unchanged for an item shared with the user
- **WHEN** an oC10 account has a single item that is shared with the user, the `resharing` capability is `false`, and the item carries no reshare permission
- **THEN** the `Share` menu option is not offered, exactly as before this change

#### Scenario: oC10 gate unchanged when conditions are met
- **WHEN** an oC10 account has a single selected item that satisfies the pre-existing share-feature, resharing, and reshare-permission conditions outside the available-offline shortcut
- **THEN** the `Share` menu option is offered, exactly as before this change

### Requirement: Permalink button shown and functional in the oCIS Share header

On an oCIS account, the Share header SHALL display the permalink (private link) button when the selected item has a non-empty private link and the `filesPrivateLinks` capability is not `FALSE`. When the button is displayed and the user activates it, the system SHALL copy or send that item's private link, reusing the existing private-link copy/send action. When the user long-presses the button, the system SHALL show an informational message explaining what a private link is.

#### Scenario: Item with a private link and private links not disabled
- **WHEN** the oCIS Share header is shown for an item whose private link is non-empty and the `filesPrivateLinks` capability is not `FALSE`
- **THEN** the permalink button is visible
- **AND** activating it copies or sends that item's private link

#### Scenario: Long-pressing the permalink button
- **WHEN** the oCIS Share header is shown for an item whose private link is non-empty and the user long-presses the permalink button
- **THEN** an informational message explaining the private link is shown

#### Scenario: Private links capability disabled
- **WHEN** the oCIS Share header is shown and the `filesPrivateLinks` capability is `FALSE`
- **THEN** the permalink button is hidden

#### Scenario: Item without a private link
- **WHEN** the oCIS Share header is shown for an item whose private link is null or empty
- **THEN** the permalink button is hidden

### Requirement: Share screen section gating unchanged

The visibility of the "share with users" and "share by link" sections inside the Share screen SHALL continue to be governed by their existing capabilities and build-time feature flags, on both oCIS and oC10. This change SHALL NOT alter that section-level gating.

#### Scenario: Sections still governed by their own rules
- **WHEN** the Share screen is opened on either server type
- **THEN** the "share with users" and "share by link" sections are shown or hidden according to their existing capabilities and feature flags, unchanged by this change