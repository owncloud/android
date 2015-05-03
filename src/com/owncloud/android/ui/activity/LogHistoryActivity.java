/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2015 ownCloud Inc.
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

package com.owncloud.android.ui.activity;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.services.LoadingLogService;
import com.owncloud.android.ui.dialog.LoadingDialog;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileStorageUtils;


public class LogHistoryActivity extends SherlockFragmentActivity  {

    public static final String LOG_RECEIVER_FILTER = "LogHistoryActivity_LogReceiver";

    private static final String MAIL_ATTACHMENT_TYPE = "text/plain";

    private static final String KEY_LOG_TEXT = "LOG_TEXT";

    private static final String TAG = LogHistoryActivity.class.getSimpleName();

    private static final String DIALOG_WAIT_TAG = "DIALOG_WAIT";

    private String mLogPath = FileStorageUtils.getLogPath();
    private File logDIR = null;
    private String mLogText;
    private LoadingLogReceiver logReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.log_send_file);
        setTitle(getText(R.string.actionbar_logger));
        ActionBar actionBar = getSherlock().getActionBar();
        actionBar.setIcon(DisplayUtils.getSeasonalIconId());
        actionBar.setDisplayHomeAsUpEnabled(true);
        Button deleteHistoryButton = (Button) findViewById(R.id.deleteLogHistoryButton);
        Button sendHistoryButton = (Button) findViewById(R.id.sendLogHistoryButton);
        TextView logTV = (TextView) findViewById(R.id.logTV);

        deleteHistoryButton.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {

                Log_OC.deleteHistoryLogging();
                finish();
            }
        });

        sendHistoryButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                sendMail();
            }
        });

        if (savedInstanceState == null) {
            if (mLogPath != null) {
                logDIR = new File(mLogPath);
            }

            if (logDIR != null && logDIR.isDirectory()) {
                // Show a dialog while log data is being loaded
                showLoadingDialog();

                // Start a new service that will load all the log data
                logReceiver = new LoadingLogReceiver(logTV);
                LocalBroadcastManager.getInstance(this)
                    .registerReceiver(logReceiver, new IntentFilter(LOG_RECEIVER_FILTER));
                Intent logService = new Intent(this, LoadingLogService.class);
                logService.putExtra("mLogPath", mLogPath);
                logService.putExtra("TAG", TAG);
                this.startService(logService);
            }
        } else {
            mLogText = savedInstanceState.getString(KEY_LOG_TEXT);
            logTV.setText(mLogText);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (logReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(logReceiver);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        super.onMenuItemSelected(featureId, item);
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        default:
            return false;
        }
        return true;
    }


    /**
     * Start activity for sending email with logs attached
     */
    private void sendMail() {

        // For the moment we need to consider the possibility that setup.xml
        // does not include the "mail_logger" entry. This block prevents that
        // compilation fails in this case.
        String emailAddress;
        try {
            Class<?> stringClass = R.string.class;
            Field mailLoggerField = stringClass.getField("mail_logger");
            int emailAddressId = (Integer) mailLoggerField.get(null);
            emailAddress = getString(emailAddressId);
        } catch (Exception e) {
            emailAddress = "";
        }
        
        ArrayList<Uri> uris = new ArrayList<Uri>();

        // Convert from paths to Android friendly Parcelable Uri's
        for (String file : Log_OC.getLogFileNames())
        {
            File logFile = new File(mLogPath, file);
            if (logFile.exists()) {
                uris.add(Uri.fromFile(logFile));
            }
        }

        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);

        intent.putExtra(Intent.EXTRA_EMAIL, emailAddress);
        String subject = String.format(getString(R.string.log_send_mail_subject), getString(R.string.app_name));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setType(MAIL_ATTACHMENT_TYPE);
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.log_send_no_mail_app), Toast.LENGTH_LONG).show();
            Log_OC.i(TAG, "Could not find app for sending log history.");
        }

    }

    /**
     *
     * Receiver for loading the log data async
     *
     */
    private class LoadingLogReceiver extends BroadcastReceiver {
        private final WeakReference<TextView> textViewReference;

        public LoadingLogReceiver(TextView logTV){
            // Use of a WeakReference to ensure the TextView can be garbage collected
            textViewReference  = new WeakReference<TextView>(logTV);
        }

        public void onReceive(Context receiverContext, Intent receiverIntent) {
            String result = receiverIntent.getStringExtra("result");
            if (textViewReference != null && result != null) {
                final TextView logTV = textViewReference.get();
                if (logTV != null) {
                    mLogText = result;
                    logTV.setText(mLogText);
                    dismissLoadingDialog();
                }
            }
        }
    }

    /**
     * Show loading dialog
     */
    public void showLoadingDialog() {
        // Construct dialog
        LoadingDialog loading = new LoadingDialog(
                getResources().getString(R.string.log_progress_dialog_text)
        );
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        loading.show(ft, DIALOG_WAIT_TAG);
    }

    /**
     * Dismiss loading dialog
     */
    public void dismissLoadingDialog(){
        Fragment frag = getSupportFragmentManager().findFragmentByTag(DIALOG_WAIT_TAG);
        if (frag != null) {
            LoadingDialog loading = (LoadingDialog) frag;
            loading.dismiss();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        /// global state
        if (mLogText != null) {
            outState.putString(KEY_LOG_TEXT, mLogText);
        }
    }
}