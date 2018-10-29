/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2018 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.files.services;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;
import android.support.annotation.RequiresApi;

import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.Extras;

/**
 * Schedule the periodic job responsible for synchronizing available offline files, a.k.a. kept-in-sync files, that
 * have been updated locally, with the remote server
 */
public class AvailableOfflineHandler {

    private static final String TAG = AvailableOfflineHandler.class.getSimpleName();
    private static final long MILLISECONDS_INTERVAL_AVAILABLE_OFFLINE = 900000;

    // It needs to be always the same so that the previous job is removed and replaced with a new one with the recent
    // configuration
    private static final int JOB_ID_AVAILABLE_OFFLINE = 2;

    private String mAccountName;
    private JobScheduler mJobScheduler;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public AvailableOfflineHandler(Context context, String accountName) {
        mAccountName = accountName;
        mJobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    }

    /**
     * Schedule a periodic job to check whether recently updated available offline files need to be synchronized
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void scheduleAvailableOfflineJob(Context context) {
        ComponentName serviceComponent = new ComponentName(context, AvailableOfflineSyncJobService.class);
        JobInfo.Builder builder;

        builder = new JobInfo.Builder(JOB_ID_AVAILABLE_OFFLINE, serviceComponent);

        builder.setPersisted(true);

        // Execute job every 15 minutes
        builder.setPeriodic(MILLISECONDS_INTERVAL_AVAILABLE_OFFLINE);

        // Extra data
        PersistableBundle extras = new PersistableBundle();

        extras.putInt(Extras.EXTRA_AVAILABLE_OFFLINE_SYNC_JOB_ID, JOB_ID_AVAILABLE_OFFLINE);

        extras.putString(Extras.EXTRA_ACCOUNT_NAME, mAccountName);

        builder.setExtras(extras);

        Log_OC.d(TAG, "Scheduling an AvailableOfflineSyncJobService");

        mJobScheduler.schedule(builder.build());
    }
}
