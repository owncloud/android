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
import android.os.Handler;

import com.owncloud.android.db.PreferenceManager.InstantUploadsConfiguration;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;

/**
 * Observer watching a folder to request the upload of new pictures or videos inside it.
 *
 * Implementation based in {@link FileAlterationObserver}, from package {@link org.apache.commons.io.monitor},
 * a component for monitoring file system events included inf Commons IO utilities by Apache Software Foundation.
 *
 * {@see https://commons.apache.org/proper/commons-io/}
 */
public class InstantUploadsObserverBasedOnCommonsIOFileMonitor
    extends FileAlterationObserver implements InstantUploadsObserver {

    private static final String TAG = InstantUploadsObserverBasedOnCommonsIOFileMonitor.class.getName();

    private static final long serialVersionUID = 4821269467392712079L;

    private static final int POLL_PERIOD_IN_MS = 1000 * 60 * 5;     // 5 minutes

    private InstantUploadsConfiguration mConfiguration;
    private Context mContext;
    private InstantUploadsHandler mInstantUploadsHandler;

    private FileAlterationMonitor mMonitor;

    private final Object mLock = new Object();  // to sync mConfiguration, mainly

    public InstantUploadsObserverBasedOnCommonsIOFileMonitor(
        InstantUploadsConfiguration configuration,
        Context context) {

        super(configuration.getSourcePath());

        if (context == null) {
            throw new IllegalArgumentException("NULL context argument received");
        }

        mConfiguration = configuration;
        mContext = context;
        mInstantUploadsHandler = new InstantUploadsHandler();

        mMonitor = new FileAlterationMonitor(POLL_PERIOD_IN_MS); // TODO some reasonable period
    }

    /**
     * Updates the configuration for instant uploads with the one received.
     *
     * Source path of both the new and the current configurations must be the same.
     *
     * @param configuration     New configuration for instant uploads to replace the current one.
     * @return                  'True' if the new configuration could be handled by the observer,
     *                          'false' otherwise.
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
        try {
            initialize();
            addListener(new CameraFolderAlterationListener());
            mMonitor.addObserver(this);
            mMonitor.start();

        } catch (Exception e) {
            Log_OC.e(
                TAG,
                "Exception starting to watch camera folder, instant uploads will not work",
                e
            );
        }
    }

    @Override
    public void stopObserving() {
        try {
            mMonitor.stop();
            mMonitor.removeObserver(this);
            checkAndNotify();
            destroy();

        } catch (Exception e) {
            Log_OC.w(
                TAG,
                "Exception stopping to watch camera folder: " + e.getMessage()
            );
        }

    }

    private class CameraFolderAlterationListener implements FileAlterationListener {

        @Override
        public void onStart(FileAlterationObserver observer) {
            Log_OC.v(TAG, "onStart called");
        }

        @Override
        public void onDirectoryCreate(File directory) {
            Log_OC.i(TAG, "onDirectoryCreate called for " + directory.getAbsolutePath());
        }

        @Override
        public void onDirectoryChange(File directory) {
            Log_OC.i(TAG, "onDirectoryChange called for " + directory.getAbsolutePath());
        }

        @Override
        public void onDirectoryDelete(File directory) {
            Log_OC.i(TAG, "onDirectoryDelete called for " + directory.getAbsolutePath());
        }

        @Override
        public void onFileCreate(final File file) {
            if (file != null) {
                Log_OC.i(TAG, "onFileCreate called for " + file.getAbsolutePath());
                synchronized (mLock) {
                    final String fileName = file.getAbsolutePath().substring(
                        mConfiguration.getSourcePath().length() + 1
                    );
                    mInstantUploadsHandler.handleNewFile(fileName, mConfiguration, mContext);
                }

            } else {
                Log_OC.i(TAG, "onFileCrete called with NULL file");
            }
        }

        @Override
        public void onFileChange(File file) {
            Log_OC.i(TAG, "onFileChange called for " + file.getAbsolutePath());
        }

        @Override
        public void onFileDelete(File file) {
            Log_OC.i(TAG, "onFileDelete called for " + file.getAbsolutePath());
        }

        @Override
        public void onStop(FileAlterationObserver observer) {
            Log_OC.v(TAG, "onStop called");
        }

    }
}
