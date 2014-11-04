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

import android.content.Context;
import android.net.Uri;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.network.NetworkUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.resources.files.CopyRemoteFileOperation;
import com.owncloud.android.lib.test_project.R;
import com.owncloud.android.lib.test_project.SelfSignedConfidentSslSocketFactory;
import com.owncloud.android.lib.test_project.TestActivity;

import junit.framework.AssertionFailedError;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

import java.io.File;
import java.security.GeneralSecurityException;

//import android.test.AndroidTestCase;

/**
 * Class to test CopyRemoteFileOperation
 * <p/>
 * With this TestCase we are experimenting a bit to improve the test suite design, in two aspects:
 * <p/>
 * - Reduce the dependency from the set of test cases on the "test project" needed to
 * have an instrumented APK to install in the device, as required by the testing framework
 * provided by Android. To get there, this class avoids calling TestActivity methods in the test
 * method.
 * <p/>
 * - Reduce the impact of creating a remote fixture over the Internet, while the structure of the
 * TestCase is kept easy to maintain. To get this, all the tests are done in a single test method,
 * granting this way that setUp and tearDown are run only once.
 *
 * @author David A. Velasco
 */

//public class CopyFileTest extends AndroidTestCase {
public class CopyFileTest extends ActivityInstrumentationTestCase2<TestActivity> {

    private static final String LOG_TAG = CopyFileTest.class.getCanonicalName();


    /// Paths to files and folders in fixture

    private static final String SRC_BASE_FOLDER = "/src/";
    private static final String TARGET_BASE_FOLDER = "/target/";
    private static final String NO_FILE = "nofile.txt";
    private static final String FILE1 = "file1.txt";
    private static final String FILE2 = "file2.txt";
    private static final String FILE3 = "file3.txt";
    private static final String FILE4 = "file4.txt";
    private static final String FILE5 = "file5.txt";
    private static final String FILE6 = "file6.txt";
    private static final String FILE7 = "file7.txt";
    private static final String EMPTY = "empty/";
    private static final String NO_FOLDER = "nofolder/";
    private static final String FOLDER1 = "folder1/";
    private static final String FOLDER2 = "folder2/";
    private static final String FOLDER3 = "folder3/";
    private static final String FOLDER4 = "folder4/";

    private static final String SRC_PATH_TO_FILE_1 = SRC_BASE_FOLDER + FILE1;
    private static final String TARGET_PATH_TO_FILE_1 = TARGET_BASE_FOLDER + FILE1;

    private static final String SRC_PATH_TO_FILE_2 = SRC_BASE_FOLDER + FILE2;
    private static final String TARGET_PATH_TO_FILE_2_RENAMED =
            TARGET_BASE_FOLDER + "renamed_" + FILE2;

    private static final String SRC_PATH_TO_FILE_3 = SRC_BASE_FOLDER + FILE3;
    private static final String SRC_PATH_TO_FILE_3_RENAMED = SRC_BASE_FOLDER + "renamed_" + FILE3;

    private static final String SRC_PATH_TO_FILE_4 = SRC_BASE_FOLDER + FILE4;

    private static final String SRC_PATH_TO_FILE_5 = SRC_BASE_FOLDER + FILE5;

    private static final String SRC_PATH_TO_FILE_6 = SRC_BASE_FOLDER + FILE6;

    private static final String SRC_PATH_TO_FILE_7 = SRC_BASE_FOLDER + FILE7;

    private static final String SRC_PATH_TO_NON_EXISTENT_FILE = SRC_BASE_FOLDER + NO_FILE;

    private static final String SRC_PATH_TO_EMPTY_FOLDER = SRC_BASE_FOLDER + EMPTY;
    private static final String TARGET_PATH_TO_EMPTY_FOLDER = TARGET_BASE_FOLDER + EMPTY;

    private static final String SRC_PATH_TO_FULL_FOLDER_1 = SRC_BASE_FOLDER + FOLDER1;
    private static final String TARGET_PATH_TO_FULL_FOLDER_1 = TARGET_BASE_FOLDER + FOLDER1;

    private static final String SRC_PATH_TO_FULL_FOLDER_2 = SRC_BASE_FOLDER + FOLDER2;

    private static final String TARGET_PATH_TO_FULL_FOLDER_2_RENAMED =
            TARGET_BASE_FOLDER + "renamed_" + FOLDER2;

