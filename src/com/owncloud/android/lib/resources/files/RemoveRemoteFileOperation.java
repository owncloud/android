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

package com.owncloud.android.lib.resources.files;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

/**
 * Remote operation performing the removal of a remote file or folder in the ownCloud server.
 * 
 * @author David A. Velasco
 * @author masensio
 */
public class RemoveRemoteFileOperation extends RemoteOperation {
    private static final String TAG = RemoveRemoteFileOperation.class.getSimpleName();

    private static final int REMOVE_READ_TIMEOUT = 30000;
    private static final int REMOVE_CONNECTION_TIMEOUT = 5000;

	private String mRemotePath;

    /**
     * Constructor
     * 
     * @param remotePath	RemotePath of the remote file or folder to remove from the server
     */
	public RemoveRemoteFileOperation(String remotePath) {
		mRemotePath = remotePath;
	}

	/**
	 * Performs the rename operation.
	 * 
	 * @param client	Client object to communicate with the remote ownCloud server.
	 */
	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {
		RemoteOperationResult result = null;
        DeleteMethod delete = null;
        
        try {
        	delete = new DeleteMethod(client.getWebdavUri() + WebdavUtils.encodePath(mRemotePath));
        	int status = client.executeMethod(delete, REMOVE_READ_TIMEOUT, REMOVE_CONNECTION_TIMEOUT);
        	
        	delete.getResponseBodyAsString();   // exhaust the response, although not interesting
        	result = new RemoteOperationResult((delete.succeeded() ||
                    status == HttpStatus.SC_NOT_FOUND), status, delete.getResponseHeaders());
        	Log_OC.i(TAG, "Remove " + mRemotePath + ": " + result.getLogMessage());

        } catch (Exception e) {
        	result = new RemoteOperationResult(e);
        	Log_OC.e(TAG, "Remove " + mRemotePath + ": " + result.getLogMessage(), e);

        } finally {
        	if (delete != null)
        		delete.releaseConnection();
        }
        
		return result;
	}

}
