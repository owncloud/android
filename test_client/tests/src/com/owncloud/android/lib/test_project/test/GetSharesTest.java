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
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.test_project.TestActivity;

/** 
 * Class to test Get Shares Operation
 * 
 * @author masensio
 *
 */

public class GetSharesTest extends RemoteTest {

	private static final String LOG_TAG = GetSharesTest.class.getCanonicalName();

	private static final String SHARED_FILE = "/sharedFileToGet.txt";
	
	private TestActivity mActivity;
	private String mFullPath2SharedFile;
	
	@Override
	  protected void setUp() throws Exception {
	    super.setUp();
	    setActivityInitialTouchMode(false);
	    mActivity = getActivity();
	    mFullPath2SharedFile = mBaseFolderPath + SHARED_FILE; 
	    		
		File textFile = mActivity.extractAsset(TestActivity.ASSETS__TEXT_FILE_NAME);
		RemoteOperationResult result = mActivity.uploadFile(
				textFile.getAbsolutePath(), 
				mFullPath2SharedFile, 
				"txt/plain");
		if (!result.isSuccess()) {
			Utils.logAndThrow(LOG_TAG, result);
		}
		
		result = mActivity.createShare(mFullPath2SharedFile, ShareType.PUBLIC_LINK, "", false, "", 1);
		if (!result.isSuccess()  && result.getCode() != ResultCode.TIMEOUT) {
			Utils.logAndThrow(LOG_TAG, result);
		}
		
	}
	
	/**
	 * Test Get Shares: the server must support SHARE API
	 */
	public void testGetShares() {
		RemoteOperationResult result = mActivity.getShares();
		assertTrue(result.isSuccess());
		assertTrue(result.getData() != null && result.getData().size() == 1);
	}
	

	@Override
	protected void tearDown() throws Exception {
		RemoteOperationResult removeResult = mActivity.removeFile(mFullPath2SharedFile);
		if (!removeResult.isSuccess() && removeResult.getCode() != ResultCode.TIMEOUT) {
			Utils.logAndThrow(LOG_TAG, removeResult);
		}
		super.tearDown();
	}
	
	
}