    private static final String SRC_PATH_TO_FULL_FOLDER_3 = SRC_BASE_FOLDER + FOLDER3;
    private static final String SRC_PATH_TO_FULL_FOLDER_4 = SRC_BASE_FOLDER + FOLDER4;

    private static final String SRC_PATH_TO_FULL_FOLDER_3_RENAMED =
            SRC_BASE_FOLDER + "renamed_" + FOLDER3;

    private static final String TARGET_PATH_RENAMED_WITH_INVALID_CHARS =
            SRC_BASE_FOLDER + "renamed:??_" + FILE6;

    private static final String TARGET_PATH_TO_ALREADY_EXISTENT_EMPTY_FOLDER_4 = TARGET_BASE_FOLDER
            + FOLDER4;

    private static final String TARGET_PATH_TO_NON_EXISTENT_FILE = TARGET_BASE_FOLDER + NO_FILE;

    private static final String TARGET_PATH_TO_FILE_5_INTO_NON_EXISTENT_FOLDER =
            TARGET_BASE_FOLDER + NO_FOLDER + FILE5;

    private static final String TARGET_PATH_TO_ALREADY_EXISTENT_FILE_7 = TARGET_BASE_FOLDER + FILE7;

    private static final String[] FOLDERS_IN_FIXTURE = {
            SRC_PATH_TO_EMPTY_FOLDER,

            SRC_PATH_TO_FULL_FOLDER_1,
            SRC_PATH_TO_FULL_FOLDER_1 + FOLDER1,
            SRC_PATH_TO_FULL_FOLDER_1 + FOLDER2,
            SRC_PATH_TO_FULL_FOLDER_1 + FOLDER2 + FOLDER1,
            SRC_PATH_TO_FULL_FOLDER_1 + FOLDER2 + FOLDER2,

            SRC_PATH_TO_FULL_FOLDER_2,
            SRC_PATH_TO_FULL_FOLDER_2 + FOLDER1,
            SRC_PATH_TO_FULL_FOLDER_2 + FOLDER2,
            SRC_PATH_TO_FULL_FOLDER_2 + FOLDER2 + FOLDER1,
            SRC_PATH_TO_FULL_FOLDER_2 + FOLDER2 + FOLDER2,

            SRC_PATH_TO_FULL_FOLDER_3,
            SRC_PATH_TO_FULL_FOLDER_3 + FOLDER1,
            SRC_PATH_TO_FULL_FOLDER_3 + FOLDER2,
            SRC_PATH_TO_FULL_FOLDER_3 + FOLDER2 + FOLDER1,
            SRC_PATH_TO_FULL_FOLDER_3 + FOLDER2 + FOLDER2,

            SRC_PATH_TO_FULL_FOLDER_4,
            SRC_PATH_TO_FULL_FOLDER_4 + FOLDER1,
            SRC_PATH_TO_FULL_FOLDER_4 + FOLDER2,
            SRC_PATH_TO_FULL_FOLDER_4 + FOLDER2 + FOLDER1,
            SRC_PATH_TO_FULL_FOLDER_4 + FOLDER2 + FOLDER2,

            TARGET_BASE_FOLDER,
            TARGET_PATH_TO_ALREADY_EXISTENT_EMPTY_FOLDER_4
    };

    private static final String[] FILES_IN_FIXTURE = {
            SRC_PATH_TO_FILE_1,
            SRC_PATH_TO_FILE_2,
            SRC_PATH_TO_FILE_3,
            SRC_PATH_TO_FILE_4,
            SRC_PATH_TO_FILE_5,

            SRC_PATH_TO_FULL_FOLDER_1 + FILE1,
            SRC_PATH_TO_FULL_FOLDER_1 + FOLDER2 + FILE1,
            SRC_PATH_TO_FULL_FOLDER_1 + FOLDER2 + FILE2,
            SRC_PATH_TO_FULL_FOLDER_1 + FOLDER2 + FOLDER2 + FILE2,

            SRC_PATH_TO_FULL_FOLDER_2 + FILE1,
            SRC_PATH_TO_FULL_FOLDER_2 + FOLDER2 + FILE1,
            SRC_PATH_TO_FULL_FOLDER_2 + FOLDER2 + FILE2,
            SRC_PATH_TO_FULL_FOLDER_2 + FOLDER2 + FOLDER2 + FILE2,

            SRC_PATH_TO_FULL_FOLDER_3 + FILE1,
            SRC_PATH_TO_FULL_FOLDER_3 + FOLDER2 + FILE1,
            SRC_PATH_TO_FULL_FOLDER_3 + FOLDER2 + FILE2,
            SRC_PATH_TO_FULL_FOLDER_3 + FOLDER2 + FOLDER2 + FILE2,

            SRC_PATH_TO_FULL_FOLDER_4 + FILE1,
            SRC_PATH_TO_FULL_FOLDER_4 + FOLDER2 + FILE1,
            SRC_PATH_TO_FULL_FOLDER_4 + FOLDER2 + FILE2,
            SRC_PATH_TO_FULL_FOLDER_4 + FOLDER2 + FOLDER2 + FILE2,

            TARGET_PATH_TO_ALREADY_EXISTENT_FILE_7
    };


