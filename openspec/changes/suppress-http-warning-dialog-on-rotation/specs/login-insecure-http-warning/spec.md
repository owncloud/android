## Purpose

Defines the user-facing behavior of the insecure-HTTP warning shown on the login screen when the user enters a non-TLS server URL, including when it is presented, when it is suppressed, and how it reacts to configuration changes and URL edits during the current login session.

## ADDED Requirements

### Requirement: Insecure-HTTP warning is shown when a non-TLS server URL is checked

When the user submits a server URL that resolves to a non-TLS (`http://`) connection on the login screen, the app SHALL present an insecure-connection warning dialog with a **Continue** action and a **Cancel** action before allowing the login flow to proceed to credential entry.

#### Scenario: First check of an insecure server URL

- **WHEN** the user enters an `http://` server URL on the login screen and the app finishes verifying the server
- **THEN** the insecure-connection warning dialog is shown
- **AND** the user can tap **Continue** to proceed or **Cancel** to abort the login flow

#### Scenario: Continue proceeds to the credentials step

- **WHEN** the insecure-connection warning dialog is shown for the first time in the login session
- **AND** the user taps **Continue**
- **THEN** the login flow proceeds to detect the server type and, when applicable, reveal the credentials fields

#### Scenario: Cancel keeps the credentials fields hidden

- **WHEN** the insecure-connection warning dialog is shown
- **AND** the user taps **Cancel**
- **THEN** the credentials fields remain hidden and the login flow does not advance

### Requirement: Insecure-HTTP warning is suppressed after acceptance for the same URL

Once the user has tapped **Continue** on the insecure-connection warning for a given server URL in the current login session, the dialog SHALL NOT be re-presented while that same URL remains the target of the login flow. This suppression MUST survive configuration changes (device rotation, dark-mode toggle, font-scale change) and any re-emission of the same server-check result.

#### Scenario: Dialog does not reappear after device rotation

- **WHEN** the user has accepted the insecure-connection warning for an `http://` server URL
- **AND** the user then rotates the device
- **THEN** the login screen is re-drawn without showing the insecure-connection warning dialog again
- **AND** the user's progress in the login flow is preserved

#### Scenario: Dialog does not reappear after other configuration changes

- **WHEN** the user has accepted the insecure-connection warning for an `http://` server URL
- **AND** a non-rotation configuration change occurs (for example the system dark-mode setting changes, or the font scale changes)
- **THEN** the insecure-connection warning dialog is not shown again for the same URL

#### Scenario: Acceptance is per login session

- **WHEN** the user has accepted the insecure-connection warning for an `http://` server URL and then leaves the login screen
- **AND** the user later opens the login screen again from scratch
- **THEN** the insecure-connection warning dialog is shown again the first time the same URL is re-checked

### Requirement: Editing the server URL invalidates a prior acceptance

If the user changes the server URL text after accepting the insecure-connection warning, the acceptance SHALL be discarded for the new URL, so a subsequent non-TLS server check triggers the warning dialog again.

#### Scenario: Editing the URL then re-checking shows the warning again

- **WHEN** the user has accepted the insecure-connection warning for `http://server-a.example`
- **AND** the user edits the URL field to `http://server-b.example`
- **AND** the app verifies the new server and returns a non-TLS result
- **THEN** the insecure-connection warning dialog is shown again for the new URL

#### Scenario: Cancel does not record acceptance

- **WHEN** the user taps **Cancel** on the insecure-connection warning
- **AND** the app subsequently re-checks the same URL (for example after a rotation or by tapping the check-server control again)
- **THEN** the insecure-connection warning dialog is shown again