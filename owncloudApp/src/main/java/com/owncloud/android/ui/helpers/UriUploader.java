/**
 * ownCloud Android client application
 * <p>
 * Copyright (C) 2016 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.helpers;

import android.accounts.Account;
import android.content.ContentResolver;
import android.net.Uri;

import androidx.fragment.app.FragmentManager;
import com.owncloud.android.R;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.asynctasks.CopyAndUploadContentUrisTask;
import com.owncloud.android.ui.fragment.TaskRetainerFragment;
import timber.log.Timber;

import java.util.ArrayList;
import java.util.List;

/**
 * This class examines URIs pointing to files to upload.
 * <p>
 * Legacy class. Needs a refactor, but not today..
 *
 * <p>
 * URIs with scheme file:// will be ignored since it's not recommended anymore. Apps should use FileProviders
 * to share their files.
 * <p>
 * URIs with scheme content:// are handling assuming that file is in private storage owned by a different app,
 * and that persistency permission is not granted. Due to this, contents of the file are temporary copied by
 * the OC app, and then an upload is enqueued.
 */
public class UriUploader {

    private FileActivity mActivity;
    private ArrayList<Uri> mUrisToUpload;
    private CopyAndUploadContentUrisTask.OnCopyTmpFilesTaskListener mCopyTmpTaskListener;

    private String mUploadPath;
    private Account mAccount;
    private String mSpaceId;
    private boolean mShowWaitingDialog;

    private UriUploaderResultCode mCode = UriUploaderResultCode.OK;

    public enum UriUploaderResultCode {
        OK,
        COPY_THEN_UPLOAD,
        ERROR_UNKNOWN,
        ERROR_NO_FILE_TO_UPLOAD,
        ERROR_READ_PERMISSION_NOT_GRANTED
    }

    public UriUploader(
            FileActivity activity,
            ArrayList<Uri> uris,
            String uploadPath,
            Account account,
            String spaceId,
            boolean showWaitingDialog,
            CopyAndUploadContentUrisTask.OnCopyTmpFilesTaskListener copyTmpTaskListener
    ) {
        mActivity = activity;
        mUrisToUpload = uris;
        mUploadPath = uploadPath;
        mAccount = account;
        mSpaceId = spaceId;
        mShowWaitingDialog = showWaitingDialog;
        mCopyTmpTaskListener = copyTmpTaskListener;
    }

    public UriUploaderResultCode uploadUris() {

        try {
            List<Uri> contentUris = new ArrayList<>();

            int schemeFileCounter = 0;

            for (Uri sourceUri : mUrisToUpload) {
                if (sourceUri != null) {

                    if (ContentResolver.SCHEME_CONTENT.equals(sourceUri.getScheme())) {
                        contentUris.add(sourceUri);

                    } else if (ContentResolver.SCHEME_FILE.equals(sourceUri.getScheme())) {
                        schemeFileCounter++;
                        Timber.w("File with scheme file has been received. We don't support this scheme anymore.");
                    }
                }
            }

            if (!contentUris.isEmpty()) {
                /// content: uris will be copied to temporary files before calling the upload usecase
                copyThenUpload(contentUris.toArray(new Uri[0]), mUploadPath, mSpaceId);

                // Listen to CopyAndUploadContentUrisTask before killing the app or a SecurityException may appear.
                // At least when receiving files to upload.
                mCode = UriUploaderResultCode.COPY_THEN_UPLOAD;

            } else if (schemeFileCounter == 0) {
                mCode = UriUploaderResultCode.ERROR_NO_FILE_TO_UPLOAD;
            }

        } catch (SecurityException e) {
            mCode = UriUploaderResultCode.ERROR_READ_PERMISSION_NOT_GRANTED;
            Timber.e(e, "Permissions fail");

        } catch (Exception e) {
            mCode = UriUploaderResultCode.ERROR_UNKNOWN;
            Timber.e(e, "Unexpected error");

        }
        return mCode;
    }

    /**
     * @param sourceUris Array of content:// URIs to the files to upload
     * @param uploadPath Absolute paths where we want to upload the selected files
     */
    private void copyThenUpload(Uri[] sourceUris, String uploadPath, String spaceId) {
        if (mShowWaitingDialog) {
            mActivity.showLoadingDialog(R.string.wait_for_tmp_copy_from_private_storage);
        }

        CopyAndUploadContentUrisTask copyTask = new CopyAndUploadContentUrisTask
                (mCopyTmpTaskListener, mActivity);

        FragmentManager fm = mActivity.getSupportFragmentManager();

        // Init Fragment without UI to retain AsyncTask across configuration changes
        TaskRetainerFragment taskRetainerFragment =
                (TaskRetainerFragment) fm.findFragmentByTag(TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT);

        if (taskRetainerFragment != null) {
            taskRetainerFragment.setTask(copyTask);
        }

        copyTask.execute(
                CopyAndUploadContentUrisTask.makeParamsToExecute(
                        mAccount,
                        sourceUris,
                        uploadPath,
                        mActivity.getContentResolver(),
                        spaceId
                )
        );
    }
}
