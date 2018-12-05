/**
 * ownCloud Android client application
 *
 * @author Shashvat Kedia
 * @author Christian Schabesberger
 * Copyright (C) 2018 ownCloud GmbH.
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

package com.owncloud.android.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.andrognito.patternlockview.PatternLockView;
import com.andrognito.patternlockview.listener.PatternLockViewListener;
import com.andrognito.patternlockview.utils.PatternLockUtils;
import com.owncloud.android.BuildConfig;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.util.List;

public class PatternLockActivity extends AppCompatActivity {

    private static final String TAG = PatternLockActivity.class.getSimpleName();

    public final static String PREFERENCE_SET_PATTERN = "set_pattern";
    public final static String ACTION_REQUEST_WITH_RESULT = "ACTION_REQUEST_WITH_RESULT";
    public final static String ACTION_CHECK_WITH_RESULT = "ACTION_CHECK_WITH_RESULT";
    public final static String ACTION_CHECK = "ACTION_CHECK_PATTERN";

    public final static String KEY_PATTERN = "KEY_PATTERN";
    public final static String KEY_CHECK_RESULT = "KEY_CHECK_PATTERN_RESULT";

    private static String KEY_CONFIRMING_PATTERN = "CONFIRMING_PATTERN";
    private static String KEY_PATTERN_STRING = "PATTERN_STRING";
    private static String PATTERN_HEADER_VIEW_TEXT = "PATTERN_HEADER_VIEW_TEXT";
    private static String PATTERN_EXP_VIEW_STATE = "PATTERN_EXP_VIEW_STATE";
    private static String COUNT_VALUE = "COUNT_VALUE";

    private boolean mPatternPresent = false;
    private String mPatternValue;
    private String mNewPatternValue;

    private TextView mPatternHeader;
    private TextView mPatternExplanation;
    private PatternLockView mPatternLockView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!BuildConfig.DEBUG) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        setContentView(R.layout.activity_pattern_lock);
        String mPatternHeaderViewText = "";
        /**
        * mPatternExpShouldVisible holds the boolean value that signifies weather the patternExpView should be visible or not.
        * it is set to true when the pattern is set and when the pattern is removed.
         */
        boolean mPatternExpShouldVisible = false;
        mPatternHeader = findViewById(R.id.header_pattern);
        mPatternExplanation = findViewById(R.id.explanation_pattern);
        mPatternLockView = findViewById(R.id.pattern_lock_view);
        mPatternLockView.clearPattern();
        if (ACTION_CHECK.equals(getIntent().getAction())) {
            /**
             * This block is executed when the user opens the app after setting the pattern lock
             * this block takes the pattern input by the user and check it with the pattern intially set by the user.
             */
            mPatternHeader.setText(R.string.pattern_enter_pattern);
            mPatternExplanation.setVisibility(View.INVISIBLE);
            setCancelButtonEnabled(false);
        } else if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction())) {
            /**
             * This block is executed when the user is setting the pattern lock (i.e enabling the pattern lock)
             */
            if (savedInstanceState != null) {
                mPatternPresent = savedInstanceState.getBoolean(KEY_CONFIRMING_PATTERN);
                mPatternValue = savedInstanceState.getString(KEY_PATTERN_STRING);
                mPatternHeaderViewText = savedInstanceState.getString(PATTERN_HEADER_VIEW_TEXT);
                mPatternExpShouldVisible = savedInstanceState.getBoolean(PATTERN_EXP_VIEW_STATE);
            }
            if (mPatternPresent) {
                mPatternHeader.setText(mPatternHeaderViewText);
                if (!mPatternExpShouldVisible) {
                    mPatternExplanation.setVisibility(View.INVISIBLE);
                }
                checkPattern();
            } else {
                mPatternHeader.setText(R.string.pattern_configure_pattern);
                mPatternExplanation.setVisibility(View.VISIBLE);
                setCancelButtonEnabled(true);
            }
        } else if (ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())) {
            /**
             * This block is executed when the user is removing the pattern lock (i.e disabling the pattern lock)
             */
            mPatternHeader.setText(R.string.pattern_remove_pattern);
            mPatternExplanation.setText(getResources().getString(R.string.pattern_no_longer_required));
            mPatternExplanation.setVisibility(View.VISIBLE);
            setCancelButtonEnabled(true);
        } else {
            throw new IllegalArgumentException(R.string.illegal_argument_exception_message + " " +
                    TAG);
        }
        setPatternListener();
    }

    /**
     * Binds the appropiate listener to the pattern view.
     */
    protected void setPatternListener() {
        mPatternLockView.addPatternLockListener(new PatternLockViewListener() {
            @Override
            public void onStarted() {
                Log_OC.d(TAG, "Pattern Drawing Started");
            }

            @Override
            public void onProgress(List<PatternLockView.Dot> list) {
                Log_OC.d(TAG, "Pattern Progress " +
                        PatternLockUtils.patternToString(mPatternLockView, list));
            }

            @Override
            public void onComplete(List<PatternLockView.Dot> list) {
                if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction())) {
                    /**
                     * This block gets executed when the pattern has to be set.
                     * count variable holds the number of time the pattern has been input.
                     * if the value of count is two then the pattern input first (which is stored in patternValue variable)
                     * is compared with the pattern value input the second time
                     * (which is stored in newPatternValue) if both the variables hold the same value
                     * then the pattern is set.
                     */
                    if (mPatternValue == null || mPatternValue.length() <= 0) {
                        mPatternValue = PatternLockUtils.patternToString(mPatternLockView, list);
                    } else {
                        mNewPatternValue = PatternLockUtils.patternToString(mPatternLockView, list);
                    }
                } else {
                    mPatternValue = PatternLockUtils.patternToString(mPatternLockView, list);
                }
                Log_OC.d(TAG, "Pattern " + PatternLockUtils.patternToString(mPatternLockView, list));
                processPattern();
            }

            @Override
            public void onCleared() {
                Log_OC.d(TAG, "Pattern has been cleared");
            }
        });
    }

    private void processPattern() {
        if (ACTION_CHECK.equals(getIntent().getAction())) {
            /**
             * This block is executed when the user opens the app after setting the pattern lock
             * this block takes the pattern input by the user and check it with the pattern intially set by the user.
             */
            if (checkPattern()) {
                finish();
            } else {
                showErrorAndRestart(R.string.pattern_incorrect_pattern,
                        R.string.pattern_enter_pattern, View.INVISIBLE);
            }
        }
        else if (ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())) {
            //This block is executed when the user is removing the pattern lock (i.e disabling the pattern lock)
            if (checkPattern()) {
                Intent result = new Intent();
                result.putExtra(KEY_CHECK_RESULT, true);
                setResult(RESULT_OK, result);
                finish();
            } else {
                showErrorAndRestart(R.string.pattern_incorrect_pattern,
                        R.string.pattern_enter_pattern, View.INVISIBLE);
            }
        } else if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction())) {
              //This block is executed when the user is setting the pattern lock (i.e enabling the pattern lock)
            if (!mPatternPresent) {
                requestPatternConfirmation();
            } else if (confirmPattern()) {
                savePatternAndExit();
            } else {
                showErrorAndRestart(R.string.pattern_not_same_pattern,
                        R.string.pattern_enter_pattern, View.VISIBLE);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_CONFIRMING_PATTERN, mPatternPresent);
        outState.putString(KEY_PATTERN_STRING, mPatternValue);
        outState.putString(PATTERN_HEADER_VIEW_TEXT, mPatternHeader.getText().toString());
        if (mPatternExplanation.getVisibility() == View.VISIBLE) {
            outState.putBoolean(PATTERN_EXP_VIEW_STATE, true);
        } else {
            outState.putBoolean(PATTERN_EXP_VIEW_STATE, false);
        }
    }

    protected void savePatternAndExit() {
        Intent result = new Intent();
        result.putExtra(KEY_PATTERN, mPatternValue);
        setResult(RESULT_OK, result);
        finish();
    }


    /**
     * Ask to the user to re-enter the pattern just entered before saving it as the current pattern.
     */
    protected void requestPatternConfirmation() {
        mPatternLockView.clearPattern();
        mPatternHeader.setText(R.string.pattern_reenter_pattern);
        mPatternExplanation.setVisibility(View.INVISIBLE);
        mPatternPresent = true;
    }

    protected boolean confirmPattern() {
        mPatternPresent = false;
        return mNewPatternValue != null && mNewPatternValue.equals(mPatternValue);
    }

    private void showErrorAndRestart(int errorMessage, int headerMessage,
                                     int explanationVisibility) {
        mPatternValue = null;
        CharSequence errorSeq = getString(errorMessage);
        Snackbar snackbar = Snackbar.make(
                findViewById(android.R.id.content),
                errorSeq,
                Snackbar.LENGTH_LONG
        );
        snackbar.show();
        mPatternHeader.setText(headerMessage);
        mPatternExplanation.setVisibility(explanationVisibility);
    }

    protected boolean checkPattern() {
        SharedPreferences appPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        String savedPattern = appPrefs.getString(KEY_PATTERN, null);
        return savedPattern != null && savedPattern.equals(mPatternValue);
    }

    /**
     * Enables or disables the cancel button to allow the user interrupt the ACTION
     * requested to the activity.
     *
     * @param enabled  'True' makes the cancel button available, 'false' hides it.
     */
    protected void setCancelButtonEnabled(boolean enabled) {
        Button cancelButton = findViewById(R.id.cancel_pattern);
        if (enabled) {
            cancelButton.setVisibility(View.VISIBLE);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        } else {
            cancelButton.setVisibility(View.GONE);
            cancelButton.setVisibility(View.INVISIBLE);
            cancelButton.setOnClickListener(null);
        }
    }

    /**
     * Overrides click on the BACK arrow to correctly cancel ACTION_ENABLE or ACTION_DISABLE, while
     * preventing than ACTION_CHECK may be worked around.
     *
     * @param keyCode       Key code of the key that triggered the down event.
     * @param event         Event triggered.
     * @return              'True' when the key event was processed by this method.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount()== 0){
            if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction()) ||
                    ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())) {
                finish();
            }   // else, do nothing, but report that the key was consumed to stay alive
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
