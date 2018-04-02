package com.owncloud.android.ui.helpers;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.TransferRequester;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.ui.activity.UploadFilesActivity;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.owncloud.android.ui.activity.UploadFilesActivity.EXTRA_CHOSEN_FILES;
import static com.owncloud.android.ui.activity.UploadFilesActivity.RESULT_OK_AND_MOVE;

public class FilesUploadHelper {

    private static final String TAG = FilesUploadHelper.class.toString();

    public static final int REQUEST_IMAGE_CAPTURE = 1;

    private String capturedPhotoPath;
    private File image = null;

    private final Activity activity;
    private final String accountName;

    public FilesUploadHelper(Activity activity, String accountName) {
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
            return (new Boolean(FileStorageUtils.getUsableSpace(accountName) >= total));
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

    public static String getCapturedImageName(){
        Calendar calendar = Calendar.getInstance();
        String year = "" + calendar.get(Calendar.YEAR);
        String month = ("0" + (calendar.get(Calendar.MONTH) + 1));
        String day = ("0" + calendar.get(Calendar.DAY_OF_MONTH));
        String hour = ("0" + calendar.get(Calendar.HOUR_OF_DAY));
        String minute = ("0" + calendar.get(Calendar.MINUTE));
        String second = ("0" + calendar.get(Calendar.SECOND));
        month = month.length() == 3 ? month.substring(1,month.length()) : month;
        day = day.length() == 3 ? day.substring(1,day.length()) : day;
        hour = hour.length() == 3 ? hour.substring(1,hour.length()) : hour;
        minute = minute.length() == 3 ? minute.substring(1,minute.length()) : minute;
        second = second.length() == 3 ? second.substring(1,second.length()) : second;
        String newImageName = "IMG_" + year + month + day + "_" + hour + minute + second;
        return newImageName;
    }


    public File getCapturedImageFile(){
        File capturedImage = new File(capturedPhotoPath);
        File parent = capturedImage.getParentFile();
        File newImage = new File(parent,getCapturedImageName() + ".jpg");
        capturedImage.renameTo(newImage);
        capturedImage.delete();
        capturedPhotoPath = newImage.getAbsolutePath();
        return newImage;
    }

    private File createImageFile(){
        try {
            File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            image = File.createTempFile(getCapturedImageName(), ".jpg", storageDir);
            capturedPhotoPath = image.getAbsolutePath();
        } catch(IOException exception){
            Log_OC.d(TAG,exception.toString());
        }
        return image;
    }

    /**
     * Function to send an intent to the device's camera to capture a picture
     * */
    public void uploadFromCamera(final int requestCode){
        Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = createImageFile();
        if(photoFile != null){
            Uri photoUri = FileProvider.getUriForFile(activity.getApplicationContext(),
                    activity.getResources().getString(R.string.file_provider_authority),photoFile);
            pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoUri);
        }
        if (pictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivityForResult(pictureIntent, requestCode);
        }
    }

    public void onActivityResult(final OnCheckAvailableSpaceListener callback) {
        checkIfAvailableSpace(new String[]{getCapturedImageFile().getAbsolutePath()}, callback);

    }

    public void deleteImageFile() {
        if(image != null) {
            image.delete();
            Log_OC.d(TAG, "File deleted");
        }
    }
}
