## Context

See `proposal.md` — Why. Design-relevant current state:

- `FileDisplayActivity.navigateTo(FileListOption.SHARED_BY_LINK)` (`FileDisplayActivity.kt:1833-1848`) forks on `storageManager.getRootSharesFolder()`: when it is `null` and the personal folder has a `spaceId`, it shows `SharesFragment`; otherwise it mounts `MainFileListFragment` rooted at the jail root. `SharesFragment` is a placeholder that only renders the `item_empty_dataset` include, and it is referenced from nowhere else (`FileDisplayActivity.kt:112` and `:432`).
- `MainFileListViewModel.retrieveFlowForShareByLink()` (`MainFileListViewModel.kt:424`) already forks on `remotePath == ROOT_PATH && spaceId == null`, which is exactly the legacy-versus-spaces boundary this change needs.
- `MainFileListFragment.showOrHideEmptyView()` (`:984-1001`) **already** renders the `No shares` / `shares_list_empty_subtitle` empty-dataset view for `isSharedByLink() && space != null`, under the comment "Temporary solution for shares space".
- `FileDisplayActivity`'s back handler (`:782-792`) never reaches `manageBrowseUp()` at a root folder: for `parentId == ROOT_PARENT_ID` it goes to the spaces list for a project space and calls `finish()` otherwise — the comment names "personal or shares" explicitly. Below the root it delegates to `onBrowseUp()`.
- `FileListAdapter.onBindViewHolder()` (`:200-205`) binds `share_icons_layout` and its two 15dp `ImageView`s generically through `itemView.findViewById`, for every non-footer view type. `item_file_list.xml:69` and `grid_item.xml:45` both carry that row; only `item_file_list.xml` carries the `space_path_line` include (`:133`), which `setSpecificViewHolder()` (`:293`) fills with the parent path for the available-offline and legacy shared-by-link lists.
- `FileRepository.getSharesRootFolderForAccount()` resolves the jail root through `sharesSpace.root.id`, while `DocumentsStorageProvider.queryChildDocuments()` resolves it through `sharesSpace.id` (`DocumentsStorageProvider.kt:229-244`). `SpacesDao` keys both the space row and `getWebDavUrlForSpace()` by `space.id`, and `OCFileRepository.refreshFolder()` stamps `OCFile.spaceId` with the value it is given — so `space.id` is the canonical `OCFile.spaceId` and the two surfaces currently key the jail root differently.
- `OCLocalFileDataSource.getFileByRemotePath()` creates and returns the row when `remotePath == ROOT_PATH` and it is missing, so resolving the jail root never fails as long as the Shares space row exists. The Documents Provider therefore has no dependency on the in-app list having run first; `syncSpacesWithServer()` already keeps the space row fresh.
- `FileCursor.addFile()` already strips delete/rename/copy/move/write/create flags for `spaceId == SPACE_ID_SHARES`, so the provider's Shares folder is already read-only.
- `GetRemoteGraphSharesForFileOperation` + `ShareService.getGraphShares()` + `OCRemoteShareDataSource.getGraphShares()` + `OCShareRepository.getGraphShares()` + `GetGraphSharesAsyncUseCase` is the established Graph `GET` chain to copy.
- `FilterFileMenuOptionsUseCase` already takes an `onlySharedByLinkFiles` flag, and the single-file options bottom sheet lives inside `MainFileListFragment.observeMenuOptionsSingleFile()` — reachable from this change because the list stays inside that fragment.
- The two `sharedByMe` lists the `Shares` tab will eventually host are expected to be *shortcut* lists: a flat listing whose rows navigate to the resource's real location. That is the shape `FileListOption.SHARED_BY_LINK` and `FileListOption.AV_OFFLINE` already have today, down to `space_path_line` showing where the row really lives.

## Goals / Non-Goals

**Goals:**

- One source of truth for which resources are shared with the user — the Graph `sharedWithMe` payload — consumed by both the in-app list and the Documents Provider's `Shares` folder, under one matching rule and one failure rule.
- Entries that remain ordinary `OCFile`s underneath, so browsing, download, preview and available-offline keep running through the existing file machinery with no parallel implementation.
- The smallest presentation delta that delivers the three missing pieces (hidden-share filtering, sharer, sync badge): the tab keeps the file list it already mounts and gains a filter, one rendered field and one badge.

**Non-Goals:**

