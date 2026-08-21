## Purpose

Defines the required UI state of the passcode screen during and after the brute-force protection countdown, ensuring the keyboard is inaccessible while locked out and the screen is clean when the user is allowed to try again.

## ADDED Requirements

### Requirement: Keyboard hidden during brute-force countdown

The number keyboard SHALL be invisible while the brute-force protection countdown is active so that users cannot interact with it.

#### Scenario: Keyboard hides when countdown starts

- **WHEN** the user enters 3 or more consecutive wrong passcodes and the countdown timer becomes active
- **THEN** the number keyboard is no longer visible on screen
- **AND** tapping the area where the keyboard was has no effect

#### Scenario: Keyboard restores when countdown ends

- **WHEN** the brute-force countdown timer reaches zero
- **THEN** the number keyboard becomes visible again
- **AND** the user can enter a new passcode attempt

### Requirement: Error message cleared after countdown ends

The wrong-passcode error message SHALL be cleared when the brute-force countdown ends so the screen is in a clean state when the user is allowed to try again.

#### Scenario: Error message gone after countdown finishes

- **WHEN** the brute-force countdown timer reaches zero
- **THEN** the error message is no longer visible on screen
- **AND** the passcode entry screen is ready for a fresh attempt with no residual error text
