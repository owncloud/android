/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
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
package com.owncloud.android.ui.preview;

import android.accounts.Account;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.ui.controller.TransferProgressController;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.PreferenceUtils;
import timber.log.Timber;

/**
 * This Fragment is used to monitor the progress of a file downloading.
 */
public class FileDownloadFragment extends FileFragment implements OnClickListener {

    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";
    private static final String EXTRA_ERROR = "ERROR";

    private static final String ARG_FILE = "FILE";
    private static final String ARG_IGNORE_FIRST = "IGNORE_FIRST";
    private static final String ARG_ACCOUNT = "ACCOUNT";

    private Account mAccount;

    public TransferProgressController mProgressController;

    private boolean mIgnoreFirstSavedState;
    private boolean mError;
    private ProgressBar mProgressBar;

    /**
     * Public factory method to create a new fragment that shows the progress of a file download.
     *
     * Android strongly recommends keep the empty constructor of fragments as the only public constructor, and
     * use {@link #setArguments(Bundle)} to set the needed arguments.
     *
     * This method hides to client objects the need of doing the construction in two steps.
     *
     * When 'file' is null creates a dummy layout (useful when a file wasn't tapped before).
     *
     * @param file                      An {@link OCFile} to show in the fragment
     * @param account                   An OC account; needed to start downloads
     * @param ignoreFirstSavedState     Flag to work around an unexpected behaviour of {@link FragmentStatePagerAdapter}
     *                                  TODO better solution
     */
    public static Fragment newInstance(OCFile file, Account account, boolean ignoreFirstSavedState) {
        FileDownloadFragment frag = new FileDownloadFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, file);
        args.putParcelable(ARG_ACCOUNT, account);
        args.putBoolean(ARG_IGNORE_FIRST, ignoreFirstSavedState);
        frag.setArguments(args);
        return frag;
    }

    /**
     * Creates an empty details fragment.
     *
     * It's necessary to keep a public constructor without parameters; the system uses it when tries to
     * reinstantiate a fragment automatically.
     */
    public FileDownloadFragment() {
        super();
        mAccount = null;
        mProgressController = null;
        mIgnoreFirstSavedState = false;
        mError = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        setFile((OCFile) args.getParcelable(ARG_FILE));
        // TODO better in super, but needs to check ALL the class extending FileFragment; not right now

        mIgnoreFirstSavedState = args.getBoolean(ARG_IGNORE_FIRST);
        mAccount = args.getParcelable(ARG_ACCOUNT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        if (savedInstanceState != null) {
            if (!mIgnoreFirstSavedState) {
                setFile((OCFile) savedInstanceState.getParcelable(FileDownloadFragment.EXTRA_FILE));
                mAccount = savedInstanceState.getParcelable(FileDownloadFragment.EXTRA_ACCOUNT);
                mError = savedInstanceState.getBoolean(FileDownloadFragment.EXTRA_ERROR);
            } else {
                mIgnoreFirstSavedState = false;
            }
        }

        View rootView = inflater.inflate(R.layout.file_download_fragment, container, false);

        mProgressBar = rootView.findViewById(R.id.progressBar);

        (rootView.findViewById(R.id.cancelBtn)).setOnClickListener(this);

        rootView.setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(getContext())
        );

        if (mError) {
            setButtonsForRemote(rootView);
        } else {
            setButtonsForTransferring(rootView);
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);
        mProgressController = new TransferProgressController(mContainerActivity);
        mProgressController.setProgressBar(mProgressBar);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(FileDownloadFragment.EXTRA_FILE, getFile());
        outState.putParcelable(FileDownloadFragment.EXTRA_ACCOUNT, mAccount);
        outState.putBoolean(FileDownloadFragment.EXTRA_ERROR, mError);
    }

    @Override
    public void onStart() {
        super.onStart();
        listenForTransferProgress();
    }

    @Override
    public void onStop() {
        leaveTransferProgress();
        super.onStop();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancelBtn: {
                mContainerActivity.getFileOperationsHelper().cancelTransference(getFile());
                getActivity().finish();
                break;
            }
            default:
                Timber.e("Incorrect view clicked!");
        }
    }

    /**
     * Enables buttons for a file being downloaded
     */
    private void setButtonsForTransferring(View rootView) {
        if (rootView != null) {
            rootView.findViewById(R.id.cancelBtn).setVisibility(View.VISIBLE);

            // show the progress bar for the transfer
            rootView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            TextView progressText = rootView.findViewById(R.id.progressText);
            progressText.setText(R.string.downloader_download_in_progress_ticker);
            progressText.setVisibility(View.VISIBLE);

            // hides the error icon
            rootView.findViewById(R.id.errorText).setVisibility(View.GONE);
            rootView.findViewById(R.id.error_image).setVisibility(View.GONE);
        }
    }

    /**
     * Enables or disables buttons for a file locally available
     */
    private void setButtonsForDown(View rootView) {
        if (rootView != null) {
            rootView.findViewById(R.id.cancelBtn).setVisibility(View.GONE);

            // hides the progress bar
            rootView.findViewById(R.id.progressBar).setVisibility(View.GONE);

            // updates the text message
            TextView progressText = rootView.findViewById(R.id.progressText);
            progressText.setText(R.string.common_loading);
            progressText.setVisibility(View.VISIBLE);

            // hides the error icon
            rootView.findViewById(R.id.errorText).setVisibility(View.GONE);
            rootView.findViewById(R.id.error_image).setVisibility(View.GONE);
        }
    }

    /**
     * Enables or disables buttons for a file not locally available
     * <p/>
     * Currently, this is only used when a download was failed
     */
    private void setButtonsForRemote(View rootView) {
        if (rootView != null) {
            rootView.findViewById(R.id.cancelBtn).setVisibility(View.GONE);

            // hides the progress bar and message
            rootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
            rootView.findViewById(R.id.progressText).setVisibility(View.GONE);

            // shows the error icon and message
            rootView.findViewById(R.id.errorText).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.error_image).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onTransferServiceConnected() {
        listenForTransferProgress();
    }

    @Override
    public void onFileMetadataChanged(OCFile updatedFile) {
        if (updatedFile != null) {
            setFile(updatedFile);
        }
        // view does not need any update
    }

    @Override
    public void onFileMetadataChanged() {
        FileDataStorageManager storageManager = mContainerActivity.getStorageManager();
        if (storageManager != null) {
            setFile(storageManager.getFileByPath(getFile().getRemotePath()));
        }
        // view does not need any update
    }

    @Override
    public void onFileContentChanged() {
        // view does not need any update, parent activity will replace this fragment
    }

    @Override
    public void updateViewForSyncInProgress() {
        setButtonsForTransferring(getView());
    }

    @Override
    public void updateViewForSyncOff() {
        if (getFile().isDown()) {
            setButtonsForDown(getView());
        } else {
            setButtonsForRemote(getView());
        }
    }

    private void listenForTransferProgress() {
        if (mProgressController != null) {
            mProgressController.startListeningProgressFor(getFile(), mAccount);
            setButtonsForTransferring(getView());
        }
    }

    private void leaveTransferProgress() {
        if (mProgressController != null) {
            mProgressController.stopListeningProgressFor(getFile(), mAccount);
        }
    }

    public void setError(boolean error) {
        mError = error;
    }

}
