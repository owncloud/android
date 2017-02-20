/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * Copyright (C) 2016 ownCloud GmbH.
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
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
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
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudBasicCredentials;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.OwnCloudSamlSsoCredentials;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.controller.TransferProgressController;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.DisplayUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;


/**
 * This fragment shows a preview of a downloaded video file.
 * <p>
 * Trying to get an instance with NULL {@link OCFile} or ownCloud {@link Account} values will
 * produce an {@link IllegalStateException}.
 * <p>
 * If the {@link OCFile} passed is not downloaded, an {@link IllegalStateException} is
 * generated on instantiation too.
 */
public class PreviewVideoFragment extends FileFragment implements ExoPlayer.EventListener {

    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";

    /** Key to receive a flag signaling if the video should be started immediately */
    private static final String EXTRA_AUTOPLAY = "AUTOPLAY";

    /** Key to receive the position of the playback where the video should be put at start */
    private static final String EXTRA_PLAY_POSITION = "START_POSITION";

    private Account mAccount;
    private ProgressBar mProgressBar;
    private TransferProgressController mProgressController;

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    private Handler mainHandler;
    private SimpleExoPlayerView simpleExoPlayerView;

    private SimpleExoPlayer player;
    private DefaultTrackSelector trackSelector;

    private boolean mAutoplay;
    private long mPlaybackPosition;

    private static final String TAG = PreviewVideoFragment.class.getSimpleName();


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
        args.putInt(EXTRA_PLAY_POSITION, startPlaybackPosition);
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

        ImageButton fullScreen = (ImageButton) view.findViewById(R.id.exo_fullscreen_button);

