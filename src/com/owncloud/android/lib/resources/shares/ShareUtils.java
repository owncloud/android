/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2015 ownCloud Inc.
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.resources.shares;

import com.owncloud.android.lib.resources.status.OwnCloudVersion;

/**
 * Contains Constants for Share Operation
 * 
 * @author masensio
 *
 */

public class ShareUtils {

	// OCS Route
	public static final String SHARING_API_PATH ="/ocs/v1.php/apps/files_sharing/api/v1/shares"; 

    // String to build the link with the token of a share:
    public static final String SHARING_LINK_PATH_BEFORE_VERSION_8 = "/public.php?service=files&t=";
    public static final String SHARING_LINK_PATH_AFTER_VERSION_8 = "/index.php/s/";

    public static String getSharingLinkPath(OwnCloudVersion version){
        if (version!= null && version.isAfter8Version()){
            return SHARING_LINK_PATH_AFTER_VERSION_8;
        } else {
            return SHARING_LINK_PATH_BEFORE_VERSION_8;
        }
    }

}
