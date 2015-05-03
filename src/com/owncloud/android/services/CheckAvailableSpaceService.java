package com.owncloud.android.services;

import java.io.File;

import android.accounts.Account;
import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.owncloud.android.ui.activity.UploadFilesActivity;
import com.owncloud.android.utils.FileStorageUtils;

public class CheckAvailableSpaceService extends IntentService {
    Account mAccountOnCreation;

    public CheckAvailableSpaceService(String name) {
        super(name);
    }

    private void init(Intent intent) {
        this.mAccountOnCreation = (Account) intent.getParcelableExtra("mAccountOnCreation");
    }

    public void onHandleIntent(Intent intent) {
        init(intent);
        String[] checkedFilePaths = intent.getStringArrayExtra("filePaths");
        long total = 0;
        for (int i = 0; checkedFilePaths != null && i < checkedFilePaths.length; i++) {
            String localPath = checkedFilePaths[i];
            File localFile = new File(localPath);
            total += localFile.length();
        }
        Intent resultIntent = new Intent(UploadFilesActivity.CHECK_SPACE_RECEIVER_FILTER);
        resultIntent.putExtra("result", Boolean.valueOf(
                FileStorageUtils.getUsableSpace(mAccountOnCreation.name) >= total));
        LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent);
        return;
    }
}
