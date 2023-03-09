/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * @author Shashvat Kedia
 * Copyright (C) 2021 ownCloud GmbH.
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
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.domain.files.model.MimeTypeConstantsKt;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.extensions.ActivityExtKt;
import com.owncloud.android.extensions.FragmentExtKt;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.presentation.files.operations.FileOperation;
import com.owncloud.android.presentation.files.operations.FileOperationsViewModel;
import com.owncloud.android.presentation.files.removefile.RemoveFilesDialogFragment;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.controller.TransferProgressController;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import timber.log.Timber;

import java.util.ArrayList;
import java.util.Collections;

import static org.koin.java.KoinJavaComponent.get;

/**
 * This fragment shows a preview of a downloaded video file, or starts streaming if file is not
 * downloaded yet.
 * <p>
 * Trying to get an instance with NULL {@link OCFile} or ownCloud {@link Account} values will
 * produce an {@link IllegalStateException}.
 */
public class PreviewVideoFragment extends FileFragment implements View.OnClickListener,
        Player.Listener, PrepareVideoPlayerAsyncTask.OnPrepareVideoPlayerTaskListener {

    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";

    /**
     * Key to receive a flag signaling if the video should be started immediately
     */
    private static final String EXTRA_AUTOPLAY = "AUTOPLAY";

    /**
     * Key to receive the position of the playback where the video should be put at start
     */
    private static final String EXTRA_PLAY_POSITION = "START_POSITION";

    private Account mAccount;
    private ProgressBar mProgressBar;
    private TransferProgressController mProgressController;

    private PlayerView exoPlayerView;

    private ExoPlayer player;
    private DefaultTrackSelector trackSelector;

    private ImageButton fullScreenButton;

    private boolean mExoPlayerBooted = false;
    private boolean mAutoplay;
    private long mPlaybackPosition;

    FileOperationsViewModel fileOperationsViewModel = get(FileOperationsViewModel.class);

    /**
     * Public factory method to create new PreviewVideoFragment instances.
     *
     * @param file                  An {@link OCFile} to preview in the fragment
     * @param account               ownCloud account containing file
     * @param startPlaybackPosition Time in milliseconds where the play should be started
     * @param autoplay              If 'true', the file will be played automatically when
     *                              the fragment is displayed.
     * @return Fragment ready to be used.
     */
    public static PreviewVideoFragment newInstance(
            OCFile file,
            Account account,
            int startPlaybackPosition,
            boolean autoplay
    ) {
        PreviewVideoFragment frag = new PreviewVideoFragment();
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_FILE, file);
        args.putParcelable(EXTRA_ACCOUNT, account);
        args.putLong(EXTRA_PLAY_POSITION, startPlaybackPosition);
        args.putBoolean(EXTRA_AUTOPLAY, autoplay);
        frag.setArguments(args);
        return frag;
    }

    /**
     * Creates an empty fragment to preview video files.
     * <p>
     * MUST BE KEPT: the system uses it when tries to reinstantiate a fragment automatically
     * (for instance, when the device is turned a aside).
     * <p>
     * DO NOT CALL IT: an {@link OCFile} and {@link Account} must be provided for a successful
     * construction
     */
    public PreviewVideoFragment() {
        super();
        mAccount = null;
        mAutoplay = true;
    }

    // Fragment and activity lifecycle

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.video_preview, container, false);

        mProgressBar = view.findViewById(R.id.syncProgressBar);
        mProgressBar.setVisibility(View.GONE);

        exoPlayerView = view.findViewById(R.id.video_player);

        fullScreenButton = view.findViewById(R.id.fullscreen_button);

        fullScreenButton.setOnClickListener(this);

        return view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        OCFile file;
        if (savedInstanceState == null) {
            Bundle args = requireArguments();
            file = args.getParcelable(PreviewVideoFragment.EXTRA_FILE);
            setFile(file);
            mAccount = args.getParcelable(PreviewVideoFragment.EXTRA_ACCOUNT);
            mAutoplay = args.getBoolean(PreviewVideoFragment.EXTRA_AUTOPLAY);
            mPlaybackPosition = args.getLong(PreviewVideoFragment.EXTRA_PLAY_POSITION);

        } else {
            file = savedInstanceState.getParcelable(PreviewVideoFragment.EXTRA_FILE);
            setFile(file);
            mAccount = savedInstanceState.getParcelable(PreviewVideoFragment.EXTRA_ACCOUNT);
            mAutoplay = savedInstanceState.getBoolean(PreviewVideoFragment.EXTRA_AUTOPLAY);
            mPlaybackPosition = savedInstanceState.getLong(PreviewVideoFragment.EXTRA_PLAY_POSITION);
        }

        if (file == null) {
            throw new IllegalStateException("Instanced with a NULL OCFile");
        }
        if (mAccount == null) {
            throw new IllegalStateException("Instanced with a NULL ownCloud Account");
        }
        if (!file.isVideo()) {
            throw new IllegalStateException("Not a video file");
        }

        mProgressController = new TransferProgressController(mContainerActivity);
        mProgressController.setProgressBar(mProgressBar);
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.v("onResume");

        preparePlayer();
    }

    @Override
    public void onPause() {
        super.onPause();
        releasePlayer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Timber.v("onSaveInstanceState");

        outState.putParcelable(PreviewVideoFragment.EXTRA_FILE, getFile());
        outState.putParcelable(PreviewVideoFragment.EXTRA_ACCOUNT, mAccount);
        if (player != null) {
            outState.putBoolean(PreviewVideoFragment.EXTRA_AUTOPLAY, mAutoplay);
            outState.putLong(PreviewVideoFragment.EXTRA_PLAY_POSITION, player.getCurrentPosition());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Timber.v("onActivityResult %s", this);
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            mAutoplay = data.getExtras().getBoolean(PreviewVideoActivity.EXTRA_AUTOPLAY);
            mPlaybackPosition = data.getExtras().getLong(PreviewVideoActivity.EXTRA_START_POSITION);
            mExoPlayerBooted = false;
        }
    }

    // OnClickListener methods

    public void onClick(View view) {
        if (view == fullScreenButton) {
            releasePlayer();
            startFullScreenVideo();
        }
    }

    private void startFullScreenVideo() {

        Intent i = new Intent(getActivity(), PreviewVideoActivity.class);
        i.putExtra(EXTRA_AUTOPLAY, player.getPlayWhenReady());
        i.putExtra(EXTRA_PLAY_POSITION, player.getCurrentPosition());
        i.putExtra(FileActivity.EXTRA_FILE, getFile());

        startActivityForResult(i, FileActivity.REQUEST_CODE__LAST_SHARED + 1);
    }

    @Override
    public void updateViewForSyncInProgress() {
        mProgressController.showProgressBar();
    }

    @Override
    public void updateViewForSyncOff() {
        mProgressController.hideProgressBar();
    }

    // Menu options

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.file_actions_menu, menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);

        FileMenuFilter mf = new FileMenuFilter(
                getFile(),
                mAccount,
                mContainerActivity,
                getActivity()
        );
        mf.filter(menu, false, false, false, false);

        // additional restrictions for this fragment

        MenuItem item = menu.findItem(R.id.action_search);
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
                releasePlayer();
                mContainerActivity.getFileOperationsHelper().showShareFile(getFile());
                return true;
            }
            case R.id.action_open_file_with: {
                openFile();
                return true;
            }
            case R.id.action_remove_file: {
                RemoveFilesDialogFragment dialog = RemoveFilesDialogFragment.newInstanceForSingleFile(getFile());
                dialog.show(getParentFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
                return true;
            }
            case R.id.action_see_details: {
                seeDetails();
                return true;
            }
            case R.id.action_send_file: {
                releasePlayer();
                ActivityExtKt.sendDownloadedFilesByShareSheet(requireActivity(), Collections.singletonList(getFile()));
                return true;
            }
            case R.id.action_sync_file: {
                mContainerActivity.getFileOperationsHelper().syncFile(getFile());
                return true;
            }
            case R.id.action_set_available_offline: {
                ArrayList<OCFile> fileToSetAsAvailableOffline = new ArrayList<>();
                fileToSetAsAvailableOffline.add(getFile());
                fileOperationsViewModel.performOperation(new FileOperation.SetFilesAsAvailableOffline(fileToSetAsAvailableOffline));
                return true;
            }
            case R.id.action_unset_available_offline: {

                ArrayList<OCFile> fileToUnsetAsAvailableOffline = new ArrayList<>();
                fileToUnsetAsAvailableOffline.add(getFile());
                fileOperationsViewModel.performOperation(new FileOperation.UnsetFilesAsAvailableOffline(fileToUnsetAsAvailableOffline));
                return true;
            }
            case R.id.action_download_file: {
                releasePlayer();
                // Show progress bar
                mProgressBar.setVisibility(View.VISIBLE);
                mContainerActivity.getFileOperationsHelper().syncFile(getFile());
                return true;
            }
            case R.id.action_cancel_sync: {
                ArrayList<OCFile> fileList = new ArrayList<>();
                fileList.add(getFile());
                ((FileDisplayActivity) mContainerActivity).cancelFileTransference(fileList);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Video player internal methods
    private void preparePlayer() {

        AdaptiveTrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory();
        trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        // Video streaming is only supported at Jelly Bean or higher Android versions (>= API 16)

        // Create the player
        player = new ExoPlayer.Builder(requireContext()).setTrackSelector(trackSelector).setLoadControl(new DefaultLoadControl()).build();

        player.addListener(this);

        // Bind the player to the view.
        exoPlayerView.setPlayer(player);

        // Prepare video player asynchronously
        new PrepareVideoPlayerAsyncTask(getActivity(), this, getFile(), mAccount).execute();
    }

    /**
     * Called after preparing the player asynchronously
     *
     * @param mediaSource media to be played
     */
    @Override
    public void OnPrepareVideoPlayerTaskCallback(MediaSource mediaSource) {
        Timber.v("playerPrepared");
        player.prepare(mediaSource);
    }

    public void releasePlayer() {
        if (player != null) {
            mAutoplay = player.getPlayWhenReady();
            updateResumePosition();
            player.release();
            trackSelector = null;
            Timber.v("playerReleased");
        }
    }

    private void updateResumePosition() {
        mPlaybackPosition = player.getCurrentPosition();
    }

    // Video player eventListener implementation

    @Override
    public void onPlayerError(@NonNull PlaybackException error) {

        Timber.e(error, "Error in video player");

        showAlertDialog(PreviewVideoErrorAdapter.handlePreviewVideoError((ExoPlaybackException) error, getContext()));
    }

    /**
     * Show an alert dialog with the error produced while playing the video and initialize a
     * specific behaviour when necessary
     *
     * @param previewVideoError player error with the needed info
     */
    private void showAlertDialog(final PreviewVideoError previewVideoError) {
        new AlertDialog.Builder(requireActivity())
                .setMessage(previewVideoError.getErrorMessage())
                .setPositiveButton(android.R.string.VideoView_error_button,
                        (dialog, whichButton) -> {
                            if (previewVideoError.isFileSyncNeeded() && mContainerActivity != null) {
                                // Initialize the file download
                                mContainerActivity.getFileOperationsHelper().syncFile(getFile());
                            }

                            // This solution is not the best one but is an easy way to handle
                            // expiration error from here, without modifying so much code
                            // or involving other parts
                            if (previewVideoError.isParentFolderSyncNeeded()) {
                                // Start to sync the parent file folder
                                // TODO: Check if startSyncFolderOperation requires a folder or whether it would be enough with the remote path.
                                OCFile folder = new OCFile(
                                        getFile().getParentRemotePath(),
                                        MimeTypeConstantsKt.MIME_DIR,
                                        OCFile.ROOT_PARENT_ID,
                                        mAccount.name,
                                        getFile().getSpaceId()
                                );
                                ((FileDisplayActivity) requireActivity()).startSyncFolderOperation(folder, false);
                            }
                        })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        // If player is already, show full screen button
        if (playbackState == ExoPlayer.STATE_READY) {
            fullScreenButton.setVisibility(View.VISIBLE);
            if (player != null && !mExoPlayerBooted) {
                mExoPlayerBooted = true;
                player.seekTo(mPlaybackPosition);
                player.setPlayWhenReady(mAutoplay);
            }

        } else if (playbackState == ExoPlayer.STATE_ENDED) {
            fullScreenButton.setVisibility(View.GONE);
        }
    }

    // File extra methods
    @Override
    public void onFileMetadataChanged(OCFile updatedFile) {
        if (updatedFile != null) {
            setFile(updatedFile);
        }
        requireActivity().invalidateOptionsMenu();
    }

    @Override
    public void onFileMetadataChanged() {
        FileDataStorageManager storageManager = mContainerActivity.getStorageManager();
        if (storageManager != null) {
            setFile(storageManager.getFileByPath(getFile().getRemotePath(), null));
        }
        requireActivity().invalidateOptionsMenu();
    }

    @Override
    public void onFileContentChanged() {
        // Reset the player with the updated file
        releasePlayer();
        preparePlayer();

        if (player != null) {
            mPlaybackPosition = 0;
            player.setPlayWhenReady(mAutoplay);
        }
    }

    private void seeDetails() {
        releasePlayer();
        mContainerActivity.showDetails(getFile());
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Timber.v("onConfigurationChanged %s", this);
    }

    /**
     * Opens the previewed file with an external application.
     */
    private void openFile() {
        releasePlayer();
        mContainerActivity.getFileOperationsHelper().openFile(getFile());
        finish();
    }

    private void finish() {
        requireActivity().onBackPressed();
    }

    /**
     * Helper method to test if an {@link OCFile} can be passed to a {@link PreviewVideoFragment}
     * to be previewed.
     *
     * @param file File to test if can be previewed.
     * @return 'True' if the file can be handled by the fragment.
     */
    public static boolean canBePreviewed(OCFile file) {
        return (file != null && file.isVideo());
    }
}
