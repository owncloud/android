/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud (http://www.owncloud.org/)
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

import com.owncloud.android.lib.operations.common.OCShare;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;
import com.owncloud.android.lib.test_project.TestActivity;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

public class RemoveShareTest extends ActivityInstrumentationTestCase2<TestActivity> {
	
	private static final String TAG = RemoveShareTest.class.getSimpleName();
	
	private TestActivity mActivity;

	public RemoveShareTest() {
		super(TestActivity.class);
		
	}

	@Override
	  protected void setUp() throws Exception {
	    super.setUp();
	    setActivityInitialTouchMode(false);
	    mActivity = getActivity();
	}
	
	/**
	 * Test Remove Share: the server must support SHARE API
	 */
	public void testRemoveShare() {
		// Get the shares
		RemoteOperationResult result = mActivity.getShares();
		if (result.isSuccess()) {
			int size = result.getData().size();

			if (size > 0) {
				OCShare share = ((OCShare) result.getData().get(size -1));
				long id = share.getIdRemoteShared();
				Log.d(TAG, "File to unshare: " + share.getPath() );
				result = mActivity.removeShare((int) id);	// Unshare
				assertTrue(result.isSuccess());
			} else {
				assertTrue(true);
			}
		} else {
			assertTrue(true);
		}
	}
}
