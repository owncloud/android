/**
 * ownCloud Android client application
 *
 * @author LukeOwncloud
 * @author Christian Schabesberger
 * Copyright (C) 2020 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ListView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCUpload;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.files.services.TransferRequester;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.adapter.ExpandableUploadListAdapter;
import timber.log.Timber;

/**
 * A Fragment that lists all files and folders in a given LOCAL path.
 *
 */
public class UploadListFragment extends ExpandableListFragment implements OptionsInUploadListClickListener {

    /**
     * Reference to the Activity which this fragment is attached to. For
     * callbacks
     */
    private UploadListFragment.ContainerActivity mContainerActivity;

    private ExpandableUploadListAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        setMessageForEmptyList(getString(R.string.upload_list_empty));
        setOnRefreshListener(this);
        return v;
    }

    @Override
    public void onRefresh() {
        // remove the progress circle as soon as pull is triggered, like in the list of files
        mRefreshEmptyLayout.setRefreshing(false);
        mRefreshListLayout.setRefreshing(false);

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStart() {
        Timber.v("onStart() start");
        super.onStart();
        mAdapter = new ExpandableUploadListAdapter((FileActivity) getActivity(), this);
        setListAdapter(mAdapter);
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        boolean handled = false;
        OCUpload OCUpload = (OCUpload) mAdapter.getChild(groupPosition, childPosition);
        if (OCUpload != null) {
            // notify the click to container Activity
            handled = mContainerActivity.onUploadItemClick(OCUpload);
        } else {
            Timber.w("Null object in ListAdapter!!");
        }
        return handled;
    }

    /**
     * Interface to implement by any Activity that includes some instance of
     * UploadListFragment
     *
     * @author LukeOwncloud
     */
    public interface ContainerActivity {

        /**
         * Callback method invoked when an upload item is clicked by the user on
         * the upload list
         *
         * @param file
         * @return return true if click was handled.
         */
        boolean onUploadItemClick(OCUpload file);

    }

    public enum OptionsInUploadList {
        CLEAR_FAILED, CLEAR_SUCCESSFUL, RETRY_FAILED;
    }

    @Override
    public void onClick(OptionsInUploadList option) {
        UploadsStorageManager storageManager;

        switch (option) {
            case RETRY_FAILED:
                TransferRequester requester = new TransferRequester();
                requester.retryFailedUploads(requireContext(), null, null, false);
                break;
            case CLEAR_FAILED:
                storageManager = new UploadsStorageManager(requireActivity().getContentResolver());
                storageManager.clearFailedButNotDelayedForWifiUploads();
                break;
            case CLEAR_SUCCESSFUL:
                storageManager = new UploadsStorageManager(requireActivity().getContentResolver());
                storageManager.clearSuccessfulUploads();
                break;
        }
        updateUploads();
    }

    public void binderReady() {
        if (mAdapter != null) {
            mAdapter.addBinder();
        }
    }

    public void updateUploads() {
        if (mAdapter != null) {
            mAdapter.refreshView();
        }
    }
}