    String mServerUri, mUser, mPass;
    OwnCloudClient mClient = null;

    public CopyFileTest() {
        super(TestActivity.class);

        Protocol pr = Protocol.getProtocol("https");
        if (pr == null || !(pr.getSocketFactory() instanceof SelfSignedConfidentSslSocketFactory)) {
            try {
                ProtocolSocketFactory psf = new SelfSignedConfidentSslSocketFactory();
                Protocol.registerProtocol(
                        "https",
                        new Protocol("https", psf, 443));

            } catch (GeneralSecurityException e) {
                throw new AssertionFailedError(
                        "Self-signed confident SSL context could not be loaded");
            }
        }

    }


    protected Context getContext() {
        return getActivity();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Next initialization cannot be done in the constructor because getContext() is not
        // ready yet, returns NULL.
        initAccessToServer(getContext());

        Log.v(LOG_TAG, "Setting up the remote fixture...");

        RemoteOperationResult result = null;
        for (String folderPath : FOLDERS_IN_FIXTURE) {
            result = TestActivity.createFolder(folderPath, true, mClient);
            if (!result.isSuccess()) {
                Utils.logAndThrow(LOG_TAG, result);
            }
        }

        File txtFile = TestActivity.extractAsset(
                TestActivity.ASSETS__TEXT_FILE_NAME, getContext()
        );
        for (String filePath : FILES_IN_FIXTURE) {
            result = TestActivity.uploadFile(
                    txtFile.getAbsolutePath(), filePath, "txt/plain", mClient
            );
            if (!result.isSuccess()) {
                Utils.logAndThrow(LOG_TAG, result);
            }
        }

        Log.v(LOG_TAG, "Remote fixture created.");

    }


