## 1. Verify the existing data pipeline (no code changes expected)

- [x] 1.1 Confirm `OCPermissions.links: List<OCLink>` is populated from the permissions endpoint by inspecting `OCRemoteShareDataSource.getGraphShares(...)` / `PermissionsResponse.toModel()`; verify by adding a temporary log or a debugger breakpoint in `GraphShareViewModel` showing `links` is non-empty for an item that has links on the server.
- [x] 1.2 Confirm `OCLinkType.toStringResId()` maps `view`→"Can view", `edit`→"Can edit", `createOnly`→"Secret file drop"; verify by reading the extension and the referenced string resources. If `createOnly` does not map to "Secret file drop", note the gap for task 2.1.

## 2. Strings (owncloudApp)

- [x] 2.1 Add/verify string resources in `owncloudApp/src/main/res/values/strings.xml`: two item-type warnings — "Anyone with the link has access to the file" and "Anyone with the link has access to the folder" — and the three permission labels ("Can view", "Can edit", "Secret file drop"); verify `./gradlew lintDebug` passes with no missing-resource errors.

## 3. Render public links in the Sharing NG panel (owncloudApp)

- [x] 3.1 Expose `OCPermissions.links` to the UI in `GraphShareViewModel`/`GraphShareFragment.observeShares()` (currently only `.roles`/`.members` are read); verify by logging the links list reaches the Fragment when the panel opens.
- [x] 3.2 Refactor the shared links row UI: rename `SpaceLinksAdapter`→`PublicLinksAdapter` and `SpaceLinksDiffUtil`→`PublicLinksDiffUtil` (nested `PublicLinksViewHolder`/`PublicLinksAdapterListener`; `setSpaceLinks`/`spaceLinks`→`setPublicLinks`/`publicLinks`), move them to a new shared package `com.owncloud.android.presentation.links`, and update both `GraphShareFragment` and `SpaceMembersFragment` to the new names/package; verify the sharing panel shows one row per link (name, permission, expiration when present) and the spaces screen still renders links unchanged.
- [x] 3.3 Bind the adapter to `public_links_recycler_view` in the sharing panel and toggle `no_public_links_message` / section visibility (mirroring `SpaceMembersFragment.showOrHideEmptyView`/`showSpaceLinks`); verify an item with no links shows the empty message and an item with links shows the list.
- [x] 3.4 Render the link name directly from `link["@libre.graph.displayName"]` (no fallback — the server guarantees a non-empty name); verify a link renders with its display name.
- [x] 3.5 Show the expiration row only when `OCLink.expirationDateTime` is non-null; verify a link without expiration shows no expiration row and one with expiration shows the date.
- [x] 3.6 Wire the chain icon to share `OCLink.webUrl` through the Android share sheet; verify tapping the icon opens the share sheet offering the URL.
- [x] 3.7 Set the `public_links_warning` text programmatically in `GraphShareFragment` based on `OCFile.isFolder` (file → "...the file", folder → "...the folder"), leaving `SpaceMembersFragment`'s "space" wording unchanged; verify the file and folder sharing panels show the correct warning.
- [x] 3.8 Suppress out-of-scope affordances (create/edit/remove link) in this flow; verify no add/edit/delete link controls are shown or actionable in the Sharing NG public-links section.

## 4. Changelog

- [x] 4.1 Create `changelog/unreleased/XXXX` (placeholder PR number) with an `Enhancement` entry titled under 80 chars, body in present perfect passive tense, linking the issue (`https://github.com/owncloud/android/issues/4956`) and PR; verify the file follows the format in CLAUDE.md and `./gradlew` build is unaffected.

## 5. Static analysis & build

- [ ] 5.1 Run `./gradlew detekt ktlintCheck lintDebug` and `./gradlew testDebugUnitTest testMdmDebugUnitTest --continue`; verify all pass (detekt zero-tolerance). Note: no new unit tests are added, as this change touches only the presentation layer (owncloudApp), where unit tests are not established per project convention.

## List links over sharing NG

**Server(s)**: <br>
**Device(s)**: <br>
**Execution date**: <br>
**Tester**: <br>

