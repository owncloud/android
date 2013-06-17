/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
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
package com.owncloud.android.ui.activity;

import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.internal.view.menu.MenuWrapper;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.owncloud.android.AccountUtils;
import com.owncloud.android.Log_OC;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.DbHandler;
import com.owncloud.android.files.InstantUploadBroadcastReceiver;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.utils.FileStorageUtils;

/**
 * This Activity is used to display a list with images they could not be
 * uploaded instantly. The images can be selected for delete or for a try again
 * upload
 * 
 * The entry-point for this activity is the 'Failed upload Notification" and a
 * sub-menu underneath the 'Upload' menu-item
 * 
 * @author andomaex / Matthias Baumann
 */
public class InstantUploadActivity extends SherlockActivity {

    private static final String LOG_TAG = InstantUploadActivity.class.getSimpleName();
    private ListView listView;
    public static final boolean IS_ENABLED = true;
    private InstantUploadAdapter listAdapter = null;
    private boolean listItemSelectState = false;
    private MenuWrapper optionsMenu = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(this.getString(R.string.failed_upload_headline_text));
        setContentView(R.layout.failed_upload_files);
        // add detail data
        listView = (ListView) findViewById(R.id.failed_upload_files_list_view);
        if (listView != null) {
            listAdapter = new InstantUploadAdapter(this);
            listView.setAdapter(listAdapter);
            listAdapter.notifyDataSetChanged();
        }
    }

    /**
     * provide a list of CheckBox instances, looked up from parent listview this
     * list is used to select/deselect all checkboxes at the list
     * 
     * @return List<CheckBox>
     */
    private List<CheckBox> getCheckboxList() {
        List<CheckBox> list = new ArrayList<CheckBox>();
        for (int i = 0; i < listView.getChildCount(); i++) {
            Log_OC.d(LOG_TAG, "ListView has Childs: " + listView.getChildCount());
            View childView = listView.getChildAt(i);
            if (childView != null && childView instanceof ViewGroup) {
                View checkboxView = getChildViews((ViewGroup) childView);
                if (checkboxView != null && checkboxView instanceof CheckBox) {
                    Log_OC.d(LOG_TAG, "found Child: " + checkboxView.getId() + " " + checkboxView.getClass());
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
    private OnMenuItemClickListener getCheckAllListener(final boolean isChecked) {
        return new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                List<CheckBox> list = getCheckboxList();
                for (CheckBox checkbox : list) {
                    ((CheckBox) checkbox).setChecked(isChecked);
                }
                listItemSelectState = isChecked;
                setMenuState();
                return true;
            }
        };
    }

    /**
     * Button click Listener for the retry button at the headline
     * 
     * @return a Listener to perform a retry for all selected images
     */
    private OnMenuItemClickListener getRetryListner() {
        return new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                try {

                    List<CheckBox> list = getCheckboxList();
                    for (CheckBox checkbox : list) {
                        boolean to_retry = checkbox.isChecked();

                        Log_OC.d(LOG_TAG, "Checkbox for " + checkbox.getId() + " was checked: " + to_retry);

                        String img_path = (String) checkbox.getTag(R.string.failed_upload_cb_path_tag);
                        final String msg = "Image-Path " + checkbox.getId() + " was checked: " + img_path;
                        Log_OC.d(LOG_TAG, msg);
                        startUpload(img_path);
                    }

                } finally {

                    listAdapter.notifyDataSetChanged();
                    setMenuState();
                }

                return true;
            }
        };
    }

    /**
     * Button click Listener for the delete button at the headline
     * 
     * @return a Listener to perform a delete for all selected images
     */
    private OnMenuItemClickListener getDeleteListner() {

        return new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final DbHandler dbh = new DbHandler(getApplicationContext());
                try {
                    List<CheckBox> list = getCheckboxList();
                    for (CheckBox checkbox : list) {
                        boolean to_be_delete = checkbox.isChecked();

                        Log_OC.d(LOG_TAG, "Checkbox for " + checkbox.getId() + " was checked: " + to_be_delete);
                        String img_path = (String) checkbox.getTag(R.string.failed_upload_cb_path_tag);
                        Log_OC.d(LOG_TAG, "Image-Path " + checkbox.getId() + " was checked: " + img_path);
                        if (to_be_delete && img_path != null) {
                            boolean deleted = dbh.removeIUPendingFile(img_path);
                            Log_OC.d(LOG_TAG, "removing " + checkbox.getId() + " was : " + deleted);
                        }

                    }
                } finally {
                    dbh.close();
                    // refresh the List
                    listAdapter.notifyDataSetChanged();
                    setMenuState();

                }
                return true;
            }
        };
    }

    public static boolean hasFailedItems(Context context) {
        boolean result = false;
        if (context != null) {
            DbHandler db = new DbHandler(context);
            Cursor c = db.getFailedFiles();
            if (c != null) {
                result = c.getCount() > 0;
            }
        }

        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSherlock().getMenuInflater();
        inflater.inflate(R.menu.failed_upload_menu, menu);
        addListeners(menu);
        optionsMenu = (MenuWrapper) menu;
        return true;
    }

    @Override
    public void onOptionsMenuClosed(android.view.Menu menu) {
        setMenuState();
    }

    public void setMenuState() {
        android.view.Menu menu = null;
        if (optionsMenu != null) {
            menu = optionsMenu.unwrap();
        }
        if (menu != null) {

            List<CheckBox> list = getCheckboxList();
            boolean isLeastOneChecked = false;
            boolean isLeastOne = false;
            if (list != null) {
                isLeastOne = list.size() > 0;
                for (CheckBox checkbox : list) {
                    if (checkbox.isChecked()) {
                        isLeastOneChecked = true;
                        break;
                    }
                }
            }

            android.view.MenuItem select_all = (android.view.MenuItem) menu
                    .findItem(R.id.failed_upload_menu_select_all);
            if (select_all != null) {
                select_all.setVisible(!this.listItemSelectState);
                select_all.setEnabled(isLeastOne);
            }
            android.view.MenuItem deselect_all = (android.view.MenuItem) menu
                    .findItem(R.id.failed_upload_menu_deselect_all);
            if (deselect_all != null) {
                deselect_all.setVisible(this.listItemSelectState);
                deselect_all.setEnabled(isLeastOne);
            }

            android.view.MenuItem retryAll = (android.view.MenuItem) menu.findItem(R.id.failed_upload_menu_retry_all);
            if (retryAll != null) {
                retryAll.setEnabled(isLeastOneChecked);
            }

            android.view.MenuItem deleteAll = (android.view.MenuItem) menu.findItem(R.id.failed_upload_menu_delete_all);
            if (deleteAll != null) {
                deleteAll.setEnabled(isLeastOneChecked);
            }
        }
    }

    private void addListeners(Menu menu) {

        MenuItem delete_all_btn = (MenuItem) menu.findItem(R.id.failed_upload_menu_delete_all);
        if (delete_all_btn != null) {
            delete_all_btn.setOnMenuItemClickListener(getDeleteListner());
        }

        MenuItem retry_all_btn = (MenuItem) menu.findItem(R.id.failed_upload_menu_retry_all);
        if (retry_all_btn != null) {
            retry_all_btn.setOnMenuItemClickListener(getRetryListner());
        }

        MenuItem select_all = (MenuItem) menu.findItem(R.id.failed_upload_menu_select_all);
        if (select_all != null) {
            select_all.setOnMenuItemClickListener(getCheckAllListener(true));
        }
        MenuItem deselect_all = (MenuItem) menu.findItem(R.id.failed_upload_menu_deselect_all);
        if (deselect_all != null) {
            deselect_all.setOnMenuItemClickListener(getCheckAllListener(false));
        }
    }

    /**
     * start uploading a file to the INSTANT_UPLOD_DIR
     * 
     * @param img_path
     */
    public void startUpload(String img_path) {
        // extract filename
        String filename = FileStorageUtils.getInstantUploadFilePath(this, getFileName(img_path));
        if (canInstantUpload()) {
            Account account = AccountUtils.getCurrentOwnCloudAccount(this);
            // add file again to upload queue
            DbHandler db = new DbHandler(this);
            try {
                db.updateFileState(img_path, DbHandler.UPLOAD_STATUS_UPLOAD_LATER, null);
            } finally {
                db.close();
                listAdapter.notifyDataSetChanged();
            }

            Intent i = new Intent(this, FileUploader.class);
            i.putExtra(FileUploader.KEY_ACCOUNT, account);
            i.putExtra(FileUploader.KEY_LOCAL_FILE, img_path);
            i.putExtra(FileUploader.KEY_REMOTE_FILE, filename);
            i.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_SINGLE_FILE);
            i.putExtra(com.owncloud.android.files.services.FileUploader.KEY_INSTANT_UPLOAD, true);

            final String msg = "try to upload file with name :" + filename;
            Log_OC.d(LOG_TAG, msg);
            Toast toast = Toast.makeText(this, this.getString(R.string.failed_upload_retry_text) + filename,
                    Toast.LENGTH_LONG);
            toast.show();

            this.startService(i);
        } else {
            Toast toast = Toast.makeText(this, this.getString(R.string.failed_upload_retry_do_nothing_text) + filename,
                    Toast.LENGTH_LONG);
            toast.show();
        }
    }

    // to ensure we will not add the slash twice between filename and
    // folder-name and to cut of the local filepath
    private static String getFileName(String filepath) {
        if (filepath != null && !"".equals(filepath)) {
            int psi = filepath.lastIndexOf(OCFile.PATH_SEPARATOR);
            String filename = filepath;
            if (psi > -1) {
                filename = filepath.substring(psi + 1, filepath.length());
            }
            return filename;
        } else {
            return "";
        }
    }

    private boolean canInstantUpload() {

        if (!InstantUploadBroadcastReceiver.isOnline(this)
                || (InstantUploadBroadcastReceiver.instantUploadViaWiFiOnly(this) && !InstantUploadBroadcastReceiver
                        .isConnectedViaWiFi(this))) {
            return false;
        } else {
            return true;
        }
    }
}