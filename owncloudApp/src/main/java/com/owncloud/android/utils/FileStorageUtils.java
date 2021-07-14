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
import android.os.Environment;
import android.webkit.MimeTypeMap;

import com.owncloud.android.MainApp;
import com.owncloud.android.data.LocalStorageProvider;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.resources.files.RemoteFile;
import timber.log.Timber;

import java.io.File;
import java.util.Collections;
import java.util.Vector;

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
        return new LocalStorageProvider(MainApp.Companion.getDataFolder());
    }

    /**
     * Get local owncloud storage path for accountName.
     */
    public static String getSavePath(String accountName) {
        return getLocalStorageProvider().getSavePath(accountName);
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
        OCFile file = new OCFile(remote.getRemotePath());
        file.setCreationTimestamp(remote.getCreationTimestamp());
        if (remote.isFolder()) {
            file.setFileLength(remote.getSize());
        } else {
            file.setFileLength(remote.getLength());
        }
        file.setMimetype(remote.getMimeType());
        file.setModificationTimestamp(remote.getModifiedTimestamp());
        file.setEtag(remote.getEtag());
        file.setPermissions(remote.getPermissions());
        file.setRemoteId(remote.getRemoteId());
        file.setPrivateLink(remote.getPrivateLink());
        return file;
    }

    /**
     * Sorts all filenames, regarding last user decision
     */
    public static Vector<OCFile> sortFolder(Vector<OCFile> files, int sortOrder, boolean isAscending) {
        switch (sortOrder) {
            case SORT_NAME:
                FileStorageUtils.sortByName(files, isAscending);
                break;
            case SORT_DATE:
                FileStorageUtils.sortByDate(files, isAscending);
                break;
            case SORT_SIZE:
                FileStorageUtils.sortBySize(files, isAscending);
                break;
        }

        return files;
    }

    /**
     * Sorts list by Date
     */
    private static void sortByDate(Vector<OCFile> files, boolean isAscending) {
        final int val;
        if (isAscending) {
            val = 1;
        } else {
            val = -1;
        }

        Collections.sort(files, (ocFile1, ocFile2) -> {
            if (ocFile1.getModificationTimestamp() == 0 || ocFile2.getModificationTimestamp() == 0) {
                return 0;
            } else {
                Long obj1 = ocFile1.getModificationTimestamp();
                return val * obj1.compareTo(ocFile2.getModificationTimestamp());
            }
        });

    }

    /**
     * Sorts list by Size
     */
    private static void sortBySize(Vector<OCFile> files, boolean isAscending) {
        final int val;
        if (isAscending) {
            val = 1;
        } else {
            val = -1;
        }

        Collections.sort(files, (ocFile1, ocFile2) -> {
            Long obj1 = ocFile1.getFileLength();
            return val * obj1.compareTo(ocFile2.getFileLength());
        });

    }

    /**
     * Sorts list by Name
     *
     * @param files files to sort
     */
    private static void sortByName(Vector<OCFile> files, boolean isAscending) {
        final int val;
        if (isAscending) {
            val = 1;
        } else {
            val = -1;
        }

        Collections.sort(files, (ocFile1, ocFile2) -> {
            if (ocFile1.isFolder() && ocFile2.isFolder()) {
                return val * ocFile1.getFileName().toLowerCase().compareTo(ocFile2.getFileName().toLowerCase());
            } else if (ocFile1.isFolder()) {
                return -1;
            } else if (ocFile2.isFolder()) {
                return 1;
            }
            return val * ocFile1.getFileName().toLowerCase().compareTo(ocFile2.getFileName().toLowerCase());
        });

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

    public static String getDefaultCameraSourcePath() {
        return getLocalStorageProvider().getDefaultCameraSourcePath();
    }
}
