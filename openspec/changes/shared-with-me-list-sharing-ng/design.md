## Context

See `proposal.md` — Why. Design-relevant current state:

- `FileDisplayActivity.navigateTo(FileListOption.SHARED_BY_LINK)` (`FileDisplayActivity.kt:1833-1848`) forks on `storageManager.getRootSharesFolder()`: when it is `null` and the personal folder has a `spaceId`, it shows `SharesFragment`; otherwise it mounts `MainFileListFragment` rooted at the jail root. `SharesFragment` is a placeholder that only renders the `item_empty_dataset` include.
- `FileRepository.getSharesRootFolderForAccount()` resolves the jail root through `sharesSpace.root.id`, while `DocumentsStorageProvider.queryChildDocuments()` resolves it through `sharesSpace.id` (`DocumentsStorageProvider.kt:229-244`). `SpacesDao` keys both the space row and `getWebDavUrlForSpace()` by `space.id`, and `OCFileRepository.refreshFolder()` stamps `OCFile.spaceId` with the value it is given — so `space.id` is the canonical `OCFile.spaceId` and the two surfaces currently key the jail root differently.
- `OCLocalFileDataSource.getFileByRemotePath()` creates and returns the row when `remotePath == ROOT_PATH` and it is missing, so resolving the jail root never fails as long as the Shares space row exists. The Documents Provider therefore has no dependency on the in-app list having run first; `syncSpacesWithServer()` already keeps the space row fresh.
- `FileCursor.addFile()` already strips delete/rename/copy/move/write/create flags for `spaceId == SPACE_ID_SHARES`, so the provider's Shares folder is already read-only.
- `item_file_list.xml` has a `share_icons_layout` badge row (15dp `ImageView`s tinted `@color/list_item_lastmod_and_filesize_text`) anchored to the start of `file_list_size`, plus a `space_path_line` include below the size row.
- `GetRemoteGraphSharesForFileOperation` + `ShareService.getGraphShares()` + `OCRemoteShareDataSource.getGraphShares()` + `OCShareRepository.getGraphShares()` + `GetGraphSharesAsyncUseCase` is the established Graph `GET` chain to copy.
- `FilterFileMenuOptionsUseCase` already takes an `onlySharedByLinkFiles` flag and is reused by five ViewModels; the single-file options bottom sheet, however, exists only inside `MainFileListFragment.observeMenuOptionsSingleFile()`.

## Goals / Non-Goals

**Goals:**

- One source of truth for which resources are shared with the user — the Graph `sharedWithMe` payload — consumed by both the in-app list and the Documents Provider's `Shares` folder.
- Entries that remain ordinary `OCFile`s underneath, so browsing, download, preview and available-offline keep running through the existing file machinery with no parallel implementation.
- A list surface that the two remaining lists (`sharedByMe` based) can be added next to without being rebuilt, since neither of them has a WebDAV jail to fall back on.

**Non-Goals:**

- Persisting sharer or synchronize state in Room. The payload is read per load; no entity or migration is added.
- Extracting the single-file options bottom sheet out of `MainFileListFragment` for reuse. Tempting, but it is a refactor of the main file list and is deliberately kept out of this change.
- Touching `FileListAdapter` or `MainFileListFragment`. The shares list gets its own adapter and item layout so the main list carries zero regression risk.
- Grid mode. `grid_item.xml` carries the same badge row, but the shares list ships list-mode only here.

## Decisions

### D1. The Graph response drives the list; `OCFile` rows are joined onto it eagerly

On load the ViewModel does three things in order:

1. Resolve the jail root with `GetFileByRemotePathUseCase(accountName, ROOT_PATH, spaceId = SPACE_ID_SHARES)` — the same call the Documents Provider makes, so both surfaces key the jail root identically and the row is created on demand if absent. `getSharesRootFolderForAccount()` is **not** used for this list.
2. Refresh the jail root over WebDAV with `SynchronizeFolderUseCase(remotePath = ROOT_PATH, accountName, spaceId = SPACE_ID_SHARES, syncMode = REFRESH_FOLDER)` so the `OCFile` rows exist with their ids, etags and permissions.
3. Call `GetSharedWithMeAsyncUseCase(accountName)` and join each returned entry to an `OCFile`.