| Test Case | Steps | Expected | Result | Comments |
|---|---|---|---|---|
| **Link list** | | | | |
| Links section visible | 1. Open the sharing panel for a file or folder | The `PUBLIC LINKS` section is shown with the warning text and either the link list or the empty state | | |
| No links — file | 1. Use a file with no public links<br>2. Open the sharing panel | `PUBLIC LINKS` section shows the warning "Anyone with the link has access to the file" and the message "No public links created yet." Check in both orientations | | |
| No links — folder | 1. Use a folder with no public links<br>2. Open the sharing panel | `PUBLIC LINKS` section shows the warning "Anyone with the link has access to the folder" and the message "No public links created yet." | | |
| Links listed — file | 1. Use a file with 2+ public links created from the web<br>2. Open the sharing panel | All links appear in the `PUBLIC LINKS` section, each with name, permission, and expiration date if set. Counts match the web. Check in both orientations | | |
| Links listed — folder | 1. Use a folder with 2+ public links created from the web<br>2. Open the sharing panel | All links appear in the `PUBLIC LINKS` section with correct details. Check in both orientations | | |
| Long link name | 1. Use an item with a link whose name is very long<br>2. Open the sharing panel | Long link name is displayed and ellipsized when it does not fit on one line. Check in both orientations | | |
| Permission label — view | 1. Use an item with a link of type `view`<br>2. Open the sharing panel | Link shows the label "Can view" | | |
| Permission label — edit | 1. Use an item with a link of type `edit`<br>2. Open the sharing panel | Link shows the label "Can edit" | | |
| Permission label — createOnly | 1. Use an item with a link of type `createOnly`<br>2. Open the sharing panel | Link shows the label "Secret file drop" | | |
| Expiration present | 1. Use an item with a link that has an expiration date<br>2. Open the sharing panel | The link's expiration date row is shown with the correct date | | |
| Expiration absent | 1. Use an item with a link that has no expiration date<br>2. Open the sharing panel | No expiration date row is shown for that link | | |
| Long list | 1. Use an item with 20+ public links<br>2. Open the sharing panel | All links are listed and the section scrolls correctly. Check in both orientations | | |
| Member shares excluded | 1. Use an item that has member shares but no public links<br>2. Open the sharing panel | No rows appear in the `PUBLIC LINKS` section; only the empty state is shown | | |
| Share link | 1. Use an item with a public link<br>2. Open the sharing panel<br>3. Tap the chain icon on any link<br>4. Share the URL and open it in a browser | Android share sheet opens offering the link URL; the URL resolves to the correct shared item | | |
| **External changes** | | | | |
| Add link | 1. Open the sharing panel in the app<br>2. From the web, add a new link to the item<br>3. Close and reopen the sharing panel | New link appears in the list. Newest link at the top | | |
| Edit link name | 1. Open the sharing panel in the app<br>2. From the web, change the name of a link<br>3. Close and reopen the sharing panel | Updated link name is shown | | |
| Edit permission | 1. Open the sharing panel in the app<br>2. From the web, change the permission of a link<br>3. Close and reopen the sharing panel | Updated permission label is shown | | |
| Add expiration date | 1. Open the sharing panel in the app<br>2. From the web, add an expiration date to a link that had none<br>3. Close and reopen the sharing panel | Expiration date row is now shown for that link | | |
| Change expiration date | 1. Open the sharing panel in the app<br>2. From the web, change the expiration date of an existing link<br>3. Close and reopen the sharing panel | Updated expiration date is shown | | |
| Remove expiration date | 1. Open the sharing panel in the app<br>2. From the web, remove the expiration date from a link<br>3. Close and reopen the sharing panel | Expiration date row is no longer shown for that link | | |
| Remove link | 1. Open the sharing panel in the app<br>2. From the web, delete one of the links<br>3. Close and reopen the sharing panel | Deleted link no longer appears in the list | | |
| Remove all links | 1. Open the sharing panel in the app<br>2. From the web, delete all public links<br>3. Close and reopen the sharing panel | `PUBLIC LINKS` section shows the empty state "No public links created yet." | | |
| **Errors** | | | | |
| No connection | 1. Disable the network on the device<br>2. Try to open the sharing panel for an item | The sharing panel does not open or shows an appropriate error; no crash | | |
| **Regression** | | | | |
| No create/edit/delete | 1. Open the sharing panel for any item<br>2. Inspect the `PUBLIC LINKS` section | No add, edit, or delete link controls are shown or actionable | | |
| Spaces screen | 1. Open the spaces screen<br>2. Open any space's members view | Links still render correctly; warning still reads "Anyone with the link has access to the space" | | |
| **Accessibility** | **One device** | | | |
| Talkback | 1. Enable Talkback on the device<br>2. Navigate through the `PUBLIC LINKS` section in the sharing panel | Every field (link name, permission, expiration, chain icon) is correctly announced | | |
| Keyboard | 1. Attach a physical keyboard via Bluetooth<br>2. Navigate through the `PUBLIC LINKS` section | Every element is reachable and no dead paths exist in navigation | | |

### Gherkin scenario (E2E suite proposal)

```gherkin
Feature: List existing public links in the Sharing NG panel

  Scenario: Existing public links are shown when opening the sharing panel
    Given a file that has an existing public link named "Report" of type "view" with an expiration date
    When I open the sharing panel for that file
    Then the PUBLIC LINKS section lists a link named "Report"
    And the link shows the permission "Can view"
    And the link shows its expiration date
    And the warning reads "Anyone with the link has access to the file"

  Scenario: Sharing a public link URL
    Given the sharing panel for a file lists a public link
    When I tap the chain icon on that link
    Then the Android share sheet opens offering the link URL

  Scenario: Folder with no public links
    Given a folder that has no public links
    When I open the sharing panel for that folder
    Then the PUBLIC LINKS section shows the empty message
    And the warning reads "Anyone with the link has access to the folder"
```