/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.owncloud.android.DisplayUtils;
import com.owncloud.android.Log_OC;
import com.owncloud.android.R;
import com.owncloud.android.db.DbHandler;

public class InstantUploadAdapter extends ArrayAdapter implements ListAdapter {

    private static final String LOG_TAG = InstantUploadAdapter.class.getSimpleName();
    private ArrayList<FailedImage> mFiles = null;
    private Cursor failedFilesCursor;
    private InstantUploadActivity iuaContext;
    private HashSet<Integer> loadedRow;

    public InstantUploadAdapter(Context context) {
        super(context, R.id.faild_upload_message, R.id.last_mod);
        this.iuaContext = (InstantUploadActivity) context;
    }

    @Override
    public int getCount() {
        return mFiles != null ? mFiles.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        if (mFiles != null && mFiles.size() > position) {
            return mFiles.get(position);
        } else {
            return null;
        }
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        refreshListItems();
    }

    private void refreshListItems() {
        Log_OC.d(LOG_TAG, "start refreshListItems");
        DbHandler db = null;
        mFiles = new ArrayList<FailedImage>();
        loadedRow = new HashSet<Integer>();
        try {
            db = new DbHandler(getContext());
            failedFilesCursor = db.getFailedFiles();
            if (failedFilesCursor != null) {
                while (failedFilesCursor.moveToNext()) {
                    FailedImage fi = new FailedImage();

                    String filePath = failedFilesCursor.getString(1);
                    String errMsg = failedFilesCursor.getString(4);
                    Log_OC.d(LOG_TAG, "Load image at idx:" + failedFilesCursor.getPosition() + " for: " + filePath);
                    File file = null;
                    if (filePath != null) {
                        file = new File(filePath);
                    }
                    if (file != null) {
                        if (file.exists()) {
                            fi.size = file.length();
                            fi.lastModified = file.lastModified();
                            fi.filename = file.getName();
                            fi.filePath = file.getAbsolutePath();
                            fi.errormessage = errMsg;
                            mFiles.add(fi);
                        } else {
                            db.removeIUPendingFile(fi.filePath);
                        }
                    }

                }
            } else {
                Log_OC.w(LOG_TAG, "failedFilesCursor was null, no failed files found at database");
            }
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;
        if (view == null) {
            LayoutInflater inflator = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflator.inflate(R.layout.failed_upload_files_row, null, true);
            holder = new ViewHolder();
            view.setTag(holder);
            holder.fileSizeV = (TextView) view.findViewById(R.id.file_size);
            holder.checkbox = (CheckBox) view.findViewById(R.id.failedImagePreviewCheckBox);
            holder.lastModV = (TextView) view.findViewById(R.id.last_mod);
            holder.imageButton = (ImageView) view.findViewById(R.id.failedImagePreviewImage);
            holder.errorMessage = (TextView) view.findViewById(R.id.faild_upload_message);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        if (!loadedRow.contains(position)) {
            if (mFiles != null && mFiles.size() > position) {
                FailedImage file = mFiles.get(position);
                addImageRow(view, file, holder);
                loadedRow.add(position);
            }
        }
        return view;
    }

    private void addImageRow(View view, FailedImage failedImagefile, ViewHolder holder) {

        if (view != null && holder != null) {
            if (failedImagefile != null) {
                if (failedImagefile.filePath != null) {
                    Log_OC.d(LOG_TAG, "add row to for: " + failedImagefile.filePath);
                    if (holder.fileSizeV != null) {
                        holder.fileSizeV.setVisibility(View.VISIBLE);
                        holder.fileSizeV.setText(DisplayUtils.bytesToHumanReadable(failedImagefile.size));
                    }

                    if (holder.checkbox != null) {
                        holder.checkbox.setVisibility(View.VISIBLE);
                        holder.checkbox.setChecked(false);
                        holder.checkbox.setTag(R.string.failed_upload_cb_path_tag, failedImagefile.filePath);
                        holder.checkbox.setOnCheckedChangeListener(getOnCheckedChangeListener());
                    }

                    if (holder.lastModV != null) {
                        holder.lastModV.setVisibility(View.VISIBLE);
                        holder.lastModV.setText(DisplayUtils.unixTimeToHumanReadable(failedImagefile.lastModified));
                    }

                    if (holder.imageButton != null) {
                        holder.imageButton.setClickable(true);
                        holder.imageButton.setVisibility(View.VISIBLE);
                        holder.imageButton.setOnClickListener(getImageButtonOnClickListener(failedImagefile.filePath));

                        // scale and add a thumbnail to the imagebutton
                        ImageLoader il = new ImageLoader();
                        il.filePath = failedImagefile.filePath;
                        il.imageButton = holder.imageButton;
                        il.load();
                    }
                    if (holder.errorMessage != null) {
                        holder.errorMessage.setVisibility(View.VISIBLE);
                        holder.errorMessage.setText(failedImagefile.errormessage);
                    }
                } else {
                    Log_OC.d(LOG_TAG, "could not load image, imagepath was null");
                }
            }
        }

    }

    private OnCheckedChangeListener getOnCheckedChangeListener() {
        return new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                iuaContext.setMenuState();
                Log_OC.d(LOG_TAG, "set checkbox:" + buttonView.getId() + " to " + isChecked);
            }
        };
    }

    private OnClickListener getImageButtonOnClickListener(final String img_path) {
        return new OnClickListener() {

            @Override
            public void onClick(View v) {
                iuaContext.startUpload(img_path);
                refreshListItems();
            }

        };
    }

    private class ImageLoader {
        int base_scale_size = 32;
        int scale = 3;
        String filePath;
        ImageView imageButton;

        public void load() {

            Log_OC.d(LOG_TAG, "add " + filePath + " to Image Button");
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
            int width_tpm = options.outWidth;
            int height_tmp = options.outHeight;

            while (true) {
                if (width_tpm / 2 < base_scale_size || height_tmp / 2 < base_scale_size) {
                    break;
                }
                width_tpm /= 2;
                height_tmp /= 2;
                scale++;
            }

            Log_OC.d(LOG_TAG, "scale Imgae with: " + scale);
            BitmapFactory.Options options2 = new BitmapFactory.Options();
            options2.inSampleSize = scale;
            bitmap = BitmapFactory.decodeFile(filePath, options2);

            if (bitmap != null) {
                Log_OC.d(LOG_TAG, "loaded Bitmap Bytes: " + bitmap.getRowBytes());
                imageButton.setImageBitmap(bitmap);
            } else {
                Log_OC.d(LOG_TAG, "could not load imgage: " + filePath);
            }
        }

    }

    private static class ViewHolder {
        TextView errorMessage;
        CheckBox checkbox;
        TextView fileSizeV;
        TextView lastModV;
        ImageView imageButton;
    }

    class FailedImage {
        CharSequence errormessage;
        String filePath;
        long size;
        long lastModified;
        String filename;
    }
}
