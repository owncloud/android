## MODIFIED Requirements

### Requirement: Share screen section gating unchanged

The visibility of the "share with users" and "share by link" sections inside the Share screen SHALL continue to be governed by their existing capabilities and build-time feature flags, on both oCIS and oC10, with one exception: for an item that was shared with the current user, both sections SHALL be hidden regardless of those capabilities and flags, leaving the header permalink as the only sharing affordance. The permalink affordance SHALL keep its own visibility rules in every case.

#### Scenario: Sections still governed by their own rules
- **WHEN** the Share screen is opened on either server type for an item that was not shared with the current user
- **THEN** the "share with users" and "share by link" sections are shown or hidden according to their existing capabilities and feature flags, unchanged by this change

#### Scenario: Item shared with the current user
- **WHEN** the Share screen is opened for an item that was shared with the current user
- **THEN** the "share with users" section is hidden
- **AND** the "share by link" section is hidden
- **AND** the header permalink affordance keeps its own visibility rules
