/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * @author Shashvat Kedia
 * <p>
 * Copyright (C) 2018 ownCloud GmbH.
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
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.webkit.MimeTypeMap;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.resources.files.RemoteFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import third_parties.daveKoeller.AlphanumComparator;


/**
 * Static methods to help in access to local file system.
 */
public class FileStorageUtils {
    public static final int SORT_NAME = 0;
    public static final int SORT_DATE = 1;
    public static final int SORT_SIZE = 2;
    public static final int FILE_DISPLAY_SORT = 3;
    public static final int UPLOAD_SORT = 4;
    public static Integer mSortOrderFileDisp = SORT_NAME;
    public static Boolean mSortAscendingFileDisp = true;
    public static Integer mSortOrderUpload = SORT_DATE;
    public static Boolean mSortAscendingUpload = true;

    /**
     * Get local storage path for all data of the app in public storages.
     */
    public static String getDataFolder() {
        File sdCard = Environment.getExternalStorageDirectory();
        return sdCard.getAbsolutePath() + "/" + MainApp.getDataFolder();
    }

    /**
     * Get local owncloud storage path for accountName.
     */
    public static String getSavePath(String accountName) {
        File sdCard = Environment.getExternalStorageDirectory();
        return sdCard.getAbsolutePath() + "/" + MainApp.getDataFolder() + "/" + Uri.encode(accountName, "@");
        // URL encoding is an 'easy fix' to overcome that NTFS and FAT32 don't allow ":" in file names,
        // that can be in the accountName since 0.1.190B
    }

    /**
     * Get local path where OCFile file is to be stored after upload. That is,
     * corresponding local path (in local owncloud storage) to remote uploaded
     * file.
     */
    public static String getDefaultSavePathFor(String accountName, OCFile file) {
        return getSavePath(accountName) + file.getRemotePath();
    }

    /**
     * Get absolute path to tmp folder inside datafolder in sd-card for given accountName.
     */
    public static String getTemporalPath(String accountName) {
        File sdCard = Environment.getExternalStorageDirectory();
        return sdCard.getAbsolutePath() + "/" + MainApp.getDataFolder() + "/tmp/" + Uri.encode(accountName, "@");
        // URL encoding is an 'easy fix' to overcome that NTFS and FAT32 don't allow ":" in file names,
        // that can be in the accountName since 0.1.190B
    }

