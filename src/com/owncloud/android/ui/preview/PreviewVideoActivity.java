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
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudBasicCredentials;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.OwnCloudSamlSsoCredentials;
import com.owncloud.android.media.MediaService;
import com.owncloud.android.ui.activity.FileActivity;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.VideoView;

import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 *  Activity implementing a basic video player.
 * 
 *  Used as an utility to preview video files contained in an ownCloud account.
 *  
 *  Currently, it always plays in landscape mode, full screen. When the playback ends,
 *  the activity is finished.
 */
public class PreviewVideoActivity extends FileActivity implements OnCompletionListener, OnPreparedListener, OnErrorListener, ExoPlayer.EventListener {

    /** Key to receive a flag signaling if the video should be started immediately */
    public static final String EXTRA_AUTOPLAY = "AUTOPLAY";
    
    /** Key to receive the position of the playback where the video should be put at start */
    public static final String EXTRA_START_POSITION = "START_POSITION";
    
    private static final String TAG = PreviewVideoActivity.class.getSimpleName();

    private Handler mainHandler;
    private SimpleExoPlayerView simpleExoPlayerView;

    private DataSource.Factory mediaDataSourceFactory;
    private SimpleExoPlayer player;
    private DefaultTrackSelector trackSelector;

    private boolean mAutoplay; // when 'true', the playback starts immediately with the activity
    private long mPlaybackPosition; // continue the playback in the specified position

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    /** 
     *  Called when the activity is first created.
     *  
     *  Searches for an {@link OCFile} and ownCloud {@link Account} holding it in the starting {@link Intent}.
     *  
     *  The {@link Account} is unnecessary if the file is downloaded; else, the {@link Account} is used to 
     *  try to stream the remote file - TODO get the streaming works
     * 
     *  {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log_OC.v(TAG, "onCreate");

        // Enable full screen
        this.requestWindowFeature(Window.FEATURE_NO_TITLE); //Remove title bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN); //Remove notification bar

        setContentView(R.layout.video_preview);

        simpleExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.video_player);
    
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            mPlaybackPosition = extras.getInt(EXTRA_START_POSITION);
            mAutoplay = extras.getBoolean(EXTRA_AUTOPLAY);
            
        } else {
            mPlaybackPosition = savedInstanceState.getInt(EXTRA_START_POSITION);
            mAutoplay = savedInstanceState.getBoolean(EXTRA_AUTOPLAY);
        }
    }


    private void preparePlayer() {

        // Create a default TrackSelector
        mainHandler = new Handler();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveVideoTrackSelection.Factory(BANDWIDTH_METER);
        trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, new DefaultLoadControl());
        player.addListener(this);
        simpleExoPlayerView.setPlayer(player);
        player.setPlayWhenReady(mAutoplay);

        try {

            // If the file is already downloaded, reproduce it locally
            Uri uri = getFile().isDown() ? getFile().getStorageUri() :
                    Uri.parse(AccountUtils.constructFullURLForAccount(this, getAccount()) +
                            Uri.encode(getFile().getRemotePath(), "/"));

            mediaDataSourceFactory = buildDataSourceFactory(true);

            MediaSource mediaSource = buildMediaSource(uri);

            player.prepare(mediaSource);

        } catch (AccountUtils.AccountNotFoundException e) {
            e.printStackTrace();
        }
    }

    private MediaSource buildMediaSource(Uri uri) {
        return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
                mainHandler, null);
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
        return new DefaultDataSourceFactory(this, bandwidthMeter,
                buildHttpDataSourceFactory(bandwidthMeter));
    }

    private HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {

        if (getFile().isDown()) {
            return new DefaultHttpDataSourceFactory(MainApp.getUserAgent(), bandwidthMeter);
        } else {

            OwnCloudAccount ocAccount = null;
            try {
                ocAccount = new OwnCloudAccount(getAccount(), this);

                //Get account credentials asynchronously
                final PreviewVideoFragment.GetCredentialsTask task = new PreviewVideoFragment.GetCredentialsTask();
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
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
//        outState.putInt(PreviewVideoActivity.EXTRA_START_POSITION, mVideoPlayer.getCurrentPosition());
//        outState.putBoolean(PreviewVideoActivity.EXTRA_AUTOPLAY , mVideoPlayer.isPlaying());
    }

    
    @Override
    public void onBackPressed() {
        Log_OC.v(TAG, "onBackPressed");
        Intent i = new Intent();
//        i.putExtra(EXTRA_AUTOPLAY, mVideoPlayer.isPlaying());
//        i.putExtra(EXTRA_START_POSITION, mVideoPlayer.getCurrentPosition());
        setResult(RESULT_OK, i);
//        mVideoPlayer.stopPlayback();
        super.onBackPressed();
    }

    
    /** 
     * Called when the file is ready to be played.
     * 
     * Just starts the playback.
     * 
     * @param   mp    {@link MediaPlayer} instance performing the playback.
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        Log_OC.v(TAG, "onPrepare");
//        mVideoPlayer.seekTo(mSavedPlaybackPosition);
        if (mAutoplay) { 
//            mVideoPlayer.start();
        }
//        mMediaController.show(5000);
    }
    
    
    /**
     * Called when the file is finished playing.
     *  
     * Rewinds the video
     * 
     * @param   mp    {@link MediaPlayer} instance performing the playback.
     */
    @Override
    public void onCompletion(MediaPlayer  mp) {
//        mVideoPlayer.seekTo(0);
    }
    
    
    /**
     * Called when an error in playback occurs.
     * 
     * @param   mp      {@link MediaPlayer} instance performing the playback.
     * @param   what    Type of error
     * @param   extra   Extra code specific to the error
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log_OC.e(TAG, "Error in video playback, what = " + what + ", extra = " + extra);
        
//        if (mMediaController != null) {
//            mMediaController.hide();
//        }
//
//        if (mVideoPlayer.getWindowToken() != null) {
//            String message = MediaService.getMessageForMediaError(this, what, extra);
//            new AlertDialog.Builder(this)
//                    .setMessage(message)
//                    .setPositiveButton(android.R.string.VideoView_error_button,
//                            new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog, int whichButton) {
//                                    PreviewVideoActivity.this.onCompletion(null);
//                                }
//                            })
//                    .setCancelable(false)
//                    .show();
//        }
        return true;
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

    }

    @Override
    public void onPositionDiscontinuity() {

    }

//    @Override
//    protected void onAccountSet(boolean stateWasRecovered) {
//        super.onAccountSet(stateWasRecovered);
//        if (getAccount() != null) {
//            OCFile file = getFile();
//            /// Validate handled file  (first image to preview)
//            if (file == null) {
//                throw new IllegalStateException("Instanced with a NULL OCFile");
//            }
//            if (!file.isVideo()) {
//                throw new IllegalArgumentException("Non-video file passed as argument");
//            }
//            file = getStorageManager().getFileById(file.getFileId());
//            if (file != null) {
//                if (file.isDown()) {
////                    mVideoPlayer.setVideoURI(file.getStorageUri());
//
//                } else {
//                    // not working yet
//                    String url;
//                    try {
//                        url = AccountUtils.constructFullURLForAccount(this, getAccount()) + file.getRemotePath();
//                        mVideoPlayer.setVideoURI(Uri.parse(url));
//                    } catch (AccountNotFoundException e) {
//                        onError(null, MediaService.OC_MEDIA_ERROR, R.string.media_err_no_account);
//                    }
//                }
//
//                // create and prepare control panel for the user
//                mMediaController = new MediaController(this);
//                mMediaController.setMediaPlayer(mVideoPlayer);
//                mMediaController.setAnchorView(mVideoPlayer);
//                mVideoPlayer.setMediaController(mMediaController);
//
//            } else {
//                finish();
//            }
//        } else {
//            finish();
//        }
//   }

    @Override
    public void onStart() {
        super.onStart();
        Log_OC.v(TAG, "onStart");

        if (Util.SDK_INT > 23) {
            preparePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((Util.SDK_INT <= 23 || player == null)) {
            preparePlayer();
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

        super.onStop();

        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    private void releasePlayer() {
        if (player != null) {
            mAutoplay = player.getPlayWhenReady();
            updateResumePosition();
            player.release();
            trackSelector = null;
        }
    }

    private void updateResumePosition() {
        mPlaybackPosition = player.getCurrentPosition();
    }
}