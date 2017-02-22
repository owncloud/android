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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.google.android.exoplayer2.C;
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
import com.google.android.exoplayer2.util.Util;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileActivity;

/**
 * An activity that plays media using {@link SimpleExoPlayer}.
 */
public class PreviewVideoActivity extends FileActivity implements   ExoPlayer.EventListener {

    private static final String TAG = PreviewVideoActivity.class.getSimpleName();

    /** Key to receive a flag signaling if the video should be started immediately */
    public static final String EXTRA_AUTOPLAY = "AUTOPLAY";

    /** Key to receive the position of the playback where the video should be put at start */
    public static final String EXTRA_START_POSITION = "START_POSITION";

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    private Handler mainHandler;
    private SimpleExoPlayerView simpleExoPlayerView;

    private SimpleExoPlayer player;
    private DefaultTrackSelector trackSelector;

    private boolean mAutoplay; // when 'true', the playback starts immediately with the activity
    private long mPlaybackPosition; // continue the playback in the specified position

    // Activity lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable full screen
        this.requestWindowFeature(Window.FEATURE_NO_TITLE); //Remove title bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN); //Remove notification bar

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
                releasePlayer();
                finish();
            }
        });


        Bundle extras = getIntent().getExtras();

        mAutoplay = extras.getBoolean(EXTRA_AUTOPLAY);
        mPlaybackPosition = extras.getLong(EXTRA_START_POSITION);
    }

    @Override
    public void onStart() {
        super.onStart();
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
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
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

        try {

            // If the file is already downloaded, reproduce it locally
            Uri uri = getFile().isDown() ? getFile().getStorageUri() :
                    Uri.parse(AccountUtils.constructFullURLForAccount(this, getAccount()) +
                            Uri.encode(getFile().getRemotePath(), "/"));

            DataSource.Factory mediaDataSourceFactory = PreviewUtils.buildDataSourceFactory(true,
                    this, getFile(), getAccount());

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

    private void releasePlayer() {
        if (player != null) {
            mAutoplay = player.getPlayWhenReady();
            updateResumePosition();
            player.release();
            player = null;
            trackSelector = null;
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
        String message = error.getCause().getMessage();
        if (message == null) {
            message = getString(R.string.common_error_unknown);
        }
        new AlertDialog.Builder(this)
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