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

import java.io.File;

import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.test_project.TestActivity;

/**
 * Class to test Download File Operation
 * @author masensio
 * @author David A. Velasco
 */

public class DownloadFileTest extends RemoteTest {

	
	private static final String LOG_TAG = DownloadFileTest.class.getCanonicalName();
	
	/* Files to download. These files must exist on the account */
	private static final String IMAGE_PATH = "/fileToDownload.png";
	private static final String IMAGE_PATH_WITH_SPECIAL_CHARS = "/@file@download.png";
	private static final String IMAGE_NOT_FOUND = "/fileNotFound.png";
	
	private String mFullPath2Image;
	private String mFullPath2ImageWitSpecialChars;
	private String mFullPath2ImageNotFound;
	private String mDownloadedFilePath;
	private TestActivity mActivity;

	
	@Override
	  protected void setUp() throws Exception {
	    super.setUp();
	    setActivityInitialTouchMode(false);
	    mActivity = getActivity();
	    mDownloadedFilePath = null;
    	mFullPath2Image = mBaseFolderPath + IMAGE_PATH;
    	mFullPath2ImageWitSpecialChars = mBaseFolderPath + IMAGE_PATH_WITH_SPECIAL_CHARS;
    	mFullPath2ImageNotFound = mBaseFolderPath + IMAGE_NOT_FOUND;
	    
		File imageFile = mActivity.extractAsset(TestActivity.ASSETS__IMAGE_FILE_NAME);

    	RemoteOperationResult result = mActivity.uploadFile(
				imageFile.getAbsolutePath(), 
				mFullPath2Image, 
				"image/png");
		if (!result.isSuccess()) {
			Utils.logAndThrow(LOG_TAG, result);
		}
		
		result = mActivity.uploadFile(
				imageFile.getAbsolutePath(), 
				mFullPath2ImageWitSpecialChars, 
				"image/png");
		if (!result.isSuccess()) {
			Utils.logAndThrow(LOG_TAG, result);
		}
		
		result = mActivity.removeFile(mFullPath2ImageNotFound);
		if (!result.isSuccess() && result.getCode() != ResultCode.FILE_NOT_FOUND) {
			Utils.logAndThrow(LOG_TAG, result);
		}
		
	}

	/**
	 * Test Download a File
	 */
	public void testDownloadFile() {
		RemoteOperationResult result = mActivity.downloadFile(
				new RemoteFile(mFullPath2Image), 
				mActivity.getFilesDir().getAbsolutePath()
				);
		mDownloadedFilePath = mFullPath2Image;
		assertTrue(result.isSuccess());
		// TODO some checks involving the local file
	}
	
	/**
	 * Test Download a File with special chars
	 */
	public void testDownloadFileSpecialChars() {
		RemoteOperationResult result = mActivity.downloadFile(
				new RemoteFile(mFullPath2ImageWitSpecialChars),
				mActivity.getFilesDir().getAbsolutePath()
				);
		mDownloadedFilePath = mFullPath2ImageWitSpecialChars;
		assertTrue(result.isSuccess());
		// TODO some checks involving the local file
	}
	
	/**
	 * Test Download a Not Found File 
	 */
	public void testDownloadFileNotFound() {
		RemoteOperationResult result = mActivity.downloadFile(
				new RemoteFile(mFullPath2ImageNotFound), 
				mActivity.getFilesDir().getAbsolutePath()
				);
		assertFalse(result.isSuccess());
	}
	
	
	@Override
	protected void tearDown() throws Exception {
		if (mDownloadedFilePath != null) {
			RemoteOperationResult removeResult = mActivity.removeFile(mDownloadedFilePath);
			if (!removeResult.isSuccess()  && removeResult.getCode() != ResultCode.TIMEOUT) {
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