The `@UI.Hidden` exclusion lives in `GetSharedWithMeAsyncUseCase`, not in the ViewModel: the use case returns only visible entries, so the in-app list and the Documents Provider cannot drift apart. The domain model still carries the flag so the decision stays inspectable.

The list is then observed through `GetFolderContentAsStreamUseCase(jailRoot.id)`, which emits `List<OCFileWithSyncInfo>`; every emission is re-joined against the last Graph payload. That keeps the available-offline indicator and transfer state live without re-querying Graph, while Graph stays the authority on membership, order and the empty state.

Join key: `OCFile.remoteId == entry.id`. `OCFile.remoteId` is the WebDAV `oc:fileid` of the jail child and `entry.id` is the same virtual-drive item id (`<storage>$<shares-drive>!<share-id>`), so they are expected to match exactly. `fileName == entry.name` is the documented fallback for any entry that fails to match, because oCIS de-duplicates names inside the jail.

*Alternative rejected:* resolve the `OCFile` lazily on tap. One fewer network call on open, but no available-offline indicator in the list and a resolution step plus a new failure mode on every action.

*Alternative rejected:* keep `MainFileListFragment` rooted at the jail and overlay the Graph data onto `FileListAdapter`. Least new code, but it puts shares-only rendering into the shared adapter and does not generalise to the two `sharedByMe` lists, which have no jail to root at.

### D2. Ordering between the WebDAV refresh and the Graph call

Both run on load; the Graph call is the one whose failure is user-visible. If the WebDAV refresh fails but Graph succeeds, entries are still listed from the Graph payload with whatever `OCFile` rows are cached (unmatched entries render name, size, sharer and badges but defer their actions until a successful refresh). If Graph fails, the spec's error behaviour applies: snackbar, previous list kept.

If `getWebDavUrlForSpace(SPACE_ID_SHARES)` returns `null` the Shares space row is not in the database yet; the ViewModel runs `RefreshSpacesFromServerAsyncUseCase` once before step 2 rather than failing the load.

### D3. `FileListOption.SHARED_BY_LINK` is reused; no new enum value

The tab keeps feeding `getMenuItemForFileListOption()`, `updateToolbar()` (which already prints `bottom_nav_shares` when the chosen file has a `spaceId`) and `MainFileListViewModel.onlySharedByLinkFiles` through the existing value. Adding `SHARED_WITH_ME` now would ripple into `FileRepository.getSearchFolderContent()` and the legacy shared-by-link path, which this change otherwise leaves untouched. The split belongs to the issue that introduces the three-list tab bar.

### D4. Routing and back navigation

`navigateTo(FileListOption.SHARED_BY_LINK)` becomes: spaces-enabled account (`storageManager.getRootPersonalFolder()?.spaceId != null`) → always `initAndShowListOfShares()`; otherwise the existing legacy branch, unchanged. `initAndShowListOfShares()` gains a `TAG_LIST_OF_SHARES` fragment tag and sets `fileActions = this@FileDisplayActivity`.

Tapping a folder hands off to `FileDisplayActivity`, which sets `file = folder` and mounts `MainFileListFragment` with `FileListOption.SHARED_BY_LINK` — so everything below the first level is today's list, with today's FAB, action mode and toolbar. Pressing back at the jail root returns to `SharesFragment` instead of leaving the tab.

### D5. Dedicated adapter and item layout

New `SharedWithMeAdapter` over a `SharedWithMeListItem` (entry + optional `OCFileWithSyncInfo`) and a new `item_shared_with_me.xml` cloned from `item_file_list.xml`. The clone adds a third 15dp `ImageView` to `share_icons_layout` for the sync badge, keeping the 2dp spacing and `@color/list_item_lastmod_and_filesize_text` tint, and renders the sharer display name in the existing `space_path_line` row (the slot the available-offline list already uses for context). Thumbnails come from `MimetypeIconUtil` + `ThumbnailsCacheManager`, as in `FileListAdapter`.

