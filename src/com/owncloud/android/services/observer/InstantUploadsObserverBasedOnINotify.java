/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2017 ownCloud GmbH.
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

import android.content.Context;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;

import com.owncloud.android.db.PreferenceManager.InstantUploadsConfiguration;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.util.HashMap;

/**
 * Observer watching a folder to request the upload of new pictures or videos inside it.
 *
 * Implementation based in {@link FileObserver}, class in the Android framework acting as a hook
 * with iNotify ({@see https://en.wikipedia.org/wiki/Inotify}).
 */
public class InstantUploadsObserverBasedOnINotify extends FileObserver implements InstantUploadsObserver {

    private static final String TAG = InstantUploadsObserverBasedOnINotify.class.getSimpleName();

    private static final int CREATE_MASK = (
            FileObserver.CREATE | FileObserver.MODIFY | FileObserver.CLOSE_WRITE |
            FileObserver.MOVED_TO
    );

    // never use 0xffffffff ; that would include the bit 0x80000000, that means ONE_SHOT, and only one
    // event occurrence would be received
    private static final int ALL_EVENTS_EVEN_THOSE_NOT_DOCUMENTED = 0x7fffffff;   // NEVER use 0xffffffff
    private static final int IN_IGNORE = 32768;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final Object mLock = new Object();  // to sync mConfiguration

    private InstantUploadsConfiguration mConfiguration;
    private Context mContext;
    private HashMap<String, Boolean> mObservedChildren;
    private InstantUploadsHandler mInstantUploadsHandler;

    /**
     * Constructor.
     *
     * Initializes the observer to receive events about files created in the source folder
     * included in parameter 'configuration'.
     *
     *
     * @param configuration     Full configuration for instant uploads to apply, including folder to watch.
     * @param context           Used to start an operation to upload a file, when needed.
     */
    public InstantUploadsObserverBasedOnINotify(InstantUploadsConfiguration configuration, Context context) {
        super(configuration.getSourcePath(), CREATE_MASK);
        
        if (context == null) {
            throw new IllegalArgumentException("NULL context argument received");
        }

        mConfiguration = configuration;
        mContext = context;
        mObservedChildren = new HashMap<>();
        mInstantUploadsHandler = new InstantUploadsHandler();
    }

    /**
     * Receives and processes events about updates of the monitored folder.
     *
     * This is almost heuristic. Do no expect it works magically with any camera.
     *
     * For instance, Google Camera creates a new video file when the user enters in "video mode", before
     * start to record, and saves it empty if the user leaves recording nothing. True store. Life is magic.
     *
     * @param event     Kind of event occurred.
     * @param path      Relative path of the file referred by the event.
     */
    @Override
    public void onEvent(int event, String path) {
        Log_OC.d(TAG, "Got event " + event + " about "  + ((path != null) ? path : "") +
            " in current camera folder");

        if (path != null && path.length() > 0) {
            synchronized (mLock) {
                Log_OC.d(TAG, "Observed camera folder is " + mConfiguration.getSourcePath());

                if ((event & FileObserver.CREATE) != 0) {
                    // new file created, let's watch it; false -> not modified yet
                    mObservedChildren.put(path, false);
                }
                if (((event & FileObserver.MODIFY) != 0) &&
                        mObservedChildren.containsKey(path) &&
                        !mObservedChildren.get(path)
                    ) {
                    // watched file was written for the first time after creation
                    mObservedChildren.put(path, true);
                }
                if ((event & FileObserver.CLOSE_WRITE) != 0 &&
                        mObservedChildren.containsKey(path)    &&
                        mObservedChildren.get(path)
                    ) {
                    // a file that was previously created and written has been closed;
                    // testing for FileObserver.MODIFY is needed because some apps
                    // close the video file right after creating it when the recording
                    // is started, and reopen it to write with the first chunk of video
                    // to save; for instance, Camera MX does so.
                    mObservedChildren.remove(path);
                    handleNewFile(path);
                }
                if ((event & FileObserver.MOVED_TO) != 0) {
                    // a file has been moved or renamed into the folder;
                    // for instance, Google Camera does so right after
                    // saving a video recording
                    handleNewFile(path);
                }
            }
        }

        if ((event & IN_IGNORE) != 0 &&
            (path == null || path.length() == 0)) {
            Log_OC.d(TAG, "Stopping the observance on " + mConfiguration.getSourcePath());
        }
    }


    /**
     * Request the upload of a file just created if matches the criteria of the current
     * configuration for instant uploads.
     *
     * @param fileName      Name of the file just created
     */
    private void handleNewFile(final String fileName) {
        mInstantUploadsHandler.handleNewFile(fileName, mConfiguration, mContext);
    }

    /**
     * Updates the configuration for instant uploads with the one received.
     *
     * Source path of both the new and the current configurations must be the same.
     *
     * @param configuration     New configuration for instant uploads to replace the current one.
     */
    @Override
    public boolean updateConfiguration(InstantUploadsConfiguration configuration) {
        if (configuration == null) {
            return false;
        }
        synchronized (mLock) {
            if (mConfiguration.getSourcePath().equals(configuration.getSourcePath())) {
                mConfiguration = configuration;
                return true;
            }
        }
        return false;
    }

    @Override
    public void startObserving() {
        startWatching();
    }

    @Override
    public void stopObserving() {
        stopWatching();
    }
}
