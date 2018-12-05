/**
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author Juan Carlos González Cabrero
 * @author David González Verdugo
 * @author Christian Schabesberger
 * Copyright (C) 2018 ownCloud GmbH.
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareParserResult;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.operations.CreateShareViaLinkOperation;
import com.owncloud.android.operations.GetSharesForFileOperation;
import com.owncloud.android.operations.RemoveShareOperation;
import com.owncloud.android.operations.UpdateSharePermissionsOperation;
import com.owncloud.android.operations.UpdateShareViaLinkOperation;
import com.owncloud.android.providers.UsersAndGroupsSearchProvider;
import com.owncloud.android.ui.asynctasks.GetSharesForFileAsyncTask;
import com.owncloud.android.ui.errorhandling.ErrorMessageAdapter;
import com.owncloud.android.ui.fragment.EditShareFragment;
import com.owncloud.android.ui.fragment.PublicShareDialogFragment;
import com.owncloud.android.ui.fragment.SearchShareesFragment;
import com.owncloud.android.ui.fragment.ShareFileFragment;
import com.owncloud.android.ui.fragment.ShareFragmentListener;


/**
 * Activity for sharing files
 */

public class ShareActivity extends FileActivity
        implements ShareFragmentListener {

    private static final String TAG = ShareActivity.class.getSimpleName();

    private static final String TAG_SHARE_FRAGMENT = "SHARE_FRAGMENT";
    private static final String TAG_SEARCH_FRAGMENT = "SEARCH_USER_AND_GROUPS_FRAGMENT";
    private static final String TAG_EDIT_SHARE_FRAGMENT = "EDIT_SHARE_FRAGMENT";
    private static final String TAG_PUBLIC_SHARE_DIALOG_FRAGMENT = "PUBLIC_SHARE_DIALOG_FRAGMENT";
    public static final String TAG_REMOVE_SHARE_DIALOG_FRAGMENT = "REMOVE_SHARE_DIALOG_FRAGMENT";

    GetSharesForFileAsyncTask mGetSharesForFileAsyncTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGetSharesForFileAsyncTask = null;

        setContentView(R.layout.share_activity);

        // Set back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        if (savedInstanceState == null) {
            // Add Share fragment on first creation
            Fragment fragment = ShareFileFragment.newInstance(getFile(), getAccount());
            ft.replace(R.id.share_fragment_container, fragment, TAG_SHARE_FRAGMENT);
            ft.commit();
        }

    }

    protected void onAccountSet(boolean stateWasRecovered) {
        super.onAccountSet(stateWasRecovered);

        // Load data into the list
        Log_OC.d(TAG, "Refreshing lists on account set");
        refreshSharesFromStorageManager();

        // Request for a refresh of the data through the server (starts an Async Task)
        refreshSharesFromServer();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        // Verify the action and get the query
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log_OC.w(TAG, "Ignored Intent requesting to query for " + query);

        } else if (UsersAndGroupsSearchProvider.getSuggestIntentAction().equals(intent.getAction())) {
            Uri data = intent.getData();
            String dataString = intent.getDataString();
            String shareWith = dataString.substring(dataString.lastIndexOf('/') + 1);
            doShareWith(
                    shareWith,
                    data.getAuthority()
            );

        } else {
            Log_OC.e(TAG, "Unexpected intent " + intent.toString());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGetSharesForFileAsyncTask != null) {
            mGetSharesForFileAsyncTask.cancel(true);
            mGetSharesForFileAsyncTask = null;
        }
    }

    @Override
    public void copyOrSendPrivateLink(OCFile file) {
        getFileOperationsHelper().copyOrSendPrivateLink(file);
    }

    private void doShareWith(String shareeName, String dataAuthority) {

        ShareType shareType = UsersAndGroupsSearchProvider.getShareType(dataAuthority);

        getFileOperationsHelper().shareFileWithSharee(
                getFile(),
                shareeName,
                shareType,
                getAppropiatePermissions(shareType)
        );
    }

    private int getAppropiatePermissions(ShareType shareType) {

        // check if the Share is FERERATED
        boolean isFederated = ShareType.FEDERATED.equals(shareType);

        if (getFile().isSharedWithMe()) {
            return OCShare.READ_PERMISSION_FLAG;    // minimum permissions

        } else if (isFederated) {
            OwnCloudVersion serverVersion =
                    com.owncloud.android.authentication.AccountUtils.getServerVersion(getAccount());
            if (serverVersion != null && serverVersion.isNotReshareableFederatedSupported()) {
                return (
                        getFile().isFolder() ?
                                OCShare.FEDERATED_PERMISSIONS_FOR_FOLDER_AFTER_OC9 :
                                OCShare.FEDERATED_PERMISSIONS_FOR_FILE_AFTER_OC9
                );
            } else {
                return (
                        getFile().isFolder() ?
                                OCShare.FEDERATED_PERMISSIONS_FOR_FOLDER_UP_TO_OC9 :
                                OCShare.FEDERATED_PERMISSIONS_FOR_FILE_UP_TO_OC9
                );
            }
        } else {
            return (
                    getFile().isFolder() ?
                            OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER :
                            OCShare.MAXIMUM_PERMISSIONS_FOR_FILE
            );
        }
    }

    @Override
    public void showSearchUsersAndGroups() {
        Fragment searchFragment = SearchShareesFragment.newInstance(getFile(), getAccount());
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.share_fragment_container, searchFragment, TAG_SEARCH_FRAGMENT);
        ft.addToBackStack(null);    // BACK button will recover the ShareFragment
        ft.commit();
    }

    @Override
    public void showEditPrivateShare(OCShare share) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(TAG_EDIT_SHARE_FRAGMENT);
        if (prev != null) {
            ft.remove(prev);    // BACK button will recover the previous fragment
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = EditShareFragment.newInstance(share, getFile(), getAccount());
        newFragment.show(ft, TAG_EDIT_SHARE_FRAGMENT);

    }

    @Override
    // Call to Remove share operation
    public void removeShare(OCShare share) {
        getFileOperationsHelper().removeShare(share);
    }

    /**
     * Get users and groups from the server to fill in the "share with" list
     */
    @Override
    public void refreshSharesFromServer() {
        // Show loading
        showLoadingDialog(R.string.common_loading);
        // Get Users and Groups
        mGetSharesForFileAsyncTask = new GetSharesForFileAsyncTask(this);
        Object[] params = {getFile(), getAccount(), getStorageManager()};
        mGetSharesForFileAsyncTask.execute(params);
    }

    @Override
    public void showAddPublicShare(String defaultLinkName) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(TAG_PUBLIC_SHARE_DIALOG_FRAGMENT);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog
        DialogFragment newFragment = PublicShareDialogFragment.newInstanceToCreate(
            getFile(),
            getAccount(),
            defaultLinkName
        );
        newFragment.show(ft, TAG_PUBLIC_SHARE_DIALOG_FRAGMENT);
    }

    @Override
    public void showEditPublicShare(OCShare share) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(TAG_PUBLIC_SHARE_DIALOG_FRAGMENT);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = PublicShareDialogFragment.newInstanceToUpdate(getFile(), share,
                getAccount());
        newFragment.show(ft, TAG_PUBLIC_SHARE_DIALOG_FRAGMENT);
    }

    @Override
    public void copyOrSendPublicLink(OCShare share) {
        getFileOperationsHelper().copyOrSendPublicLink(share);
    }

    /**
     * Updates the view associated to the activity after the finish of some operation over files
     * in the current account.
     *
     * @param operation Removal operation performed.
     * @param result    Result of the removal.
     */
    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        super.onRemoteOperationFinish(operation, result);

        if (result.isSuccess() ||
                (operation instanceof GetSharesForFileOperation &&
                        result.getCode() == RemoteOperationResult.ResultCode.SHARE_NOT_FOUND
                )
                ) {
            Log_OC.d(TAG, "Refreshing view on successful operation or finished refresh");
            refreshSharesFromStorageManager();
            if (operation instanceof GetSharesForFileOperation) {
                mGetSharesForFileAsyncTask = null;
            }
        }

        if (operation instanceof CreateShareViaLinkOperation) {
            onCreateShareViaLinkOperationFinish((CreateShareViaLinkOperation) operation, result);
        }

        if (operation instanceof UpdateShareViaLinkOperation) {
            onUpdateShareViaLinkOperationFinish((UpdateShareViaLinkOperation) operation, result);
        }

        if (operation instanceof RemoveShareOperation && result.isSuccess() && getEditShareFragment() != null) {
            getSupportFragmentManager().popBackStack();
        }

        if (operation instanceof UpdateSharePermissionsOperation
                && getEditShareFragment() != null && getEditShareFragment().isAdded()) {
            getEditShareFragment().onUpdateSharePermissionsFinished(result);
        }
    }

    private void onCreateShareViaLinkOperationFinish(CreateShareViaLinkOperation operation,
                                                     RemoteOperationResult<ShareParserResult> result) {
        if (result.isSuccess()) {
            updateFileFromDB();

            getPublicShareFragment().dismiss();

            getFileOperationsHelper().copyOrSendPublicLink(result.getData().getShares().get(0));

        } else {
            getPublicShareFragment().showError(
                    ErrorMessageAdapter.getResultMessage(result, operation, getResources())
            );
        }
    }

    private void onUpdateShareViaLinkOperationFinish(UpdateShareViaLinkOperation operation,
                                                     RemoteOperationResult<ShareParserResult> result) {
        if (result.isSuccess()) {
            updateFileFromDB();

            getPublicShareFragment().dismiss();

            getFileOperationsHelper().copyOrSendPublicLink(result.getData().getShares().get(0));

        } else {
            getPublicShareFragment().showError(
                    ErrorMessageAdapter.getResultMessage(result, operation, getResources())
            );
        }
    }

    /**
     * Updates the view, reading data from {@link com.owncloud.android.datamodel.FileDataStorageManager}
     */
    private void refreshSharesFromStorageManager() {

        ShareFileFragment shareFileFragment = getShareFileFragment();
        if (shareFileFragment != null
                && shareFileFragment.isAdded()) {   // only if added to the view hierarchy!!
            shareFileFragment.refreshCapabilitiesFromDB();
            shareFileFragment.refreshUsersOrGroupsListFromDB();
            shareFileFragment.refreshPublicSharesListFromDB();
        }

        SearchShareesFragment searchShareesFragment = getSearchFragment();
        if (searchShareesFragment != null &&
                searchShareesFragment.isAdded()) {  // only if added to the view hierarchy!!
            searchShareesFragment.refreshUsersOrGroupsListFromDB();
        }

        PublicShareDialogFragment publicShareDialogFragment = getPublicShareFragment();
        if (publicShareDialogFragment != null &&
                publicShareDialogFragment.isAdded()) {  // only if added to the view hierarchy!!
            publicShareDialogFragment.refreshModelFromStorageManager();
        }

        EditShareFragment editShareFragment = getEditShareFragment();
        if (editShareFragment != null &&
                editShareFragment.isAdded()) {
            editShareFragment.refreshUiFromDB();
        }
    }

    /**
     * Shortcut to get access to the {@link ShareFileFragment} instance, if any
     *
     * @return A {@link ShareFileFragment} instance, or null
     */
    private ShareFileFragment getShareFileFragment() {
        return (ShareFileFragment) getSupportFragmentManager().findFragmentByTag(TAG_SHARE_FRAGMENT);
    }

    /**
     * Shortcut to get access to the {@link SearchShareesFragment} instance, if any
     *
     * @return A {@link SearchShareesFragment} instance, or null
     */
    private SearchShareesFragment getSearchFragment() {
        return (SearchShareesFragment) getSupportFragmentManager().findFragmentByTag(TAG_SEARCH_FRAGMENT);
    }

    /**
     * Shortcut to get access to the {@link PublicShareDialogFragment} instance, if any
     *
     * @return A {@link PublicShareDialogFragment} instance, or null
     */
    private PublicShareDialogFragment getPublicShareFragment() {
        return (PublicShareDialogFragment) getSupportFragmentManager().findFragmentByTag(TAG_PUBLIC_SHARE_DIALOG_FRAGMENT);
    }

    /**
     * Shortcut to get access to the {@link EditShareFragment} instance, if any
     *
     * @return A {@link EditShareFragment} instance, or null
     */
    private EditShareFragment getEditShareFragment() {
        return (EditShareFragment) getSupportFragmentManager().findFragmentByTag(TAG_EDIT_SHARE_FRAGMENT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case android.R.id.home:
                if (!getSupportFragmentManager().popBackStackImmediate()) {
                    finish();
                }
                break;
            default:
                retval = super.onOptionsItemSelected(item);
        }
        return retval;
    }
}