Per-item actions: the three-dot opens a bottom sheet built in `SharesFragment` from the existing `file_options_bottom_sheet_fragment.xml` layout, with its option set produced by `FilterFileMenuOptionsUseCase(onlySharedByLinkFiles = true)`. Available-offline goes through `FileOperationsViewModel.performOperation(SetFilesAsAvailableOffline / UnsetFilesAsAvailableOffline)`; `Share`, `Open with`, `Send` and `Details` delegate to `MainFileListFragment.FileActions`, already implemented by `FileDisplayActivity`.

### D6. The response is modelled down to the eight fields the list consumes

The payload is large and most of it is irrelevant here, so the response models keep only what the list reads: the entry's own `id` (the join key), `remoteItem.id`, `name`, `size`, `file.mimeType`, `@UI.Hidden`, `@client.synchronize` and `createdBy.user.displayName`. Everything else — `eTag`, `lastModifiedDateTime`, `parentReference`, `folder`, `remoteItem.spaceId`, `remoteItem.parentReference` and the whole `permissions` array — is left unparsed. Moshi ignores unmapped keys, so the entries still round-trip.

This settles the sharer question the issue raised. `createdBy.user` and `remoteItem.permissions[].invitation.invitedBy.user` hold the same user, and since `permissions` is not modelled at all, `createdBy` is the only source: top-level, single-valued, present on every entry, needing no array traversal and no tie-breaking when an entry has several permissions. A welcome side effect is that `MemberPermissionResponse` in `SpacesResponse.kt` does **not** gain an `invitation` field, so the shared spaces models are untouched; only `UserResponse` and `FileResponse` are reused from there.

`name`, `size` and `file.mimeType` are read from the entry itself rather than from `remoteItem` — all three appear at both levels with the same values, and taking them top-level collapses the nested `remoteItem` model to a single `id` field.

The two annotated booleans are named `isHidden` and `isSynchronized` in Kotlin, matching the domain model rather than the raw JSON spellings, with the wire names supplied by `@Json`. One name per concept across all four layers keeps the mapper trivial to read; codegen keys off the annotation, so the `is` prefix has no effect on parsing.

Two fields are deliberately derived rather than carried. A folder entry carries `folder: {}` and no `file`, so a null `mimeType` identifies a folder; for a matched entry the joined `OCFile.isFolder` is authoritative anyway. And `remoteItem.spaceId` is dropped because nothing needs it: the join key is the entry id, and every action runs against the Shares-jail `OCFile`, whose `spaceId` is `SPACE_ID_SHARES`.

### D7. "Shared with me" in the share panel is decided by the Shares jail

`GraphShareFragment` hides `members_section` and `public_links_section` when `file.spaceId == SPACE_ID_SHARES || file.isSharedWithMe`. The `spaceId` test is the reliable one on spaces-enabled accounts — `OCFile.isSharedWithMe` reads the OCS-style `permissions` string, whose contents on an oCIS jail child are not something this change can rely on — and `isSharedWithMe` keeps the legacy path working. The permalink lives in `ShareActivity.setupFileHeader()`, outside the fragment, so it needs no change.

### D8. The Documents Provider filters `@UI.Hidden` itself

Whether the jail's WebDAV listing already excludes hidden shares server-side is unverified, so `queryChildDocuments()` applies the exclusion explicitly: when the requested folder is the jail root (`spaceId == SPACE_ID_SHARES` and `remotePath == ROOT_PATH`), it calls `GetSharedWithMeAsyncUseCase` and keeps only the children that match a visible entry, under exactly the rule D1 uses in the app — `OCFile.remoteId == entry.id`, falling back to `fileName == entry.name`. Both surfaces must apply the same two rules, or an entry that matches only by name would show in the app and vanish from the picker. If that call fails, the unfiltered WebDAV children are listed rather than failing the query — a picker that shows one share too many beats a picker that will not open. The space row itself keeps using `space.name` + `ic_spaces` and its current `FLAG_DIR_SUPPORTS_CREATE` derivation; both are cosmetic/behavioural questions for design and are left as they are.

