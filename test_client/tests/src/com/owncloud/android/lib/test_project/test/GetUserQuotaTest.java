/* ownCloud Android Library is available under MIT license
 *
 *   Copyright (C) 2015 ownCloud Inc.
 *   Copyright (C) 2015 Bartosz Przybylski
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

import java.util.*;

import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.users.GetRemoteUserQuotaOperation.Quota;
import com.owncloud.android.lib.test_project.TestActivity;


/**
 * Class to test Get User Quota
 *
 * @author Bartosz Przybylski
 * @author David A. Velasco
 */
public class GetUserQuotaTest extends RemoteTest {


    private static final String LOG_TAG = GetUserQuotaTest.class.getCanonicalName();

    private TestActivity mActivity;


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(false);
        mActivity = getActivity();
    }

    /**
     * Test GetUserQuota
     */
    public void testGetUserQuota() {
        RemoteOperationResult result = mActivity.getQuota();
        assertTrue(result.isSuccess());
        Quota quota = (Quota)((ArrayList<Object>)result.getData()).get(0);
        assertTrue(quota.getFree() >= 0);
        assertTrue(quota.getUsed() >= 0);
        assertTrue(quota.getTotal() > 0);
    }

}
