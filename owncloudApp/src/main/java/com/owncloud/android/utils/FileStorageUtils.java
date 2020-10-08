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

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.webkit.MimeTypeMap;

import com.owncloud.android.data.files.datasources.mapper.RemoteFileMapper;
import com.owncloud.android.data.storage.LocalStorageProvider;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.lib.resources.files.RemoteFile;
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
     * Get local owncloud storage path for accountName.
     */
    public static String getSavePath(String accountName) {
        return getLocalStorageProvider().getAccountDirectoryPath(accountName);
    }

    /**
     * Get local path where OCFile file is to be stored after upload. That is,
     * corresponding local path (in local owncloud storage) to remote uploaded
     * file.
     */
    public static String getDefaultSavePathFor(String accountName, OCFile file) {
        return getLocalStorageProvider().getDefaultSavePathFor(accountName, file.getRemotePath());
    }

    /**
     * Get absolute path to tmp folder inside datafolder in sd-card for given accountName.
     */
    public static String getTemporalPath(String accountName) {
        return getLocalStorageProvider().getTemporalPath(accountName);
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

    public static String getParentPath(String remotePath) {
        String parentPath = new File(remotePath).getParent();
        parentPath = parentPath.endsWith(File.separator) ? parentPath : parentPath + File.separator;
        return parentPath;
    }

    /**
     * Creates and populates a new {@link OCFile} object with the data read from the server.
     *
     * @param remote remote file read from the server (remote file or folder).
     * @return New OCFile instance representing the remote resource described by remote.
     */
    public static OCFile createOCFileFromRemoteFile(RemoteFile remote) {
        RemoteFileMapper remoteFileMapper = new RemoteFileMapper();
        return remoteFileMapper.toModel(remote);
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

    /**
     * Cleans up unused files, such as deprecated user directories
     */
    public static void deleteUnusedUserDirs(Account[] remainingAccounts) {
        getLocalStorageProvider().deleteUnusedUserDirs(remainingAccounts);
    }
}
