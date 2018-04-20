/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2018 ownCloud GmbH.
 *
 *   @author David González Verdugo
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

package com.owncloud.android.lib.sampleclient;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.DownloadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.resources.files.RemoveRemoteFileOperation;
import com.owncloud.android.lib.resources.files.UploadRemoteFileOperation;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends Activity implements OnRemoteOperationListener, OnDatatransferProgressListener {
	
	private static String LOG_TAG = MainActivity.class.getCanonicalName();

	private static final String NODE_VERSION = "version";
	private static final String WEBDAV_PATH_4_0 = "/remote.php/webdav/";
	private static final String NEW_WEBDAV_PATH = "/remote.php/dav/files/";
	private static final String OC_TOTAL_LENGTH_HEADER = "OC-Total-Length";
	private static final String OC_X_OC_MTIME_HEADER = "X-OC-Mtime";
	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String USER_AGENT_HEADER = "User-Agent";
	private static final String CONTENT_TYPE_HEADER = "Content-Type";
	private static final String USER_AGENT_VALUE = "Mozilla/5.0 (Android) ownCloud-android/2.7.0";
	private static final String CONTENT_TYPE_VALUE = "multipart/form-data";
	
	private Handler mHandler;
	
	private OwnCloudClient mClient; 
	
	private FilesArrayAdapter mFilesAdapter;
	
	private View mFrame;

	private OkHttpClient mOkHttpClient;

	private String mCredentials;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mHandler = new Handler();
        
    	Uri serverUri = Uri.parse(getString(R.string.server_base_url));
    	mClient = OwnCloudClientFactory.createOwnCloudClient(serverUri, this, true);
    	mClient.setCredentials(
    			OwnCloudCredentialsFactory.newBasicCredentials(
    					getString(R.string.username), 
    					getString(R.string.password)
				)
		);
    	
    	mFilesAdapter = new FilesArrayAdapter(this, R.layout.file_in_list);
//    	((ListView)findViewById(R.id.list_view)).setAdapter(mFilesAdapter);
    	
    	// TODO move to background thread or task
    	AssetManager assets = getAssets();
		try {
			String sampleFileName = getString(R.string.sample_file_name); 
	    	File upFolder = new File(getCacheDir(), getString(R.string.upload_folder_path));
	    	upFolder.mkdir();
	    	File upFile = new File(upFolder, sampleFileName);
	    	FileOutputStream fos = new FileOutputStream(upFile);
	    	InputStream is = assets.open(sampleFileName);
	    	int count = 0;
	    	byte[] buffer = new byte[1024];
	    	while ((count = is.read(buffer, 0, buffer.length)) >= 0) {
	    		fos.write(buffer, 0, count);
	    	}
	    	is.close();
	    	fos.close();
		} catch (IOException e) {
			Toast.makeText(this, R.string.error_copying_sample_file, Toast.LENGTH_SHORT).show();
			Log.e(LOG_TAG, getString(R.string.error_copying_sample_file), e);
		}

		((TextView) findViewById(R.id.server_address)).setText(getString(R.string.server_base_url));

		mOkHttpClient = new OkHttpClient();

		mCredentials = Credentials.basic(getString(R.string.username), getString(R.string.password));
		
//		mFrame = findViewById(R.id.frame);
    }
    
    
    @Override
    public void onDestroy() {
    	File upFolder = new File(getCacheDir(), getString(R.string.upload_folder_path));
    	File upFile = upFolder.listFiles()[0];
    	upFile.delete();
    	upFolder.delete();
    	super.onDestroy();
    }
    
    
    public void onClickHandler(View button) {
    	switch (button.getId())	{
			case R.id.button_check_server:
				startCheck();
				break;
	    	case R.id.button_refresh:
	    		startRefresh();
	    		break;
	    	case R.id.button_upload:
	    		startUpload();
	    		break;
			case R.id.button_download:
	    		startDownload();
	    		break;
	    	case R.id.button_delete_remote:
	    		startRemoteDeletion();
	    		break;
//	    	case R.id.button_delete_local:
//	    		startLocalDeletion();
//	    		break;
			default:
	    		Toast.makeText(this, R.string.youre_doing_it_wrong, Toast.LENGTH_SHORT).show();
    	}
    }

    private void startCheck() {

		if (!validServerAddress()) return;

		Request request = new Request.Builder()
				.url(getString(R.string.server_base_url) + "/status.php")
				.get()
				.build();

		mOkHttpClient.newCall(request).enqueue(new Callback() {

			@Override public void onResponse(Call call, final Response response) throws IOException {

				if (!response.isSuccessful()) {

					showMessage("Response not successful with code: " + response.code());

					throw new IOException("Unexpected code " + response);
				}

				try { // Successful response

					String jsonData = response.body().string();

					JSONObject Jobject = new JSONObject(jsonData);

					final String serverVersion = Jobject.get("version").toString();

					showMessage("Server with version " + serverVersion + " detected");

				} catch (JSONException e) {
					e.printStackTrace();
				}

				Headers responseHeaders = response.headers();
				for (int i = 0, size = responseHeaders.size(); i < size; i++) {
					System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
				}
			}

			@Override public void onFailure(Call call, final IOException e) {
				e.printStackTrace();
				showMessage("Something was wrong: " + e.toString());
			}
		});
	}

	private void startRefresh() {

//		Let's first use OKHttp with the new endpoint without depending on our library operations

//    	ReadRemoteFolderOperation refreshOperation = new ReadRemoteFolderOperation(FileUtils.PATH_SEPARATOR);
//    	refreshOperation.execute(mClient, this, mHandler);

		if (!validServerAddress()) return;

		final Request request = new Request.Builder()
				.url(getString(R.string.server_base_url) + NEW_WEBDAV_PATH + getString(R.string.username))
				.addHeader(AUTHORIZATION_HEADER, mCredentials)
				.addHeader(USER_AGENT_HEADER, USER_AGENT_VALUE)
				.method("PROPFIND", null)
				.build();

		mOkHttpClient.newCall(request).enqueue(new Callback() {

			@Override public void onResponse(Call call, final Response response) throws IOException {

				if (!response.isSuccessful()) {

					showMessage("Response not successful with code: " + response.code());

					throw new IOException("Unexpected code " + response);

				} else { // Successful response

					final String propFindResult = response.body().string();

					showMessage(propFindResult);

					Headers responseHeaders = response.headers();
					for (int i = 0, size = responseHeaders.size(); i < size; i++) {
						System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
					}
				}
			}

			@Override public void onFailure(Call call, IOException e) {
				showMessage("Something was wrong: " + e.toString());
				e.printStackTrace();
			}
		});
	}
    
    private void startUpload() {

    	File upFolder = new File(getCacheDir(), getString(R.string.upload_folder_path));
    	final File fileToUpload = upFolder.listFiles()[0];
    	String remotePath = FileUtils.PATH_SEPARATOR + fileToUpload.getName(); 
    	String mimeType = getString(R.string.sample_file_mimetype);

		MediaType mediaType = MediaType.parse(mimeType);

		// Get the last modification date of the file from the file system
		Long timeStampLong = fileToUpload.lastModified()/1000;
		String timeStamp = timeStampLong.toString();

//		Let's first use OKHttp with the new endpoint without depending on our library operations

//    	UploadRemoteFileOperation uploadOperation = new UploadRemoteFileOperation(fileToUpload.getAbsolutePath(), remotePath, mimeType, timeStamp);
//    	uploadOperation.addDatatransferProgressListener(this);
//    	uploadOperation.execute(mClient, this, mHandler);

		if (!validServerAddress()) return;

		RequestBody requestBody = RequestBody.create(mediaType, fileToUpload);

		final Request request = new Request.Builder()
				.url(getString(R.string.server_base_url) + NEW_WEBDAV_PATH + getString(R.string.username) + remotePath)
				.addHeader(AUTHORIZATION_HEADER, mCredentials)
				.addHeader(USER_AGENT_HEADER, USER_AGENT_VALUE)
                .addHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE)
				.addHeader(OC_TOTAL_LENGTH_HEADER, String.valueOf(fileToUpload.length()))
				.addHeader(OC_X_OC_MTIME_HEADER, timeStamp)
				.put(requestBody)
				.build();

		mOkHttpClient.newCall(request).enqueue(new Callback() {

			@Override public void onResponse(Call call, final Response response) throws IOException {

				if (!response.isSuccessful()) {

					showMessage("Response not successful with code: " + response.code());

					throw new IOException("Unexpected code " + response);

				} else { // Successful response

					showMessage("Successful upload of " + fileToUpload.getName());
				}
			}

			@Override public void onFailure(Call call, IOException e) {
				e.printStackTrace();
				showMessage("Something was wrong: " + e.toString());
			}
		});
    }

	private void startDownload() {
		File downFolder = new File(getCacheDir(), getString(R.string.download_folder_path));
		downFolder.mkdir();
		File upFolder = new File(getCacheDir(), getString(R.string.upload_folder_path));
		final File fileToUpload = upFolder.listFiles()[0];
		String remotePath = FileUtils.PATH_SEPARATOR + fileToUpload.getName();

//		Let's first use OKHttp with the new endpoint without depending on our library operations

//		DownloadRemoteFileOperation downloadOperation = new DownloadRemoteFileOperation(remotePath, downFolder.getAbsolutePath());
//		downloadOperation.addDatatransferProgressListener(this);
//		downloadOperation.execute(mClient, this, mHandler);

		if (!validServerAddress()) return;

		final Request request = new Request.Builder()
				.url(getString(R.string.server_base_url) + NEW_WEBDAV_PATH + getString(R.string.username) + remotePath)
				.addHeader(AUTHORIZATION_HEADER, mCredentials)
				.addHeader(USER_AGENT_HEADER, USER_AGENT_VALUE)
				.get()
				.build();


		mOkHttpClient.newCall(request).enqueue(new Callback() {

			@Override public void onResponse(Call call, final Response response) throws IOException {

				if (!response.isSuccessful()) {

					showMessage("Response not successful with code: " + response.code());

					throw new IOException("Unexpected code " + response);

				} else { // Successful response

					showMessage("Successful download of " + fileToUpload.getName() + " although local file " +
							"won't be created in this stage");
				}
			}

			@Override public void onFailure(Call call, IOException e) {
				e.printStackTrace();
				showMessage("Something was wrong: " + e.toString());
			}
		});
	}

	private void startRemoteDeletion() {
    	File upFolder = new File(getCacheDir(), getString(R.string.upload_folder_path));
    	final File fileToUpload = upFolder.listFiles()[0];
    	String remotePath = FileUtils.PATH_SEPARATOR + fileToUpload.getName();

//		Let's first use OKHttp with the new endpoint without depending on our library operations
//    	RemoveRemoteFileOperation removeOperation = new RemoveRemoteFileOperation(remotePath);
//    	removeOperation.execute(mClient, this, mHandler);

		if (!validServerAddress()) return;

		final Request request = new Request.Builder()
				.url(getString(R.string.server_base_url) + NEW_WEBDAV_PATH + getString(R.string.username) + remotePath)
				.addHeader(AUTHORIZATION_HEADER, mCredentials)
				.addHeader(USER_AGENT_HEADER, USER_AGENT_VALUE)
				.delete()
				.build();

		mOkHttpClient.newCall(request).enqueue(new Callback() {

			@Override public void onResponse(Call call, final Response response) throws IOException {

				if (!response.isSuccessful()) {

					showMessage("Response not successful with code: " + response.code());

					throw new IOException("Unexpected code " + response);

				} else { // Successful response

					showMessage("Successful deletion of " + fileToUpload.getName());
				}
			}

			@Override public void onFailure(Call call, IOException e) {
				e.printStackTrace();
				showMessage("Something was wrong: " + e.toString());
			}
		});
    }
    
    @SuppressWarnings("deprecation")
	private void startLocalDeletion() {
    	File downFolder = new File(getCacheDir(), getString(R.string.download_folder_path));
    	File downloadedFile = downFolder.listFiles()[0];
    	if (!downloadedFile.delete() && downloadedFile.exists()) {
    		Toast.makeText(this, R.string.error_deleting_local_file, Toast.LENGTH_SHORT).show();
    	} else {
//    		((TextView) findViewById(R.id.download_progress)).setText("0%");
//    		findViewById(R.id.frame).setBackgroundDrawable(null);
    	}
    }

	@Override
	public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
		if (!result.isSuccess()) {
			Toast.makeText(this, R.string.todo_operation_finished_in_fail, Toast.LENGTH_SHORT).show();
			Log.e(LOG_TAG, result.getLogMessage(), result.getException());
			
		} else if (operation instanceof ReadRemoteFolderOperation) {
			onSuccessfulRefresh((ReadRemoteFolderOperation)operation, result);
			
		} else if (operation instanceof UploadRemoteFileOperation ) {
			onSuccessfulUpload((UploadRemoteFileOperation)operation, result);
			
		} else if (operation instanceof RemoveRemoteFileOperation ) {
			onSuccessfulRemoteDeletion((RemoveRemoteFileOperation)operation, result);
			
		} else if (operation instanceof DownloadRemoteFileOperation ) {
			onSuccessfulDownload((DownloadRemoteFileOperation)operation, result);
			
		} else {
			Toast.makeText(this, R.string.todo_operation_finished_in_success, Toast.LENGTH_SHORT).show();
		}
	}

	private void onSuccessfulRefresh(ReadRemoteFolderOperation operation, RemoteOperationResult result) {
		mFilesAdapter.clear();
		List<RemoteFile> files = new ArrayList<RemoteFile>();
        for(Object obj: result.getData()) {
            files.add((RemoteFile) obj);
        }
		if (files != null) {
			Iterator<RemoteFile> it = files.iterator();
			while (it.hasNext()) {
				mFilesAdapter.add(it.next());
			}
			mFilesAdapter.remove(mFilesAdapter.getItem(0));
		}
		mFilesAdapter.notifyDataSetChanged();
	}

	private void onSuccessfulUpload(UploadRemoteFileOperation operation, RemoteOperationResult result) {
		startRefresh();
	}

	private void onSuccessfulRemoteDeletion(RemoveRemoteFileOperation operation, RemoteOperationResult result) {
		startRefresh();
//		TextView progressView = (TextView) findViewById(R.id.upload_progress);
//		if (progressView != null) {
//			progressView.setText("0%");
//		}
	}

	@SuppressWarnings("deprecation")
	private void onSuccessfulDownload(DownloadRemoteFileOperation operation, RemoteOperationResult result) {
    	File downFolder = new File(getCacheDir(), getString(R.string.download_folder_path));
    	File downloadedFile = downFolder.listFiles()[0];
    	BitmapDrawable bDraw = new BitmapDrawable(getResources(), downloadedFile.getAbsolutePath());
    	mFrame.setBackgroundDrawable(bDraw);
	}

	@Override
	public void onTransferProgress(long progressRate, long totalTransferredSoFar, long totalToTransfer, String fileName) {
		final long percentage = (totalToTransfer > 0 ? totalTransferredSoFar * 100 / totalToTransfer : 0);
		final boolean upload = fileName.contains(getString(R.string.upload_folder_path));
		Log.d(LOG_TAG, "progressRate " + percentage);
    	mHandler.post(new Runnable() {
            @Override
            public void run() {
				TextView progressView = null;
//				if (upload) {
//					progressView = (TextView) findViewById(R.id.upload_progress);
//				} else {
//					progressView = (TextView) findViewById(R.id.download_progress);
//				}
				if (progressView != null) {
	    			progressView.setText(Long.toString(percentage) + "%");
				}
            }
        });
	}

	private boolean validServerAddress() {

    	String serverAddress = ((TextView) findViewById(R.id.server_address)).getText().toString();

		if (serverAddress.equals("") || (!serverAddress.contains("http://") && !serverAddress.contains("https://"))) {
			showToastMessage("Introduce a proper server address with http/https");
			return false;
		}
		return true;
	}

	private void showMessage(final String message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				showToastMessage(message);
			}
		});
	}

	private void showToastMessage(String message) {
		Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);

		toast.setGravity(Gravity.CENTER, 0, 0);

		toast.show();
	}
}
