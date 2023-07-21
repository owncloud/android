/*
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * @author Shashvat Kedia
 * @author Juan Carlos Garrote Gascón
 * <p>
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

package com.owncloud.android.utils;

import android.annotation.SuppressLint;
import android.webkit.MimeTypeMap;

import com.owncloud.android.data.providers.LocalStorageProvider;
import kotlin.Lazy;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

import java.io.File;

import static org.koin.java.KoinJavaComponent.inject;

/**
 * Static methods to help in access to local file system.
 */
public class FileStorageUtils {

    public static final int SORT_NAME = 0;
    public static final int SORT_DATE = 1;
    public static final int SORT_SIZE = 2;
    public static final int FILE_DISPLAY_SORT = 3;
    public static Integer mSortOrderFileDisp = SORT_NAME;
    public static Boolean mSortAscendingFileDisp = true;

    // Let's use the LocalStorageProvider from now on.
    // It is in the data module, and it will be beneficial for new architecture.
    private static LocalStorageProvider getLocalStorageProvider() {
        @NotNull Lazy<LocalStorageProvider> localStorageProvider = inject(LocalStorageProvider.class);
        return localStorageProvider.getValue();
    }

    /**
     * Get absolute path to tmp folder inside datafolder in sd-card for given accountName.
     */
    public static String getTemporalPath(String accountName, String spaceId) {
        return getLocalStorageProvider().getTemporalPath(accountName, spaceId);
    }

    /**
     * Optimistic number of bytes available on sd-card.
     *
     * @return Optimistic number of available bytes (can be less)
     */
    @SuppressLint("UsableSpace")
    public static long getUsableSpace() {
        return getLocalStorageProvider().getUsableSpace();
    }

    /**
     * Mimetype String of a file
     */
    public static String getMimeTypeFromName(String path) {
        String extension = "";
        int pos = path.lastIndexOf('.');
        if (pos >= 0) {
            extension = path.substring(pos + 1);
        }
        String result = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        return (result != null) ? result : "";
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        Timber.w("File NOT deleted %s", child);
                        return false;
                    } else {
                        Timber.d("File deleted %s", child);
                    }
                }
            } else {
                return false;
            }
        }

        return dir.delete();
    }
}
