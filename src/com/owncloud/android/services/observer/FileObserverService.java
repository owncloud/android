/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   @author David González Verdugo
 *   Copyright (C) 2012 Bartek Przybylski
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
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;

import com.owncloud.android.MainApp;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.utils.Extras;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Service keeping a list of {@link AvailableOfflineObserver} instances that watch for local
 * changes in favorite files (formerly known as kept-in-sync files) and try to
 * synchronize them with the OC server as soon as possible.
 * 
 * Tries to be alive as long as possible; that is the reason why stopSelf() is
 * never called.
 * 
 * It is expected that the system eventually kills the service when runs low of
 * memory. To minimize the impact of this, the service always returns
 * Service.START_STICKY, and the later restart of the service is explicitly
 * considered in {@link FileObserverService#onStartCommand(Intent, int, int)}.
 */
public class FileObserverService extends Service {

    private final static String MY_NAME = FileObserverService.class.getCanonicalName();
    private final static String ACTION_START_OBSERVE =
        MY_NAME + ".action.START_OBSERVATION";
    private final static String ACTION_ADD_OBSERVED_FILE =
        MY_NAME + ".action.ADD_OBSERVED_FILE";
    private final static String ACTION_DEL_OBSERVED_FILE =
        MY_NAME + ".action.DEL_OBSERVED_FILE";

    private final static String ARG_FILE = "ARG_FILE";
    private final static String ARG_ACCOUNT = "ARG_ACCOUNT";

    private static String TAG = FileObserverService.class.getSimpleName();

    /**
     * Map of observers watching for changes in available offline files in local 'ownCloud' folder
     */
    private Map<String, AvailableOfflineObserver> mAvailableOfflineObserversMap;

    private LocalBroadcastManager mLocalBroadcastManager;


    /**
     * Broadcast receiver being notified about downloads new and finished downloads to pause and resume
     * observance of available offline files when downloading
     */
    private DownloadCompletedReceiver mDownloadReceiver;

    /**
     * Requests an ACTION_START_OBSERVE command to (re)initialize the observer service.
     * 
     * @param context   Android context of the caller component.
     */
    public static void initialize(Context context) {
        Intent i = new Intent(context, FileObserverService.class);
        i.setAction(ACTION_START_OBSERVE);
        context.startService(i);
    }

    /**
     * Requests to start or stop the observance of a given file.
     * 
     * @param context       Android context of the caller component.
     * @param file          OCFile to start or stop to watch.
     * @param account       OC account containing file.
     * @param watchIt       'True' creates an intent to watch, 'false' an intent to stop watching.
     */
    public static void observeFile(
        Context context,
        OCFile file,
        Account account,
        boolean watchIt
    ) {
        Intent intent = new Intent(context, FileObserverService.class);
        intent.setAction(watchIt ? FileObserverService.ACTION_ADD_OBSERVED_FILE
                : FileObserverService.ACTION_DEL_OBSERVED_FILE);
        intent.putExtra(FileObserverService.ARG_FILE, file);
        intent.putExtra(FileObserverService.ARG_ACCOUNT, account);
        context.startService(intent);
    }


    /**
     * Initialize the service. 
     */
    @Override
    public void onCreate() {
        Log_OC.d(TAG, "onCreate");
        super.onCreate();

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mDownloadReceiver = new DownloadCompletedReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(FileDownloader.getDownloadAddedMessage());
        filter.addAction(FileDownloader.getDownloadFinishMessage());
        mLocalBroadcastManager.registerReceiver(mDownloadReceiver, filter);

        mAvailableOfflineObserversMap = new HashMap<>();
    }

    /**
     * Release resources.
     */
    @Override
    public void onDestroy() {
        Log_OC.d(TAG, "onDestroy - finishing observation of favorite files");

        mLocalBroadcastManager.unregisterReceiver(mDownloadReceiver);
        mLocalBroadcastManager = null;

        for (AvailableOfflineObserver availableOfflineObserver : mAvailableOfflineObserversMap.values()) {
            availableOfflineObserver.stopWatching();
        }
        mAvailableOfflineObserversMap.clear();
        mAvailableOfflineObserversMap = null;

        super.onDestroy();
    }

    /**
     * This service cannot be bound.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Handles requests to:
     *  - (re)start watching                    (ACTION_START_OBSERVE)
     *  - add an {@link OCFile} to be watched   (ATION_ADD_OBSERVED_FILE)
     *  - stop observing an {@link OCFile}      (ACTION_DEL_OBSERVED_FILE) 
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log_OC.d(TAG, "Starting command " + intent);

        if (intent == null || ACTION_START_OBSERVE.equals(intent.getAction())) {
            // NULL occurs when system tries to restart the service after its
            // process was killed
            startObservation();
            return Service.START_STICKY;

        } else if (ACTION_ADD_OBSERVED_FILE.equals(intent.getAction())) {
            OCFile file = intent.getParcelableExtra(ARG_FILE);
            Account account = intent.getParcelableExtra(ARG_ACCOUNT);
            addObservedFile(file, account);

        } else if (ACTION_DEL_OBSERVED_FILE.equals(intent.getAction())) {
            removeObservedFile(
                (OCFile) intent.getParcelableExtra(ARG_FILE),
                (Account) intent.getParcelableExtra(ARG_ACCOUNT)
            );

        } else {
            Log_OC.e(TAG, "Unknown action received; ignoring it: " + intent.getAction());
        }

        return Service.START_STICKY;
    }

    /**
     * Read from the local database the list of files that must to be kept
     * synchronized and starts observers to monitor local changes on them.
     * 
     * Updates the list of currently observed files if called multiple times.
     */
    private void startObservation() {
        Log_OC.d(TAG, "Loading all available offline files from database to start watching them");
        FileDataStorageManager fds = new FileDataStorageManager(
                getApplicationContext(),
                null,   // this is dangerous - handle with care
                getContentResolver()
        );

        List<Pair<OCFile, String>> availableOfflineFiles = fds.getAvailableOfflineFilesFromEveryAccount();
        OCFile file;
        String accountName;
        Account account;
        for (Pair<OCFile, String> pair : availableOfflineFiles) {
            file = pair.first;
            accountName = pair.second;
            account = new Account(accountName, MainApp.getAccountType());
            if (!AccountUtils.exists(account, this)) {
                continue;
            }
            addObservedFile(file, account);
        }

        // service does not stopSelf() ; that way it tries to be alive forever
    }

    
    /**
     * Registers the local copy of a file to be observed for local changes.
     * 
     * This method does NOT perform a {@link SynchronizeFileOperation} over the
     * file.
     * 
     * @param file      File which local copy must be observed.
     * @param account   ownCloud account containing file.
     */
    private void addObservedFile(OCFile file, Account account) {
        Log_OC.v(TAG, "Adding a file to be watched");

        if (file == null) {
            Log_OC.e(TAG, "Trying to add a NULL file to observer");
            return;
        }
        if (file.getFileId() < 0) {
            Log_OC.e(TAG, "Trying to add an invalid file to observer");
        }
        if (account == null) {
            Log_OC.e(TAG, "Trying to add a file with a NULL account to observer");
            return;
        }

        String localPath = file.getStoragePath();
        if (localPath == null || localPath.length() <= 0) {
            // file downloading or to be downloaded for the first time, or a folder
            localPath = FileStorageUtils.getDefaultSavePathFor(account.name, file);
        }

        File localFile = new File(localPath);
        String observerPath;
        if (file.isFolder()) {
            observerPath = localPath.endsWith(File.separator) ?
                localPath.substring(0, localPath.length()-1) :
                localPath
            ;
        } else {
            observerPath = localFile.getParent();
        }

        AvailableOfflineObserver observer = mAvailableOfflineObserversMap.get(observerPath);
        if (observer == null) {
            observer = new AvailableOfflineObserver(observerPath, account, getApplicationContext());
            mAvailableOfflineObserversMap.put(observerPath, observer);
            Log_OC.d(TAG, "Observer added for folder " + observerPath);
        }

        if(file.isFolder()) {
            // remove any observer that will be overlapped by the new one
            removeOverlappedObservers(observerPath);
            // watch every file and folder below in a single observer
            observer.startWatchingAll();
            Log_OC.d(TAG, "Started recursive observation of folders");
        } else {
            // add a file to watch; it's ADDITIVE; if the observer is already watching ALL, WILL KEEP ON
            // WATCHING ALL; but it doesn't go in depth, if the observer is NOT WATCHING ALL, localFile
            // needs to be a child of the observed folder, not a descendant deeper in the tree
            observer.startWatching(localFile.getName()); // fileId too ¿? ; OCFile ¿?
            Log_OC.d(TAG, "Added " + localPath + " to list of observed children");
        }
    }

    /**
     * Unregisters the local copy of a file to be observed for local changes.
     * 
     * @param file      File which local copy must be not observed longer.
     * @param account   ownCloud account containing file.
     */
    private void removeObservedFile(OCFile file, Account account) {
        Log_OC.v(TAG, "Removing a file from being watched");

        if (file == null) {
            Log_OC.e(TAG, "Trying to remove a NULL file");
            return;
        }
        if (account == null) {
            Log_OC.e(TAG, "Trying to add a file with a NULL account to observer");
            return;
        }

        String localPath = file.getStoragePath();
        if (localPath == null || localPath.length() <= 0) {
            localPath = FileStorageUtils.getDefaultSavePathFor(account.name, file);
        }

        File localFile = new File(localPath);
        String observerPath;
        if (file.isFolder()) {
            observerPath = localPath.endsWith(File.separator) ?
                localPath.substring(0, localPath.length()-1) :
                localPath
            ;
        } else {
            observerPath = localFile.getParent();
        }

        AvailableOfflineObserver observer = mAvailableOfflineObserversMap.get(observerPath);
        if (observer != null) {
            if(file.isFolder()) {
                observer.stopWatchingAll();
                mAvailableOfflineObserversMap.remove(observerPath);
                Log_OC.d(TAG, "Recursive observer removed for folder " + observerPath);
            } else {
                observer.stopWatching(localFile.getName());
                if (observer.isEmpty()) {
                    mAvailableOfflineObserversMap.remove(observerPath);
                    Log_OC.d(TAG, "Observer removed for parent folder " + observerPath);
                } // else keep watching the rest of av-off files
            }

        } else {
            Log_OC.d(TAG, "No observer to remove for path " + observerPath);
        }
    }

    /**
     * Stops and removes all the observers watching a folder hanging below the given path.
     *
     * @param ancestorPath      Path to remove any observer watching below.
     */
    private void removeOverlappedObservers(String ancestorPath) {
        Iterator<Map.Entry<String, AvailableOfflineObserver>> iterator =
            mAvailableOfflineObserversMap.entrySet().iterator();
        Map.Entry<String, AvailableOfflineObserver> entry;
        while (iterator.hasNext()) {
            entry = iterator.next();
            if (entry.getKey().startsWith(ancestorPath) && !entry.getKey().equals(ancestorPath)) {
                Log_OC.e(TAG, "Parando overlapped: " + entry.getKey());
                entry.getValue().stopWatching();
                iterator.remove();
            }
        }
    }

    /**
     * Private receiver listening to events broadcasted by the {@link FileDownloader} service.
     * 
     * Pauses and resumes the observance on registered files while being download,
     * in order to avoid to unnecessary synchronizations.
     */
    private class DownloadCompletedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log_OC.d(TAG, "Received broadcast intent " + intent);

            File downloadedFile = new File(intent.getStringExtra(Extras.EXTRA_FILE_PATH));
            String parentPath = downloadedFile.getParent();
            String topPath = FileStorageUtils.getDataFolder();
            AvailableOfflineObserver observer;
            while (parentPath != null && !parentPath.equals(topPath)) {
                observer = mAvailableOfflineObserversMap.get(parentPath);
                if (observer != null) {
                    if (intent.getAction().equals(FileDownloader.getDownloadFinishMessage())
                        && downloadedFile.exists()) {
                        // no matter if the download was successful or not; the
                        // file could be down anyway due to a former download or upload
                        observer.startWatching(downloadedFile.getName());
                        Log_OC.d(TAG, "Resuming observance of " + downloadedFile.getAbsolutePath());

                    } else if (intent.getAction().equals(FileDownloader.getDownloadAddedMessage())) {
                        observer.stopWatching(downloadedFile.getName());
                        Log_OC.d(TAG, "Pausing observance of " + downloadedFile.getAbsolutePath());
                    }

                } else {
                    Log_OC.d(TAG, "No observer for path " + downloadedFile.getAbsolutePath());
                }
                parentPath = new File(parentPath).getParent();
            }
        }
    }
}
