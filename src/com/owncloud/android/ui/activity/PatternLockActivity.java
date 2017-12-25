package com.owncloud.android.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

    public final static String KEY_PATTERN  = "KEY_PATTERN";
    public final static String KEY_CHECK_RESULT = "KEY_CHECK_PATTERN_RESULT";

    private static String KEY_CONFIRMING_PATTERN = "CONFIRMING_PATTERN";
    private static String KEY_PATTERN_STRING = "PATTERN_STRING";

    private boolean patternPresent = false;
    private String patternValue;
    private String newPatternValue;
    private int count = 0;

    private TextView patternHeaderView;
    private TextView patternExpView;
    private Button cancelButton;
    private PatternLockView patternLockView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!BuildConfig.DEBUG) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
            setContentView(R.layout.activity_pattern_lock);
        patternHeaderView = (TextView) findViewById(R.id.header_pattern);
        patternExpView = (TextView) findViewById(R.id.explanation_pattern);
        cancelButton = (Button) findViewById(R.id.cancel_pattern);
        patternLockView = (PatternLockView) findViewById(R.id.pattern_lock_view);
        patternLockView.clearPattern();
        if(ACTION_CHECK.equals(getIntent().getAction())){
            patternHeaderView.setText(R.string.pattern_enter_pattern);
            patternExpView.setVisibility(View.INVISIBLE);
            setCancelButtonEnabled(false);
        }
        else if(ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction())){
            if(savedInstanceState != null){
                patternPresent = savedInstanceState.getBoolean(KEY_CONFIRMING_PATTERN);
                patternValue = savedInstanceState.getString(KEY_PATTERN_STRING);
            }
            if(patternPresent){
                checkPattern();
            }
            else{
                patternHeaderView.setText(R.string.pattern_configure_pattern);
                patternExpView.setVisibility(View.VISIBLE);
                setCancelButtonEnabled(true);
            }
        }
        else if(ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())){
            patternHeaderView.setText(R.string.pattern_remove_pattern);
            patternExpView.setVisibility(View.VISIBLE);
            setCancelButtonEnabled(true);
        }
        else{
            throw new IllegalArgumentException(R.string.illegal_argument_exception_message + " " +
                    TAG);
        }
        setPatternListener();
    }

    /**
     * Binds the appropiate listener to the pattern view.
     */
    protected void setPatternListener(){
        patternLockView.addPatternLockListener(new PatternLockViewListener() {
            @Override
            public void onStarted() {
                Log_OC.d(TAG,"Pattern Drawing Started");
            }

            @Override
            public void onProgress(List<PatternLockView.Dot> list) {
                Log_OC.d(TAG,"Pattern Progress " +
                        PatternLockUtils.patternToString(patternLockView,list));
            }

            @Override
            public void onComplete(List<PatternLockView.Dot> list) {
                if(ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction())){
                    if(count == 0){
                        patternValue = PatternLockUtils.patternToString(patternLockView,list);
                        count++;
                    }
                    else{
                        newPatternValue = PatternLockUtils.patternToString(patternLockView,list);
                        count = 0;
                    }
                }
                else {
                    patternValue = PatternLockUtils.patternToString(patternLockView, list);
                }
                Log_OC.d(TAG,"Pattern " + PatternLockUtils.patternToString(patternLockView,list));
                processPattern();
            }

            @Override
            public void onCleared() {
                Log_OC.d(TAG,"Pattern has been cleared");
            }
        });
    }

    private void processPattern() {
        if (ACTION_CHECK.equals(getIntent().getAction())) {
            if(checkPattern()){
                finish();
            }
            else {
                showErrorAndRestart(R.string.pattern_incorrect_pattern,
                        R.string.pattern_enter_pattern, View.INVISIBLE);
            }
        }
        else if(ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())){
            if(checkPattern()){
                Intent result = new Intent();
                result.putExtra(KEY_CHECK_RESULT,true);
                setResult(RESULT_OK,result);
                finish();
            }
            else{
                showErrorAndRestart(R.string.pattern_incorrect_pattern,
                        R.string.pattern_enter_pattern,View.INVISIBLE);
            }
        }
        else if(ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction())){
            if(!patternPresent){
                requestPatternConfirmation();
            }
            else if(confirmPattern()){
                savePatternAndExit();
            }
            else{
                showErrorAndRestart(R.string.pattern_not_same_pattern,
                        R.string.pattern_enter_pattern,View.VISIBLE);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_CONFIRMING_PATTERN,patternPresent);
        outState.putString(KEY_PATTERN_STRING,patternValue);
    }

    protected void savePatternAndExit(){
        Intent result = new Intent();
        result.putExtra(KEY_PATTERN,patternValue);
        setResult(RESULT_OK,result);
        finish();
    }


    /**
     * Ask to the user to re-enter the pattern just entered before saving it as the current pattern.
     */
    protected void requestPatternConfirmation(){
        patternLockView.clearPattern();
        patternHeaderView.setText(R.string.pattern_reenter_pattern);
        patternExpView.setVisibility(View.INVISIBLE);
        patternPresent = true;
    }

    protected boolean confirmPattern(){
        patternPresent = false;
        if(newPatternValue != null && newPatternValue.equals(patternValue)){
            return true;
        }
        return false;
    }

    private void showErrorAndRestart(int errorMessage, int headerMessage,
                                     int explanationVisibility) {
        patternValue = null;
        CharSequence errorSeq = getString(errorMessage);
        Snackbar snackbar = Snackbar.make(
                findViewById(android.R.id.content),
                errorSeq,
                Snackbar.LENGTH_LONG
        );
        snackbar.show();
        patternHeaderView.setText(headerMessage);                // TODO check if really needed
        patternExpView.setVisibility(explanationVisibility); // TODO check if really needed
    }

    protected boolean checkPattern(){
        SharedPreferences appPrefs  = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        String savedPattern = appPrefs.getString(KEY_PATTERN,null);
        if(savedPattern != null && savedPattern.equals(patternValue)){
            return true;
        }
        return false;
    }

    /**
     * Enables or disables the cancel button to allow the user interrupt the ACTION
     * requested to the activity.
     *
     * @param enabled       'True' makes the cancel button available, 'false' hides it.
     */
    protected void setCancelButtonEnabled(boolean enabled){
        if(enabled){
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

}
