/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2016 ownCloud Inc.
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
package com.owncloud.android.ui.controller;

import android.accounts.Account;
import android.widget.ProgressBar;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.ComponentsGetter;


public class TransferProgressController implements OnDatatransferProgressListener {

    private static final String TAG = TransferProgressController.class.getCanonicalName();

    private ProgressBar mProgressBar = null;
    private ComponentsGetter mComponentsGetter = null;

    private int mLastPercent = 0;


    public TransferProgressController(ComponentsGetter componentsGetter) {
        if (componentsGetter == null) {
            throw new IllegalArgumentException("Received NULL componentsGetter");
        }
        mComponentsGetter = componentsGetter;
    }


    public void setProgressBar(ProgressBar progressBar) {
        mProgressBar = progressBar;
        if (mProgressBar != null) {
            mProgressBar.setIndeterminate(false);
            mLastPercent = -1;
        }
    }

    public void startListeningProgressFor(OCFile file, Account account) {
        if (mComponentsGetter.getFileDownloaderBinder() != null) {
            mComponentsGetter.getFileDownloaderBinder().
                addDatatransferProgressListener(this, account, file);
        } else {
            Log_OC.e(TAG, "Download service not ready to notify progress");
        }
        if (mComponentsGetter.getFileUploaderBinder() != null) {
            mComponentsGetter.getFileUploaderBinder().
                addDatatransferProgressListener(this, account, file);
        } else {
            Log_OC.e(TAG, "Upload service not ready to notify progress");
        }
    }

    public void stopListeningProgressFor(OCFile file, Account account) {
        if (mComponentsGetter.getFileDownloaderBinder() != null) {
            mComponentsGetter.getFileDownloaderBinder().
                removeDatatransferProgressListener(this, account, file);
        }
        if (mComponentsGetter.getFileUploaderBinder() != null) {
            mComponentsGetter.getFileUploaderBinder().
                removeDatatransferProgressListener(this, account, file);
        }
    }

    @Override
    public void onTransferProgress(
        long progressRate,
        long totalTransferredSoFar,
        long totalToTransfer,
        String filename
    ) {
        if (mProgressBar != null) {
            int percent = (int) (100.0 * ((double) totalTransferredSoFar) / ((double) totalToTransfer));
            if (percent != mLastPercent) {
                mProgressBar.setProgress(percent);
                mProgressBar.postInvalidate();
            }
            mLastPercent = percent;
        }
    }

}
