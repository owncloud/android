/**
 *  ownCloud Android client application
 *
 *  @author David A. Velasco
 *
 *  Copyright (C) 2017 ownCloud GmbH.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2,
 *  as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.utils;

public class Extras {

    // from both
    public static final String EXTRA_REMOTE_PATH = "REMOTE_PATH";
    public static final String EXTRA_ACCOUNT_NAME = "EXTRA_ACCOUNT_NAME";
    public static final String EXTRA_LINKED_TO_PATH = "LINKED_TO";

    // from FileDownloader
    public static final String EXTRA_FILE_PATH = "FILE_PATH";
    public static final String EXTRA_DOWNLOAD_RESULT = "RESULT";

    // from FileUploader
    public static final String EXTRA_OLD_REMOTE_PATH = "OLD_REMOTE_PATH";
    public static final String EXTRA_OLD_FILE_PATH = "OLD_FILE_PATH";
    public static final String EXTRA_UPLOAD_RESULT = "RESULT";

    // for Insta uploads
    public static final String EXTRA_LOCAL_CAMERA_PATH = "LOCAL_CAMERA_PATH";
    public static final String EXTRA_UPLOAD_PICTURES_PATH = "UPLOAD_IMAGES_PATH";
    public static final String EXTRA_UPLOAD_VIDEOS_PATH = "UPLOAD_VIDEOS_PATH";
    public static final String EXTRA_BEHAVIOR_AFTER_UPLOAD = "BEHAVIOR_AFTER_UPLOAD";
}
