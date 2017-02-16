/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2016 ownCloud GmbH.
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
 *
 */
package com.owncloud.android.ui.preview;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.VideoView;

import com.google.android.exoplayer2.BuildConfig;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
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
import com.owncloud.android.media.MediaControlView;
import com.owncloud.android.media.MediaService;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.controller.TransferProgressController;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.DisplayUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


/**
 * This fragment shows a preview of a downloaded video file.
 *
 * Trying to get an instance with NULL {@link OCFile} or ownCloud {@link Account} values will
 * produce an {@link IllegalStateException}.
 * 
 * If the {@link OCFile} passed is not downloaded, an {@link IllegalStateException} is
 * generated on instantiation too.
 */
public class PreviewVideoFragment extends FileFragment implements OnTouchListener {

    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";
    private static final String EXTRA_PLAY_POSITION = "PLAY_POSITION";
    private static final String EXTRA_PLAYING = "PLAYING";

    private Account mAccount;
    private ProgressBar mProgressBar;
    private TransferProgressController mProgressController;
//    private VideoView mVideoPreview;
    private int mSavedPlaybackPosition;

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    private Handler mainHandler;
    private SimpleExoPlayerView simpleExoPlayerView;

    private DataSource.Factory mediaDataSourceFactory;
    private SimpleExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private boolean playerNeedsSource;

    private MediaControlView mMediaController = null;
    private boolean mAutoplay;
    public boolean mPrepared;

    private boolean shouldAutoPlay;
    private int resumeWindow;
    private long resumePosition;

    private static final String TAG = PreviewVideoFragment.class.getSimpleName();


    /**
     * Public factory method to create new PreviewVideoFragment instances.
     *
     * @param file                      An {@link OCFile} to preview in the fragment
     * @param account                   ownCloud account containing file
     * @param startPlaybackPosition     Time in milliseconds where the play should be started
     * @param autoplay                  If 'true', the file will be played automatically when
     *                                  the fragment is displayed.
     * @return                          Fragment ready to be used.
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
        args.putBoolean(EXTRA_PLAYING, autoplay);
        frag.setArguments(args);
        return frag;
    }


    /**
     * Creates an empty fragment to preview video files.

     * MUST BE KEPT: the system uses it when tries to reinstantiate a fragment automatically
     * (for instance, when the device is turned a aside).

     * DO NOT CALL IT: an {@link OCFile} and {@link Account} must be provided for a successful
     * construction
     */
    public PreviewVideoFragment() {
        super();
        mAccount = null;
        mSavedPlaybackPosition = 0;
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

        shouldAutoPlay = true;
        clearResumePosition();
        mediaDataSourceFactory = buildDataSourceFactory(true);
        mainHandler = new Handler();

        View view = inflater.inflate(R.layout.preview_video_fragment, container, false);
//        View rootView = view.findViewById(R.id.root);
//        rootView.setOnClickListener(getContext());

        mProgressBar = (ProgressBar) view.findViewById(R.id.syncProgressBar);
        DisplayUtils.colorPreLollipopHorizontalProgressBar(mProgressBar);

        simpleExoPlayerView = (SimpleExoPlayerView) view.findViewById(R.id.video_preview_full);
//        simpleExoPlayerView.setControllerVisibilityListener(getContext());
//        simpleExoPlayerView.requestFocus();


        /**
         * PREVIOUS
         */

//        mVideoPreview = (VideoView) view.findViewById(R.id.video_preview_fragment);
//        mVideoPreview.setOnTouchListener(this);
//        mMediaController = (MediaControlView) view.findViewById(R.id.media_controller);

        return view;
    }

    /**
     * Returns a new DataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *     DataSource factory.
     * @return A new DataSource factory.
     */
    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return buildDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *     DataSource factory.
     * @return A new HttpDataSource factory.
     */
    private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
        return buildHttpDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    private DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultDataSourceFactory(getContext(), bandwidthMeter,
                buildHttpDataSourceFactory(bandwidthMeter));
    }

    private HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new CustomHttpDataSourceFactory(Util.getUserAgent(getContext(), "ExoPlayerDemo"), bandwidthMeter);
    }

    private void clearResumePosition() {
        resumeWindow = C.INDEX_UNSET;
        resumePosition = C.TIME_UNSET;
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
            mSavedPlaybackPosition = args.getInt(PreviewVideoFragment.EXTRA_PLAY_POSITION);
            mAutoplay = args.getBoolean(PreviewVideoFragment.EXTRA_PLAYING);

        } else {
            file = savedInstanceState.getParcelable(PreviewVideoFragment.EXTRA_FILE);
            setFile(file);
            mAccount = savedInstanceState.getParcelable(PreviewVideoFragment.EXTRA_ACCOUNT);
            mSavedPlaybackPosition = savedInstanceState.getInt(PreviewVideoFragment.EXTRA_PLAY_POSITION);
            mAutoplay = savedInstanceState.getBoolean(PreviewVideoFragment.EXTRA_PLAYING);
        }

        if (file == null) {
            throw new IllegalStateException("Instanced with a NULL OCFile");
        }
        if (mAccount == null) {
            throw new IllegalStateException("Instanced with a NULL ownCloud Account");
        }
