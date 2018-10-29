/**
 *   ownCloud Android client application
 *
 *   @author David González Verdugo
 *   Copyright (C) 2018 ownCloud GmbH.
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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.logs.Log;
import com.owncloud.android.ui.adapter.LogListAdapter;
import com.owncloud.android.ui.dialog.LoadingDialog;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class LogHistoryActivity extends ToolbarActivity {

    private static final String MAIL_ATTACHMENT_TYPE = "text/plain";

    private static final String TAG = LogHistoryActivity.class.getSimpleName();

    private static final String DIALOG_WAIT_TAG = "DIALOG_WAIT";

    private String mLogPath = FileStorageUtils.getLogPath();
    private File mLogDIR = null;

    private RecyclerView.LayoutManager mLayoutManager;
    private RecyclerView mLogsRecycler;
    private LogListAdapter mLogListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.logs);
        setupToolbar();

        mLayoutManager = new LinearLayoutManager(this);
        mLogsRecycler = findViewById(R.id.log_recycler);
        mLogsRecycler.setHasFixedSize(true);
        mLogsRecycler.setLayoutManager(mLayoutManager);

        setTitle(getText(R.string.actionbar_logger));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Button deleteHistoryButton = findViewById(R.id.deleteLogHistoryButton);
        Button sendHistoryButton = findViewById(R.id.sendLogHistoryButton);

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
                mLogDIR = new File(mLogPath);
            }

            if (mLogDIR != null && mLogDIR.isDirectory()) {
                // Show a dialog while log data is being loaded
                showLoadingDialog();

                // Start a new thread that will load all the log data
                LoadingLogTask task = new LoadingLogTask();
                task.execute();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                retval = super.onOptionsItemSelected(item);
        }
        return retval;
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

                Uri mExposedLogFileUri = FileProvider.getUriForFile(this, this.
                        getString(R.string.file_provider_authority), logFile);

                uris.add(mExposedLogFileUri);
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
            Snackbar snackbar = Snackbar.make(
                    findViewById(android.R.id.content),
                    R.string.log_send_no_mail_app,
                    Snackbar.LENGTH_LONG
            );
            snackbar.show();
            Log_OC.i(TAG, "Could not find app for sending log history.");
        }
    }

    /**
     *
     * Class for loading the log data async
     *
     */
    private class LoadingLogTask extends AsyncTask<String, Void, ArrayList<Log>> {

        protected ArrayList<Log> doInBackground(String... args) {
            return readLogFile();
        }

        protected void onPostExecute(ArrayList<Log> result) {
            if (result != null) {
                mLogListAdapter = new LogListAdapter(result);
                mLogsRecycler.setAdapter(mLogListAdapter);
                dismissLoadingDialog();
            }
        }

        /**
         * Read and show log file info
         */
        private ArrayList<Log> readLogFile() {
            String[] logFileName = Log_OC.getLogFileNames();
            ArrayList<Log> logList = new ArrayList<>();
            BufferedReader br = null;

            try {
                String line;
                for (int i = logFileName.length-1; i >= 0; i--) {
                    File file = new File(mLogPath,logFileName[i]);
                    if (file.exists()) {
                        // Check if FileReader is ready
                        if (new FileReader(file).ready()) {
                            br = new BufferedReader(new FileReader(file));
                            Date timestamp = null;
                            int count = 0;

                            while ((line = br.readLine()) != null) {
                                switch (count) {
                                    case 0: // Empty line, skip it
                                        count++;
                                        break;

                                    case 1: // Log timestamp
                                        try {
                                            SimpleDateFormat simpleDateFormat =
                                                    new SimpleDateFormat("yyyy/MM/dd H:mm:ss");
                                            timestamp = simpleDateFormat.parse(line);
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }
                                        count++;
                                        break;

                                    case 2: // Log content, create the instance
                                        logList.add(new Log(timestamp, line));
                                        count = 0;
                                        break;
                                }
                            }
                        }
                    }
                }
            }
            catch (IOException e) {
                Log_OC.d(TAG, e.getMessage().toString());

            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
            return logList;
        }
    }

    /**
     * Show loading dialog
     */
    public void showLoadingDialog() {
        // Construct dialog
        LoadingDialog loading = LoadingDialog.newInstance(R.string.log_progress_dialog_text, false);
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
}