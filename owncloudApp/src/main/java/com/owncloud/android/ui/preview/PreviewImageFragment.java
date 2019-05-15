/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * @author Abel García de Prada
 * @author Shashvat Kedia
 * Copyright (C) 2019 ownCloud GmbH.
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
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentStatePagerAdapter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.controller.TransferProgressController;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.PreferenceUtils;

import java.io.File;

/**
 * This fragment shows a preview of a downloaded image.
 *
 * Trying to get an instance with a NULL {@link OCFile} will produce an
 * {@link IllegalStateException}.
 *
 * If the {@link OCFile} passed is not downloaded, an {@link IllegalStateException} is generated on
 * instantiation too.
 */
public class PreviewImageFragment extends FileFragment {

    private static final String TAG = PreviewImageFragment.class.getSimpleName();

    public static final String EXTRA_FILE = "FILE";

    private static final String ARG_FILE = "FILE";
    private static final String ARG_ACCOUNT = "ACCOUNT";
    private static final String ARG_IGNORE_FIRST = "IGNORE_FIRST";

    private ProgressBar mProgressBar;
    private TransferProgressController mProgressController;
    private PhotoView mImageView;

    private Bitmap mBitmap = null;

    private Account mAccount;
    private boolean mIgnoreFirstSavedState;