    /**
     * Optimistic number of bytes available on sd-card. accountName is ignored.
     *
     * @param accountName not used. can thus be null.
     * @return Optimistic number of available bytes (can be less)
     */
    @SuppressLint("NewApi")
    public static long getUsableSpace(String accountName) {
        File savePath = Environment.getExternalStorageDirectory();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
            return savePath.getUsableSpace();

        } else {
            StatFs stats = new StatFs(savePath.getAbsolutePath());
            return stats.getAvailableBlocks() * stats.getBlockSize();
        }

    }

    public static String getLogPath() {
        return Environment.getExternalStorageDirectory() + File.separator + MainApp.getDataFolder() + File.separator + "log";
    }

    /**
     * Gets the composed path when video is or must be stored
     *
     * @param context
     * @param fileName: video file name
     * @return String: video file path composed
     */
    public static String getInstantVideoUploadFilePath(Context context, String fileName) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String uploadVideoPathdef = context.getString(R.string.camera_upload_path);
        String uploadVideoPath = pref.getString("instant_video_upload_path", uploadVideoPathdef);
        String value = uploadVideoPath + OCFile.PATH_SEPARATOR + (fileName == null ? "" : fileName);
        return value;
    }

    public static String getParentPath(String remotePath) {
        String parentPath = new File(remotePath).getParent();
        parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath : parentPath + OCFile.PATH_SEPARATOR;
        return parentPath;
    }

    /**
     * Creates and populates a new {@link OCFile} object with the data read from the server.
     *
     * @param remote remote file read from the server (remote file or folder).
     * @return New OCFile instance representing the remote resource described by remote.
     */
    public static OCFile createOCFileFrom(RemoteFile remote) {
        OCFile file = new OCFile(remote.getRemotePath());
        file.setCreationTimestamp(remote.getCreationTimestamp());
        if (remote.getMimeType().equalsIgnoreCase("DIR")) {
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
     * Creates and populates a list of new {@link OCFile} objects with the data read from the server.
     *
     * @param remoteFiles remote files read from the server (remote files or folders)
     * @return New OCFile list instance representing the remote resource described by remote.
     */
    public static ArrayList<OCFile> createOCFilesFromRemoteFilesList(ArrayList<RemoteFile>
                                                                             remoteFiles) {
        ArrayList<OCFile> files = new ArrayList<>();

        for (RemoteFile remoteFile : remoteFiles) {
            files.add(createOCFileFrom(remoteFile));
        }

        return files;
    }

    /**
     * Cast list of objects into a list of {@link RemoteFile}
     *
     * @param remoteObjects objects to cast into remote files
     * @return New remote files list
     */
    public static ArrayList<RemoteFile> castObjectsIntoRemoteFiles(ArrayList<Object>
                                                                           remoteObjects) {

        ArrayList<RemoteFile> remoteFiles = new ArrayList<>(remoteObjects.size());

        for (Object object : remoteObjects) {
            remoteFiles.add((RemoteFile) object);
        }

        return remoteFiles;
    }

    /**
     * Creates and populates a new {@link RemoteFile} object with the data read from an {@link OCFile}.
     *
     * @param ocFile OCFile
     * @return New RemoteFile instance representing the resource described by ocFile.
     */
    public static RemoteFile fillRemoteFile(OCFile ocFile) {
        RemoteFile file = new RemoteFile(ocFile.getRemotePath());
        file.setCreationTimestamp(ocFile.getCreationTimestamp());
        file.setLength(ocFile.getFileLength());
        file.setMimeType(ocFile.getMimetype());
        file.setModifiedTimestamp(ocFile.getModificationTimestamp());
        file.setEtag(ocFile.getEtag());
        file.setPermissions(ocFile.getPermissions());
        file.setRemoteId(ocFile.getRemoteId());
        file.setPrivateLink(ocFile.getPrivateLink());
        return file;
    }

    /**
     * Sort all OCFile Objects
     * Used for sorting files that have already been uploaded
     *
     * @param files
     */
    public static Vector<OCFile> sortFolderFileDisp(Vector<OCFile> files) {
        Vector<Object> sortedFiles = sortFolder(transformVecOCFile(files), mSortOrderFileDisp, mSortAscendingFileDisp);
        files.clear();
        for (Object object : sortedFiles) {
            files.add((OCFile) object);
        }
        return files;
    }

    /**
     * Sort all File Objects
     * Used for sorting while selecting files to upload
     *
     * @param files
     */
    public static File[] sortFolderUpload(File[] files) {
        Vector<Object> sortedFiles = sortFolder(transformFileArray(files), mSortOrderUpload, mSortAscendingUpload);
        files = new File[sortedFiles.size()];
        for (int i = 0; i < files.length; i++) {
            files[i] = ((File) sortedFiles.get(i));
        }
        return files;
    }

    /**
     * Function to cast Vector<OCFile> to Vector<Object>
     */
    public static Vector<Object> transformVecOCFile(Vector<OCFile> files) {
        Vector<Object> filesObj = new Vector<Object>();
        for (OCFile file : files) {
            filesObj.add((Object) file);
        }
        return filesObj;
    }

    /**
     * Function to cast File[] to Vector<Object>
     */
    public static Vector<Object> transformFileArray(File[] files) {
        Vector<Object> filesObj = new Vector<Object>();
        for (File file : files) {
            filesObj.add((Object) file);
        }
        return filesObj;
    }

    /**
     * Sorts all filenames, regarding last user decision
     */
    public static Vector<Object> sortFolder(Vector<Object> files, int sortOrder, boolean isAscending) {
        switch (sortOrder) {
            case SORT_NAME:
                files = FileStorageUtils.sortByName(files, isAscending);
                break;
            case SORT_DATE:
                files = FileStorageUtils.sortByDate(files, isAscending);
                break;
            case SORT_SIZE:
                files = FileStorageUtils.sortBySize(files, isAscending);
                break;
        }

        return files;
    }

    /**
     * Sorts list by Date
     *
     * @param files
     */
    public static Vector<Object> sortByDate(Vector<Object> files, boolean isAscending) {
        final Integer val;
        if (isAscending) {
            val = 1;
        } else {
            val = -1;
        }

        Collections.sort(files, new Comparator<Object>() {
            public int compare(Object o1, Object o2) {
                boolean o1Folder = false, o2Folder = false;
                long o1ModTime = 0, o2ModTime = 0;
                if (o1 instanceof OCFile) {
                    o1Folder = ((OCFile) o1).isFolder();
                    o2Folder = ((OCFile) o2).isFolder();
                    o1ModTime = ((OCFile) o1).getModificationTimestamp();
                    o2ModTime = ((OCFile) o2).getModificationTimestamp();
                } else if (o1 instanceof File) {
                    o1Folder = ((File) o1).isDirectory();
                    o2Folder = ((File) o2).isDirectory();
                    o1ModTime = ((File) o1).lastModified();
                    o2ModTime = ((File) o2).lastModified();
                }
                if (o1Folder && o2Folder) {
                    Long obj1 = o1ModTime;
                    return val * obj1.compareTo(o2ModTime);
                } else if (o1Folder) {
                    return -1;
                } else if (o2Folder) {
                    return 1;
                } else if (o1ModTime == 0 || o2ModTime == 0) {
                    return 0;
                } else {
                    Long obj1 = o1ModTime;
                    return val * obj1.compareTo(o2ModTime);
                }
            }
        });

        return files;
    }

    /**
     * Sorts list by Size
     */
    public static Vector<Object> sortBySize(Vector<Object> files, boolean isAscending) {
        final Integer val;
        if (isAscending) {
            val = 1;
        } else {
            val = -1;
        }

        Collections.sort(files, new Comparator<Object>() {
            public int compare(Object o1, Object o2) {
                boolean o1Folder = false, o2Folder = false;
                long o1FileLength = 0, o2FileLength = 0;
                if (o1 instanceof OCFile) {
                    o1Folder = ((OCFile) o1).isFolder();
                    o2Folder = ((OCFile) o2).isFolder();
                    o1FileLength = ((OCFile) o1).getFileLength();
                    o2FileLength = ((OCFile) o2).getFileLength();
                } else if (o1 instanceof File) {
                    o1Folder = ((File) o1).isDirectory();
                    o2Folder = ((File) o2).isDirectory();
                    o1FileLength = ((File) o1).length();
                    o2FileLength = ((File) o1).length();
                }
                if (o1Folder && o2Folder) {
                    Long obj1 = o1FileLength;
                    return val * obj1.compareTo(o2FileLength);
                } else if (o1Folder) {
                    return -1;
                } else if (o2Folder) {
                    return 1;
                } else if (o1FileLength == 0 || o2FileLength == 0) {
                    return 0;
                } else {
                    Long obj1 = o1FileLength;
                    return val * obj1.compareTo(o2FileLength);
                }
            }
        });

        return files;
    }

    /**
     * Sorts list by Name
     *
     * @param files files to sort
     */
    public static Vector<Object> sortByName(Vector<Object> files, boolean isAscending) {
        final Integer val;
        if (isAscending) {
            val = 1;
        } else {
            val = -1;
        }

        Collections.sort(files, new Comparator<Object>() {
            public int compare(Object o1, Object o2) {
                boolean o1Folder = false, o2Folder = false;
                if (o1 instanceof OCFile) {
                    o1Folder = ((OCFile) o1).isFolder();
                    o2Folder = ((OCFile) o2).isFolder();
                } else if (o1 instanceof File) {
                    o1Folder = ((File) o1).isDirectory();
                    o2Folder = ((File) o2).isDirectory();
                }
                if (o1Folder && o2Folder) {
                    return val * new AlphanumComparator().compare(o1, o2);
                } else if (o1Folder) {
                    return -1;
                } else if (o2Folder) {
                    return 1;
                }
                return val * new AlphanumComparator().compare(o1, o2);
            }
        });

        return files;
    }

    /**
     * Mimetype String of a file
     *
     * @param path
     * @return
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

}
