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
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareParserResult;
import com.owncloud.android.operations.RemoveShareOperation;
import com.owncloud.android.operations.UpdateShareViaLinkOperation;
import com.owncloud.android.ui.adapter.FileListListAdapter;
import com.owncloud.android.ui.adapter.SharePublicLinkListAdapter;
import com.owncloud.android.ui.dialog.RemoveShareDialogFragment;
import com.owncloud.android.ui.errorhandling.ErrorMessageAdapter;
import com.owncloud.android.ui.fragment.EditShareFragment;
import com.owncloud.android.ui.fragment.PublicShareDialogFragment;
import com.owncloud.android.ui.fragment.ShareFragmentListener;

import java.util.ArrayList;
import java.util.Vector;

public class SharedByLinkActivity extends FileActivity {
    private final String TAG = SharedByLinkActivity.class.getSimpleName();

    private ListView allSharesList;
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
        getSupportActionBar().setTitle(getString(R.string.drawer_item_shared_by_link));
        allSharesList = (ListView) findViewById(R.id.all_shares_list);
        noShares = (TextView) findViewById(R.id.no_shares_text_view);
        account = getIntent().getExtras().getParcelable(DrawerActivity.KEY_ACCOUNT);
        setAccount(account,savedInstanceState != null);
    }

    @Override
    protected void onAccountSet(boolean stateWasRecovered){
        super.onAccountSet(stateWasRecovered);
        allShares = getStorageManager().getSharedFiles();
        setUpAllSharedFilesList();
    }

    private void setUpAllSharedFilesList(){
        if(allShares.size() > 0){
            allSharesList.setVisibility(View.VISIBLE);
            Vector<OCFile> vectorOfSharedFiles = new Vector<>();
            vectorOfSharedFiles.addAll(allShares);
            adapter = new FileListListAdapter(getApplicationContext(),vectorOfSharedFiles, this);
            allSharesList.setAdapter(adapter);
            noShares.setVisibility(View.INVISIBLE);
        } else{
            noShares.setVisibility(View.VISIBLE);
            allSharesList.setVisibility(View.INVISIBLE);
        }
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
        allShares = getStorageManager().getSharedFiles();
    }

    private PublicShareDialogFragment getPublicShareDialogFragment(){
        return  (PublicShareDialogFragment) getSupportFragmentManager()
                .findFragmentByTag(ShareActivity.TAG_PUBLIC_SHARE_DIALOG_FRAGMENT);
    }
}