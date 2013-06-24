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
import java.util.Collection;
import java.util.Hashtable;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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

public class InstantUploadAdapter extends BaseAdapter implements ListAdapter {

    private static final String LOG_TAG = InstantUploadAdapter.class.getSimpleName();
    private ArrayList<FailedImage> mFiles = null;
    private Hashtable<Integer, View> viewList = null;
    private Cursor failedFilesCursor;
    private InstantUploadActivity iuaContext;
    private Context mContext;

    public InstantUploadAdapter(Context context) {
        // super(context, R.id.faild_upload_message);
        this.mContext = context;
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
        iuaContext.setMenuState();
    }

    private void refreshListItems() {
        Log_OC.d(LOG_TAG, "start refresh list items");
        DbHandler db = null;
        mFiles = new ArrayList<FailedImage>();
        viewList = new Hashtable<Integer, View>();
        long id = 0;
        try {
            db = new DbHandler(mContext);
            failedFilesCursor = db.getFailedFiles();
            if (failedFilesCursor != null) {
                while (failedFilesCursor.moveToNext()) {

                    String filePath = failedFilesCursor.getString(1);
                    String errMsg = failedFilesCursor.getString(4);
                    Log_OC.d(LOG_TAG, "Load image at idx:" + failedFilesCursor.getPosition() + " for: " + filePath);
                    File file = null;
                    if (filePath != null) {
                        file = new File(filePath);
                    }
                    if (file != null) {
                        if (file.exists()) {
                            FailedImage fi = new FailedImage();
                            id++;
                            fi.id = id;
                            fi.size = file.length();
                            fi.lastModified = file.lastModified();
                            fi.filename = file.getName();
                            fi.filePath = file.getAbsolutePath();
                            fi.errormessage = errMsg;
                            mFiles.add(fi);
                        } else {
                            db.removeIUPendingFile(filePath);
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

    public Collection<View> getCachedViews() {
        if (viewList != null) {
            return viewList.values();
        } else {
            return new ArrayList();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Log.d(LOG_TAG, "get View for position: " + position);
        if (viewList.containsKey(position)) {
            Log.d(LOG_TAG, "get View from cache for position: " + position);
            return viewList.get(position);
        } else {
            Log.d(LOG_TAG, "create new View for position: " + position);
            LayoutInflater inflator = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflator.inflate(R.layout.failed_upload_files_row, null, true);
            ViewHolder holder = new ViewHolder();
            viewList.put(position, convertView);
            holder.fileSizeV = (TextView) convertView.findViewById(R.id.file_size);
            holder.checkbox = (CheckBox) convertView.findViewById(R.id.failedImagePreviewCheckBox);
            holder.lastModV = (TextView) convertView.findViewById(R.id.last_mod);
            holder.imageButton = (ImageView) convertView.findViewById(R.id.failedImagePreviewImage);
            holder.errorMessage = (TextView) convertView.findViewById(R.id.failed_upload_message);
            if (mFiles != null && mFiles.size() > position) {
                FailedImage file = mFiles.get(position);
                addImageRow(file, holder);
            }
            return convertView;
        }
    }

    private void addImageRow(FailedImage failedImagefile, ViewHolder holder) {

        if (holder != null) {
            if (failedImagefile != null) {
                if (failedImagefile.filePath != null) {
                    Log_OC.d(LOG_TAG, "add row for: " + failedImagefile.filePath);
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

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((checkbox == null) ? 0 : checkbox.hashCode());
            result = prime * result + ((errorMessage == null) ? 0 : errorMessage.hashCode());
            result = prime * result + ((fileSizeV == null) ? 0 : fileSizeV.hashCode());
            result = prime * result + ((imageButton == null) ? 0 : imageButton.hashCode());
            result = prime * result + ((lastModV == null) ? 0 : lastModV.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ViewHolder other = (ViewHolder) obj;
            if (checkbox == null) {
                if (other.checkbox != null)
                    return false;
            } else if (!checkbox.equals(other.checkbox))
                return false;
            if (errorMessage == null) {
                if (other.errorMessage != null)
                    return false;
            } else if (!errorMessage.equals(other.errorMessage))
                return false;
            if (fileSizeV == null) {
                if (other.fileSizeV != null)
                    return false;
            } else if (!fileSizeV.equals(other.fileSizeV))
                return false;
            if (imageButton == null) {
                if (other.imageButton != null)
                    return false;
            } else if (!imageButton.equals(other.imageButton))
                return false;
            if (lastModV == null) {
                if (other.lastModV != null)
                    return false;
            } else if (!lastModV.equals(other.lastModV))
                return false;
            return true;
        }
    }

    class FailedImage {
        long id;
        String filePath;
        String filename;
        long size;
        long lastModified;
        CharSequence errormessage;
    }

    @Override
    public long getItemId(int position) {
        if (mFiles == null || mFiles.size() < position) {
            return 0;
        } else {
            return mFiles.get(position).id;
        }
    }
}
