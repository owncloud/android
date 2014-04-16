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

import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.test_project.TestActivity;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

/**
 * Class to test Delete a File Operation
 * @author masensio
 *
 */

public class DeleteFileTest extends ActivityInstrumentationTestCase2<TestActivity> {

	private static final String LOG_TAG = DeleteFileTest.class.getCanonicalName();

	/* Folder data to delete. */
	private static final String FOLDER_PATH = "/folderToDelete";
	
	/* File to delete. */
	private static final String FILE_PATH = "/fileToDelete.txt";

	private static boolean mGlobalSetupDone = false;
	
	private TestActivity mActivity;
	
	public DeleteFileTest() {
	    super(TestActivity.class);
	}
	
	@Override
	  protected void setUp() throws Exception {
	    super.setUp();
	    setActivityInitialTouchMode(false);
	    mActivity = getActivity();
	    
	    if (!mGlobalSetupDone) {
	    	
			Log.v(LOG_TAG, "Starting global set up");
			RemoteOperationResult result = mActivity.createFolder(FOLDER_PATH, true);
			if (!result.isSuccess()) {
				Utils.logAndThrow(LOG_TAG, result);
			}
			
			File textFile = mActivity.extractAsset(TestActivity.ASSETS__TEXT_FILE_NAME);
			result = mActivity.uploadFile(
					textFile.getAbsolutePath(), 
					FILE_PATH, 
					"txt/plain");
			if (!result.isSuccess()) {
				Utils.logAndThrow(LOG_TAG, result);
			}
			
			Log.v(LOG_TAG, "Global set up done");
		    mGlobalSetupDone = true;
	    }
		
	}
	
	/**
	 * Test Remove Folder
	 */
	public void testRemoveFolder() {

		RemoteOperationResult result = mActivity.removeFile(FOLDER_PATH);
		assertTrue(result.isSuccess());
	}
	
	/**
	 * Test Remove File
	 */
	public void testRemoveFile() {
		
		RemoteOperationResult result = mActivity.removeFile(FILE_PATH);
		assertTrue(result.isSuccess());
	}

	
	@Override
	protected void tearDown() throws Exception {
		// nothing to do:
		//	- if tests were fine, there is nothing to clean up in the server side
		//	- if tests failed, there is nothing we can do to clean up the server side
		super.tearDown();
	}
	
}