- Persisting sharer or synchronize state in Room. The payload is read per load; no entity or migration is added.
- A dedicated list surface for the `Shares` tab. Explicitly retired — see D1.
- Grid-mode parity for the sharer. The badge reaches grid for free (D5); the sharer does not, because `grid_item.xml` has no slot for it.
- Splitting `FileListOption` into one value per list. Deferred — see D3.
- Sorting, search, grid and multiselection in the `Shares` tab. They arrive as a side effect of the reuse rather than as designed scope; no requirement is written for them, and if design wants them constrained that is its own issue.

## Decisions

### D1. The Shares jail listing drives the list; the Graph payload filters and enriches it

The `Shares` tab keeps mounting `MainFileListFragment` rooted at the Shares jail root exactly as it does today, through `storageManager.getRootSharesFolder()`. That call resolves the root with `getFileByRemotePath(ROOT_PATH, owner, sharesSpace.root.id)`, which creates the `OCFile` row on demand when it is missing, and for the Shares jail `sharesSpace.root.id` holds the same value as `sharesSpace.id` — `SPACE_ID_SHARES`. So the mounted root carries `spaceId == SPACE_ID_SHARES`, the D5 condition matches, and the Documents Provider — which resolves the same root through `sharesSpace.id` — keys it identically.

`MainFileListViewModel.retrieveFlowForShareByLink()` gains a second branch, for `remotePath == ROOT_PATH && spaceId == SPACE_ID_SHARES`:

```
combine(
    retrieveFlowForAllFiles(currentFolderDisplayed, accountName),  // the jail content, live
    sharedWithMeEntries,                                           // nullable: null means unavailable (D2)
) { jailContent, entries ->
    if (entries == null) jailContent else jailContent.filterAndEnrich(entries)
}
```

Reusing `retrieveFlowForAllFiles()` rather than calling `getFolderContentAsStreamUseCase()` directly keeps the jail content path identical to every other folder.

`filterAndEnrich` keeps only the children that match a visible entry and copies that entry onto the row (D5). The matching entry is resolved **once** and only then acted on — `remoteId == entry.id`, falling back to `fileName == entry.name`, then enrich when non-null and drop otherwise — the same two rules, in the same order, with the same two-step shape that D8 applies in the Documents Provider. Resolving once rather than repeating the action per branch is what keeps the two surfaces from drifting. The existing branch (`spaceId == null`) is untouched, so legacy accounts cannot reach any of this.

`sharedWithMeEntries` is refreshed by a public `refreshSharedWithMe()` on the ViewModel. It is **not** called from `init` — see D2 for why the trigger lives on the tab-open path instead — and both of its call sites are guarded by the single list condition of D5, because the ViewModel and the refresh listener are shared by every file list and by `FolderPickerActivity`. The `@UI.Hidden` exclusion stays inside `GetSharedWithMeAsyncUseCase`, so the in-app list and the provider cannot drift apart; the domain model still carries the flag so the decision stays inspectable.

The top-level `combine` in `fileListUiState` (`:132`) already takes five flows, which is the last typed `combine` overload — so the entries flow is combined inside this branch, which is where it belongs anyway.

*Alternative rejected:* the Graph payload as the source of truth, with `OCFile` rows joined onto it and rendered by a dedicated ViewModel, adapter, item layout and fragment. That buys Graph ownership of membership and ordering, and it costs re-implementing, inside a new surface, what `MainFileListFragment` already does: thumbnails, the local-state pin, the three-dot bottom sheet and its option filtering, selection and diffing, swipe-to-refresh, the empty state, plus a new error channel in `FileListUiState`. It was chosen originally to also serve the two `sharedByMe` lists; now that those are expected to be shortcut lists over real paths — the shape the existing file list already has — the dedicated surface would serve exactly one of the three lists, and the cost stops being amortised.

Two consequences are accepted deliberately:

- **Membership is the intersection of the jail and Graph.** A share the server reports but the jail has not listed yet appears only after the next refresh; a jail child Graph does not report as visible is dropped. Dropping is what makes the `@UI.Hidden` exclusion work at all — a hidden share's jail child matches no visible entry — so it cannot be softened into "list the row un-enriched" without losing that requirement. The cost is that a match failure removes a row rather than merely stripping its sharer and badge; see the match-key risk below.
- **Ordering follows the user's sort preference** rather than the response's array order, because the branch feeds the existing `sortList()` path. This is a gain, not a loss.

### D2. A failed Graph request degrades to the unfiltered jail, with no error surface

