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

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.google.android.exoplayer2.C;
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
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileActivity;

/**
 * An activity that plays media using {@link SimpleExoPlayer}.
 */
public class PreviewVideoActivity extends FileActivity implements ExoPlayer.EventListener,
        PrepareVideoPlayerAsyncTask.OnPrepareVideoPlayerTaskListener {

    private static final String TAG = PreviewVideoActivity.class.getSimpleName();

    /** Key to receive a flag signaling if the video should be started immediately */
    public static final String EXTRA_AUTOPLAY = "AUTOPLAY";

    /** Key to receive the position of the playback where the video should be put at start */
    public static final String EXTRA_START_POSITION = "START_POSITION";

    private final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    private Handler mainHandler;
    private SimpleExoPlayerView simpleExoPlayerView;

    private SimpleExoPlayer player;
    private DefaultTrackSelector trackSelector;

    private boolean mAutoplay; // when 'true', the playback starts immediately with the activity
    private long mPlaybackPosition; // continue the playback in the specified position

    private static final int NOT_FOUND_ERROR = 404;

    // Activity lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log_OC.v(TAG, "onCreate");

        clearResumePosition();

        setContentView(R.layout.video_preview);

        simpleExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.video_player);

        // Hide sync bar
        ProgressBar syncProgressBar = (ProgressBar) findViewById(R.id.syncProgressBar);
        syncProgressBar.setVisibility(View.GONE);

        // Hide full screen button
        ImageButton fullScreen = (ImageButton) findViewById(R.id.fullscreen_button);
        fullScreen.setVisibility(View.GONE);

        // Show exit full screen button
        ImageButton exitFullScreen = (ImageButton) findViewById(R.id.exit_fullscreen_button);
        exitFullScreen.setVisibility(View.VISIBLE);

        exitFullScreen.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


        Bundle extras = getIntent().getExtras();

        mAutoplay = extras.getBoolean(EXTRA_AUTOPLAY);
        mPlaybackPosition = extras.getLong(EXTRA_START_POSITION);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log_OC.v(TAG, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log_OC.v(TAG, "onResume");
        preparePlayer();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log_OC.v(TAG, "onPause");
        releasePlayer();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log_OC.v(TAG, "onStop");
    }

    // Handle full screen modes
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Let app go truly full screen using immersive mode, user swipes to display the system bars
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            } else {
                /**
                 * Use full screen but with a limitation, the least user interaction will cause
                 * navigation controls to reappear immediately
                 */
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
        }
    }

    // Video player internal methods

    private void preparePlayer() {

        // Create a default TrackSelector
        mainHandler = new Handler();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveVideoTrackSelection.Factory(BANDWIDTH_METER);
        trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, new DefaultLoadControl());
        player.addListener(this);
        simpleExoPlayerView.setPlayer(player);
        player.seekTo(mPlaybackPosition);
        player.setPlayWhenReady(mAutoplay);

        // Prepare video player asynchronously
        new PrepareVideoPlayerAsyncTask(getApplicationContext(), this, getFile(), getAccount(),
                mainHandler).execute();
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

    private void releasePlayer() {
        if (player != null) {
            mAutoplay = player.getPlayWhenReady();
            updateResumePosition();
            player.release();
            player = null;
            trackSelector = null;
            Log_OC.v(TAG, "playerReleased");
        }
    }

    private void updateResumePosition() {
        mPlaybackPosition = player.isCurrentWindowSeekable() ? Math.max(0, player.getCurrentPosition())
                : C.TIME_UNSET;
    }

    private void clearResumePosition() {
        mPlaybackPosition = C.TIME_UNSET;
    }

    // Video player eventListener implementation

    @Override
    public void onPlayerError(ExoPlaybackException error) {

        Log_OC.v(TAG, "Error in video player, what = " + error);

        showAlertDialog(PreviewVideoErrorAdapter.handlePreviewVideoError(error, this));
    }

    /**
     * Show an alert dialog with the error produced while playing the video
     *
     * @param previewVideoError player error with the needed info
     */
    private void showAlertDialog(PreviewVideoError previewVideoError) {

        new AlertDialog.Builder(this)
                .setMessage(previewVideoError.getErrorMessage())
                .setPositiveButton(android.R.string.VideoView_error_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                finish();
                            }
                        })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        // Do nothing.
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        // Do nothing.
    }

    @Override
    public void onPositionDiscontinuity() {
        // Do nothing
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
        // Do nothing
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        // Do nothing
    }

    // Back button behaviour

    @Override
    public void onBackPressed() {
        Log_OC.v(TAG, "onBackPressed");
        Intent i = new Intent();
        i.putExtra(EXTRA_AUTOPLAY, player.getPlayWhenReady());
        i.putExtra(EXTRA_START_POSITION, player.getCurrentPosition());
        player.release();
        setResult(RESULT_OK, i);
        super.onBackPressed();
    }
}