### D9. Layering

- `owncloudComLibrary`: `GetRemoteSharedWithMeOperation` under `lib/resources/shares/`, response models under `lib/resources/shares/responses/`, `ShareService.getSharedWithMe()` + `OCShareService` implementation.
- `owncloudData`: `RemoteShareDataSource.getSharedWithMe()` + `OCRemoteShareDataSource` implementation with a response→model mapper; `ShareRepository.getSharedWithMe()` + `OCShareRepository` implementation. Unit tests for both.
- `owncloudDomain`: `OCShareWithMe` model under `domain/sharing/shares/model/` with the eight fields from D6 — `id`, `remoteItemId`, `name`, `size`, `mimeType`, `isHidden`, `sharerDisplayName`, `isSynchronized` — the `ShareRepository` contract method, and `GetSharedWithMeAsyncUseCase` extending `BaseUseCaseWithResult`.
- `owncloudApp`: `SharesViewModel`, `SharedWithMeAdapter`, `item_shared_with_me.xml`, `shares_fragment.xml` (RecyclerView + `SwipeRefreshLayout`), the sync badge drawable and its content-description string, `FileDisplayActivity` routing and back handling, `GraphShareFragment` gating, `DocumentsStorageProvider` filtering.
- Koin: `RemoteDataSourceModule` (nothing new — `OCRemoteShareDataSource` already registered), `RepositoryModule` (unchanged, same reason), `UseCaseModule` (`GetSharedWithMeAsyncUseCase`), `ViewModelModule` (`SharesViewModel` with an `accountName` parameter, following the `SpacesListViewModel` registration).

Data flow: `SharesFragment` → `SharesViewModel` → `GetSharedWithMeAsyncUseCase` → `ShareRepository` → `RemoteShareDataSource` → `ShareService` → `GET /graph/v1beta1/me/drive/sharedWithMe`, joined against `GetFolderContentAsStreamUseCase` → `FileRepository` → Room.

No Room schema change. All three flavors (`original`, `mdm`, `qa`) are affected equally.

## Risks / Trade-offs

- **The join key is unverified** — `entry.id` is expected to equal the jail child's `oc:fileid`, but that is an assumption about oCIS. → `fileName` fallback, and an unmatched entry still renders fully (name, size, sharer, badges) so the list never loses a row; only its actions wait for a matching `OCFile`.
- **Two requests on every tab open** (WebDAV jail refresh + Graph) where there used to be one. → Both already happen implicitly today (the jail sync on mount, the spaces refresh on activity start); the cost is one extra Graph `GET` per open and per pull-to-refresh.
- **The bottom sheet is duplicated** from `MainFileListFragment`. → Accepted to keep the main file list untouched; the extraction is a worthwhile follow-up but is a refactor, not this change.
- **The Documents Provider gains a network call** inside `queryChildDocuments()` for the jail root, on a path that must stay responsive. → The call only fires for that one folder, the provider already syncs in that method, and a failure degrades to the unfiltered list.
- **Two different jail-root keys remain in the codebase** — `getSharesRootFolderForAccount()` still uses `sharesSpace.root.id` while this list and the provider use `space.id`. → This change stops using that method for the shares list, so both surfaces agree; the method itself is left alone rather than changed under an unrelated issue's scope. Worth its own issue.
- **The sync badge drawable is invented until design signs off.** → `ic_available_offline` and `sync_pin` already mean the app's own available-offline state, so neither can be reused. A new vector is added and flagged for design review; swapping the asset later is a one-file change.

## Open Questions

- Whether the sync badge belongs in grid mode too. `grid_item.xml` carries the same badge row, so it is cheap to add once the shares list gains a grid mode — out of scope here, and answering it does not change this design.
- Whether the Documents Provider's `Shares` row should keep `space.name` + `ic_spaces` or align with the tab's `bottom_nav_shares` + `ic_ocis_shares`, and whether `FLAG_DIR_SUPPORTS_CREATE` is right on a jail root. Both are cosmetic/permission questions for design; left as-is here.