`FileListUiState` has only `Loading` and `Success`. Adding an error path would mean changing the state every file list observes and its observer in `MainFileListFragment`, for a case that degrades safely — so the failure is handled by falling back instead: when the incoming-shares request fails, the branch emits the jail content unfiltered and un-enriched.

An unavailable list is not an empty one, and the two must stay distinguishable: "no shares available yet" has to pass everything through, while "the server reported zero visible shares" has to filter everything out so the `No shares` view is right. That is why the entries are held in a nullable flow — `null` for unavailable, an empty list for a genuine zero — rather than defaulting to `emptyList()`, which would make a failed request silently hide every child.

This is the rule D8 already chose for the Documents Provider — *a picker that shows one share too many beats a picker that will not open* — now applied on both surfaces, which is what keeps them from diverging on failure. The jail refresh keeps its own existing error reporting, so the tab is not silent about everything.

*Trade-off:* while the request is failing, a share hidden through `@UI.Hidden` can appear. Accepted: it is the same trade-off D8 already took, `@UI.Hidden` is a display preference rather than an access control, and the server still enforces permissions.

*Alternative rejected:* add an error channel to `FileListUiState` and show a snackbar. Correct in isolation, but it touches the shared state of five lists to report a condition that has a safe fallback.

The request is triggered on the tab-open path when the `Shares` tab is opened, not from the ViewModel's `init`. `MainFileListViewModel` backs all five file lists and `FolderPickerActivity`, and switching tabs only updates its `fileListOption` flow rather than recreating it — so an `init` call would fire on app start in `All files` and in the folder picker, fire on legacy accounts where the endpoint must never be requested, and still never retry when the user re-enters the tab. Driving it from the navigation path is what makes "once per tab open" literally true.

### D3. `FileListOption.SHARED_BY_LINK` is reused; no new enum value

The tab keeps feeding `getMenuItemForFileListOption()`, `updateToolbar()` (which already prints `bottom_nav_shares` when the chosen file has a `spaceId`) and `MainFileListViewModel.onlySharedByLinkFiles` through the existing value, and it is now also what gates the new flow branch and the adapter's rendering. Adding `SHARED_WITH_ME` now would ripple into `FileRepository.getSearchFolderContent()` and the legacy shared-by-link path, which this change otherwise leaves untouched.

The split belongs to the issue that introduces the three-list tab bar — and after this change that split is a change *inside* `MainFileListViewModel` (a third branch next to the two it will already have) rather than a new surface.

### D4. Routing stays as it is; only the request is added

`navigateTo(FileListOption.SHARED_BY_LINK)` is left untouched, fork included: while `storageManager.getRootSharesFolder()` is `null` on a spaces-enabled account the tab still shows the `SharesFragment` placeholder, and otherwise it mounts or updates `MainFileListFragment` at the jail root. `SharesFragment`, `shares_fragment.xml` and `initAndShowListOfShares()` are kept — the placeholder is the tab's rendering of "nothing to show" and this change does not replace it. The only addition on this path is the trigger that requests the incoming shares when the tab is opened.

The placeholder branch is narrow, because `getRootSharesFolder()` returns `null` only while the Shares *space* row is unknown, not while the jail root folder is uncached: `refreshSpacesForAccount()` saves every space including the jail and `AccountDiscoveryWorker` runs it when the account is added, and from the first successful spaces sync onwards the call also creates the jail-root `OCFile` row itself if it is missing. What remains is the window before that sync lands: tapping `Shares` there shows the static `No shares` view and makes no request — today's behaviour — and re-entering the tab afterwards lists the shares. Giving that window a real loading/empty state is out of scope here.

*Alternative rejected:* replacing the fork with a single path that resolves the jail root through `GetFileByRemotePathUseCase(..., SPACE_ID_SHARES)` and deletes the placeholder. It would list the shares on the very first tap of a brand-new account, but it retires a surface this change has no requirement to retire, and the window it buys closes by itself on the next tab entry.

Back navigation needs **no new code**: `FileDisplayActivity.kt:782-792` already calls `finish()` at a root folder whose space is not a project space, which is what the `Shares` tab does today, and below the jail root `onBrowseUp()` navigates within the same fragment. Tapping a folder is likewise already handled — the row click navigates in place via `updateFolderToDisplay()`, so there is no hand-off to arrange and no toolbar work beyond what `updateToolbar()` already does.

### D5. The sharing context rides on `OCFileWithSyncInfo`; the shared adapter renders it behind guards

