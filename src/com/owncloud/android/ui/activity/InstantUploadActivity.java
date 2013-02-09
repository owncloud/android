/* ownCloud Android client application
 *   Copyright (C) 2013  Matthias Baumann
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
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
package com.owncloud.android.ui.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.R;
import com.owncloud.android.db.DbHandler;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.InstantUploadService;

/**
 * This Activity is used to display a list with images they could not be
 * uploaded instantly. The images can be selected for delete or for a try again
 * upload
 * 
 * The entrypoint for this activity is the 'Failed upload Notification" and a
 * submenue underneath the 'Upload' menuentry
 * 
 * @author andomaex
 * 
 */
public class InstantUploadActivity extends Activity {

    private static final String LOG_TAG = InstantUploadActivity.class.getSimpleName();
    private LinearLayout listView;
    private static final String retry_chexbox_tag = "retry_chexbox_tag";

    private SparseArray<String> fileList = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.failed_upload_files);

        Button delete_all_btn = (Button) findViewById(R.id.failed_upload_delete_all_btn);
        delete_all_btn.setOnClickListener(getDeleteListner());
        Button retry_all_btn = (Button) findViewById(R.id.failed_upload_retry_all_btn);
        retry_all_btn.setOnClickListener(getRetryListner());
        CheckBox failed_upload_all_cb = (CheckBox) findViewById(R.id.failed_upload_headline_cb);
        failed_upload_all_cb.setOnCheckedChangeListener(getCheckAllListener());
        listView = (LinearLayout) findViewById(R.id.failed_upload_scrollviewlayout);

        initListView();

    }

    /**
     * init the listview with ImageButtons, checkboxes and filename for every
     * Image that was not successfully uploaded
     * 
     * this method is call at Activity creation and on delete one ore more
     * list-entry an on retry the upload by clickinh the ImageButton or by click
     * to the 'retry all' button
     * 
     */
    // TODO add lazy loading, so the list of failed items not loaded completely
    // at the onCreate call
    private void initListView() {
        DbHandler db = new DbHandler(getApplicationContext());

        Cursor c = db.getFailedFiles();
        fileList = new SparseArray<String>();
        listView.removeAllViews();
        int id = 0;
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    id++;
                    String imp_path = c.getString(1);
                    fileList.put(id, imp_path);
                    LinearLayout rowLayout = getLinearLayout(id);
                    rowLayout.addView(getFileCheckbox(id));
                    rowLayout.addView(getImageButton(imp_path, id));
                    rowLayout.addView(getFileButton(imp_path, id));
                    listView.addView(rowLayout);
                    Log.i(LOG_TAG, imp_path);
                }
            } finally {
                db.close();
            }
        }
    }

    /**
     * provide a list of CheckBox instances, looked up from parent listview this
     * list ist used to select/deselect all checkboxes at the list
     * 
     * @return List<CheckBox>
     */
    private List<CheckBox> getCheckboxList() {
        List<CheckBox> list = new ArrayList<CheckBox>();
        for (int i = 0; i < listView.getChildCount(); i++) {
            Log.d(LOG_TAG, "ListView has Childs: " + listView.getChildCount());
            View childView = listView.getChildAt(i);
            if (childView != null && childView instanceof ViewGroup) {
                View checkboxView = getChildViews((ViewGroup) childView);
                if (checkboxView != null && checkboxView instanceof CheckBox) {
                    Log.d(LOG_TAG, "found Child: " + checkboxView.getId() + " " + checkboxView.getClass());
                    list.add((CheckBox) checkboxView);
                }
            }
        }
        return list;
    }

    /**
     * recursive called method, used from getCheckboxList method
     * 
     * @param View
     * @return View
     */
    private View getChildViews(ViewGroup view) {
        if (view != null) {
            for (int i = 0; i < view.getChildCount(); i++) {
                View cb = view.getChildAt(i);
                if (cb != null && cb instanceof ViewGroup) {
                    return getChildViews((ViewGroup) cb);
                } else if (cb instanceof CheckBox) {
                    return cb;
                }
            }
        }
        return null;
    }

    /**
     * create a new OnCheckedChangeListener for the 'check all' checkbox *
     * 
     * @return OnCheckedChangeListener to select all checkboxes at the list
     */
    private OnCheckedChangeListener getCheckAllListener() {
        return new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                List<CheckBox> list = getCheckboxList();
                for (CheckBox checkbox : list) {
                    ((CheckBox) checkbox).setChecked(isChecked);
                }
            }

        };
    }

    /**
     * Button click Listner for the retry button at the headline
     * 
     * @return a Listner to perform a retry for all selected images
     */
    private OnClickListener getRetryListner() {
        return new OnClickListener() {

            @Override
            public void onClick(View v) {

                try {
                    List<CheckBox> list = getCheckboxList();
                    for (CheckBox checkbox : list) {
                        boolean to_retry = checkbox.isChecked();

                        Log.d(LOG_TAG, "Checkbox for " + checkbox.getId() + " was checked: " + to_retry);
                        String img_path = fileList.get(checkbox.getId());
                        if (to_retry) {

                            removeCurrentFileFromDatabase(img_path);
                            final String msg = "Image-Path " + checkbox.getId() + " was checked: " + img_path;
                            Log.d(LOG_TAG, msg);

                            Toast toast = Toast.makeText(InstantUploadActivity.this,
                                    getString(R.string.failed_upload_retry_text) + img_path, Toast.LENGTH_SHORT);
                            toast.show();
                            startUpload(img_path);
                        }

                    }
                } finally {
                    // refresh the List
                    listView.removeAllViews();
                    initListView();
                }

            }
        };
    }

    /**
     * Button click Listener for the delete button at the headline
     * 
     * @return a Listener to perform a delete for all selected images
     */
    private OnClickListener getDeleteListner() {

        return new OnClickListener() {

            @Override
            public void onClick(View v) {
                final DbHandler dbh = new DbHandler(getApplicationContext());
                try {
                    List<CheckBox> list = getCheckboxList();
                    for (CheckBox checkbox : list) {
                        boolean to_be_delete = checkbox.isChecked();

                        Log.d(LOG_TAG, "Checkbox for " + checkbox.getId() + " was checked: " + to_be_delete);
                        String img_path = fileList.get(checkbox.getId());
                        Log.d(LOG_TAG, "Image-Path " + checkbox.getId() + " was checked: " + img_path);
                        if (to_be_delete) {
                            boolean deleted = dbh.removeIUPendingFile(img_path);
                            Log.d(LOG_TAG, "removing " + checkbox.getId() + " was : " + deleted);

                        }

                    }
                } finally {
                    dbh.close();
                    // refresh the List
                    listView.removeAllViews();
                    initListView();
                }

            }
        };
    }

    private LinearLayout getLinearLayout(int id) {
        LinearLayout linearLayout = new LinearLayout(getApplicationContext());
        linearLayout.setId(id);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        linearLayout.setGravity(Gravity.RIGHT);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        return linearLayout;
    }

    private Button getFileButton(final String img_path, int id) {
        Button retryButton = new Button(this);
        retryButton.setId(id);
        retryButton.setText(img_path);
        retryButton.setBackgroundResource(R.color.owncloud_white);
        retryButton.setTextSize(8);
        retryButton.setOnClickListener(getImageButtonOnClickListener(img_path));
        return retryButton;
    }

    private CheckBox getFileCheckbox(int id) {
        CheckBox retryCB = new CheckBox(this);
        retryCB.setId(id);
        retryCB.setBackgroundResource(R.color.owncloud_white);
        retryCB.setTextSize(8);
        retryCB.setTag(retry_chexbox_tag);
        return retryCB;
    }

    private ImageButton getImageButton(String img_path, int id) {
        ImageButton imageButton = new ImageButton(this);
        imageButton.setId(id);
        imageButton.setClickable(true);
        imageButton.setOnClickListener(getImageButtonOnClickListener(img_path));

        // scale and add a thumbnail to the imagebutton
        int base_scale_size = 32;
        if (img_path != null) {
            Log.d(LOG_TAG, "add " + img_path + " to Image Button");
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            Bitmap bitmap = BitmapFactory.decodeFile(img_path, options);
            int width_tpm = options.outWidth, height_tmp = options.outHeight;
            int scale = 3;
            while (true) {
                if (width_tpm / 2 < base_scale_size || height_tmp / 2 < base_scale_size) {
                    break;
                }
                width_tpm /= 2;
                height_tmp /= 2;
                scale++;
            }

            Log.d(LOG_TAG, "scale Imgae with: " + scale);
            BitmapFactory.Options options2 = new BitmapFactory.Options();
            options2.inSampleSize = scale;
            bitmap = BitmapFactory.decodeFile(img_path, options2);

            if (bitmap != null) {
                Log.d(LOG_TAG, "loaded Bitmap Bytes: " + bitmap.getRowBytes());
                imageButton.setImageBitmap(bitmap);
            } else {
                Log.d(LOG_TAG, "could not load imgage: " + img_path);
            }
        }
        return imageButton;
    }

    private OnClickListener getImageButtonOnClickListener(final String img_path) {
        return new OnClickListener() {

            @Override
            public void onClick(View v) {

                removeCurrentFileFromDatabase(img_path);
                initListView();
                startUpload(img_path);
            }

        };
    }

    /**
     * start uploading a file to the INSTANT_UPLOD_DIR
     * 
     * @param img_path
     */
    private void startUpload(String img_path) {
        Intent i = new Intent(InstantUploadActivity.this, FileUploader.class);
        i.putExtra(FileUploader.KEY_ACCOUNT, AccountUtils.getCurrentOwnCloudAccount(InstantUploadActivity.this));
        i.putExtra(FileUploader.KEY_LOCAL_FILE, img_path);
        String filename = img_path.substring(img_path.lastIndexOf('/'), img_path.length());
        final String msg = "try to upload file with name :" + filename;
        Log.d(LOG_TAG, msg);
        Toast toast = Toast.makeText(InstantUploadActivity.this, getString(R.string.failed_upload_retry_text)
                + filename, Toast.LENGTH_LONG);
        toast.show();
        i.putExtra(FileUploader.KEY_REMOTE_FILE, InstantUploadService.INSTANT_UPLOAD_DIR + "/" + filename);
        i.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_SINGLE_FILE);
        startService(i);
    }

    private void removeCurrentFileFromDatabase(String img_path) {
        // first of all delete file from database, new failed uploads will put
        // again to this database by the FileUpload Service
        final DbHandler dbh = new DbHandler(getApplicationContext());
        try {
            boolean status = dbh.removeIUPendingFile(img_path);
            Log.d(LOG_TAG, "removing file " + img_path + " from pending upload database was: " + status);
        } finally {
            dbh.close();
        }

    }

}