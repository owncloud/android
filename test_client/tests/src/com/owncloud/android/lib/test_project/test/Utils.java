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

package com.owncloud.android.lib.test_project.test;

import android.content.Context;
import android.util.Log;

import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.test_project.R;

public class Utils {

	private static String LOG_TAG = Utils.class.getSimpleName();
	
	private static String sBuildNumber = null;
	
	public static void logAndThrow(String tag, RemoteOperationResult result) throws Exception {
		Log.e(tag, result.getLogMessage(), result.getException());
		throw new Exception(result.getLogMessage(), result.getException());
	}
	
	public static String getBuildNumber(Context context) {
		if (sBuildNumber == null) {
			sBuildNumber = context.getString(R.string.build_number);
			if (sBuildNumber == null || sBuildNumber.length() == 0) {
				Log.w(LOG_TAG, "Build number unknown, using current time instead");
				sBuildNumber = Long.toString(System.currentTimeMillis());
			}
		}
		return sBuildNumber;
	}
	
}