`OCFileWithSyncInfo` gains one optional field:

```kotlin
data class OCFileWithSyncInfo(
    val file: OCFile,
    val uploadWorkerUuid: UUID? = null,
    val downloadWorkerUuid: UUID? = null,
    val isSynchronizing: Boolean = false,
    val space: OCSpace? = null,
    val sharedWithMe: OCSharedWithMe? = null,
)
```

It is additive and defaulted, so no existing construction site changes; it is populated only by the D1 branch and never read from or written to Room, which keeps the "nothing persisted" non-goal intact. `OCSharedWithMe` is reused as-is rather than introducing a narrower type — it already carries exactly `sharerDisplayName` and `isSynchronized`, and renaming the field generically if the `sharedByMe` lists ever need the same slot is a one-line change.

Carrying it *on the row* rather than beside it is what keeps `DiffUtil` honest: `FileListDiffCallback.areContentsTheSame()` compares with `==`, so a data-class field diffs automatically. A `Map<remoteId, OCSharedWithMe>` exposed alongside the list would not — a change arriving only from the Graph payload would produce no diff and leave stale rows on screen.

`FileListAdapter` gains two branches, both unreachable unless `sharedWithMe != null`:

1. In the generic bind (`:200`), `share_icons_layout` becomes visible for a row that has a sharing context, and a new 15dp `sync_badge_icon` is visible when `sharedWithMe.isSynchronized` is true. The `ImageView` is added to **both** `item_file_list.xml` and `grid_item.xml` with the same id, so this one generic bind covers list and grid alike — which is why the badge reaches grid at no extra cost, and why the previous design's open question about grid mode is now answered rather than deferred.
2. In the `LIST_ITEM` branch of `setSpecificViewHolder()` (`:293`), the `spacePathLine` condition gains the shares case and renders `sharerDisplayName` in place of the parent path. No existing list changes behaviour: for a jail child `fileWithSyncInfo.space` is the Shares space, so that row is hidden today.

The badge is not a control — it is an `ImageView` with a content description and no click listener, and nothing derives it from available-offline state, which keeps its own `localFileIndicator` pin.

*Alternative rejected:* `SharedWithMeAdapter` + `item_shared_with_me.xml` cloned from `item_file_list.xml`. It guarantees zero regression risk on the main list, but it re-implements thumbnails, the pin, selection, diffing and the three-dot sheet, it ships list-mode only, and the `sharedByMe` lists would not use it either.

**One condition identifies the list, and all three call sites must use it:**

```
fileListOption == SHARED_BY_LINK && remotePath == ROOT_PATH && spaceId == SPACE_ID_SHARES
```

The `Shared with me` list *is* the Shares jail root shown under the shares option — nothing deeper. Browsing into a shared folder leaves the list behind: that content is served by `retrieveFlowForAllFiles()`, the entries are not read, and the sharer and badge are not rendered, so requesting the shares there would be work nobody consumes. Note that `remotePath == ROOT_PATH` is load-bearing and not implied by the space: `refreshFolder()` stamps `SPACE_ID_SHARES` onto every jail child, so a subfolder satisfies the space test while not being the list.

The three sites are the branch in `retrieveFlowForShareByLink()` (task 4.1), the pull-to-refresh guard (task 4.2) and the tab-open trigger (task 6.1). They must test the same three parts, or the request fires where it is useless or fails to fire where it is needed.

### D6. The response is modelled down to the eight fields the list consumes

The payload is large and most of it is irrelevant here, so the response models keep only what the list reads: the entry's own `id` (the match key), `remoteItem.id`, `name`, `size`, `file.mimeType`, `createdBy.user.displayName`, `@UI.Hidden` and `@client.synchronize`. `OCSharedWithMe`'s fields follow this same order. Everything else — `eTag`, `lastModifiedDateTime`, `parentReference`, `folder`, `remoteItem.spaceId`, `remoteItem.parentReference` and the whole `permissions` array — is left unparsed. Moshi ignores unmapped keys, so the entries still round-trip.

This settles the sharer question the issue raised. `createdBy.user` and `remoteItem.permissions[].invitation.invitedBy.user` hold the same user, and since `permissions` is not modelled at all, `createdBy` is the only source: top-level, single-valued, present on every entry, needing no array traversal and no tie-breaking when an entry has several permissions. A welcome side effect is that `MemberPermissionResponse` in `SpacesResponse.kt` does **not** gain an `invitation` field, so the shared spaces models are untouched; only `UserResponse` and `FileResponse` are reused from there.

