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

package com.owncloud.android.lib.resources.shares;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.http.HttpStatus;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;

/**
 * Creates a new share.  This allows sharing with a user or group or as a link.
 * 
 * @author masensio
 *
 */
public class CreateRemoteShareOperation extends RemoteOperation {

	private static final String TAG = CreateRemoteShareOperation.class.getSimpleName();

	private static final String PARAM_PATH = "path";
	private static final String PARAM_SHARE_TYPE = "shareType";
	private static final String PARAM_SHARE_WITH = "shareWith";
	private static final String PARAM_PUBLIC_UPLOAD = "publicUpload";
	private static final String PARAM_PASSWORD = "password";
	private static final String PARAM_PERMISSIONS = "permissions";

	private ArrayList<OCShare> mShares;  // List of shares for result, one share in this case
	
	private String mRemoteFilePath;
	private ShareType mShareType;
	private String mShareWith;
	private boolean mPublicUpload;
	private String mPassword;
	private int mPermissions;

	/**
	 * Constructor
	 * @param remoteFilePath	Full path of the file/folder being shared. Mandatory argument
	 * @param shareType			0 = user, 1 = group, 3 = Public link. Mandatory argument
	 * @param shareWith			User/group ID with who the file should be shared.  This is mandatory for shareType of 0 or 1
	 * @param publicUpload		If false (default) public cannot upload to a public shared folder.
	 * 							If true public can upload to a shared folder. Only available for public link shares
	 * @param password			Password to protect a public link share. Only available for public link shares
	 * @param permissions		1 - Read only Default for public shares
	 * 							2 - Update
	 * 							4 - Create
	 * 							8 - Delete
	 * 							16- Re-share
	 * 							31- All above Default for private shares
	 * 							For user or group shares.
	 * 							To obtain combinations, add the desired values together.  
	 * 							For instance, for Re-Share, delete, read, update, add 16+8+2+1 = 27.
	 */
	public CreateRemoteShareOperation(String remoteFilePath, ShareType shareType, String shareWith, boolean publicUpload, 
			String password, int permissions) {

		mRemoteFilePath = remoteFilePath;
		mShareType = shareType;
		mShareWith = shareWith;
		mPublicUpload = publicUpload;
		mPassword = password;
		mPermissions = permissions;
	}

	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {
		RemoteOperationResult result = null;
		int status = -1;

		PostMethod post = null;

		try {
			// Post Method
			post = new PostMethod(client.getBaseUri() + ShareUtils.SHARING_API_PATH);
			//Log_OC.d(TAG, "URL ------> " + client.getBaseUri() + ShareUtils.SHARING_API_PATH);

			post.setRequestHeader( "Content-Type", "application/x-www-form-urlencoded; charset=utf-8"); // necessary for special characters
			post.addParameter(PARAM_PATH, mRemoteFilePath);
			post.addParameter(PARAM_SHARE_TYPE, Integer.toString(mShareType.getValue()));
			post.addParameter(PARAM_SHARE_WITH, mShareWith);
			post.addParameter(PARAM_PUBLIC_UPLOAD, Boolean.toString(mPublicUpload));
			if (mPassword != null && mPassword.length() > 0) {
				post.addParameter(PARAM_PASSWORD, mPassword);
			}
			post.addParameter(PARAM_PERMISSIONS, Integer.toString(mPermissions));

			post.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);
            
			status = client.executeMethod(post);

			if(isSuccess(status)) {
				String response = post.getResponseBodyAsString();

				result = new RemoteOperationResult(ResultCode.OK);
				
				// Parse xml response --> obtain the response in ShareFiles ArrayList
				// convert String into InputStream
				InputStream is = new ByteArrayInputStream(response.getBytes());
				ShareXMLParser xmlParser = new ShareXMLParser();
				mShares = xmlParser.parseXMLResponse(is);
				if (xmlParser.isSuccess()) {
					if (mShares != null) {
						Log_OC.d(TAG, "Created " + mShares.size() + " share(s)");
						result = new RemoteOperationResult(ResultCode.OK);
						ArrayList<Object> sharesObjects = new ArrayList<Object>();
						for (OCShare share: mShares) {
							sharesObjects.add(share);
						}
						result.setData(sharesObjects);
					}
				} else if (xmlParser.isFileNotFound()){
					result = new RemoteOperationResult(ResultCode.SHARE_NOT_FOUND);
					
				} else if (xmlParser.isFailure()) {
					result = new RemoteOperationResult(ResultCode.SHARE_FORBIDDEN);

				} else {
					result = new RemoteOperationResult(false, status, post.getResponseHeaders());	
				}

			} else {
				result = new RemoteOperationResult(false, status, post.getResponseHeaders());
			}
			
		} catch (Exception e) {
			result = new RemoteOperationResult(e);
			Log_OC.e(TAG, "Exception while Creating New Share", e);
			
		} finally {
			if (post != null) {
				post.releaseConnection();
			}
		}
		return result;
	}

	private boolean isSuccess(int status) {
		return (status == HttpStatus.SC_OK);
	}

}
