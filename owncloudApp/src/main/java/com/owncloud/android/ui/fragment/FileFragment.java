/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Christian Schabesberger
 * @author David González Verdugo
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

import android.content.Context;

import androidx.fragment.app.Fragment;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.presentation.manager.TransferManager;
import com.owncloud.android.ui.activity.ComponentsGetter;

/**
 * Common methods for {@link Fragment}s containing {@link OCFile}s
 */
public abstract class FileFragment extends Fragment {

    private OCFile mFile;

    protected ContainerActivity mContainerActivity;

    /**
     * Creates an empty fragment.
     *
     * It's necessary to keep a public constructor without parameters; the system uses it when
     * tries to reinstantiate a fragment automatically.
     */
    public FileFragment() {
        mFile = null;
    }

    /**
     * Getter for the hold {@link OCFile}
     *
     * @return The {@link OCFile} hold
     */
    public OCFile getFile() {
        return mFile;
    }

    protected void setFile(OCFile file) {
        mFile = file;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mContainerActivity = (ContainerActivity) context;

        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " +
                    ContainerActivity.class.getSimpleName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDetach() {
        mContainerActivity = null;
        super.onDetach();
    }

    public void onSyncEvent(String syncEvent, boolean success, OCFile updatedFile) {
        if (syncEvent.equals(FileUploader.getUploadStartMessage())) {
            updateViewForSyncInProgress();

        } else if (syncEvent.equals(FileUploader.getUploadFinishMessage())) {
            if (success) {
                if (updatedFile != null) {
                    onFileMetadataChanged(updatedFile);
                } else {
                    onFileMetadataChanged();
                }
            }
            updateViewForSyncOff();

        } else if (syncEvent.equals(TransferManager.DOWNLOAD_ADDED_MESSAGE)) {
            updateViewForSyncInProgress();

        } else if (syncEvent.equals(TransferManager.DOWNLOAD_FINISH_MESSAGE)) {
            if (success) {
                if (updatedFile != null) {
                    onFileMetadataChanged(updatedFile);
                } else {
                    onFileMetadataChanged();
                }
                onFileContentChanged();
            }
            updateViewForSyncOff();
        }
    }

    public abstract void updateViewForSyncInProgress();

    public abstract void updateViewForSyncOff();

    public abstract void onTransferServiceConnected();

    public abstract void onFileMetadataChanged(OCFile updatedFile);

    public abstract void onFileMetadataChanged();

    public abstract void onFileContentChanged();

    /**
     * Interface to implement by any Activity that includes some instance of FileListFragment
     * Interface to implement by any Activity that includes some instance of FileFragment
     */
    public interface ContainerActivity extends ComponentsGetter {

        /**
         * Request the parent activity to show the details of an {@link OCFile}.
         *
         * @param file      File to show details
         */
        void showDetails(OCFile file);

        ///// TO UNIFY IN A SINGLE CALLBACK METHOD - EVENT NOTIFICATIONs  -> something happened
        // inside the fragment, MAYBE activity is interested --> unify in notification method

        /**
         * Callback method invoked when a the user browsed into a different folder through the
         * list of files
         *
         * @param folder
         */
        void onBrowsedDownTo(OCFile folder);

    }
}