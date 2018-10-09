/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
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

package com.owncloud.android.services.observer;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.os.FileObserver;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Observer watching a folder to request the synchronization of kept-in-sync files
 * inside it.
 * 
 * Takes into account two possible update cases:
 *  - an editor directly updates the file;
 *  - an editor works on a temporary file, and later replaces the kept-in-sync
 *  file with the former.
 *  
 *  The second case requires to monitor the folder parent of the files, since a direct 
 *  {@link FileObserver} on it will not receive more events after the file is deleted to
 *  be replaced.
 */
public class AvailableOfflineObserver extends FileObserver {

    private static String TAG = AvailableOfflineObserver.class.getSimpleName();

    private static int UPDATE_MASK = (
        FileObserver.ATTRIB | FileObserver.MODIFY |
        FileObserver.MOVED_TO | FileObserver.CLOSE_WRITE |
        FileObserver.CREATE |   // to detect new subfolders in recursive mode
        FileObserver.MOVED_TO   // to detect renamed subfolders in recursive mode
    );

    private static int IN_ISDIR = 0x40000000;

    private static int IN_IGNORE = 32768;
    /*
    private static int ALL_EVENTS_EVEN_THOSE_NOT_DOCUMENTED = 0x7fffffff;   // NEVER use 0xffffffff
    */

    private String mPath;
    private Account mAccount;
    private Context mContext;
    private boolean mRecursiveWatch;

    private final Object mIncludedLock = new Object();
    private Map<String, Boolean> mIncludedChildren;
    private Set<String> mExcludedChildren;

    private List<SubfolderObserver> mFolderTreeObservers;

    /**
     * Constructor.
     *
     * Initializes the observer to receive events about the update of the passed folder, and
     * its children files.
     *
     * @param path          Absolute path to the local folder to watch.
     * @param account       OwnCloud account associated to the folder.
     * @param context       Used to start an operation to synchronize the file, when needed.
     */
    public AvailableOfflineObserver(String path, Account account, Context context) {
        super(path, UPDATE_MASK);

        if (path == null)
            throw new IllegalArgumentException("NULL path argument received");
        if (account == null)
            throw new IllegalArgumentException("NULL account argument received");
        if (context == null)
            throw new IllegalArgumentException("NULL context argument received");
        
        mPath = path;
        mAccount = account;
        mContext = context;
        mRecursiveWatch = false;
        mIncludedChildren = new HashMap<>();
        mExcludedChildren = Collections.newSetFromMap(new HashMap<String, Boolean>());

        mFolderTreeObservers = new ArrayList<>();
    }

    /**
     * Adds a child file to the list of files observed by the folder observer.
     *
     * If {@link AvailableOfflineObserver#stopWatchingAll()} was never called,
     * the received file will start to be watched only if it's a direct child of the observed
     * folder. If the path received as a parameter points to a descendant of the current
     * folder that is not a direct child, the observation will not work.
     *
     * It's additive. If {@link AvailableOfflineObserver#stopWatchingAll()} was called before,
     * the observer will keep on doing so after, the observer will not get out of full-tree
     * observance mode calling this method.
     *
     * @param relativePath         Name of a file inside the observed folder.
     */
    public void startWatching(String relativePath) {
        if (!mRecursiveWatch) {
            // selective mode - add a new file to care about
            synchronized (mIncludedLock) {
                if (!mIncludedChildren.containsKey(relativePath)) {
                    mIncludedChildren.put(relativePath, false);
                }
            }
            if (new File(mPath).exists()) {
                startWatching();
                Log_OC.d(TAG, "Watching folder " + mPath + " for changes in " + relativePath);
            } else {
                Log_OC.w(
                    TAG,
                    "Observance of " + relativePath + " could not start, parent " + mPath + " does not exist"
                );
            }

        } else {
            // recursive mode - out of exclusions, if there
            boolean removed = mExcludedChildren.remove(relativePath);
            if (!removed && relativePath.contains(File.separator)) {
                Log_OC.w(
                    TAG,
                    "Watching a file deep-down the tree out of recursive mode; ignored: "
                        + relativePath
                );
            }
        }
    }

    /**
     * Removes a child file from the list of files observed by the folder observer.
     * 
     * @param relativePath         Name of a file inside the observed folder.
     */
    public void stopWatching(String relativePath) {
        if (!mRecursiveWatch) {
            // selective mode
            synchronized (mIncludedLock) {
                mIncludedChildren.remove(relativePath);
                if (mIncludedChildren.isEmpty()) {
                    super.stopWatching();
                    Log_OC.d(TAG, "Stopped watching " + mPath + " for changes in selected children");
                }
            }
        } else {
            // recursive mode
            mExcludedChildren.add(relativePath);
        }
    }


    /**
     * Scans and starts watching.
     *
     * Watch every file and folder below, going down through the full tree.
     *
     * Recursive mode will continue enabled until {@link #stopWatchingAll()} is called.
     */
    public void startWatchingAll() {
        if (!mRecursiveWatch) {
            // TODO - should do this?
            synchronized (mIncludedLock) {
                super.stopWatching();
                mIncludedChildren.clear();
                Log_OC.d(TAG, "Stopped watching " + mPath + " for changes in selected children");
            }
            mRecursiveWatch = true;
        } else {
            stopWatchingAll();  // stop & clean to rescan folder tree below
        }

        mExcludedChildren.clear();
        Stack<String> stack = new Stack<>();
        stack.push(mPath);
        // scan file tree and create subordinate observers
        while (!stack.empty()) {
            String parent = stack.pop();
            Log_OC.d(TAG, "Adding observer for all files in " + parent);
            mFolderTreeObservers.add(new SubfolderObserver(parent, UPDATE_MASK));
            File path = new File(parent);
            File[] files = path.listFiles();
            if (files == null) continue;
            for (File file : files) {
                if (file.isDirectory()
                    && !".".equals(file.getName())
                    && !"..".equals(file.getName())
                ) {
                    stack.push(file.getPath());
                }
            }
        }

        for (int i = 0; i < mFolderTreeObservers.size(); i++) {
            mFolderTreeObservers.get(i).startWatching();
        }
        Log_OC.d(TAG, "Watching folder tree hanging from " + mPath);
    }

