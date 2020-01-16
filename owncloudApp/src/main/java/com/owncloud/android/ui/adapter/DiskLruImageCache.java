/*
 *   ownCloud Android client application
 *
 * @author Christian Schabesberger
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.ui.adapter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;

import com.jakewharton.disklrucache.DiskLruCache;
import com.owncloud.android.MainApp;
import timber.log.Timber;

public class DiskLruImageCache {

    private DiskLruCache mDiskCache;
    private CompressFormat mCompressFormat;
    private int mCompressQuality;
    private static final int CACHE_VERSION = 1;
    private static final int VALUE_COUNT = 1;
    private static final int IO_BUFFER_SIZE = 8 * 1024;

    //public DiskLruImageCache( Context context,String uniqueName, int diskCacheSize,
    public DiskLruImageCache(
            File diskCacheDir, int diskCacheSize, CompressFormat compressFormat, int quality
    ) throws IOException {

        mDiskCache = DiskLruCache.open(
                diskCacheDir, CACHE_VERSION, VALUE_COUNT, diskCacheSize
        );
        mCompressFormat = compressFormat;
        mCompressQuality = quality;
    }

    private boolean writeBitmapToFile(Bitmap bitmap, DiskLruCache.Editor editor)
            throws IOException {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(editor.newOutputStream(0), IO_BUFFER_SIZE);
            return bitmap.compress(mCompressFormat, mCompressQuality, out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public void put(String key, Bitmap data) {

        DiskLruCache.Editor editor = null;
        String validKey = convertToValidKey(key);
        try {
            editor = mDiskCache.edit(validKey);
            if (editor == null) {
                return;
            }

            if (writeBitmapToFile(data, editor)) {
                mDiskCache.flush();
                editor.commit();
                if (MainApp.Companion.isDeveloper()) {
                   Timber.d( "cache_test_DISK_ image put on disk cache %s", validKey );
                }
            } else {
                editor.abort();
                if (MainApp.Companion.isDeveloper()) {
                    Timber.d( "cache_test_DISK_ ERROR on: image put on disk cache %s", validKey );
                }
            }
        } catch (IOException e) {
            if (MainApp.Companion.isDeveloper()) {
                Timber.d( "cache_test_DISK_ ERROR on: image put on disk cache %s", validKey );
            }
            try {
                if (editor != null) {
                    editor.abort();
                }
            } catch (IOException ignored) {
            }
        }

    }

    public Bitmap getBitmap(String key) {

        Bitmap bitmap = null;
        DiskLruCache.Snapshot snapshot = null;
        String validKey = convertToValidKey(key);
        try {

            snapshot = mDiskCache.get(validKey);
            if (snapshot == null) {
                return null;
            }
            final InputStream in = snapshot.getInputStream(0);
            if (in != null) {
                final BufferedInputStream buffIn =
                        new BufferedInputStream(in, IO_BUFFER_SIZE);
                bitmap = BitmapFactory.decodeStream(buffIn);
            }
        } catch (IOException e) {
            Timber.e(e);
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }

        if (MainApp.Companion.isDeveloper()) {
            Timber.d(bitmap == null ? "not found" : "image read from disk %s", validKey);
        }

        return bitmap;

    }

    private String convertToValidKey(String key) {
        return Integer.toString(key.hashCode());
    }

    /**
     * Remove passed key from cache
     * @param key
     */
    public void removeKey(String key) {
        String validKey = convertToValidKey(key);
        try {
            mDiskCache.remove(validKey);
            Timber.d("removeKey from cache: %s", validKey);
        } catch (IOException e) {
Timber.e(e);
        }
    }
}
