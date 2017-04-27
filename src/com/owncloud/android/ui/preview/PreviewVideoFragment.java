/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * Copyright (C) 2017 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.preview;

import android.accounts.Account;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.util.Util;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.controller.TransferProgressController;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.DisplayUtils;


/**
 * This fragment shows a preview of a downloaded video file, or starts streaming if file is not
 * downloaded yet.
 *
 * Trying to get an instance with NULL {@link OCFile} or ownCloud {@link Account} values will
 * produce an {@link IllegalStateException}.
 *
 */
public class PreviewVideoFragment extends FileFragment implements View.OnClickListener,
        ExoPlayer.EventListener, PrepareVideoPlayerAsyncTask.OnPrepareVideoPlayerTaskListener {

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

    private Handler mainHandler;
    private SimpleExoPlayerView simpleExoPlayerView;

    private SimpleExoPlayer player;
    private DefaultTrackSelector trackSelector;

    private ImageButton fullScreenButton;

    private boolean mAutoplay;
    private long mPlaybackPosition;

    private static final String TAG = PreviewVideoFragment.class.getSimpleName();

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

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
     *
     * MUST BE KEPT: the system uses it when tries to reinstantiate a fragment automatically
     * (for instance, when the device is turned a aside).
     *
     * DO NOT CALL IT: an {@link OCFile} and {@link Account} must be provided for a successful
     * construction
     */
    public PreviewVideoFragment() {
        super();
        mAccount = null;
        mAutoplay = true;
    }


    // Fragment and activity lifecicle

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log_OC.v(TAG, "onCreateView");

        View view = inflater.inflate(R.layout.video_preview, container, false);

        mProgressBar = (ProgressBar) view.findViewById(R.id.syncProgressBar);
        DisplayUtils.colorPreLollipopHorizontalProgressBar(mProgressBar);
        mProgressBar.setVisibility(View.GONE);

        simpleExoPlayerView = (SimpleExoPlayerView) view.findViewById(R.id.video_player);

        fullScreenButton = (ImageButton) view.findViewById(R.id.fullscreen_button);

        fullScreenButton.setOnClickListener(this);

        return view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log_OC.v(TAG, "onActivityCreated");

        OCFile file;
        if (savedInstanceState == null) {
            Bundle args = getArguments();
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
    public void onStart() {
        super.onStart();
        Log_OC.v(TAG, "onStart");

        OCFile file = getFile();

        if (file != null) {
            mProgressController.startListeningProgressFor(file, mAccount);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log_OC.v(TAG, "onResume");

        preparePlayer();

        if (player != null) {
            player.seekTo(mPlaybackPosition);
            player.setPlayWhenReady(mAutoplay);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log_OC.v(TAG, "onPause");

        releasePlayer();
    }

    @Override
    public void onStop() {
        Log_OC.v(TAG, "onStop");

        super.onStop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log_OC.v(TAG, "onSaveInstanceState");

        outState.putParcelable(PreviewVideoFragment.EXTRA_FILE, getFile());
        outState.putParcelable(PreviewVideoFragment.EXTRA_ACCOUNT, mAccount);
        if (player!= null) {
            outState.putBoolean(PreviewVideoFragment.EXTRA_AUTOPLAY, mAutoplay);
            outState.putLong(PreviewVideoFragment.EXTRA_PLAY_POSITION, player.getCurrentPosition());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log_OC.v(TAG, "onActivityResult " + this);
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            mAutoplay = data.getExtras().getBoolean(PreviewVideoActivity.EXTRA_AUTOPLAY);
            mPlaybackPosition = data.getExtras().getLong(PreviewVideoActivity.EXTRA_START_POSITION);
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

    // Progress bar
    @Override
    public void onTransferServiceConnected() {
        if (mProgressController != null) {
            mProgressController.startListeningProgressFor(getFile(), mAccount);
        }
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

        FileMenuFilter mf = new FileMenuFilter(
                getFile(),
                mAccount,
                mContainerActivity,
                getActivity()
        );
        mf.filter(menu);

        // additional restrictions for this fragment

        MenuItem item = menu.findItem(R.id.action_sort);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

            item = menu.findItem(R.id.action_switch_view);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        item = menu.findItem(R.id.action_sync_account);
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
                RemoveFilesDialogFragment dialog = RemoveFilesDialogFragment.newInstance(getFile());
                dialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
                return true;
            }
            case R.id.action_see_details: {
                seeDetails();
                return true;
            }
            case R.id.action_send_file: {
                releasePlayer();
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
            case R.id.action_download_file: {
                releasePlayer();
                // Show progress bar
                mProgressBar.setVisibility(View.VISIBLE);
                mContainerActivity.getFileOperationsHelper().syncFile(getFile());
                return true;
            }
            case R.id.action_cancel_sync: {
                ((FileDisplayActivity)mContainerActivity).cancelTransference(getFile());
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Video player internal methods
    private void preparePlayer() {

        // Create a default TrackSelector
        mainHandler = new Handler();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveVideoTrackSelection.Factory(BANDWIDTH_METER);
        trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        // Video streaming is only supported at Jelly Bean or higher Android versions (>= API 16)
        if (Util.SDK_INT >= 16) {

            // Create the player
            player = ExoPlayerFactory.newSimpleInstance(getContext(), trackSelector,
                    new DefaultLoadControl());

            player.addListener(this);

            // Bind the player to the view.
            simpleExoPlayerView.setPlayer(player);

            // Prepare video player asynchronously
            new PrepareVideoPlayerAsyncTask(getActivity(), this,
                    getFile(), mAccount, mainHandler).execute();
        } else {

            // Show dialog with error and starts file download
            showAlertDialog(new PreviewVideoError(getString(R.string.previewing_video_not_supported),
                    true, false));
        }
    }

    /**
     * Called after preparing the player asynchronously
     * @param mediaSource media to be played
     */
    @Override
    public void OnPrepareVideoPlayerTaskCallback(MediaSource mediaSource) {
        Log_OC.v(TAG, "playerPrepared");
        player.prepare(mediaSource);
    }

    public void releasePlayer() {
        if (player != null) {
            mAutoplay = player.getPlayWhenReady();
            updateResumePosition();
            player.release();
            trackSelector = null;
            Log_OC.v(TAG, "playerReleased");
        }
    }

    private void updateResumePosition() {
        mPlaybackPosition = player.getCurrentPosition();
    }

    // Video player eventListener implementation

    @Override
    public void onPlayerError(ExoPlaybackException error) {

        Log_OC.v(TAG, "Error in video player, what = " + error);

        showAlertDialog(PreviewVideoErrorAdapter.handlePreviewVideoError(error, getContext()));
    }

    /**
     * Show an alert dialog with the error produced while playing the video and initialize a
     * specific behaviour when necessary
     *
     * @param previewVideoError player error with the needed info
     */
    private void showAlertDialog(final PreviewVideoError previewVideoError) {

        new AlertDialog.Builder(getActivity())
                .setMessage(previewVideoError.getErrorMessage())
                .setPositiveButton(android.R.string.VideoView_error_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                if (previewVideoError.isFileSyncNeeded() && mContainerActivity!= null) {
                                    // Initialize the file download
                                    mContainerActivity.getFileOperationsHelper().syncFile(getFile());
                                }

                                // This solution is not the best one but is an easy way to handle
                                // SAML expiration error from here, without modifying so much code
                                // or involving other parts
                                if (previewVideoError.isParentFolderSyncNeeded()) {
                                    // Start to sync the parent file folder
                                    OCFile folder = new OCFile(getFile().getParentRemotePath());
                                    ((FileDisplayActivity) getActivity()).
                                            startSyncFolderOperation(folder, false);
                                }
                            }
                        })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        // Do nothing
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        // Do nothing
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        // If player is already, show full screen button
        if (playbackState == ExoPlayer.STATE_READY) {
            fullScreenButton.setVisibility(View.VISIBLE);
        } else if (playbackState == ExoPlayer.STATE_ENDED) {
            fullScreenButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPositionDiscontinuity() {
        // Do nothing
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
        // Do nothing
    }


    // File extra methods
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
    public void onConfigurationChanged(Configuration newConfig) {
        Log_OC.v(TAG, "onConfigurationChanged " + this);
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
        getActivity().onBackPressed();
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