    public void stopWatchingAll() {
        for (int i = 0; i < mFolderTreeObservers.size(); ++i) {
            mFolderTreeObservers.get(i).stopWatching();
        }
        mFolderTreeObservers.clear();
        Log_OC.d(TAG, "Stopped watching folder tree hanging from " + mPath);
        mExcludedChildren.clear();
    }

    @Override
    public void stopWatching() {
        if (mRecursiveWatch) {
            stopWatchingAll();
        } else {
            super.stopWatching();
        }
    }

    /**
     * @return      'True' when the folder is not watching any file inside.
     */
    public boolean isEmpty() {
        synchronized (mIncludedLock) {
            return mIncludedChildren.isEmpty();
        }
    }

    /**
     * Receives and processes events about updates of the monitored folder and its children files.
     *
     * @param event     Kind of event occurred.
     * @param path      Relative path of the file referred by the event.
     */
    @Override
    public void onEvent(int event, String path) {
        Log_OC.v(TAG, "Got event " + event + " on FOLDER " + mPath + " about "
            + ((path != null) ? path : "") + " in THREAD " + Thread.currentThread().getName() + ", " + Thread.currentThread().getId());

        boolean shouldSynchronize = false;
        if (path != null && path.length() > 0) {
            synchronized (mIncludedLock) {
                if (mRecursiveWatch && !mExcludedChildren.contains(path)) {
                    /// recursive mode
                    if ((event & FileObserver.MODIFY) != 0 ||
                        (event & FileObserver.ATTRIB) != 0 ||
                        (event & FileObserver.MOVED_TO) != 0) {
                        mIncludedChildren.put(path, Boolean.valueOf(true));
                    }

                    if ((event & FileObserver.CLOSE_WRITE) != 0 &&
                        mIncludedChildren.containsKey(path)) {
                        mIncludedChildren.remove(path);
                        shouldSynchronize = true;
                    }

                    if ((event & IN_ISDIR) != 0) {
                        // event on a subfolder

                        if ((event & FileObserver.CREATE) != 0 ||
                            (event & FileObserver.MOVED_TO) != 0
                            ) {
                            SubfolderObserver newObserver = new SubfolderObserver(
                                mPath + File.separator + path, UPDATE_MASK
                            );
                            mFolderTreeObservers.add(newObserver);
                            newObserver.startWatching();
                        }

                    }

                } else if (mIncludedChildren.containsKey(path)) {
                    /// selective mode
                    if (((event & FileObserver.MODIFY) != 0) ||
                        ((event & FileObserver.ATTRIB) != 0) ||
                        ((event & FileObserver.MOVED_TO) != 0)) {

                        if (mIncludedChildren.get(path) != true) {
                            mIncludedChildren.put(path, Boolean.valueOf(true));
                        }
                    }

                    if ((event & FileObserver.CLOSE_WRITE) != 0 &&
                        mIncludedChildren.get(path)) {
                        mIncludedChildren.put(path, Boolean.valueOf(false));
                        shouldSynchronize = true;
                    }
                }
            }
        }
        if (shouldSynchronize) {
            startSyncOperation(path);
        }

        if ((event & IN_IGNORE) != 0 &&
            (path == null || path.length() == 0)) {
            Log_OC.d(TAG, "Stopping the observance on " + mPath);
        }

    }

    /**
     * Triggers an operation to synchronize the contents of a file inside the observed folder with
     * its remote counterpart in the associated ownCloud account.
     *    
     * @param fileName          Name of a file inside the watched folder.
     */
    private void startSyncOperation(String fileName) {
        FileDataStorageManager storageManager = 
                new FileDataStorageManager(mContext, mAccount, mContext.getContentResolver());
        // a fresh object is needed; many things could have occurred to the file
        // since it was registered to observe again, assuming that local files
        // are linked to a remote file AT MOST, SOMETHING TO BE DONE;
        OCFile file = storageManager.getFileByLocalPath(mPath + File.separator + fileName);
        if (file == null) {
            Log_OC.w(TAG, "Could not find OC file for observed " + mPath + File.separator + fileName);
        } else {
            SynchronizeFileOperation sfo =
                new SynchronizeFileOperation(file, null, mAccount, false, mContext);
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
        }
    }


    private class SubfolderObserver extends FileObserver {
        private String mRelativePath;

        SubfolderObserver(String path, int mask) {
            super(path, mask);
            mRelativePath = path.replace(AvailableOfflineObserver.this.mPath, "");
            if (mRelativePath.startsWith(File.separator)) {
                mRelativePath = mRelativePath.substring(1);
            }
        }

        @Override
        public void onEvent(int event, String path) {
            String newPath = (mRelativePath.length() > 0) ? mRelativePath + File.separator + path : path;
            AvailableOfflineObserver.this.onEvent(event, newPath);
        }

    }

}
