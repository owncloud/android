/* ownCloud Android Library is available under MIT license
 *
 *   Copyright (C) 2016 ownCloud GmbH.
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
import com.owncloud.android.lib.resources.users.GetRemoteUserAvatarOperation.ResultData;
import com.owncloud.android.lib.test_project.TestActivity;

import org.apache.commons.httpclient.HttpStatus;

/**
 * Class to test {@link GetRemoteUserAvatarOperation}
 *
 * @author David A. Velasco
 */
public class GetUserAvatarTest extends RemoteTest {


    private static final String LOG_TAG = GetUserAvatarTest.class.getCanonicalName();

    private static final int AVATAR_DIMENSION = 256;

    private TestActivity mActivity;


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    /**
     * Test get user avatar (succesful)
     */
    public void testGetUserAvatar() {
        RemoteOperationResult result = mActivity.getUserAvatar(AVATAR_DIMENSION, null);
        assertTrue(result.isSuccess());
        ResultData userAvatar = (ResultData) result.getData().get(0);
        assertTrue(userAvatar.getEtag() != null && userAvatar.getEtag().length() > 0);
        assertTrue(userAvatar.getMimeType() != null && userAvatar.getMimeType().startsWith("image"));
        assertTrue(userAvatar.getAvatarData() != null && userAvatar.getAvatarData().length > 0);
    }

    /**
     * Test get user avatar only if changed, but wasn't changed
     *
     * DISABLED: conditioned call has been disabled due to problems with the network stack;
     * see comment in src/com/owncloud/android/lib/resources/users/GetRemoteUserAvatarOperation.java#87
     */
    /*
    public void testGetUserAvatarOnlyIfChangedAfterUnchanged() {
        RemoteOperationResult result = mActivity.getUserAvatar(AVATAR_DIMENSION, null);
        ResultData userAvatar = (ResultData) result.getData().get(0);
        String etag = userAvatar.getEtag();

        // request again, with the just received etag
        result = mActivity.getUserAvatar(AVATAR_DIMENSION, etag);
        assertFalse(result.isSuccess());
        assertTrue(result.getHttpCode() == HttpStatus.SC_NOT_MODIFIED);
    }
    */

    /**
     * Test get user avatar only if changed, and was changed
     */
    public void testGetUserAvatarOnlyIfChangedAfterChanged() {
        // TODO can't test this without provisioning API or mocking the server
    }

    /**
     * Test get user avatar not found
     */
    public void testGetUserAvatarNofFound() {
        // TODO can't test this without provisioning API, mocking the server or another set of credentials
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

}