        fullScreen.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                releasePlayer();
                startFullScreenVideo();
            }
        });

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

        preparePlayer();

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
        outState.putBoolean(PreviewVideoFragment.EXTRA_AUTOPLAY, mAutoplay);
        outState.putLong(PreviewVideoFragment.EXTRA_PLAY_POSITION, player.getCurrentPosition());
    }

    private void preparePlayer() {

        // Create a default TrackSelector
        mainHandler = new Handler();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveVideoTrackSelection.Factory(BANDWIDTH_METER);
        trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        player = ExoPlayerFactory.newSimpleInstance(getContext(), trackSelector, new DefaultLoadControl());
        player.addListener(this);
        simpleExoPlayerView.setPlayer(player);

        try {

            // If the file is already downloaded, reproduce it locally
            Uri uri = getFile().isDown() ? getFile().getStorageUri() :
                    Uri.parse(AccountUtils.constructFullURLForAccount(getContext(), mAccount) +
                            Uri.encode(getFile().getRemotePath(), "/"));

            DataSource.Factory mediaDataSourceFactory = buildDataSourceFactory(true);

            MediaSource mediaSource = buildMediaSource(mediaDataSourceFactory, uri);

            player.prepare(mediaSource);

        } catch (AccountUtils.AccountNotFoundException e) {
            e.printStackTrace();
        }
    }


    private MediaSource buildMediaSource(DataSource.Factory mediaDataSourceFactory, Uri uri) {
        return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
                mainHandler, null);
    }

    private void updateResumePosition() {
        mPlaybackPosition = player.getCurrentPosition();
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

    }

    @Override
    public void updateViewForSyncInProgress() {
        mProgressController.showProgressBar();
    }

    @Override
    public void updateViewForSyncOff() {
        mProgressController.hideProgressBar();
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        String message = error.getCause().getMessage();
        if (message == null) {
            message = getString(R.string.common_error_unknown);
        }
        new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setPositiveButton(android.R.string.VideoView_error_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onPositionDiscontinuity() {

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

        FileMenuFilter mf = new FileMenuFilter(
                getFile(),
                mAccount,
                mContainerActivity,
                getActivity()
        );
        mf.filter(menu);

        // additional restriction for this fragment 
        // TODO allow renaming in PreviewVideoFragment
        MenuItem item = menu.findItem(R.id.action_rename_file);
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void seeDetails() {
        releasePlayer();
        mContainerActivity.showDetails(getFile());
    }


    private void startFullScreenVideo() {
        Intent i = new Intent(getActivity(), PlayerVideoActivity.class);
        i.putExtra(EXTRA_AUTOPLAY, player.getPlayWhenReady());
        i.putExtra(EXTRA_PLAY_POSITION, player.getCurrentPosition());
        i.putExtra(FileActivity.EXTRA_FILE, getFile());

        startActivityForResult(i, FileActivity.REQUEST_CODE__LAST_SHARED + 1);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log_OC.v(TAG, "onConfigurationChanged " + this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log_OC.v(TAG, "onActivityResult " + this);
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            mAutoplay = data.getExtras().getBoolean(PlayerVideoActivity.EXTRA_AUTOPLAY);
        }
    }

    /**
     * Opens the previewed file with an external application.
     */
    private void openFile() {
        releasePlayer();
        mContainerActivity.getFileOperationsHelper().openFile(getFile());
        finish();
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

    public void releasePlayer() {
        if (player != null) {
            mAutoplay = player.getPlayWhenReady();
            updateResumePosition();
            player.release();
            trackSelector = null;
        }
    }

    /**
     * Finishes the preview
     */
    private void finish() {
        getActivity().onBackPressed();
    }

    /**
     * Returns a new DataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *                          DataSource factory.
     * @return A new DataSource factory.
     */
    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return buildDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    private DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultDataSourceFactory(getContext(), bandwidthMeter,
                buildHttpDataSourceFactory(bandwidthMeter));
    }

    private HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {

        if (getFile().isDown()) {
            return new DefaultHttpDataSourceFactory(MainApp.getUserAgent(), bandwidthMeter);
        } else {

            OwnCloudAccount ocAccount = null;
            try {
                ocAccount = new OwnCloudAccount(mAccount, getContext());

                //Get account credentials asynchronously
                final GetCredentialsTask task = new GetCredentialsTask();
                task.execute(ocAccount);

                OwnCloudCredentials credentials = task.get();

                String login = credentials.getUsername();
                String password = credentials.getAuthToken();

                Map<String, String> params = new HashMap<String, String>(1);

                if (credentials instanceof OwnCloudBasicCredentials) {
                    // Basic auth
                    String cred = login + ":" + password;
                    String auth = "Basic " + Base64.encodeToString(cred.getBytes(), Base64.URL_SAFE);
                    params.put("Authorization", auth);
                } else if (credentials instanceof OwnCloudSamlSsoCredentials) {
                    // SAML SSO
                    params.put("Cookie", password);
                }

                return new CustomHttpDataSourceFactory(MainApp.getUserAgent(), bandwidthMeter, params);

            } catch (AccountUtils.AccountNotFoundException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static class GetCredentialsTask extends AsyncTask<Object, Void, OwnCloudCredentials> {
        @Override
        protected OwnCloudCredentials doInBackground(Object... params) {
            Object account = params[0];
            if (account instanceof OwnCloudAccount) {
                try {
                    OwnCloudAccount ocAccount = (OwnCloudAccount) account;
                    ocAccount.loadCredentials(MainApp.getAppContext());
                    return ocAccount.getCredentials();
                } catch (AccountUtils.AccountNotFoundException e) {
                    e.printStackTrace();
                } catch (OperationCanceledException e) {
                    e.printStackTrace();
                } catch (AuthenticatorException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log_OC.v(TAG, "onStart");

        OCFile file = getFile();

        if (file != null) {
            mProgressController.startListeningProgressFor(file, mAccount);
        }

        if (Util.SDK_INT > 23) {
            player.seekTo(mPlaybackPosition);
            player.setPlayWhenReady(mAutoplay);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((Util.SDK_INT <= 23 || player == null)) {
            player.seekTo(mPlaybackPosition);
            player.setPlayWhenReady(mAutoplay);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        Log_OC.v(TAG, "onStop");
        mProgressController.stopListeningProgressFor(getFile(), mAccount);

        super.onStop();

        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }
}
