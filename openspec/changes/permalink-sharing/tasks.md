## 1. oCIS Share reachability (owncloudApp — FilterFileMenuOptionsUseCase)

- [x] 1.1 In `FilterFileMenuOptionsUseCase.run()`, derive `val isOcis = capability?.isSpacesAllowed() == true` from the already-fetched `capability`; verify by reading the file that `isOcis` is computed before the `SHARE` block and reuses the existing `capability` lookup (no new repository call).
- [x] 1.2 Replace the `SHARE` gate (`FilterFileMenuOptionsUseCase.kt:90-91`) with a server-type branch: oCIS → `!onlyAvailableOfflineFiles && isSingleSelection(files)`, oC10 → the existing `!onlyAvailableOfflineFiles && shareViaLinkOrWithUsersAllowed && resharingAllowed && hasResharePermission`; verify by reading the diff that the oC10 branch is byte-for-byte the previous condition.
- [x] 1.3 Confirm no symbol became unused: `resharingAllowed`, `hasResharePermission`, `anyFileSharedWithMe`, `shareViaLinkOrWithUsersAllowed`, the `capability` lookup, `capabilityRepository`, and the `shareViaLinkAllowed`/`shareWithUsersAllowed` `Params` fields are all still referenced by the oC10 branch; verify with `./gradlew :owncloudApp:detektOriginalDebug` (or `./gradlew detekt`) reporting zero issues.
- [x] 1.4 Confirm no caller or DI change is required — `FilterFileMenuOptionsUseCase.Params` and the `UseCaseModule` factory are unchanged; verify by grepping callers (`MainFileListViewModel`, `FileDetailsViewModel`, `PreviewVideoViewModel`, `PreviewAudioViewModel`, `PreviewTextViewModel`, `PreviewImageViewModel`) still compile against the same `Params` and that `./gradlew :owncloudApp:assembleOriginalDebug` succeeds.

## 2. oCIS permalink button (owncloudApp — ShareActivity)

- [x] 2.1 Expose the stored `filesPrivateLinks` capability to `ShareActivity` — reuse an existing `CapabilityViewModel` accessor if one returns the `OCCapability`, otherwise add a thin read-only getter on `CapabilityViewModel` backed by `GetStoredCapabilitiesUseCase` (mirroring `isOcisServer()`); verify by reading that `ShareActivity.setupFileHeader()` can read `filesPrivateLinks` without querying Room directly.
- [x] 2.2 In `ShareActivity.setupFileHeader()`, replace the hard-coded `binding.permanentLinkButton.isVisible = false` (`ShareActivity.kt:172`) with the oC10 rule: hide when `file.privateLink.isNullOrEmpty()` or `filesPrivateLinks == CapabilityBooleanType.FALSE`, otherwise show the button; verify by reading that the visibility logic matches `OcsShareFileFragment.showOrHidePrivateLink()`.
- [x] 2.3 When the button is shown, wire `binding.permanentLinkButton.setOnClickListener { copyOrSendPrivateLink(file) }` (reference `SpaceMembersActivity.kt:74`); verify manually on an oCIS account that tapping the permalink button in the Share header copies/sends the item's private link.
- [x] 2.4 Manual check — hidden states: on oCIS, open the Share header for an item with no private link and for an account where `filesPrivateLinks` is `FALSE`; verify the permalink button is not shown in either case.

## 3. Release notes

- [ ] 3.1 Draft the user-facing release notes entry summarising the oCIS Share/permalink change for end users.

## 4. Changelog

- [ ] 4.1 Add `changelog/unreleased/<PR-number>` with a `Bugfix` entry (title under 80 chars, present-perfect-passive body) linking `https://github.com/owncloud/android/issues/4939` and the PR URL; verify the file matches the CLAUDE.md changelog format.

## 5. Final verification

- [ ] 5.1 Run `./gradlew detekt ktlintCheck lintDebug` and `./gradlew testDebugUnitTest testMdmDebugUnitTest --continue`; verify all pass (detekt zero-tolerance). Note: no new unit tests are added, as this change touches only the presentation layer (owncloudApp), where unit tests are not established per project convention; the oCIS reachability and permalink behavior are covered by the manual QA cases below.

## Permalink sharing reachability

**Server(s)**: <br>
**Device(s)**: <br>
**Execution date**: <br>
**Tester**: <br>

| Test Case | Steps | Expected | Result | Comments |
|---|---|---|---|---|
| **Share reachability (oCIS)** | | | | |
| Share reachable — personal space | 1. On an oCIS account, open the file list for the personal space<br>2. Open the file menu for a single item | The `Share` option is shown even when `resharing` is `false` and the item has no `R` permission | | |
| Share reachable — custom space | 1. On an oCIS account, open a custom space with items whose `oc:permissions` is empty<br>2. Open the file menu for a single item | The `Share` option is shown | | |
| Share hidden — available offline | 1. On an oCIS account, open the available-offline shortcut<br>2. Open the file menu for a single item | The `Share` option is not shown | | |
| Share hidden — multiselection | 1. On an oCIS account, select two or more items<br>2. Open the multiselection menu | The `Share` option is not shown | | |
| **Permalink button (oCIS)** | | | | |
| Button shown | 1. On an oCIS account where `filesPrivateLinks` is not `FALSE`<br>2. Open the Share screen for an item that has a private link | The permalink (permanent link) button is shown in the Share header | | |
| Copy/send permalink | 1. Open the Share header for an item with a private link<br>2. Tap the permalink button | The item's private link is copied/sent (same behavior as oC10 `copyOrSendPrivateLink`) | | |
| Button hidden — no private link | 1. Open the Share header for an item with no private link | The permalink button is not shown | | |
| Button hidden — capability FALSE | 1. Use an account where `filesPrivateLinks` is `FALSE`<br>2. Open the Share header for any item | The permalink button is not shown | | |
| **Regression (oC10)** | | | | |
| Share gate unchanged | 1. On an oC10 account, open the file menu for various items (shared-with-me, no `R`, resharing off/on) | The `Share` option visibility is identical to the current behavior (no observable difference) | | |
| Permalink unchanged | 1. On an oC10 account, open the Share screen for an item with/without a private link | The permalink button shows/hides exactly as before | | |
| **Accessibility** | **One device** | | | |
| Talkback | 1. Enable Talkback<br>2. Navigate to the permalink button in the Share header | The permalink button is correctly announced and actionable | | |
| Keyboard | 1. Attach a physical keyboard via Bluetooth<br>2. Navigate to the permalink button | The button is reachable and actionable | | |

### Gherkin scenario (E2E suite proposal)

```gherkin
Feature: Permalink sharing reachability on oCIS

  Scenario: Share option is reachable for a single item on oCIS
    Given an oCIS account where the "resharing" capability is false
    And a single item whose "oc:permissions" has no "R" permission
    When I open the file menu for that item
    Then the "Share" option is shown

  Scenario: Share option is hidden for multiselection on oCIS
    Given an oCIS account
    When I select two or more items and open the menu
    Then the "Share" option is not shown

  Scenario: Permalink button copies the private link on oCIS
    Given an oCIS account where the "filesPrivateLinks" capability is not FALSE
    And an item that has a private link
    When I open the Share header for that item
    Then the permalink button is shown
    And tapping the permalink button copies or sends the item's private link

  Scenario: Permalink button is hidden when the item has no private link
    Given an oCIS account
    And an item that has no private link
    When I open the Share header for that item
    Then the permalink button is not shown

  Scenario: oC10 Share gate is unchanged
    Given an oC10 account
    When I open the file menu for items with varying resharing/permission state
    Then the "Share" option visibility is identical to the previous behavior
```