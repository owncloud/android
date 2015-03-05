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

import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.test_project.TestActivity;

import android.test.ActivityInstrumentationTestCase2;

/**
 * Class to test Create Folder Operation
 * @author David A. Velasco
 *
 */
public class RemoteTest extends ActivityInstrumentationTestCase2<TestActivity> {

	private static final String LOG_TAG = RemoteTest.class.getSimpleName();
	
	protected String mBaseFolderPath = "/test_for_build_";
	
	public RemoteTest() {
	    super(TestActivity.class);
	}
	
	@Override
	  protected void setUp() throws Exception {
	    super.setUp();
	    setActivityInitialTouchMode(false);
	    mBaseFolderPath += Utils.getBuildNumber(getActivity());
	    
		RemoteOperationResult result = getActivity().createFolder(mBaseFolderPath, true);
		if (!result.isSuccess()  && result.getCode() != ResultCode.TIMEOUT) {
			Utils.logAndThrow(LOG_TAG, result);
		}
	}
	
	
	@Override
	protected void tearDown() throws Exception {
		RemoteOperationResult removeResult = getActivity().removeFile(mBaseFolderPath);
		if (!removeResult.isSuccess() && removeResult.getCode() != ResultCode.TIMEOUT) {
			Utils.logAndThrow(LOG_TAG, removeResult);
		}
		super.tearDown();
	}
	
}