    /**
     * Test copy folder
     */
    public void testCopyRemoteFileOperation() {
        Log.v(LOG_TAG, "testCopyFolder in");

        /// successful cases

        // copy file
        CopyRemoteFileOperation copyOperation = new CopyRemoteFileOperation(
                getContext(),
                SRC_PATH_TO_FILE_1,
                TARGET_PATH_TO_FILE_1,
                false
        );
        RemoteOperationResult result = copyOperation.execute(mClient);
        assertTrue(result.isSuccess());

        // copy & rename file, different location
        copyOperation = new CopyRemoteFileOperation(
                getContext(),
                SRC_PATH_TO_FILE_2,
                TARGET_PATH_TO_FILE_2_RENAMED,
                false
        );
        result = copyOperation.execute(mClient);
        assertTrue(result.isSuccess());

        // copy & rename file, same location (rename file)
        copyOperation = new CopyRemoteFileOperation(
                getContext(),
                SRC_PATH_TO_FILE_3,
                SRC_PATH_TO_FILE_3_RENAMED,
                false
        );
        result = copyOperation.execute(mClient);
        assertTrue(result.isSuccess());

        // copy empty folder
        copyOperation = new CopyRemoteFileOperation(
                getContext(),
                SRC_PATH_TO_EMPTY_FOLDER,
                TARGET_PATH_TO_EMPTY_FOLDER,
                false
        );
        result = copyOperation.execute(mClient);
        assertTrue(result.isSuccess());

        // copy non-empty folder
        copyOperation = new CopyRemoteFileOperation(
                getContext(),
                SRC_PATH_TO_FULL_FOLDER_1,
                TARGET_PATH_TO_FULL_FOLDER_1,
                false
        );
        result = copyOperation.execute(mClient);
        assertTrue(result.isSuccess());

        // copy & rename folder, different location
        copyOperation = new CopyRemoteFileOperation(
                getContext(),
                SRC_PATH_TO_FULL_FOLDER_2,
                TARGET_PATH_TO_FULL_FOLDER_2_RENAMED,
                false
        );
        result = copyOperation.execute(mClient);
        assertTrue(result.isSuccess());

        // copy & rename folder, same location (rename folder)
        copyOperation = new CopyRemoteFileOperation(
                getContext(),
                SRC_PATH_TO_FULL_FOLDER_3,
                SRC_PATH_TO_FULL_FOLDER_3_RENAMED,
                false
        );
        result = copyOperation.execute(mClient);
        assertTrue(result.isSuccess());

        // copy for nothing (success, but no interaction with network)
        copyOperation = new CopyRemoteFileOperation(
                getContext(),
                SRC_PATH_TO_FILE_4,
                SRC_PATH_TO_FILE_4,
                false
        );
        result = copyOperation.execute(mClient);
        assertTrue(result.isSuccess());

        // copy overwriting
        copyOperation = new CopyRemoteFileOperation(
                getContext(),
                SRC_PATH_TO_FULL_FOLDER_4,
                TARGET_PATH_TO_ALREADY_EXISTENT_EMPTY_FOLDER_4,
                true
        );
        result = copyOperation.execute(mClient);
        assertTrue(result.isSuccess());


        /// Failed cases

        // file to copy does not exist
        copyOperation = new CopyRemoteFileOperation(
                getContext(),
                SRC_PATH_TO_NON_EXISTENT_FILE,
                TARGET_PATH_TO_NON_EXISTENT_FILE,
                false
        );
        result = copyOperation.execute(mClient);
        assertTrue(result.getCode() == ResultCode.FILE_NOT_FOUND);

        // folder to copy into does no exist
        copyOperation = new CopyRemoteFileOperation(
                getContext(),
                SRC_PATH_TO_FILE_5,
                TARGET_PATH_TO_FILE_5_INTO_NON_EXISTENT_FOLDER,
                false
        );
        result = copyOperation.execute(mClient);
        assertTrue(result.getHttpCode() == HttpStatus.SC_CONFLICT);

        // target location (renaming) has invalid characters
        copyOperation = new CopyRemoteFileOperation(
                getContext(),
                SRC_PATH_TO_FILE_6,
                TARGET_PATH_RENAMED_WITH_INVALID_CHARS,
                false
        );
        result = copyOperation.execute(mClient);
        assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);

        // name collision
        copyOperation = new CopyRemoteFileOperation(
                getContext(),
                SRC_PATH_TO_FILE_1,
                TARGET_PATH_TO_ALREADY_EXISTENT_FILE_7,
                false
        );
        result = copyOperation.execute(mClient);
        assertTrue(result.getCode() == ResultCode.INVALID_OVERWRITE);

        // copy a folder into a descendant
        copyOperation = new CopyRemoteFileOperation(
                getContext(),
                SRC_BASE_FOLDER,
                SRC_PATH_TO_EMPTY_FOLDER,
                false
        );
        result = copyOperation.execute(mClient);
        assertTrue(result.getCode() == ResultCode.INVALID_COPY_INTO_DESCENDANT);
    }

    @Override
    protected void tearDown() throws Exception {
        Log.v(LOG_TAG, "Deleting remote fixture...");

        String[] mPathsToCleanUp = {
                SRC_BASE_FOLDER,
                TARGET_BASE_FOLDER
        };

        for (String path : mPathsToCleanUp) {
            RemoteOperationResult removeResult =
                    TestActivity.removeFile(path, mClient);
            if (!removeResult.isSuccess() && removeResult.getCode() != ResultCode.TIMEOUT) {
                Utils.logAndThrow(LOG_TAG, removeResult);
            }
        }

        super.tearDown();

        Log.v(LOG_TAG, "Remote fixture delete.");
    }


    private void initAccessToServer(Context context) {
        Log.v(LOG_TAG, "Setting up client instance to access OC server...");

        mServerUri = context.getString(R.string.server_base_url);
        mUser = context.getString(R.string.username);
        mPass = context.getString(R.string.password);

        mClient = new OwnCloudClient(
                Uri.parse(mServerUri),
                NetworkUtils.getMultiThreadedConnManager()
        );
        mClient.setDefaultTimeouts(
                OwnCloudClientFactory.DEFAULT_DATA_TIMEOUT,
                OwnCloudClientFactory.DEFAULT_CONNECTION_TIMEOUT);
        mClient.setFollowRedirects(true);
        mClient.setCredentials(
                OwnCloudCredentialsFactory.newBasicCredentials(
                        mUser,
                        mPass
                )
        );

        Log.v(LOG_TAG, "Client instance set up.");

    }


}
