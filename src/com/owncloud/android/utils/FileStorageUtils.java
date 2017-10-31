/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2016 ownCloud GmbH.
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
    public static final Integer SORT_NAME = 0;
    public static final Integer SORT_DATE = 1;
    public static final Integer SORT_SIZE = 2;
    public static Integer mSortOrder = SORT_NAME;
    public static Boolean mSortAscending = true;

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
    
    public static String getLogPath()  {
        return Environment.getExternalStorageDirectory() + File.separator + MainApp.getDataFolder() + File.separator + "log";
    }

    public static String getInstantUploadFilePath(Context context, String fileName) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String uploadPathdef = context.getString(R.string.instant_upload_path);
        String uploadPath = pref.getString("instant_upload_path", uploadPathdef);
        String value = uploadPath + OCFile.PATH_SEPARATOR +  (fileName == null ? "" : fileName);
        return value;
    }

    /**
     * Gets the composed path when video is or must be stored
     * @param context
     * @param fileName: video file name
     * @return String: video file path composed
     */
    public static String getInstantVideoUploadFilePath(Context context, String fileName) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String uploadVideoPathdef = context.getString(R.string.instant_upload_path);
        String uploadVideoPath = pref.getString("instant_video_upload_path", uploadVideoPathdef);
        String value = uploadVideoPath + OCFile.PATH_SEPARATOR +  (fileName == null ? "" : fileName);
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
     * @param remote    remote file read from the server (remote file or folder).
     * @return          New OCFile instance representing the remote resource described by remote.
     */
    public static OCFile createOCFileFrom(RemoteFile remote) {
        OCFile file = new OCFile(remote.getRemotePath());
        file.setCreationTimestamp(remote.getCreationTimestamp());
        if (remote.getMimeType().equalsIgnoreCase("DIR")){
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
    public static ArrayList<RemoteFile> castObjectsIntoRemoteFiles (ArrayList<Object>
                                                                         remoteObjects) {

        ArrayList<RemoteFile> remoteFiles = new ArrayList<>(remoteObjects.size());

        for (Object object : remoteObjects) {
            remoteFiles.add((RemoteFile) object);
        }

        return  remoteFiles;
    }
    
    /**
     * Creates and populates a new {@link RemoteFile} object with the data read from an {@link OCFile}.
     * 
     * @param ocFile    OCFile
     * @return          New RemoteFile instance representing the resource described by ocFile.
     */
    public static RemoteFile fillRemoteFile(OCFile ocFile){
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
     * Sorts all filenames, regarding last user decision 
     */
    public static Vector<OCFile> sortFolder(Vector<OCFile> files){
        switch (mSortOrder){
        case 0:
            files = FileStorageUtils.sortByName(files);
            break;
        case 1:
            files = FileStorageUtils.sortByDate(files);
            break;
        case 2:
            files = FileStorageUtils.sortBySize(files);
            break;
        }
       
        return files;
    }
    
    /**
     * Sorts list by Date
     * @param files
     */
    public static Vector<OCFile> sortByDate(Vector<OCFile> files){
        final Integer val;
        if (mSortAscending){
            val = 1;
        } else {
            val = -1;
        }
        
        Collections.sort(files, new Comparator<OCFile>() {
            public int compare(OCFile o1, OCFile o2) {
                if (o1.isFolder() && o2.isFolder()) {
                    Long obj1 = o1.getModificationTimestamp();
                    return val * obj1.compareTo(o2.getModificationTimestamp());
                }
                else if (o1.isFolder()) {
                    return -1;
                } else if (o2.isFolder()) {
                    return 1;
                } else if (o1.getModificationTimestamp() == 0 || o2.getModificationTimestamp() == 0){
                    return 0;
                } else {
                    Long obj1 = o1.getModificationTimestamp();
                    return val * obj1.compareTo(o2.getModificationTimestamp());
                }
            }
        });
        
        return files;
    }

    /**
     * Sorts list by Size
     */
    public static Vector<OCFile> sortBySize(Vector<OCFile> files){
        final Integer val;
        if (mSortAscending){
            val = 1;
        } else {
            val = -1;
        }

        Collections.sort(files, new Comparator<OCFile>() {
            public int compare(OCFile o1, OCFile o2) {
                if (o1.isFolder() && o2.isFolder()) {
                    Long obj1 = o1.getFileLength();
                    return val * obj1.compareTo(o2.getFileLength());
                }
                else if (o1.isFolder()) {
                    return -1;
                } else if (o2.isFolder()) {
                    return 1;
                } else if (o1.getFileLength() == 0 || o2.getFileLength() == 0){
                    return 0;
                } else {
                    Long obj1 = o1.getFileLength();
                    return val * obj1.compareTo(o2.getFileLength());
                }
            }
        });

        return files;
    }

    /**
     * Sorts list by Name
     * @param files     files to sort
     */
    public static Vector<OCFile> sortByName(Vector<OCFile> files){
        final Integer val;
        if (mSortAscending){
            val = 1;
        } else {
            val = -1;
        }

        Collections.sort(files, new Comparator<OCFile>() {
            public int compare(OCFile o1, OCFile o2) {
                if (o1.isFolder() && o2.isFolder()) {
                    return val * new AlphanumComparator().compare(o1, o2);
                } else if (o1.isFolder()) {
                    return -1;
                } else if (o2.isFolder()) {
                    return 1;
                }
                return val * new AlphanumComparator().compare(o1, o2);
            }
        });
        
        return files;
    }

    /**
     * Mimetype String of a file
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