`name`, `size` and `file.mimeType` are read from the entry itself rather than from `remoteItem` — all three appear at both levels with the same values, and taking them top-level collapses the nested `remoteItem` model to a single `id` field. Of the eight, the list now renders only `sharerDisplayName` and `isSynchronized`, and matches on `id` / `name`; `name`, `size` and `mimeType` come from the joined `OCFile` instead, and are kept on the model because they cost nothing and make an entry inspectable on its own.

The two annotated booleans are named `isHidden` and `isSynchronized` in Kotlin, matching the domain model rather than the raw JSON spellings, with the wire names supplied by `@Json`. One name per concept across all four layers keeps the mapper trivial to read; codegen keys off the annotation, so the `is` prefix has no effect on parsing.

Two fields are deliberately derived rather than carried. A folder entry carries `folder: {}` and no `file`, so a null `mimeType` identifies a folder; for a matched entry the joined `OCFile.isFolder` is authoritative anyway. And `remoteItem.spaceId` is dropped because nothing needs it: the match key is the entry id, and every action runs against the Shares-jail `OCFile`, whose `spaceId` is `SPACE_ID_SHARES`.

### D7. "Shared with me" in the share panel is decided by the Shares jail

`GraphShareFragment` hides `members_section` and `public_links_section` when `file.spaceId == SPACE_ID_SHARES || file.isSharedWithMe`. The `spaceId` test is the reliable one on spaces-enabled accounts — `OCFile.isSharedWithMe` reads the OCS-style `permissions` string, whose contents on an oCIS jail child are not something this change can rely on — and `isSharedWithMe` keeps the legacy path working. The permalink lives in `ShareActivity.setupFileHeader()`, outside the fragment, so it needs no change.

### D8. The Documents Provider filters `@UI.Hidden` itself

Whether the jail's WebDAV listing already excludes hidden shares server-side is unverified, so `queryChildDocuments()` applies the exclusion explicitly: when the requested folder is the jail root (`spaceId == SPACE_ID_SHARES` and `remotePath == ROOT_PATH`), it calls `GetSharedWithMeAsyncUseCase` and keeps only the children that match a visible entry, under exactly the rule D1 uses in the app — `OCFile.remoteId == entry.id`, falling back to `fileName == entry.name`. If that call fails, the unfiltered WebDAV children are listed rather than failing the query.

Under D1 both surfaces now do literally the same thing to the same input — filter the jail listing by the same payload, with the same two match rules and the same unfiltered fallback — so "the two must stay in agreement" is a property of the design rather than a warning to implementers. The space row itself keeps using `space.name` + `ic_spaces` and its current `FLAG_DIR_SUPPORTS_CREATE` derivation; both are cosmetic/behavioural questions for design and are left as they are.

### D9. Layering

- `owncloudComLibrary`: `GetRemoteSharedWithMeOperation` under `lib/resources/shares/`, response models under `lib/resources/shares/responses/`, `ShareService.getSharedWithMe()` + `OCShareService` implementation.
- `owncloudData`: `RemoteShareDataSource.getSharedWithMe()` + `OCRemoteShareDataSource` implementation with a response→model mapper; `ShareRepository.getSharedWithMe()` + `OCShareRepository` implementation. Unit tests for both.
- `owncloudDomain`: `OCSharedWithMe` model under `domain/sharing/shares/model/` with the eight fields from D6, the `ShareRepository` contract method, `GetSharedWithMeAsyncUseCase` extending `BaseUseCaseWithResult`, and the optional `sharedWithMe` field on `OCFileWithSyncInfo` (D5). No `SharedWithMeListItem`: the row aggregate is `OCFileWithSyncInfo` itself.
- `owncloudApp`: the `SPACE_ID_SHARES` branch and `refreshSharedWithMe()` in `MainFileListViewModel`; the pull-to-refresh hook and nothing else in `MainFileListFragment`; two guarded branches in `FileListAdapter`; the `sync_badge_icon` `ImageView` in `item_file_list.xml` and `grid_item.xml`; the badge drawable and its content-description string; the tab-open trigger on `FileDisplayActivity`'s `Shares` path, whose routing — `SharesFragment`, `shares_fragment.xml` and `initAndShowListOfShares()` included — is otherwise unchanged; `GraphShareFragment` gating; `DocumentsStorageProvider` filtering.
- Koin: `RemoteDataSourceModule` and `RepositoryModule` unchanged (`OCRemoteShareDataSource` and `OCShareRepository` are already registered). `UseCaseModule` gains `GetSharedWithMeAsyncUseCase`. `ViewModelModule` gains **no new definition**, but `MainFileListViewModel` is constructed with positional `get()` calls (`ViewModelModule.kt:103`), so its new constructor dependency needs one more `get()` inserted at the matching position.

