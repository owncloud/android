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

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.ui.adapter.SharePublicLinkListAdapter;
import com.owncloud.android.ui.dialog.RemoveShareDialogFragment;
import com.owncloud.android.ui.fragment.EditShareFragment;
import com.owncloud.android.ui.fragment.PublicShareDialogFragment;
import com.owncloud.android.ui.fragment.SearchShareesFragment;
import com.owncloud.android.ui.fragment.ShareFragmentListener;

import java.util.ArrayList;

public class SharedByLinkActivity extends FileActivity implements
        SharePublicLinkListAdapter.SharePublicLinkAdapterListener, ShareFragmentListener {
    private final String TAG = SharedByLinkActivity.class.getSimpleName();

    private ArrayList<OCShare> allShares;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_by_link);
        setupToolbar();
        setupDrawer(R.id.shared_by_link);
        getSupportActionBar().setTitle(getString(R.string.drawer_item_shared_by_link));
        allShares = getIntent().getExtras().getParcelableArrayList(DrawerActivity.KEY_ALL_SHARES_FOR_AN_ACCOUNT);
        ListView allSharesList = (ListView) findViewById(R.id.all_shares_list);
        TextView noShares = (TextView) findViewById(R.id.no_shares_text_view);
        if(allShares.size() > 0){
            allSharesList.setVisibility(View.VISIBLE);
            SharePublicLinkListAdapter adapter = new SharePublicLinkListAdapter(getApplicationContext(),R.layout.share_user_item,allShares,this);
            allSharesList.setAdapter(adapter);
        } else{
            noShares.setVisibility(View.VISIBLE);
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
    public void copyOrSendPrivateLink(OCFile file) {
        getFileOperationsHelper().copyOrSendPrivateLink(file);
    }

    @Override
    public void showSearchUsersAndGroups() {
        Fragment searchFragment = SearchShareesFragment.newInstance(getFile(), getAccount());
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.share_fragment_container, searchFragment, ShareActivity.TAG_SEARCH_FRAGMENT);
        ft.addToBackStack(null);    // BACK button will recover the ShareFragment
        ft.commit();
    }

    @Override
    public void showEditPrivateShare(OCShare share) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(ShareActivity.TAG_EDIT_SHARE_FRAGMENT);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        DialogFragment newFragment = EditShareFragment.newInstance(share, getFile(), getAccount());
        newFragment.show(ft, ShareActivity.TAG_EDIT_SHARE_FRAGMENT);
    }

    @Override
    public void refreshSharesFromServer() {
    }

    @Override
    public void removeShare(OCShare share) {
        getFileOperationsHelper().removeShare(share);
    }

    @Override
    public void showAddPublicShare(String defaultLinkName) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(ShareActivity.TAG_PUBLIC_SHARE_DIALOG_FRAGMENT);
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
        newFragment.show(ft, ShareActivity.TAG_PUBLIC_SHARE_DIALOG_FRAGMENT);
    }

    @Override
    public void showEditPublicShare(OCShare share) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(ShareActivity.TAG_PUBLIC_SHARE_DIALOG_FRAGMENT);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = PublicShareDialogFragment.newInstanceToUpdate(getFile(), share,
                getAccount());
        newFragment.show(ft, ShareActivity.TAG_PUBLIC_SHARE_DIALOG_FRAGMENT);
    }

    @Override
    public void copyOrSendPublicLink(OCShare share) {
        setFile(new OCFile(share.getPath()));
        getFileOperationsHelper().copyOrSendPublicLink(share);
    }

    @Override
    public void removePublicShare(OCShare share) {
        RemoveShareDialogFragment dialog = RemoveShareDialogFragment.newInstance(share);
        dialog.show(getSupportFragmentManager(), ShareActivity.TAG_REMOVE_SHARE_DIALOG_FRAGMENT);
    }

    @Override
    public void editPublicShare(OCShare share) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(ShareActivity.TAG_PUBLIC_SHARE_DIALOG_FRAGMENT);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        DialogFragment newFragment = PublicShareDialogFragment.newInstanceToUpdate(getFile(), share,
                getAccount());
        newFragment.show(ft, ShareActivity.TAG_PUBLIC_SHARE_DIALOG_FRAGMENT);
    }
}
