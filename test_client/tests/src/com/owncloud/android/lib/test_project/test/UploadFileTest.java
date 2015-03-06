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

import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.test_project.TestActivity;

/**
 * Class to test Update File Operation
 * @author masensio
 * @author David A. Velasco
 *
 */

public class UploadFileTest extends RemoteTest {

	private static final String LOG_TAG = UploadFileTest.class.getCanonicalName();
	
	private static final String UPLOAD_PATH = "/uploadedImage.png"; 
	
	private static final String CHUNKED_UPLOAD_PATH = "/uploadedVideo.MP4"; 
	
	private static final String FILE_NOT_FOUND_PATH = "/notFoundShouldNotBeHere.png"; 


	private TestActivity mActivity;
	private File mFileToUpload, mFileToUploadWithChunks;
	private String mUploadedFilePath;
	
	
	@Override
	  protected void setUp() throws Exception {
	    super.setUp();
	    setActivityInitialTouchMode(false);
	    mActivity = getActivity();
	    mUploadedFilePath = null;
	    
		mFileToUpload = mActivity.extractAsset(TestActivity.ASSETS__IMAGE_FILE_NAME);
		mFileToUploadWithChunks = mActivity.extractAsset(TestActivity.ASSETS__VIDEO_FILE_NAME);
	}

	
	/**
	 * Test Upload File without chunks
	 */
	public void testUploadFile() {
		
		String fullPath2Upload = mBaseFolderPath + UPLOAD_PATH;
		RemoteOperationResult result = mActivity.uploadFile(
				mFileToUpload.getAbsolutePath(), 
				fullPath2Upload, 
				"image/png"
				);
	    mUploadedFilePath = fullPath2Upload;
		assertTrue(result.isSuccess());
	}
	
	/**
	 * Test Upload File with chunks
	 */
	public void testUploadFileWithChunks() {
		
		String fullPath2Upload = mBaseFolderPath + CHUNKED_UPLOAD_PATH;
		RemoteOperationResult result = mActivity.uploadFile(
				mFileToUploadWithChunks.getAbsolutePath(),
				fullPath2Upload, 
				"video/mp4"
				);
	    mUploadedFilePath = fullPath2Upload;
		assertTrue(result.isSuccess());
	}
	
	/**
	 * Test Upload Not Found File
	 */
	public void testUploadFileNotFound() {

		String fullPath2Upload = mBaseFolderPath + FILE_NOT_FOUND_PATH;
		RemoteOperationResult result = mActivity.uploadFile(
				FILE_NOT_FOUND_PATH, 
				fullPath2Upload, 
				"image/png"
				);
		mUploadedFilePath = fullPath2Upload;
		assertFalse(result.isSuccess());
	}
	

	@Override
	protected void tearDown() throws Exception {
		if (mUploadedFilePath != null) {
			RemoteOperationResult removeResult = mActivity.removeFile(mUploadedFilePath);
			if (!removeResult.isSuccess() && removeResult.getCode() != ResultCode.TIMEOUT) {
				Utils.logAndThrow(LOG_TAG, removeResult);
			}
		}
		super.tearDown();
	}
	
}