//        if (!file.isDown()) {
//            throw new IllegalStateException("There is no local file to preview");
//        }
        if (!file.isVideo()) {
            throw new IllegalStateException("Not a video file");
        }

        mProgressController = new TransferProgressController(mContainerActivity);
        mProgressController.setProgressBar(mProgressBar);

        prepareVideo();
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

//        mSavedPlaybackPosition = mVideoPreview.getCurrentPosition();
//        mAutoplay = mVideoPreview.isPlaying();
        outState.putInt(PreviewVideoFragment.EXTRA_PLAY_POSITION, mSavedPlaybackPosition);
        outState.putBoolean(PreviewVideoFragment.EXTRA_PLAYING, mAutoplay);
    }


    @Override
    public void onStart() {
        super.onStart();
        Log_OC.v(TAG, "onStart");

        if (Util.SDK_INT > 23) {
            initializePlayer();
        }

//        OCFile file = getFile();
//
//        if (file != null) {
//            mProgressController.startListeningProgressFor(file, mAccount);
//            stopAudio();
//            playVideo();
//        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((Util.SDK_INT <= 23 || player == null)) {
            initializePlayer();
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
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }


    private void stopAudio() {
        Intent i = new Intent(getActivity(), MediaService.class);
        i.setAction(MediaService.ACTION_STOP_ALL);
        getActivity().startService(i);
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

    private void playVideo() {
        // create and prepare control panel for the user
//        mMediaController.setMediaPlayer(mVideoPreview);

        if (getFile().isDown()) {
//            mVideoPreview.setVideoURI(getFile().getStorageUri());
        } else {
            streamVideo();
        }
    }

    private void streamVideo () {

        try {

            String url = AccountUtils.constructFullURLForAccount(getContext(), mAccount) + Uri.encode(getFile().getRemotePath(), "/");

            OwnCloudAccount ocAccount = new OwnCloudAccount(mAccount, getContext());

            //Get account credentials asynchronously
            final GetCredentialsTask task = new GetCredentialsTask();
            task.execute(ocAccount);

            OwnCloudCredentials credentials = task.get();

            String login = credentials.getUsername();
            String password = credentials.getAuthToken();

            // load the video file in the video player ;
            // when done, VideoHelper#onPrepared() will be called
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

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

//                mVideoPreview.setVideoURI(Uri.parse(url), params);

            } else {

                url.replace("//", "//" + login + ":" + password + "@");
//                mVideoPreview.setVideoURI(Uri.parse(url));
            }

        } catch (AccountUtils.AccountNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static class GetCredentialsTask extends AsyncTask<Object, Void, OwnCloudCredentials> {
        @Override
        protected OwnCloudCredentials doInBackground(Object... params) {
            Object account = params[0];
            if (account instanceof OwnCloudAccount){
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

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(OwnCloudCredentials credentials) {

        }
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
                stopPreview();
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
                stopPreview();
                mContainerActivity.getFileOperationsHelper().sendDownloadedFile(getFile());
                return true;
            }
            case R.id.action_sync_file: {
                mContainerActivity.getFileOperationsHelper().syncFile(getFile());
                return true;
            }
            case R.id.action_set_available_offline:{
                mContainerActivity.getFileOperationsHelper().toggleAvailableOffline(getFile(), true);
                return true;
            }
            case R.id.action_unset_available_offline:{
                mContainerActivity.getFileOperationsHelper().toggleAvailableOffline(getFile(), false);
                return true;
            }
            case R.id.action_download_file: {
                mContainerActivity.getFileOperationsHelper().syncFile(getFile());
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void seeDetails() {
        stopPreview();
        mContainerActivity.showDetails(getFile());
    }

    private void prepareVideo() {
        // create helper to get more control on the playback
//        VideoHelper videoHelper = new VideoHelper();
//        mVideoPreview.setOnPreparedListener(videoHelper);
//        mVideoPreview.setOnCompletionListener(videoHelper);
//        mVideoPreview.setOnErrorListener(videoHelper);
    }


//    private class VideoHelper implements OnCompletionListener, OnPreparedListener, OnErrorListener {
//
//        /**
//         * Called when the file is ready to be played.
//         * <p/>
//         * Just starts the playback.
//         *
//         * @param   vp    {@link MediaPlayer} instance performing the playback.
//         */
//        @Override
//        public void onPrepared(MediaPlayer vp) {
//            Log_OC.v(TAG, "onPrepared");
//            mVideoPreview.seekTo(mSavedPlaybackPosition);
//            if (mAutoplay) {
//                mVideoPreview.start();
//            }
//            mMediaController.setEnabled(true);
//            mMediaController.updatePausePlay();
//            mPrepared = true;
//        }
//
//
//        /**
//         * Called when the file is finished playing.
//         * <p/>
//         * Finishes the activity.
//         *
//         * @param mp {@link MediaPlayer} instance performing the playback.
//         */
//        @Override
//        public void onCompletion(MediaPlayer mp) {
//            Log_OC.v(TAG, "completed");
//            if (mp != null) {
//                mVideoPreview.seekTo(0);
//            } // else : called from onError()
//            mMediaController.updatePausePlay();
//        }
//
//
//        /**
//         * Called when an error in playback occurs.
//         *
//         * @param mp    {@link MediaPlayer} instance performing the playback.
//         * @param what  Type of error
//         * @param extra Extra code specific to the error
//         */
//        @Override
//        public boolean onError(MediaPlayer mp, int what, int extra) {
//            Log_OC.e(TAG, "Error in video playback, what = " + what + ", extra = " + extra);
//            if (mVideoPreview.getWindowToken() != null) {
//                String message = MediaService.getMessageForMediaError(
//                        getActivity(), what, extra);
//                new AlertDialog.Builder(getActivity())
//                        .setMessage(message)
//                        .setPositiveButton(android.R.string.VideoView_error_button,
//                                new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog, int whichButton) {
//                                        dialog.dismiss();
//                                        PreviewVideoFragment.VideoHelper.this.onCompletion(null);
//                                    }
//                                })
//                        .setCancelable(false)
//                        .show();
//            }
//            return true;
//        }
//
//    }


    @Override
    public void onStop() {
        Log_OC.v(TAG, "onStop");
        mProgressController.stopListeningProgressFor(getFile(), mAccount);
        mPrepared = false;
        super.onStop();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
//        if (event.getAction() == MotionEvent.ACTION_DOWN && v == mVideoPreview) {
//            // added a margin on the left to avoid interfering with gesture to open navigation drawer
//            if (event.getX() / Resources.getSystem().getDisplayMetrics().density > 24.0) {
//                startFullScreenVideo();
//            }
//            return true;
//        }
        return false;
    }


    private void startFullScreenVideo() {
        Intent i = new Intent(getActivity(), PlayerVideoActivity.class);
//        i.putExtra(FileActivity.EXTRA_ACCOUNT, mAccount);
//        i.putExtra(FileActivity.EXTRA_FILE, getFile());
//        i.putExtra(PreviewVideoActivity.EXTRA_AUTOPLAY, mVideoPreview.isPlaying());
//        i.putExtra(PreviewVideoActivity.EXTRA_START_POSITION, mVideoPreview.getCurrentPosition());

        i.setData(Uri.parse("http://techslides.com/demos/sample-videos/small.mp4"))
                .putExtra("extension", "")
                .setAction("com.google.android.exoplayer.demo.action.VIEW");

//        mVideoPreview.stopPlayback();
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
            mSavedPlaybackPosition = data.getExtras().getInt(
                    PreviewVideoActivity.EXTRA_START_POSITION);
            mAutoplay = data.getExtras().getBoolean(PreviewVideoActivity.EXTRA_AUTOPLAY);
        }
    }


    /**
     * Opens the previewed file with an external application.
     */
    private void openFile() {
        stopPreview();
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


    public void stopPreview() {
//        mVideoPreview.stopPlayback();
    }


    /**
     * Finishes the preview
     */
    private void finish() {
        getActivity().onBackPressed();
    }

}
