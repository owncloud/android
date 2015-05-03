package com.owncloud.android.services;

import java.io.File;
import java.util.ArrayList;

import android.accounts.Account;
import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.ErrorsWhileCopyingHandlerActivity;
import com.owncloud.android.utils.FileStorageUtils;

public class MoveFilesService extends IntentService {
    ArrayList<String> mLocalPaths;
    Account mAccount;
    ArrayList<String> mRemotePaths;

    public MoveFilesService(String name) {
        super(name);
    }

    private void init(Intent intent) {
        this.mLocalPaths = intent.getStringArrayListExtra("mLocalPaths");
        this.mAccount = (Account) intent.getParcelableExtra("mAccount");
        this.mRemotePaths = intent.getStringArrayListExtra("mRemotePaths");
    }

    /**
     * Performs the movement
     * 
     * return 'False' when the movement of any file fails.
     */
    public void onHandleIntent(Intent intent) {
        init(intent);
        FileDataStorageManager mStorageManager = new FileDataStorageManager(mAccount, getContentResolver());
        while (!mLocalPaths.isEmpty()) {
            String currentPath = mLocalPaths.get(0);
            File currentFile = new File(currentPath);
            String expectedPath = FileStorageUtils.getSavePath(mAccount.name) + mRemotePaths.get(0);
            File expectedFile = new File(expectedPath);
            if (expectedFile.equals(currentFile) || currentFile.renameTo(expectedFile)) {
                OCFile file = mStorageManager.getFileByPath(mRemotePaths.get(0));
                file.setStoragePath(expectedPath);
                mStorageManager.saveFile(file);
                mRemotePaths.remove(0);
                mLocalPaths.remove(0);
            } else {
                Intent resultIntent = new Intent(ErrorsWhileCopyingHandlerActivity.MOVE_FILES_RECEIVER_FILTER);
                resultIntent.putExtra("result", false);
                resultIntent.putExtra("mRemotePaths", mRemotePaths);
                resultIntent.putExtra("mLocalPaths", mLocalPaths);
                LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent);
                return;
            }
        }
        Intent resultIntent = new Intent(ErrorsWhileCopyingHandlerActivity.MOVE_FILES_RECEIVER_FILTER);
        resultIntent.putExtra("result", true);
        resultIntent.putExtra("mRemotePaths", mRemotePaths);
        resultIntent.putExtra("mLocalPaths", mLocalPaths);
        LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent);
        return;
    }
}