Data flow: `MainFileListFragment` → `MainFileListViewModel` → `GetSharedWithMeAsyncUseCase` → `ShareRepository` → `RemoteShareDataSource` → `ShareService` → `GET /graph/v1beta1/me/drive/sharedWithMe`, combined with `GetFolderContentAsStreamUseCase` → `FileRepository` → Room.

No Room schema change. All three flavors (`original`, `mdm`, `qa`) are affected equally.

## Risks / Trade-offs

- **Two files shared by five lists are modified** — `MainFileListViewModel` and `FileListAdapter`. → Both additions sit behind guards no other list can satisfy: the flow branch requires `spaceId == SPACE_ID_SHARES` at `ROOT_PATH`, and the rendering requires `sharedWithMe != null`. The one genuinely shared line is the `share_icons_layout` visibility expression, which gains a disjunct; the regression test for it is that `All files`, `Available offline` and spaces keep their badge rows unchanged.
- **The match key is unverified** — `entry.id` is expected to equal the jail child's `oc:fileid`, but that is an assumption about oCIS. → `fileName` fallback on top of it, which covers the case where the ids turn out not to line up, since oCIS de-duplicates names inside the jail. This is the change's sharpest risk: because the branch keeps only matching children (D1), a share that matches on neither key is **absent from the list**, not merely un-enriched. Worth confirming against a real server before release; if both keys prove unreliable, the fallback has to become "list un-enriched", which would in turn require another way to honour `@UI.Hidden`.
- **Membership lags the server by one refresh** — a share present in Graph but not yet in the jail is not listed. → The jail is refreshed on the same open that queries Graph, so the window is one failed or racing WebDAV refresh wide; pull-to-refresh closes it.
- **A hidden share can appear while Graph is failing** (D2). → Accepted; `@UI.Hidden` is a display preference, not access control.
- **Two requests on every tab open** (WebDAV jail refresh + Graph) where there used to be one. → Both already happen implicitly today; the cost is one extra Graph `GET` per open and per pull-to-refresh.
- **The jail root is mounted with `SYNC_CONTENTS`**, not `REFRESH_FOLDER`: `MainFileListViewModel.init` (`:164`) also synchronises children that are already local or available-offline. → Pre-existing behaviour for every folder the list mounts, so the `Shares` tab is not special; left alone rather than special-cased.
- **Grid mode shows the badge but no sharer.** → `grid_item.xml` has no `space_path_line`; adding one is a layout change with its own design question, and grid already omits size and timestamp.
- **Two different jail-root keys remain in the codebase** — `getSharesRootFolderForAccount()` keys the root by `sharesSpace.root.id` while the provider keys it by `space.id`. → For the Shares jail both hold the same value (`SPACE_ID_SHARES`), so the two surfaces resolve the same row and the D5 condition matches on both; the divergence is latent rather than active. Left alone rather than changed under an unrelated issue's scope — worth its own issue.
- **The tab makes no request while the Shares space row is unknown** (D4) — a brand-new account tapped into `Shares` before the first spaces sync lands shows the static placeholder and queries nothing. → Pre-existing behaviour, narrow (the sync runs on account discovery) and self-closing: re-entering the tab lists the shares. Not in scope here.
- **The sync badge drawable is invented until design signs off.** → `ic_available_offline` and `sync_pin` already mean the app's own available-offline state, so neither can be reused. A new vector is added and flagged for design review; swapping the asset later is a one-file change.

## Open Questions

- Whether the `Shares` tab should keep the sorting, search, grid and multiselection it inherits from the shared file list, or have some of them suppressed. Answering it does not change this design — each is an existing affordance of the fragment, and suppressing one later is a guard in `MainFileListFragment`.
- Whether the Documents Provider's `Shares` row should keep `space.name` + `ic_spaces` or align with the tab's `bottom_nav_shares` + `ic_ocis_shares`, and whether `FLAG_DIR_SUPPORTS_CREATE` is right on a jail root. Both are cosmetic/permission questions for design; left as-is here.
