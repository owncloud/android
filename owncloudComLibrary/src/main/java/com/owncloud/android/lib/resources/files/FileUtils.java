/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.lib.resources.files;

import timber.log.Timber;

import java.io.File;

public class FileUtils {
    public static final String FINAL_CHUNKS_FILE = ".file";
    public static final String MIME_DIR = "DIR";
    public static final String MIME_DIR_UNIX = "httpd/unix-directory";
    public static final String MODE_READ_ONLY = "r";

    static String getParentPath(String remotePath) {
        String parentPath = new File(remotePath).getParent();
        parentPath = parentPath.endsWith(File.separator) ? parentPath : parentPath + File.separator;
        return parentPath;
    }

    /**
     * Validate the fileName to detect if contains any forbidden character: / , \ , < , > ,
     * : , " , | , ? , *
     *
     */
    public static boolean isValidName(String fileName) {
        boolean result = true;

        Timber.d("fileName =======%s", fileName);
        if (fileName.contains(File.separator)) {
            result = false;
        }
        return result;
    }
}
