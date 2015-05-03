package com.owncloud.android.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.LogHistoryActivity;

public class LoadingLogService extends IntentService {
    private String mLogPath;
    private String TAG;

    public LoadingLogService(String name) {
        super(name);
    }

    private void init(Intent intent) {
        this.mLogPath = intent.getStringExtra("mLogPath");
        this.TAG = intent.getStringExtra("TAG");
    }

    public void onHandleIntent(Intent intent) {
        init(intent);
        Intent resultIntent = new Intent(LogHistoryActivity.LOG_RECEIVER_FILTER);
        resultIntent.putExtra("result", readLogFile());
        LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent);
    }

    /**
     * Read and show log file info
     */
    private String readLogFile() {

        String[] logFileName = Log_OC.getLogFileNames();

        //Read text from files
        StringBuilder text = new StringBuilder();

        BufferedReader br = null;
        try {
            String line;

            for (int i = logFileName.length-1; i >= 0; i--) {
                File file = new File(mLogPath,logFileName[i]);
                if (file.exists()) {
                    // Check if FileReader is ready
                    if (new FileReader(file).ready()) {
                        br = new BufferedReader(new FileReader(file));
                        while ((line = br.readLine()) != null) {
                            // Append the log info
                            text.append(line);
                            text.append('\n');
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

        return text.toString();
    }
}
