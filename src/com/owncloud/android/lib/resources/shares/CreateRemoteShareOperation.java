/* ownCloud Android Library is available under MIT license
 *   @author masensio
 *   @author David A. Velasco
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

package com.owncloud.android.lib.resources.shares;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.http.HttpStatus;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

/**
 * Creates a new share.  This allows sharing with a user or group or as a link.
 */
public class CreateRemoteShareOperation extends RemoteOperation {

	private static final String TAG = CreateRemoteShareOperation.class.getSimpleName();

	private static final String PARAM_PATH = "path";
	private static final String PARAM_SHARE_TYPE = "shareType";
	private static final String PARAM_SHARE_WITH = "shareWith";
	private static final String PARAM_PUBLIC_UPLOAD = "publicUpload";
	private static final String PARAM_PASSWORD = "password";
	private static final String PARAM_PERMISSIONS = "permissions";

	private String mRemoteFilePath;
	private ShareType mShareType;
	private String mShareWith;
	private boolean mPublicUpload;
	private String mPassword;
	private int mPermissions;
	private boolean mGetShareDetails;

	/**
	 * Constructor
	 * @param remoteFilePath	Full path of the file/folder being shared. Mandatory argument
	 * @param shareType			0 = user, 1 = group, 3 = Public link. Mandatory argument
	 * @param shareWith			User/group ID with who the file should be shared.  This is mandatory for shareType
	 *                          of 0 or 1
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
	public CreateRemoteShareOperation(
			String remoteFilePath,
			ShareType shareType,
			String shareWith,
			boolean publicUpload,
			String password,
			int permissions
	) {

		mRemoteFilePath = remoteFilePath;
		mShareType = shareType;
		mShareWith = shareWith;
		mPublicUpload = publicUpload;
		mPassword = password;
		mPermissions = permissions;
		mGetShareDetails = false; 		// defaults to false for backwards compatibility
	}

	public boolean isGettingShareDetails () {
		return mGetShareDetails;
	}

	public void setGetShareDetails(boolean set) {
		mGetShareDetails = set;
	}

	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {
		RemoteOperationResult result = null;
		int status = -1;

		PostMethod post = null;

		try {
			// Post Method
			post = new PostMethod(client.getBaseUri() + ShareUtils.SHARING_API_PATH);

			post.setRequestHeader( "Content-Type",
                    "application/x-www-form-urlencoded; charset=utf-8"); // necessary for special characters

			post.addParameter(PARAM_PATH, mRemoteFilePath);
			post.addParameter(PARAM_SHARE_TYPE, Integer.toString(mShareType.getValue()));
			post.addParameter(PARAM_SHARE_WITH, mShareWith);
			if (mPublicUpload) {
				post.addParameter(PARAM_PUBLIC_UPLOAD, Boolean.toString(true));
			}
			if (mPassword != null && mPassword.length() > 0) {
				post.addParameter(PARAM_PASSWORD, mPassword);
			}
			if (OCShare.DEFAULT_PERMISSION != mPermissions) {
				post.addParameter(PARAM_PERMISSIONS, Integer.toString(mPermissions));
			}

			post.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);
            
			status = client.executeMethod(post);

			if(isSuccess(status)) {
				String response = post.getResponseBodyAsString();

				ShareToRemoteOperationResultParser parser = new ShareToRemoteOperationResultParser(
						new ShareXMLParser()
				);
				parser.setOneOrMoreSharesRequired(true);
				parser.setOwnCloudVersion(client.getOwnCloudVersion());
				parser.setServerBaseUri(client.getBaseUri());
				result = parser.parse(response);

				if (result.isSuccess() && mGetShareDetails) {
					// retrieve more info - POST only returns the index of the new share
					OCShare emptyShare = (OCShare) result.getData().get(0);
					GetRemoteShareOperation getInfo = new GetRemoteShareOperation(
							emptyShare.getRemoteId()
					);
					result = getInfo.execute(client);
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
