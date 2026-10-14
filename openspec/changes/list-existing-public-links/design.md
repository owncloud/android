## Context

The Sharing NG flow (`Graph*`-prefixed classes under `owncloudApp/.../presentation/sharing/`) already fetches an item's permissions from the Graph API and parses them end-to-end. The whole data pipeline exists:

- Networking (`owncloudComLibrary`): `GetRemoteGraphSharesForFileOperation(spaceId, itemId)` calls `graph/v1beta1/drives/<spaceId>/items/<itemId>/permissions` and parses the response with Moshi into `PermissionsResponse` (`spaces/responses/SpacesResponse.kt`). `LinkInfoResponse` already exposes `@libre.graph.displayName`, `type`, `webUrl`.
- Data (`owncloudData`): `OCRemoteShareDataSource.getGraphShares(...)` maps `PermissionsResponse.toModel()`, already **splitting** the `value` array — entries with `grantedToV2 == null` become `OCLink`s (parsing displayName/type/webUrl/hasPassword/expiration) and populate `OCPermissions.links`.
- Domain (`owncloudDomain`): `GetGraphSharesAsyncUseCase(accountName, spaceId, itemId)` returns `OCPermissions`, whose model (`sharing/shares/model/OCPermissions.kt`) already carries `links: List<OCLink>`. `OCLink` (`links/model/OCLink.kt`) and `OCLinkType` (`view/edit/upload/createOnly/internal`) already exist.

