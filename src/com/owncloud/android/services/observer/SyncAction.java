package com.owncloud.android.services.observer;

import java.io.File;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;

public class SyncAction implements ObserverActionInterface {
    
    private Account mAccount;
    private Context mContext;
    
    public SyncAction(Account account, Context context) {
        if (account == null)
            throw new IllegalArgumentException("NULL account argument received");
        if (context == null)
            throw new IllegalArgumentException("NULL context argument received");
        mAccount = account;
        mContext = context;
    }

    /**
     * Triggers an operation to synchronize the contents of a file inside the observed folder with
     * its remote counterpart in the associated ownCloud account.
     *    
     * @param fileName          Name of a file inside the watched folder.
     */
    @Override
    public void onFileChanged(String path, String fileName) {
        FileDataStorageManager storageManager = 
                new FileDataStorageManager(mAccount, mContext.getContentResolver());
        // a fresh object is needed; many things could have occurred to the file
        // since it was registered to observe again, assuming that local files
        // are linked to a remote file AT MOST, SOMETHING TO BE DONE;
        OCFile file = storageManager.getFileByLocalPath(path + File.separator + fileName);
        SynchronizeFileOperation sfo = 
                new SynchronizeFileOperation(file, null, mAccount, true, mContext);
        RemoteOperationResult result = sfo.execute(storageManager, mContext);
        if (result.getCode() == ResultCode.SYNC_CONFLICT) {
            // ISSUE 5: if the user is not running the app (this is a service!),
            // this can be very intrusive; a notification should be preferred
            Intent i = new Intent(mContext, ConflictsResolveActivity.class);
            i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra(ConflictsResolveActivity.EXTRA_FILE, file);
            i.putExtra(ConflictsResolveActivity.EXTRA_ACCOUNT, mAccount);
            mContext.startActivity(i);
        }
        // TODO save other errors in some point where the user can inspect them later;
        // or maybe just toast them;
        // or nothing, very strange fails
    }

}
