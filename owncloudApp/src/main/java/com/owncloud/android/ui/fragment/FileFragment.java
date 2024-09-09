/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Christian Schabesberger
 * @author David González Verdugo
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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
import android.os.Build;
import android.view.Menu;
import android.view.MenuInflater;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.owncloud.android.R;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.ui.activity.ComponentsGetter;

import static com.owncloud.android.usecases.transfers.TransferConstantsKt.DOWNLOAD_ADDED_MESSAGE;
import static com.owncloud.android.usecases.transfers.TransferConstantsKt.DOWNLOAD_FINISH_MESSAGE;
import static com.owncloud.android.usecases.transfers.TransferConstantsKt.UPLOAD_FINISH_MESSAGE;
import static com.owncloud.android.usecases.transfers.TransferConstantsKt.UPLOAD_START_MESSAGE;

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
        if (syncEvent.equals(UPLOAD_START_MESSAGE)) {
            updateViewForSyncInProgress();
        } else if (syncEvent.equals(UPLOAD_FINISH_MESSAGE)) {
            if (success) {
                if (updatedFile != null) {
                    onFileMetadataChanged(updatedFile);
                } else {
                    onFileMetadataChanged();
                }
            }
            updateViewForSyncOff();

        } else if (syncEvent.equals(DOWNLOAD_ADDED_MESSAGE)) {
            updateViewForSyncInProgress();

        } else if (syncEvent.equals(DOWNLOAD_FINISH_MESSAGE)) {
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

    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.file_actions_menu, menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String roleAccessibilityDescription = getString(R.string.button_role_accessibility);
            menu.findItem(R.id.action_open_file_with).setContentDescription(getString(R.string.actionbar_open_with) + " " + roleAccessibilityDescription);
            menu.findItem(R.id.action_send_file).setContentDescription(getString(R.string.actionbar_send_file) + " " + roleAccessibilityDescription);
            menu.findItem(R.id.action_set_available_offline).setContentDescription(getString(R.string.set_available_offline) + " " + roleAccessibilityDescription);
            menu.findItem(R.id.action_unset_available_offline).setContentDescription(getString(R.string.set_available_offline) + " " + roleAccessibilityDescription);
        }
    }
}