The sharing panel (`GraphShareFragment` + `GraphShareViewModel`) inflates `members_fragment.xml`, the **same layout** used by the spaces members screen (`SpaceMembersFragment`, issue #4752). That layout already contains the full public-links section (`public_links_section`, `public_links_title`, `public_links_warning`, `public_links_recycler_view`, `no_public_links_message`). The spaces screen renders links into it via `SpaceLinksAdapter` (renamed to `PublicLinksAdapter` under a shared `presentation/links` package by this change — see Decision 2) (binds `List<OCLink>`, inflates `public_link_item.xml`, maps type via `OCLinkType.toStringResId()`).

What is missing is purely presentation wiring:
- `GraphShareViewModel`/`GraphShareFragment` read only `OCPermissions.roles` and `.members`; they ignore `.links`.
- The public-links section views that already exist in `GraphShareFragment`'s layout are never populated.
- The warning in `members_fragment.xml` hardcodes `share_warning_about_forwarding_space_public_links` ("...access to the space").

Relevant strings in `owncloudApp/src/main/res/values/strings.xml`: `share_via_link_section_title` ("Public links", header), `share_warning_about_forwarding_space_public_links` ("...the space.", current), `share_warning_about_forwarding_public_links` ("...the file/folder.", generic), `share_no_public_links` ("No public links created yet.").

Affected flavors: all three (`original` / `mdm` / `qa`) — this is shared presentation code.

## Goals / Non-Goals

**Goals:**
- Render all existing public links of the selected file/folder in the sharing panel's public-links section (name, permission, expiration if present, copy-link icon).
- Reuse the existing data pipeline (`GetGraphSharesAsyncUseCase` → `OCPermissions.links`) with no changes below the presentation layer.
- Reuse the spaces links row UI (`public_link_item.xml` + `SpaceLinksAdapter`) as the layout reference.
- Replace the warning text with one that references the item type (file vs folder).

**Non-Goals:**
- Creating, editing, or deleting public links (hide/disable those affordances in this flow).
- Displaying `hasPassword` status (kept in `OCLink`, not shown).
- Any change to networking, data, or domain layers (the pipeline already provides `links`).
- Classic WebDAV/OCS shares (`PublicShareDialogFragment` and friends are untouched).

## Decisions

**1. Expose `links` from `GraphShareViewModel` and render in `GraphShareFragment`.**
The ViewModel already receives the full `OCPermissions` via `_shares`. The Fragment's `observeShares()` currently reads only `.roles`/`.members`. Extend it to also pass `OCPermissions.links` to a links adapter bound to `public_links_recycler_view`, and toggle `no_public_links_message` / section visibility the way `SpaceMembersFragment.showOrHideEmptyView` / `showSpaceLinks()` already do. No new UseCase or ViewModel flow is required — the data rides on the existing shares fetch (which is already triggered on panel open), satisfying the "fetch on open" requirement with no extra network call.

**2. Rename the links row UI to a shared `PublicLinks…` package and reuse it.**
The spaces links row UI (`SpaceLinksAdapter` + `SpaceLinksDiffUtil`, with nested `SpaceLinksViewHolder` / `SpaceLinksAdapterListener`, previously under `presentation/spaces/links`) is renamed to `PublicLinksAdapter` + `PublicLinksDiffUtil` (`PublicLinksViewHolder` / `PublicLinksAdapterListener`; the `setSpaceLinks`/`spaceLinks` members become `setPublicLinks`/`publicLinks`) and moved to a shared top-level package `presentation/links`, since it is now used by both the spaces members screen and the Sharing NG panel. `GraphShareFragment` reuses `PublicLinksAdapter` read-only: it enables only the share-link (chain icon) action and suppresses edit/remove, which are out of scope. `SpaceMembersFragment` is updated to the new package and names; its behavior is unchanged. (`SpaceLinksViewModel`, part of the spaces add-link flow, is not used here and is left untouched.)

**3. Permission label mapping lives in the adapter/row binding.**
The link name is rendered directly from `link["@libre.graph.displayName"]` with no fallback (the server guarantees a non-empty name). The `OCLinkType` → label mapping (`view`→"Can view", `edit`→"Can edit", `createOnly`→"Secret file drop") reuses the existing `OCLinkType.toStringResId()` extension. Verify its mappings match the required labels; if `createOnly` does not already map to "Secret file drop", add/adjust the string mapping (not the API enum).

**4. Expiration row shown only when present.**
`OCLink.expirationDateTime` is nullable; the row binding shows the expiration row only when it is non-null, mirroring the existing `public_link_item.xml` behavior.

**5. Item-type-aware warning text.**
The acceptance criteria require the warning to read "...the file" or "...the folder" matching the selected item's type. The current layout hardcodes the "space" string. Replace the `public_links_warning` text at runtime in `GraphShareFragment` (not in the shared layout XML, so `SpaceMembersFragment` keeps its "space" wording). Introduce two strings — "Anyone with the link has access to the file" and "Anyone with the link has access to the folder" — and select based on `OCFile.isFolder`. The existing generic `share_warning_about_forwarding_public_links` ("...the file/folder.") is a weaker fit than the type-specific wording the acceptance criteria call for, so prefer the two type-specific strings. New strings are added only to the base `values/strings.xml`; translations go to Transifex (no translation PRs).

**6. drive-id / item-id source unchanged.**
`GraphShareViewModel.getGraphShares()` already derives drive-id = `file.spaceId` and item-id = `file.remoteId` from the `OCFile`, emitting `IncompleteFileDataException` if either is null. No change needed.

**Koin modules:** No new bindings expected (reuses existing ViewModel/UseCase/Repository/DataSource graph). If a dedicated `GraphLinksAdapter` is introduced it is a plain UI class, not a Koin dependency.

**Room:** No schema changes — Graph links are fetched live and never persisted.

## Risks / Trade-offs

- **Shared layout coupling:** `members_fragment.xml` is shared by the spaces members screen and the sharing panel. Changing the warning text in XML would affect both; mitigated by setting the warning text programmatically in `GraphShareFragment` only.
- **Shared adapter rename reaches the spaces screen:** renaming `SpaceLinks…`→`PublicLinks…` and moving the package means `SpaceMembersFragment` (issue #4752) must be updated to the new names; mitigated by a pure rename/move with no behavior change, verified by the spaces-screen regression test.
- **Permission label parity:** If `OCLinkType.toStringResId()` does not already yield exactly "Can view" / "Can edit" / "Secret file drop", the mapping must be reconciled; verify against the spaces screen to avoid regressing its labels.
- **`createOnly`/upload semantics:** The Graph API `createOnly` type must map to "Secret file drop"; confirm the existing enum/string mapping distinguishes `createOnly` from `upload` so the label is correct.