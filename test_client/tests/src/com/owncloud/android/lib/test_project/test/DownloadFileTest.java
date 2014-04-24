/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud Inc.
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

import java.io.File;

import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.test_project.TestActivity;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

/**
 * Class to test Download File Operation
 * @author masensio
 * @author David A. Velasco
 */

public class DownloadFileTest extends ActivityInstrumentationTestCase2<TestActivity> {

	
	private static final String LOG_TAG = DownloadFileTest.class.getCanonicalName();
	
	/* Files to download. These files must exist on the account */
	private static final String IMAGE_PATH = "/fileToDownload.png";
	private static final String IMAGE_PATH_WITH_SPECIAL_CHARS = "/@file@download.png";
	private static final String IMAGE_NOT_FOUND = "/fileNotFound.png";
	private static final String [] FILE_PATHS = { IMAGE_PATH, IMAGE_PATH_WITH_SPECIAL_CHARS }; 
	
	private static boolean mGlobalSetupDone = false;
	
	private String mDownloadedFilePath;
	private TestActivity mActivity;

	
	public DownloadFileTest() {
	    super(TestActivity.class);
	}
	
	@Override
	  protected void setUp() throws Exception {
	    super.setUp();
	    setActivityInitialTouchMode(false);
	    mActivity = getActivity();
	    mDownloadedFilePath = null;
	    
	    if (!mGlobalSetupDone) {
	    	
	    	RemoteOperationResult result = null;
			File imageFile = mActivity.extractAsset(TestActivity.ASSETS__IMAGE_FILE_NAME);

			for (int i=0; i<FILE_PATHS.length && (result == null || result.isSuccess()); i++) {
				result = mActivity.uploadFile(
						imageFile.getAbsolutePath(), 
						FILE_PATHS[i], 
						"image/png");
			}
			if (!result.isSuccess()) {
				Utils.logAndThrow(LOG_TAG, result);
			}
			
			result = mActivity.removeFile(IMAGE_NOT_FOUND);
			if (!result.isSuccess() && result.getCode() != ResultCode.FILE_NOT_FOUND) {
				Utils.logAndThrow(LOG_TAG, result);
			}
			
			Log.v(LOG_TAG, "Global set up done");
		    mGlobalSetupDone = true;
	    }
	    
	}

	/**
	 * Test Download a File
	 */
	public void testDownloadFile() {
		RemoteOperationResult result = mActivity.downloadFile(
				new RemoteFile(IMAGE_PATH), 
				mActivity.getFilesDir().getAbsolutePath()
				);
		mDownloadedFilePath = IMAGE_PATH;
		assertTrue(result.isSuccess());
		// TODO some checks involving the local file
	}
	
	/**
	 * Test Download a File with special chars
	 */
	public void testDownloadFileSpecialChars() {
		RemoteOperationResult result = mActivity.downloadFile(
				new RemoteFile(IMAGE_PATH_WITH_SPECIAL_CHARS),
				mActivity.getFilesDir().getAbsolutePath()
				);
		mDownloadedFilePath = IMAGE_PATH_WITH_SPECIAL_CHARS;
		assertTrue(result.isSuccess());
		// TODO some checks involving the local file
	}
	
	/**
	 * Test Download a Not Found File 
	 */
	public void testDownloadFileNotFound() {
		RemoteOperationResult result = mActivity.downloadFile(
				new RemoteFile(IMAGE_NOT_FOUND), 
				mActivity.getFilesDir().getAbsolutePath()
				);
		assertFalse(result.isSuccess());
	}
	
	
	@Override
	protected void tearDown() throws Exception {
		if (mDownloadedFilePath != null) {
			RemoteOperationResult removeResult = mActivity.removeFile(mDownloadedFilePath);
			if (!removeResult.isSuccess()) {
				Utils.logAndThrow(LOG_TAG, removeResult);
			}
		}
		File[] files = mActivity.getFilesDir().listFiles();
		for (File file : files) {
			file.delete();
		}
		super.tearDown();
	}
	
	
}
