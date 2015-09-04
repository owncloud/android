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

import java.io.IOException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.Status;
import org.apache.jackrabbit.webdav.client.methods.MoveMethod;

import android.util.Log;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;


/**
 * Remote operation moving a remote file or folder in the ownCloud server to a different folder
 * in the same account.
 * 
 * Allows renaming the moving file/folder at the same time.
 * 
 * @author David A. Velasco
 */
public class MoveRemoteFileOperation extends RemoteOperation {

	private static final String TAG = MoveRemoteFileOperation.class.getSimpleName();

	private static final int MOVE_READ_TIMEOUT = 600000;
	private static final int MOVE_CONNECTION_TIMEOUT = 5000;

    private String mSrcRemotePath;
    private String mTargetRemotePath;

	private boolean mOverwrite;
    
    
    /**
     * Constructor.
     * 
     * TODO Paths should finish in "/" in the case of folders. ?
     * 
     * @param srcRemotePath		Remote path of the file/folder to move.  
     * @param targetRemotePath	Remove path desired for the file/folder after moving it.
     */
	public MoveRemoteFileOperation(
			String srcRemotePath, String targetRemotePath, boolean overwrite
			) {
		
		mSrcRemotePath = srcRemotePath;
		mTargetRemotePath = targetRemotePath;
		mOverwrite = overwrite;
	}

	
	 /**
     * Performs the rename operation.
     * 
     * @param   client      Client object to communicate with the remote ownCloud server.
     */
	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {

		OwnCloudVersion version = client.getOwnCloudVersion();
		boolean versionWithForbiddenChars =
                (version != null && version.isVersionWithForbiddenCharacters());

    	/// check parameters
        if (!FileUtils.isValidPath(mTargetRemotePath, versionWithForbiddenChars)) {
        	return new RemoteOperationResult(ResultCode.INVALID_CHARACTER_IN_NAME);
        }
        
        if (mTargetRemotePath.equals(mSrcRemotePath)) {
        	// nothing to do!
            return new RemoteOperationResult(ResultCode.OK);
        }

        if (mTargetRemotePath.startsWith(mSrcRemotePath)) {
        	return new RemoteOperationResult(ResultCode.INVALID_MOVE_INTO_DESCENDANT);
        }
        

        /// perform remote operation
		//LocalMoveMethod move = null;
		MoveMethod move = null;
		RemoteOperationResult result = null;
        try {
            move = new MoveMethod(
            		client.getWebdavUri() + WebdavUtils.encodePath(mSrcRemotePath),
            		client.getWebdavUri() + WebdavUtils.encodePath(mTargetRemotePath),
            		mOverwrite
    		);
            int status = client.executeMethod(move, MOVE_READ_TIMEOUT, MOVE_CONNECTION_TIMEOUT);
            
            /// process response
        	if (status == HttpStatus.SC_MULTI_STATUS) {
        		result = processPartialError(move);
        		
        	} else if (status == HttpStatus.SC_PRECONDITION_FAILED && !mOverwrite) {
        		
        		result = new RemoteOperationResult(ResultCode.INVALID_OVERWRITE);
        		client.exhaustResponse(move.getResponseBodyAsStream());

        		
    		/// for other errors that could be explicitly handled, check first:
    		/// http://www.webdav.org/specs/rfc4918.html#rfc.section.9.9.4
        		
        	} else if (status == 400) {
				result = new RemoteOperationResult(move.succeeded(),
						move.getResponseBodyAsString(), status);
			} else {
					result = new RemoteOperationResult(
							isSuccess(status), 	// move.succeeded()? trustful?
							status,
							move.getResponseHeaders()
					);
					client.exhaustResponse(move.getResponseBodyAsStream());
			}
            
            Log.i(TAG, "Move " + mSrcRemotePath + " to " + mTargetRemotePath + ": " + 
        		result.getLogMessage());
            
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log.e(TAG, "Move " + mSrcRemotePath + " to " + mTargetRemotePath + ": " + 
        		result.getLogMessage(), e);
            
        } finally {
            if (move != null)
                move.releaseConnection();
        }
        	
        return result;
	}
	
	
	/**
	 *  Analyzes a multistatus response from the OC server to generate an appropriate result.
	 * 
	 *  In WebDAV, a MOVE request on collections (folders) can be PARTIALLY successful: some
	 *  children are moved, some other aren't.
	 *  
	 *  According to the WebDAV specification, a multistatus response SHOULD NOT include partial  
	 *  successes (201, 204) nor for descendants of already failed children (424) in the response 
	 *  entity. But SHOULD NOT != MUST NOT, so take carefully. 
	 * 
	 *  @param 	move	Move operation just finished with a multistatus response
	 *  @return	A result for the {@link MoveRemoteFileOperation} caller
	 *  
	 *  @throws IOException 	If the response body could not be parsed
	 *  @throws DavException 	If the status code is other than MultiStatus or if obtaining
	 *  						the response XML document fails
	 */
    private RemoteOperationResult processPartialError(MoveMethod move) 
    		throws IOException, DavException {
    	// Adding a list of failed descendants to the result could be interesting; or maybe not.
    	// For the moment, let's take the easy way.
    	
    	/// check that some error really occurred  
    	MultiStatusResponse[] responses = move.getResponseBodyAsMultiStatus().getResponses();
    	Status[] status = null;
    	boolean failFound = false;
    	for (int i = 0; i < responses.length && !failFound; i++ ) {
    		status = responses[i].getStatus();
    		failFound = (
    				status != null && 
    				status.length > 0 && 
    				status[0].getStatusCode() > 299
			);
    	}
    	
    	RemoteOperationResult result;
    	if (failFound) {
    		result = new RemoteOperationResult(ResultCode.PARTIAL_MOVE_DONE);
    	} else {
    		result = new RemoteOperationResult(
            		true,
            		HttpStatus.SC_MULTI_STATUS, 
            		move.getResponseHeaders()
    		);
    	}
		
    	return result; 
    			
	}


	protected boolean isSuccess(int status) {
        return status == HttpStatus.SC_CREATED || status == HttpStatus.SC_NO_CONTENT;
    }
        
}
