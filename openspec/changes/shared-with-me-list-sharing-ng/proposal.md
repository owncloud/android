## Why

On spaces-enabled accounts the `Shares` tab is a plain WebDAV listing of the Shares jail — or, when that jail root has not been cached yet, a static `No shares` placeholder that never reaches the server. Either way the list carries no sharing context: the user cannot tell who shared a resource without opening the web app, shares hidden through `@UI.Hidden` cannot be honoured, and the server's `@client.synchronize` state is invisible. Moving the list onto the Graph `sharedWithMe` endpoint fixes all three and is the first of the three lists the `Shares` tab will eventually host.

## What Changes

- New Graph operation `GET /graph/v1beta1/me/drive/sharedWithMe`, exposed through `ShareService`, a remote data source, a repository method and a `GetSharedWithMeAsyncUseCase`.
- New domain model for a shared-with-me entry: resource identity, sharer display name, `@UI.Hidden` flag and `@client.synchronize` flag.
- The `Shares` tab keeps using the existing file list (`MainFileListFragment`) rooted at the Shares jail, and that list is now filtered and enriched by the Graph payload: hidden shares are dropped, and every remaining row carries its sharer display name and synchronize state. The `SharesFragment` placeholder is removed.
- `OCFileWithSyncInfo` gains an optional sharing-context field so a file list row can carry per-row sharing data that does not exist on `OCFile`. Nothing is persisted.
- The `Shares` tab always routes to the jail-rooted list on spaces-enabled accounts, replacing the current "is the jail root cached?" fork in `FileDisplayActivity.navigateTo()`.
- A new sync badge (drawable + content description) appears in the file list badge row, in both the list and grid item layouts, for entries whose `@client.synchronize` is `true`.
- `GraphShareFragment` hides its `Share with people` and `Public links` sections for an item shared with the user, leaving the header permalink as the only sharing affordance.
- The Documents Provider's `Shares` folder excludes `@UI.Hidden` shares so it lists the same resources as the in-app list, under the same matching rule.
- When the incoming-shares request fails, both surfaces list the Shares jail unfiltered rather than failing or reporting an error.
- Legacy (non-spaces) accounts keep today's behaviour on both surfaces.

## Capabilities

### New Capabilities
- `files-sharing/shared-with-me`: the Graph-sourced `Shared with me` list — its data contract, visibility filtering, per-entry information, available operations and the matching Documents Provider folder.

### Modified Capabilities
- `files-sharing/permalink`: the "Share screen section gating unchanged" requirement gains a shared-with-me exception — both sharing sections are hidden for an item shared with the user.

## Impact

- `owncloudComLibrary`: new remote operation + Moshi response models; `ShareService` gains `getSharedWithMe()`.
- `owncloudData`: new remote data source, mapper and repository method; unit tests for both.
- `owncloudDomain`: new model, repository contract method, use case, and one additive optional field on `OCFileWithSyncInfo`.
- `owncloudApp`: one new flow branch in `MainFileListViewModel`, two guarded branches in `FileListAdapter`, the sync badge in `item_file_list.xml` and `grid_item.xml`, new drawable/string, `FileDisplayActivity` routing, `GraphShareFragment` gating, `DocumentsStorageProvider`, Koin wiring; `SharesFragment` and `shares_fragment.xml` removed.
- No new third-party dependencies; no Room schema change.
