## Purpose

Defines the on-device passcode entry flow and its brute-force protection UX, including how the numeric keypad, digit inputs, and lockout countdown are exposed to the user across failed-attempt scenarios.

## ADDED Requirements

### Requirement: Numeric keypad hidden during brute-force lockout

The passcode entry screen SHALL hide the on-screen numeric keypad for the entire duration of the brute-force lockout countdown, and SHALL restore it when the countdown finishes and passcode input is re-enabled. The keypad MUST NOT be visible or receive touch input while the countdown is displayed.

#### Scenario: Keypad hides when lockout begins after threshold of wrong attempts

- **WHEN** the user has entered wrong passcodes enough times to trigger the brute-force lockout and the countdown becomes visible
- **THEN** the on-screen numeric keypad is not visible on the passcode screen
- **AND** taps on the area previously occupied by the keypad do not enter digits

#### Scenario: Keypad hides on cold start while lockout is still active

- **WHEN** the app is launched or the passcode screen is re-opened while a previously started brute-force lockout still has time remaining
- **THEN** the countdown is displayed and the on-screen numeric keypad is not visible

#### Scenario: Keypad reappears when lockout ends

- **WHEN** the brute-force lockout countdown finishes
- **THEN** the on-screen numeric keypad becomes visible again
- **AND** the user can enter a new passcode attempt using the keypad

### Requirement: Passcode digit inputs remain disabled during lockout

While the brute-force lockout countdown is displayed, the passcode digit input fields SHALL remain disabled so no digits can be entered from any input path (on-screen keypad, hardware keyboard, or focus-based interaction).

#### Scenario: Hardware key presses do not enter digits during lockout

- **WHEN** the brute-force lockout countdown is on screen
- **AND** the user presses a numeric hardware key
- **THEN** no digit is added to the passcode field

### Requirement: "Incorrect passcode" error cleared when lockout ends

When the brute-force lockout countdown finishes, the "Incorrect passcode" error text SHALL be removed from the screen before the user makes the next attempt, so the passcode screen returns to a clean state.

#### Scenario: Error text disappears when countdown finishes

- **WHEN** the brute-force lockout countdown finishes and the user is able to make a new attempt
- **THEN** the "Incorrect passcode" error text is no longer visible on the passcode screen

#### Scenario: Error text reappears on the next wrong attempt

- **WHEN** the countdown has finished, the error text has been cleared, and the user then submits another wrong passcode
- **THEN** the "Incorrect passcode" error text is shown again for that attempt