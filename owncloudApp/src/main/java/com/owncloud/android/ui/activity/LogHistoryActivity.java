/*
 *   ownCloud Android client application
 *
 *   @author David González Verdugo
 *   Copyright (C) 2019 ownCloud GmbH.
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

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.widget.SearchView;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.adapter.LogListAdapter;
import com.owncloud.android.ui.dialog.LoadingDialog;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.PreferenceUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;


public class LogHistoryActivity extends ToolbarActivity {

    private static final String MAIL_ATTACHMENT_TYPE = "text/plain";

    private static final String TAG = LogHistoryActivity.class.getSimpleName();

    private static final String DIALOG_WAIT_TAG = "DIALOG_WAIT";

    private String mLogPath = FileStorageUtils.getLogPath();
    private File mLogDIR = null;

    private RecyclerView mLogsRecycler;
    private LogListAdapter mLogListAdapter;
    private SearchView mSearchView;
    private String mCurrentFilter = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.logs);

        // Allow or disallow touches with other visible windows
        LinearLayout logsLayout = findViewById(R.id.logsLayout);
        logsLayout.setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)
        );

        setupToolbar();

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mLogsRecycler = findViewById(R.id.log_recycler);
        mLogsRecycler.setHasFixedSize(true);
        mLogsRecycler.setLayoutManager(layoutManager);

        setTitle(getText(R.string.actionbar_logger));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        Button deleteHistoryButton = findViewById(R.id.deleteLogHistoryButton);
        Button sendHistoryButton = findViewById(R.id.sendLogHistoryButton);

        deleteHistoryButton.setOnClickListener(v -> {
            Log_OC.deleteHistoryLogging();
            finish();
        });

        sendHistoryButton.setOnClickListener(v -> sendMail());

        if (savedInstanceState == null) {
            if (mLogPath != null) {
                mLogDIR = new File(mLogPath);
            }

            if (mLogDIR != null && mLogDIR.isDirectory()) {
                // Show a dialog while log data is being loaded
                showLoadingDialog();

                // Start a new thread that will load all the log data
                LoadingLogfileTask task = new LoadingLogfileTask();
                task.execute();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.log_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        if (searchItem != null) {
            mSearchView = (SearchView) searchItem.getActionView();
        }
        if (mSearchView == null) {
            return true;
        }
        if (searchManager != null) {
            mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }
        SearchView.SearchAutoComplete searchAutoComplete = mSearchView.findViewById(R.id.search_src_text);

        if (mCurrentFilter != null && !mCurrentFilter.equals("")) {
            if (searchAutoComplete != null) {
                searchAutoComplete.setText(mCurrentFilter);
            }
            mSearchView.setIconified(false);
        } else {
            if (searchAutoComplete != null) {
                searchAutoComplete.setText("");
            }
            mSearchView.setIconified(true);
        }

        final SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                setFilter2LogAdapter(newText);
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                setFilter2LogAdapter(query);
                return true;
            }
        };
        if (searchItem != null) {
            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    setFilter2LogAdapter("");
                    return true;  // Return true to collapse action view
                }

                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    // Do something when expanded
                    return true;  // Return true to expand action view
                }
            });
        }

        if (null != mSearchView) {
            if (searchManager != null) {
                mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            }
            mSearchView.setIconifiedByDefault(true);
            mSearchView.setOnQueryTextListener(queryTextListener);
            if (mCurrentFilter != null && !mCurrentFilter.equals("")) {
                if (searchAutoComplete != null && searchItem != null) {
                    searchItem.expandActionView();
                    searchAutoComplete.setText(mCurrentFilter);
                }
            }
        }
        return true;
    }

    private void setFilter2LogAdapter(String filter) {
        mLogListAdapter.setFilter(filter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case R.id.menu_logcat:
                LoadingLogcatTask task = new LoadingLogcatTask();
                task.execute();
                break;
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

        ArrayList<Uri> uris = new ArrayList<>();

        // Convert from paths to Android friendly Parcelable Uri's
        for (String file : Log_OC.getLogFileNames()) {
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
     * Class for loading the log data async
     */
    private class LoadingLogcatTask extends AsyncTask<String, Void, ArrayList<String>> {

        protected ArrayList<String> doInBackground(String... args) {
            return readLogFile();
        }

        protected void onPostExecute(ArrayList<String> result) {
            if (result != null) {
                mLogListAdapter = new LogListAdapter(result, mCurrentFilter, LogHistoryActivity.this);
                mLogsRecycler.setAdapter(mLogListAdapter);
                mLogsRecycler.scrollToPosition(result.size() - 1);
                dismissLoadingDialog();
            }
        }

        /**
         * Read and show log file info
         */
        private ArrayList<String> readLogFile() {
            ArrayList<String> logList = new ArrayList<>();
            try {
                Process process = Runtime.getRuntime().exec("logcat -dv time");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    line = line.replace(" W/", " W: ")
                            .replace(" E/", " E: ")
                            .replace(" V/", " V: ")
                            .replace(" I/", " I: ")
                            .replace(" D/", " D: ");
                    logList.add(line);
                }
            } catch (IOException e) {
                Log_OC.e("LoadingLogcatTask", e.getMessage());
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
    public void dismissLoadingDialog() {
        Fragment frag = getSupportFragmentManager().findFragmentByTag(DIALOG_WAIT_TAG);
        if (frag != null) {
            LoadingDialog loading = (LoadingDialog) frag;
            loading.dismiss();
        }
    }

    private class LoadingLogfileTask extends AsyncTask<String, Void, ArrayList<String>> {

        protected ArrayList<String> doInBackground(String... args) {
            return readLogFile();
        }

        protected void onPostExecute(ArrayList<String> result) {
            if (result != null) {
                mLogListAdapter = new LogListAdapter(result, mCurrentFilter, LogHistoryActivity.this);
                mLogsRecycler.setAdapter(mLogListAdapter);
                dismissLoadingDialog();
            }
        }

        /**
         * Read and show log file info
         */
        private ArrayList<String> readLogFile() {
            String[] logFileName = Log_OC.getLogFileNames();
            ArrayList<String> logList = new ArrayList<>();
            BufferedReader br = null;

            try {
                String line;
                for (int i = logFileName.length - 1; i >= 0; i--) {
                    File file = new File(mLogPath, logFileName[i]);
                    if (file.exists()) {
                        // Check if FileReader is ready
                        if (new FileReader(file).ready()) {
                            br = new BufferedReader(new FileReader(file));

                            while ((line = br.readLine()) != null) {
                                logList.add(line);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Log_OC.d(TAG, e.getMessage());

            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException ignored) {
                    }
                }
            }
            return logList;
        }
    }
}