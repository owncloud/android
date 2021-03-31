/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * <p>
 * Copyright (C) 2017 ownCloud GmbH.
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

public class Extras {

    // from both
    public static final String EXTRA_REMOTE_PATH = "REMOTE_PATH";
    public static final String EXTRA_ACCOUNT_NAME = "EXTRA_ACCOUNT_NAME";
    public static final String EXTRA_LINKED_TO_PATH = "LINKED_TO";

    // from FileDownloader
    public static final String EXTRA_DOWNLOAD_RESULT = "RESULT";

    // from FileUploader
    public static final String EXTRA_OLD_REMOTE_PATH = "OLD_REMOTE_PATH";
    public static final String EXTRA_OLD_FILE_PATH = "OLD_FILE_PATH";
    public static final String EXTRA_UPLOAD_RESULT = "RESULT";

    // for Camera uploads
    public static final String EXTRA_CAMERA_UPLOADS_SYNC_JOB_ID = "EXTRA_CAMERA_UPLOADS_SYNC_JOB_ID";
    public static final String EXTRA_CAMERA_UPLOADS_PICTURES_PATH = "EXTRA_CAMERA_UPLOADS_PICTURES_PATH";
    public static final String EXTRA_CAMERA_UPLOADS_VIDEOS_PATH = "EXTRA_CAMERA_UPLOADS_VIDEOS_PATH";
    public static final String EXTRA_CAMERA_UPLOADS_SOURCE_PATH = "EXTRA_CAMERA_UPLOADS_SOURCE_PATH";
    public static final String EXTRA_CAMERA_UPLOADS_BEHAVIOR_AFTER_UPLOAD =
            "EXTRA_CAMERA_UPLOADS_BEHAVIOR_AFTER_UPLOAD";

    // for Available offline
    public static final String EXTRA_AVAILABLE_OFFLINE_SYNC_JOB_ID = "EXTRA_AVAILABLE_OFFLINE_SYNC_JOB_ID";
}