    /**
     * Public factory method to create a new fragment that previews an image.
     *
     * Android strongly recommends keep the empty constructor of fragments as the only public
     * constructor, and
     * use {@link #setArguments(Bundle)} to set the needed arguments.
     *
     * This method hides to client objects the need of doing the construction in two steps.
     *
     * @param file                      An {@link OCFile} to preview as an image in the fragment
     * @param account                   ownCloud account containing file
     * @param ignoreFirstSavedState     Flag to work around an unexpected behaviour of
     *                                  {@link FragmentStatePagerAdapter}
     *                                  ; TODO better solution
     * @return Fragment ready to be used.
     */
    public static PreviewImageFragment newInstance(
            OCFile file,
            Account account,
            boolean ignoreFirstSavedState
    ) {
        PreviewImageFragment frag = new PreviewImageFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, file);
        args.putParcelable(ARG_ACCOUNT, account);
        args.putBoolean(ARG_IGNORE_FIRST, ignoreFirstSavedState);
        frag.setArguments(args);
        return frag;
    }

    /**
     *  Creates an empty fragment for image previews.
     *
     *  MUST BE KEPT: the system uses it when tries to reinstantiate a fragment automatically
     *  (for instance, when the device is turned a aside).
     *
     *  DO NOT CALL IT: an {@link OCFile} and {@link Account} must be provided for a successful
     *  construction
     */
    public PreviewImageFragment() {
        mIgnoreFirstSavedState = false;
        mAccount = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        setFile(args.getParcelable(ARG_FILE));
        // TODO better in super, but needs to check ALL the class extending FileFragment;
        // not right now

        mIgnoreFirstSavedState = args.getBoolean(ARG_IGNORE_FIRST);
        setHasOptionsMenu(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.preview_image_fragment, container, false);
        view.setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(getContext())
        );

        mProgressBar = view.findViewById(R.id.syncProgressBar);
        DisplayUtils.colorPreLollipopHorizontalProgressBar(mProgressBar);
        mImageView = view.findViewById(R.id.photo_view);
        mImageView.setVisibility(View.GONE);
        mImageView.setOnClickListener(v -> {
            if (getActivity() != null) {
                ((PreviewImageActivity) getActivity()).toggleFullScreen();
            }
        });
        TextView messageview = view.findViewById(R.id.message);
        messageview.setVisibility(View.GONE);
        ProgressBar progressWheel = view.findViewById(R.id.progressWheel);
        progressWheel.setVisibility(View.VISIBLE);
        return view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mProgressController = new TransferProgressController(mContainerActivity);
        mProgressController.setProgressBar(mProgressBar);
        mProgressController.hideProgressBar();
        if (savedInstanceState != null) {
            if (!mIgnoreFirstSavedState) {
                OCFile file = savedInstanceState.getParcelable(PreviewImageFragment.EXTRA_FILE);
                setFile(file);
            } else {
                mIgnoreFirstSavedState = false;
            }
        }
        mAccount = getArguments().getParcelable(PreviewAudioFragment.EXTRA_ACCOUNT);

        if (mAccount == null) {
            throw new IllegalStateException("Instanced with a NULL ownCloud Account");
        }
        if (getFile() == null) {
            throw new IllegalStateException("Instanced with a NULL OCFile");
        }
        if (!getFile().isDown()) {
            throw new IllegalStateException("There is no local file to preview");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(PreviewImageFragment.EXTRA_FILE, getFile());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getFile() != null) {
            mProgressController.startListeningProgressFor(getFile(), mAccount);
            loadAndShowImage();
        }
    }

    @Override
    public void onStop() {
        Log_OC.d(TAG, "onStop starts");
        mProgressController.stopListeningProgressFor(getFile(), mAccount);
        super.onStop();
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

        if (mContainerActivity.getStorageManager() != null && getFile() != null) {
            // Update the file
            setFile(mContainerActivity.getStorageManager().getFileById(getFile().getFileId()));

            FileMenuFilter mf = new FileMenuFilter(
                    getFile(),
                    mContainerActivity.getStorageManager().getAccount(),
                    mContainerActivity,
                    getActivity()
            );
            mf.filter(menu, false, false, false,false);
        }

        // additional restriction for this fragment 
        // TODO allow renaming in PreviewImageFragment
        MenuItem item = menu.findItem(R.id.action_rename_file);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        // additional restriction for this fragment 
        // TODO allow refresh file in PreviewImageFragment
        item = menu.findItem(R.id.action_sync_file);
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
                openFile();
                return true;
            }
            case R.id.action_remove_file: {
                RemoveFilesDialogFragment dialog = RemoveFilesDialogFragment.newInstance(getFile());
                dialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
                return true;
            }
            case R.id.action_see_details: {
                seeDetails();
                return true;
            }
            case R.id.action_send_file: {
                mContainerActivity.getFileOperationsHelper().sendDownloadedFile(getFile());
                return true;
            }
            case R.id.action_sync_file: {
                mContainerActivity.getFileOperationsHelper().syncFile(getFile());
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

    private void seeDetails() {
        mContainerActivity.showDetails(getFile());
    }

    @Override
    public void onDestroy() {
        if (mBitmap != null) {
            mBitmap.recycle();
            System.gc();
            // putting this in onStop() is just the same; the fragment is always destroyed by
            // {@link FragmentStatePagerAdapter} when the fragment in swiped further than the
            // valid offscreen distance, and onStop() is never called before than that
        }
        super.onDestroy();
    }

    /**
     * Opens the previewed image with an external application.
     */
    private void openFile() {
        mContainerActivity.getFileOperationsHelper().openFile(getFile());
        finish();
    }

    @Override
    public void onTransferServiceConnected() {
        if (mProgressController != null) {
            mProgressController.startListeningProgressFor(getFile(), mAccount);
        }
    }

    @Override
    public void onFileMetadataChanged(OCFile updatedFile) {
        if (updatedFile != null) {
            setFile(updatedFile);
        }
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onFileMetadataChanged() {
        FileDataStorageManager storageManager = mContainerActivity.getStorageManager();
        if (storageManager != null) {
            setFile(storageManager.getFileByPath(getFile().getRemotePath()));
        }
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onFileContentChanged() {
        loadAndShowImage();
    }

    @Override
    public void updateViewForSyncInProgress() {
        mProgressController.showProgressBar();
    }

    @Override
    public void updateViewForSyncOff() {
        mProgressController.hideProgressBar();
    }

    private void loadAndShowImage() {

        Glide.with(getContext())
                .load(new File(getFile().getStoragePath()))
                .listener(new RequestListener<Drawable>() {

                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target,
                                                boolean isFirstResource) {
                        Log_OC.e(TAG, "Error loading image", e);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target,
                                                   DataSource dataSource, boolean isFirstResource) {
                        Log_OC.d(TAG, "Loading image " + getFile().getFileName());
                        return false;
                    }
                })
                .into(mImageView);

        mImageView.setVisibility(View.VISIBLE);
    }

    /**
     * Helper method to test if an {@link OCFile} can be passed to a {@link PreviewImageFragment}
     * to be previewed.
     *
     * @param file      File to test if can be previewed.
     * @return          'True' if the file can be handled by the fragment.
     */
    public static boolean canBePreviewed(OCFile file) {
        return (file != null && file.isImage());
    }

    /**
     * Finishes the preview
     */
    private void finish() {
        Activity container = getActivity();
        container.finish();
    }

    public PhotoView getImageView() {
        return mImageView;
    }
}