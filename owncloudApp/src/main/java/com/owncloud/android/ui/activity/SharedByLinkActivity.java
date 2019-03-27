/**
 *   ownCloud Android client application
 *
 *   @author Shashvat Kedia
 *   Copyright (C) 2018 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareParserResult;
import com.owncloud.android.operations.RemoveShareOperation;
import com.owncloud.android.operations.UpdateShareViaLinkOperation;
import com.owncloud.android.ui.adapter.FileListListAdapter;
import com.owncloud.android.ui.adapter.SharePublicLinkListAdapter;
import com.owncloud.android.ui.dialog.RemoveShareDialogFragment;
import com.owncloud.android.ui.errorhandling.ErrorMessageAdapter;
import com.owncloud.android.ui.fragment.*;

import java.util.ArrayList;
import java.util.Vector;

public class SharedByLinkActivity extends FileActivity implements FileFragment.ContainerActivity,
      OnEnforceableRefreshListener {
    private final String TAG = SharedByLinkActivity.class.getSimpleName();

    private static final String TAG_SHARED_BY_LINK_FILES = "SHARED_BY_LINK_FILES";

    private TextView noShares;
    private FileListListAdapter adapter;

    private Vector<OCFile> allShares;
    private Account account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_by_link);
        setupToolbar();
        setupDrawer(R.id.shared_by_link);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        if (savedInstanceState == null) {
            createFragments();
        }
        noShares = (TextView) findViewById(R.id.no_shares_text_view);
        account = getIntent().getExtras().getParcelable(DrawerActivity.KEY_ACCOUNT);
        setAccount(account,savedInstanceState != null);
    }

    private void createFragments(){
        OCFileListFragment listOfFiles = OCFileListFragment.newInstance(false, true, true);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, listOfFiles, TAG_SHARED_BY_LINK_FILES);
        transaction.commit();
    }

    protected OCFileListFragment getListOfFilesFragment() {
        Fragment listOfFiles = getSupportFragmentManager().findFragmentByTag(TAG_SHARED_BY_LINK_FILES);
        if (listOfFiles != null) {
            return (OCFileListFragment) listOfFiles;
        }
        Log_OC.e(TAG, "Access to unexisting list of files fragment!!");
        return null;
    }

    @Override
    public void showDetails(OCFile file) {
    }

    @Override
    public void onBrowsedDownTo(OCFile directory) {
        Log.e(TAG,directory.toString());
        setFile(directory);
        updateNavigationElementsInActionBar();
    }

    protected void updateNavigationElementsInActionBar() {
        ActionBar actionBar = getSupportActionBar();
        OCFile currentDir = getCurrentFolder();
        boolean atRoot = (currentDir == null || currentDir.getParentId() == 0);
        actionBar.setDisplayHomeAsUpEnabled(!atRoot);
        actionBar.setHomeButtonEnabled(!atRoot);
        actionBar.setTitle(
                atRoot
                        ? getString(R.string.default_display_name_for_root_folder)
                        : currentDir.getFileName()
        );
    }

    protected OCFile getCurrentFolder() {
        OCFile file = getFile();
        if (file != null) {
            if (file.isFolder()) {
                return file;
            } else if (getStorageManager() != null) {
                String parentPath = file.getRemotePath().substring(0,
                        file.getRemotePath().lastIndexOf(file.getFileName()));
                return getStorageManager().getFileByPath(parentPath);
            }
        }
        return null;
    }

    protected void refreshListOfFilesFragment() {
        OCFileListFragment fileListFragment = getListOfFilesFragment();
        if (fileListFragment != null) {
            updateShares();
            fileListFragment.setFilesInAdapter(allShares);
            // TODO Enable when "On Device" is recovered ?
            // fileListFragment.listDirectory(false);
        }
    }

    @Override
    public void onRefresh() {
        refreshListOfFilesFragment();
    }


    @Override
    protected void onAccountSet(boolean stateWasRecovered){
        super.onAccountSet(stateWasRecovered);
        updateShares();
        setUpAllSharedFilesList();
        updateNavigationElementsInActionBar();
    }

    @Override
    public void onBackPressed() {
        OCFileListFragment listOfFiles = getListOfFilesFragment();
        if (listOfFiles != null) {  // should never be null, indeed
            int levelsUp = listOfFiles.onBrowseUp();
            if (levelsUp == 0) {
                finish();
                return;
            }
            setFile(listOfFiles.getCurrentFile());
            updateNavigationElementsInActionBar();
        }
    }

    private void setUpAllSharedFilesList(){
        if(allShares.size() > 0){
            OCFileListFragment listOfFolders = getListOfFilesFragment();
            listOfFolders.setFilesInAdapter(allShares);
            noShares.setVisibility(View.INVISIBLE);
        } else{
            noShares.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case android.R.id.home:
                if(isDrawerOpen()){
                    closeDrawer();
                } else{
                    openDrawer();
                }
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result){
        super.onRemoteOperationFinish(operation,result);
        if(result.isSuccess()){
            PublicShareDialogFragment publicShareDialogFragment = getPublicShareDialogFragment();
            if(publicShareDialogFragment != null &&
                    publicShareDialogFragment.isAdded()){
                publicShareDialogFragment.refreshModelFromStorageManager();
            }
            updateShares();
        }
        if(operation instanceof UpdateShareViaLinkOperation){
            onUpdateShareViaLinkOperationFinish((UpdateShareViaLinkOperation) operation,result);
        }
        if(operation instanceof RemoveShareOperation && result.isSuccess() &&
                (EditShareFragment) getSupportFragmentManager()
                        .findFragmentByTag(ShareActivity.TAG_EDIT_SHARE_FRAGMENT) != null){
            getSupportFragmentManager().popBackStack();
        }
    }

    private void onUpdateShareViaLinkOperationFinish(UpdateShareViaLinkOperation operation,
                                                     RemoteOperationResult<ShareParserResult>  result){
        if(result.isSuccess()){
            getPublicShareDialogFragment().dismiss();
            setFile(new OCFile(result.getData().getShares().get(0).getPath()));
            getFileOperationsHelper().copyOrSendPublicLink(result.getData().getShares().get(0));
            updateShares();
        } else{
            getPublicShareDialogFragment().showError(
                    ErrorMessageAdapter.getResultMessage(result,operation,getResources()));
        }
    }

    private void updateShares(){
        allShares = getStorageManager().getAllPublicShares(OCFile.ROOT_PATH);
    }

    private PublicShareDialogFragment getPublicShareDialogFragment(){
        return  (PublicShareDialogFragment) getSupportFragmentManager()
                .findFragmentByTag(ShareActivity.TAG_PUBLIC_SHARE_DIALOG_FRAGMENT);
    }

    @Override
    public void onRefresh(boolean enforced) {
    }
}