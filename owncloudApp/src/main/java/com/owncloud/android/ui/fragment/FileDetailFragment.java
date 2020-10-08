/*
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * @author Christian Schabesberger
 * @author David González Verdugo
 * @author Abel García de Prada
 * Copyright (C) 2011  Bartek Przybylski
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

import android.accounts.Account;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.controller.TransferProgressController;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.dialog.RenameFileDialogFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimetypeIconUtil;
import com.owncloud.android.utils.PreferenceUtils;

import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

/**
 * This Fragment is used to display the details about a file.
 */
public class FileDetailFragment extends FileFragment implements OnClickListener {

    private int mLayout;
    private View mView;
    private Account mAccount;
    private TransferProgressController mProgressController;

    public static final String FTAG_CONFIRMATION = "REMOVE_CONFIRMATION_FRAGMENT";
    public static final String FTAG_RENAME_FILE = "RENAME_FILE_FRAGMENT";

    private static final String ARG_FILE = "FILE";
    private static final String ARG_ACCOUNT = "ACCOUNT";

    /**
     * Public factory method to create new FileDetailFragment instances.
     * <p>
     * When 'fileToDetail' or 'ocAccount' are null, creates a dummy layout (to use when a file wasn't tapped before).
     *
     * @param fileToDetail An {@link OCFile} to show in the fragment
     * @param account      An ownCloud account; needed to start downloads
     * @return New fragment with arguments set
     */
    public static FileDetailFragment newInstance(OCFile fileToDetail, Account account) {
        FileDetailFragment frag = new FileDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, fileToDetail);
        args.putParcelable(ARG_ACCOUNT, account);
        frag.setArguments(args);
        return frag;
    }

    /**
     * Creates an empty details fragment.
     * <p>
     * It's necessary to keep a public constructor without parameters; the system uses it when tries
     * to reinstantiate a fragment automatically.
     */
    public FileDetailFragment() {
        super();
        mAccount = null;
        mLayout = R.layout.file_details_empty;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        mProgressController = new TransferProgressController((ComponentsGetter) getActivity());
        ProgressBar progressBar = mView.findViewById(R.id.fdProgressBar);
        mProgressController.setProgressBar(progressBar);

        // Allow or disallow touches with other visible windows
        if (mLayout == R.layout.file_details_fragment) {
            RelativeLayout fileDetailsLayout = getActivity().findViewById(R.id.fileDetailsLayout);
            fileDetailsLayout.setFilterTouchesWhenObscured(
                    PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(getContext())
            );
        } else {
            LinearLayout fileDetailsEmptyLayout = getActivity().findViewById(R.id.fileDetailsEmptyLayout);
            fileDetailsEmptyLayout.setFilterTouchesWhenObscured(
                    PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(getContext())
            );
        }
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        setFile(getArguments().getParcelable(ARG_FILE));
        mAccount = getArguments().getParcelable(ARG_ACCOUNT);

        if (savedInstanceState != null) {
            setFile(savedInstanceState.getParcelable(FileActivity.EXTRA_FILE));
            mAccount = savedInstanceState.getParcelable(FileActivity.EXTRA_ACCOUNT);
        }

        if (getFile() != null && mAccount != null) {
            mLayout = R.layout.file_details_fragment;
        }

        mView = inflater.inflate(mLayout, null);

        if (mLayout == R.layout.file_details_fragment) {
            mView.findViewById(R.id.fdCancelBtn).setOnClickListener(this);
        }

        updateFileDetails(false, false);
        return mView;
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(FileActivity.EXTRA_FILE, getFile());
        outState.putParcelable(FileActivity.EXTRA_ACCOUNT, mAccount);
    }

    @Override
    public void onStart() {
        super.onStart();
        mProgressController.startListeningProgressFor(getFile(), mAccount);
    }

    @Override
    public void onStop() {
        mProgressController.stopListeningProgressFor(getFile(), mAccount);
        super.onStop();
    }

    @Override
    public void onTransferServiceConnected() {
        if (mProgressController != null) {
            mProgressController.startListeningProgressFor(getFile(), mAccount);
        }
        updateFileDetails(false, false);    // TODO - really?
    }

    @Override
    public void onFileMetadataChanged(OCFile updatedFile) {
        if (updatedFile != null) {
            setFile(updatedFile);
        }
        updateFileDetails(false, false);
    }

    @Override
    public void onFileMetadataChanged() {
        updateFileDetails(false, true);
    }

    @Override
    public void onFileContentChanged() {
        setFiletype(getFile());     // to update thumbnail
    }

    @Override
    public void updateViewForSyncInProgress() {
        updateFileDetails(true, false);
    }

    @Override
    public void updateViewForSyncOff() {
        updateFileDetails(false, false);
    }

    @Override
    public View getView() {
        return super.getView() == null ? mView : super.getView();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.file_actions_menu, menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (mContainerActivity.getStorageManager() != null) {
            FileMenuFilter mf = new FileMenuFilter(
                    getFile(),
                    mContainerActivity.getStorageManager().getAccount(),
                    mContainerActivity,
                    getActivity()
            );
            mf.filter(menu, false, false, false, false);
        }

        // additional restriction for this fragment 
        MenuItem item = menu.findItem(R.id.action_see_details);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        // additional restriction for this fragment
        item = menu.findItem(R.id.action_move);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        // additional restriction for this fragment
        item = menu.findItem(R.id.action_copy);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        item = menu.findItem(R.id.action_search);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share_file: {
                mContainerActivity.getFileOperationsHelper().showShareFile(getFile());
                return true;
            }
            case R.id.action_open_file_with: {
                mContainerActivity.getFileOperationsHelper().openFile(getFile());
                return true;
            }
            case R.id.action_remove_file: {
                RemoveFilesDialogFragment dialog = RemoveFilesDialogFragment.newInstance(getFile());
                dialog.show(getFragmentManager(), FTAG_CONFIRMATION);
                return true;
            }
            case R.id.action_rename_file: {
                RenameFileDialogFragment dialog = RenameFileDialogFragment.newInstance(getFile());
                dialog.show(getFragmentManager(), FTAG_RENAME_FILE);
                return true;
            }
            case R.id.action_cancel_sync: {
                ((FileDisplayActivity) mContainerActivity).cancelTransference(getFile());
                return true;
            }
            case R.id.action_download_file:
            case R.id.action_sync_file: {
                mContainerActivity.getFileOperationsHelper().syncFile(getFile());
                return true;
            }
            case R.id.action_send_file: {
                // Obtain the file
                if (!getFile().isDown()) {  // Download the file                    
                    Timber.d("%s : File must be downloaded", getFile().getRemotePath());
                    ((FileDisplayActivity) mContainerActivity).startDownloadForSending(getFile());
                } else {
                    mContainerActivity.getFileOperationsHelper().sendDownloadedFile(getFile());
                }
                return true;
            }
            case R.id.action_set_available_offline: {
                mContainerActivity.getFileOperationsHelper().toggleAvailableOffline(getFile(), true);
                return true;
            }
            case R.id.action_unset_available_offline: {
                mContainerActivity.getFileOperationsHelper().toggleAvailableOffline(getFile(), false);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fdCancelBtn: {
                ((FileDisplayActivity) mContainerActivity).cancelTransference(getFile());
                break;
            }
            default:
                Timber.e("Incorrect view clicked!");
        }
    }

    /**
     * Check if the fragment was created with an empty layout. An empty fragment can't show file details, must be
     * replaced.
     *
     * @return True when the fragment was created with the empty layout.
     */
    public boolean isEmpty() {
        return (mLayout == R.layout.file_details_empty || getFile() == null || mAccount == null);
    }

    /**
     * Updates the view with all relevant details about that file.
     *
     * @param forcedTransferring Flag signaling if the file should be considered as downloading or uploading,
     *                           although {@link FileDownloaderBinder#isDownloading(Account, OCFile)}  and
     *                           {@link FileUploaderBinder#isUploading(Account, OCFile)} return false.
     * @param refresh            If 'true', try to refresh the whole file from the database
     */
    private void updateFileDetails(boolean forcedTransferring, boolean refresh) {
        if (readyToShow()) {
            FileDataStorageManager storageManager = mContainerActivity.getStorageManager();
            if (refresh && storageManager != null) {
                setFile(storageManager.getFileByPath(getFile().getRemotePath()));
            }
            OCFile file = getFile();

            // set file details
            setFilename(file.getName());
            setFiletype(file);
            setFilesize(file.getLength());

            setTimeModified(file.getModificationTimestamp());

            // configure UI for depending upon local state of the file
            FileDownloaderBinder downloaderBinder = mContainerActivity.getFileDownloaderBinder();
            FileUploaderBinder uploaderBinder = mContainerActivity.getFileUploaderBinder();
            if (forcedTransferring ||
                    (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, file)) ||
                    (uploaderBinder != null && uploaderBinder.isUploading(mAccount, file))
            ) {
                setButtonsForTransferring();

            } else if (file.isDown()) {

                setButtonsForDown();

            } else {
                // TODO load default preview image; when the local file is removed, the preview
                // remains there
                setButtonsForRemote();
            }
        }
        getView().invalidate();
    }

    /**
     * Checks if the fragment is ready to show details of a OCFile
     *
     * @return 'True' when the fragment is ready to show details of a file
     */
    private boolean readyToShow() {
        return (getFile() != null && mAccount != null && mLayout == R.layout.file_details_fragment);
    }

    /**
     * Updates the filename in view
     *
     * @param filename to set
     */
    private void setFilename(String filename) {
        TextView tv = getView().findViewById(R.id.fdFilename);
        if (tv != null) {
            tv.setText(filename);
        }
    }

    /**
     * Updates the MIME type in view
     *
     * @param file : An {@link OCFile}
     */
    private void setFiletype(OCFile file) {
        String mimetype = file.getMimeType();
        TextView tv = getView().findViewById(R.id.fdType);
        if (tv != null) {
            // mimetype      MIME type to set
            String printableMimetype = DisplayUtils.convertMIMEtoPrettyPrint(mimetype);
            tv.setText(printableMimetype);
        }

        ImageView iv = getView().findViewById(R.id.fdIcon);

        if (iv != null) {
            Bitmap thumbnail;
            iv.setTag(file.getId());

            if (file.isImage()) {
                String tagId = String.valueOf(file.getRemoteId());
                thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(tagId);

                if (thumbnail != null && !file.needsUpdateThumbnail()) {
                    iv.setImageBitmap(thumbnail);
                } else {
                    // generate new Thumbnail
                    if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, iv)) {
                        final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                                new ThumbnailsCacheManager.ThumbnailGenerationTask(
                                        iv, mContainerActivity.getStorageManager(), mAccount
                                );
                        if (thumbnail == null) {
                            thumbnail = ThumbnailsCacheManager.mDefaultImg;
                        }
                        final ThumbnailsCacheManager.AsyncThumbnailDrawable asyncDrawable =
                                new ThumbnailsCacheManager.AsyncThumbnailDrawable(
                                        MainApp.Companion.getAppContext().getResources(),
                                        thumbnail,
                                        task
                                );
                        iv.setImageDrawable(asyncDrawable);
                        task.execute(file);
                    }
                }
            } else {
                // Name of the file, to deduce the icon to use in case the MIME type is not precise enough
                String filename = file.getName();
                iv.setImageResource(MimetypeIconUtil.getFileTypeIconId(mimetype, filename));
            }
        }
    }

    /**
     * Updates the file size in view
     *
     * @param filesize in bytes to set
     */
    private void setFilesize(long filesize) {
        TextView tv = getView().findViewById(R.id.fdSize);
        if (tv != null) {
            tv.setText(DisplayUtils.bytesToHumanReadable(filesize, getActivity()));
        }
    }

    /**
     * Updates the time that the file was last modified
     *
     * @param milliseconds Unix time to set
     */
    private void setTimeModified(long milliseconds) {
        TextView tv = getView().findViewById(R.id.fdModified);
        if (tv != null) {
            tv.setText(DisplayUtils.unixTimeToHumanReadable(milliseconds));
        }
    }

    /**
     * Enables or disables buttons for a file being downloaded
     */
    private void setButtonsForTransferring() {
        if (!isEmpty()) {
            // show the progress bar for the transfer
            getView().findViewById(R.id.fdProgressBlock).setVisibility(View.VISIBLE);
            TextView progressText = getView().findViewById(R.id.fdProgressText);
            progressText.setVisibility(View.VISIBLE);
            FileDownloaderBinder downloaderBinder = mContainerActivity.getFileDownloaderBinder();
            FileUploaderBinder uploaderBinder = mContainerActivity.getFileUploaderBinder();
            //if (getFile().isDownloading()) {
            if (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, getFile())) {
                progressText.setText(R.string.downloader_download_in_progress_ticker);
            } else {
                if (uploaderBinder != null && uploaderBinder.isUploading(mAccount, getFile())) {
                    progressText.setText(R.string.uploader_upload_in_progress_ticker);
                }
            }
        }
    }

    /**
     * Enables or disables buttons for a file locally available
     */
    private void setButtonsForDown() {
        if (!isEmpty()) {
            // hides the progress bar
            getView().findViewById(R.id.fdProgressBlock).setVisibility(View.GONE);
            TextView progressText = getView().findViewById(R.id.fdProgressText);
            progressText.setVisibility(View.GONE);
        }
    }

    /**
     * Enables or disables buttons for a file not locally available
     */
    private void setButtonsForRemote() {
        if (!isEmpty()) {
            // hides the progress bar
            getView().findViewById(R.id.fdProgressBlock).setVisibility(View.GONE);
            TextView progressText = getView().findViewById(R.id.fdProgressText);
            progressText.setVisibility(View.GONE);
        }
    }

}
