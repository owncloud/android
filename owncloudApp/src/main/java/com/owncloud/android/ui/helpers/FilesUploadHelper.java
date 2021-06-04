/**
 * ownCloud Android client application
 *
 * @author Christian Schabesberger
 * @author Shashvat Kedia
 * Copyright (C) 2020 ownCloud GmbH.
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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;
import com.owncloud.android.R;
import com.owncloud.android.utils.FileStorageUtils;
import timber.log.Timber;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

public class FilesUploadHelper implements Parcelable {

    private String capturedPhotoPath;
    private File image = null;

    private Activity activity;
    private String accountName;

    public FilesUploadHelper(Activity activity, String accountName) {
        this.activity = activity;
        this.accountName = accountName;
    }

    protected FilesUploadHelper(Parcel in) {
        this.capturedPhotoPath = in.readString();
        this.image = (File) in.readSerializable();
    }

    public void init(Activity activity, String accountName) {
        this.activity = activity;
        this.accountName = accountName;
    }

    public interface OnCheckAvailableSpaceListener {
        void onCheckAvailableSpaceStart();

        void onCheckAvailableSpaceFinished(boolean hasEnoughSpace, String[] capturedFilePaths);
    }

    /**
     * Asynchronous task checking if there is space enough to copy all the files chosen
     * to upload into the ownCloud local folder.
     * <p>
     * Maybe an AsyncTask is not strictly necessary, but who really knows.
     */
    private class CheckAvailableSpaceTask extends AsyncTask<Void, Void, Boolean> {

        private final String[] checkedFilePaths;
        private final OnCheckAvailableSpaceListener callback;

        public CheckAvailableSpaceTask(String[] checkedFilePaths,
                                       OnCheckAvailableSpaceListener listener) {
            super();
            this.checkedFilePaths = checkedFilePaths;
            this.callback = listener;
        }

        /**
         * Updates the UI before trying the movement
         */
        @Override
        protected void onPreExecute() {
            callback.onCheckAvailableSpaceStart();
        }

        /**
         * Checks the available space
         *
         * @return 'True' if there is space enough.
         */
        @Override
        protected Boolean doInBackground(Void... params) {
            long total = 0;
            for (int i = 0; checkedFilePaths != null && i < checkedFilePaths.length; i++) {
                String localPath = checkedFilePaths[i];
                File localFile = new File(localPath);
                total += localFile.length();
            }
            return (FileStorageUtils.getUsableSpace() >= total);
        }

        /**
         * Updates the activity UI after the check of space is done.
         * <p>
         * If there is not space enough. shows a new dialog to query the user if wants to move the
         * files instead of copy them.
         *
         * @param hasEnoughSpace 'True' when there is space enough to copy all the selected files.
         */
        @Override
        protected void onPostExecute(Boolean hasEnoughSpace) {
            callback.onCheckAvailableSpaceFinished(hasEnoughSpace, checkedFilePaths);
        }
    }

    public void checkIfAvailableSpace(String[] checkedFilePaths,
                                      OnCheckAvailableSpaceListener listener) {
        new CheckAvailableSpaceTask(checkedFilePaths, listener).execute();
    }

    public static String getCapturedImageName() {
        Calendar calendar = Calendar.getInstance();
        String year = "" + calendar.get(Calendar.YEAR);
        String month = ("0" + (calendar.get(Calendar.MONTH) + 1));
        String day = ("0" + calendar.get(Calendar.DAY_OF_MONTH));
        String hour = ("0" + calendar.get(Calendar.HOUR_OF_DAY));
        String minute = ("0" + calendar.get(Calendar.MINUTE));
        String second = ("0" + calendar.get(Calendar.SECOND));
        month = month.length() == 3 ? month.substring(1, month.length()) : month;
        day = day.length() == 3 ? day.substring(1, day.length()) : day;
        hour = hour.length() == 3 ? hour.substring(1, hour.length()) : hour;
        minute = minute.length() == 3 ? minute.substring(1, minute.length()) : minute;
        second = second.length() == 3 ? second.substring(1, second.length()) : second;
        String newImageName = "IMG_" + year + month + day + "_" + hour + minute + second;
        return newImageName;
    }

    public File getCapturedImageFile() {
        File capturedImage = new File(capturedPhotoPath);
        File parent = capturedImage.getParentFile();
        File newImage = new File(parent, getCapturedImageName() + ".jpg");
        capturedImage.renameTo(newImage);
        capturedImage.delete();
        capturedPhotoPath = newImage.getAbsolutePath();
        return newImage;
    }

    private File createImageFile() {
        try {
            File storageDir = activity.getExternalCacheDir();
            image = File.createTempFile(getCapturedImageName(), ".jpg", storageDir);
            capturedPhotoPath = image.getAbsolutePath();
        } catch (IOException exception) {
            Timber.e(exception, exception.toString());
        }
        return image;
    }

    /**
     * Function to send an intent to the device's camera to capture a picture
     */
    public void uploadFromCamera(final int requestCode) {
        Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = createImageFile();
        if (photoFile != null) {
            Uri photoUri = FileProvider.getUriForFile(activity.getApplicationContext(),
                    activity.getResources().getString(R.string.file_provider_authority), photoFile);
            pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        }
        activity.startActivityForResult(pictureIntent, requestCode);
    }

    public void onActivityResult(final OnCheckAvailableSpaceListener callback) {
        checkIfAvailableSpace(new String[]{getCapturedImageFile().getAbsolutePath()}, callback);
    }

    public void deleteImageFile() {
        if (image != null) {
            image.delete();
            Timber.d("File deleted");
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.capturedPhotoPath);
        dest.writeSerializable(this.image);
    }

    public static final Parcelable.Creator<FilesUploadHelper> CREATOR = new Parcelable.Creator<FilesUploadHelper>() {
        @Override
        public FilesUploadHelper createFromParcel(Parcel source) {
            return new FilesUploadHelper(source);
        }

        @Override
        public FilesUploadHelper[] newArray(int size) {
            return new FilesUploadHelper[size];
        }
    };
}
