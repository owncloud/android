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

import java.io.File;

import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.test_project.TestActivity;

/**
 * Class to test Rename File Operation
 * @author masensio
 *
 */

public class RenameFileTest extends RemoteTest {

	private static final String LOG_TAG = RenameFileTest.class.getCanonicalName();
	
	/* Folder data to rename. This folder must exist on the account */
	private static final String OLD_FOLDER_NAME = "folderToRename";
	private static final String OLD_FOLDER_PATH = FileUtils.PATH_SEPARATOR + OLD_FOLDER_NAME;
	private static final String NEW_FOLDER_NAME = "renamedFolder";
	private static final String NEW_FOLDER_PATH = FileUtils.PATH_SEPARATOR + NEW_FOLDER_NAME;

	/* File data to rename. This file must exist on the account */
	private static final String OLD_FILE_NAME = "fileToRename.png";
	private static final String OLD_FILE_PATH = FileUtils.PATH_SEPARATOR + OLD_FILE_NAME;
	private static final String NEW_FILE_NAME = "renamedFile.png";
	private static final String NEW_FILE_PATH = FileUtils.PATH_SEPARATOR + NEW_FILE_NAME;
	
	
	private String mToCleanUpInServer;
	private TestActivity mActivity;
	private String mFullPath2OldFolder;
	private String mFullPath2NewFolder;
	private String mFullPath2OldFile;
	private String mFullPath2NewFile;
	
	@Override
	  protected void setUp() throws Exception {
	    super.setUp();
	    setActivityInitialTouchMode(false);
	    mActivity = getActivity();
    	mFullPath2OldFolder = mBaseFolderPath + OLD_FOLDER_PATH;
    	mFullPath2NewFolder = mBaseFolderPath + NEW_FOLDER_PATH;
    	mFullPath2OldFile = mBaseFolderPath + OLD_FILE_PATH;
    	mFullPath2NewFile = mBaseFolderPath + NEW_FILE_PATH;
	    
		RemoteOperationResult result = mActivity.createFolder(mFullPath2OldFolder, true);
		if (!result.isSuccess() && result.getCode() != ResultCode.TIMEOUT) {
			Utils.logAndThrow(LOG_TAG, result);
		}
		
		File imageFile = mActivity.extractAsset(TestActivity.ASSETS__IMAGE_FILE_NAME);
		result = mActivity.uploadFile(
				imageFile.getAbsolutePath(), 
				mFullPath2OldFile, 
				"image/png");
		if (!result.isSuccess()) {
			Utils.logAndThrow(LOG_TAG, result);
		}
	    
		mToCleanUpInServer = null;
	}
	
	/**
	 * Test Rename Folder
	 */
	public void testRenameFolder() {

		mToCleanUpInServer = mFullPath2OldFolder;
		RemoteOperationResult result = mActivity.renameFile(
				OLD_FOLDER_NAME, 
				mFullPath2OldFolder, 
				NEW_FOLDER_NAME,
				true);
		assertTrue(result.isSuccess());
		mToCleanUpInServer = mFullPath2NewFolder;
	}
	
	/**
	 * Test Rename Folder with forbidden characters : \  < >  :  "  |  ?  *
	 */
	public void testRenameFolderForbiddenChars() {
		
		RemoteOperationResult result = mActivity.renameFile(OLD_FOLDER_NAME, mFullPath2OldFolder, 
				NEW_FOLDER_NAME + "\\", true);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(OLD_FOLDER_NAME, mFullPath2OldFolder, 
				NEW_FOLDER_NAME + "<", true);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(OLD_FOLDER_NAME, mFullPath2OldFolder, 
				NEW_FOLDER_NAME + ">", true);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(OLD_FOLDER_NAME, mFullPath2OldFolder, 
				NEW_FOLDER_NAME + ":", true);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(OLD_FOLDER_NAME, mFullPath2OldFolder, 
				NEW_FOLDER_NAME + "\"", true);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(OLD_FOLDER_NAME, mFullPath2OldFolder, 
				NEW_FOLDER_NAME + "|", true);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(OLD_FOLDER_NAME, mFullPath2OldFolder, 
				NEW_FOLDER_NAME + "?", true);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(OLD_FOLDER_NAME, mFullPath2OldFolder, 
				NEW_FOLDER_NAME + "*", true);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
	}
	
	/**
	 * Test Rename File
	 */
	public void testRenameFile() {
		mToCleanUpInServer = mFullPath2OldFile;
		RemoteOperationResult result = mActivity.renameFile(
				OLD_FILE_NAME, 
				mFullPath2OldFile, 
				NEW_FILE_NAME, 
				false);
		assertTrue(result.isSuccess());
		mToCleanUpInServer = mFullPath2NewFile;
	}
	
	
	/**
	 * Test Rename Folder with forbidden characters: \  < >  :  "  |  ?  *
	 */
	public void testRenameFileForbiddenChars() {		
		RemoteOperationResult result = mActivity.renameFile(
				OLD_FILE_NAME, 
				mFullPath2OldFile, 
				"\\" + NEW_FILE_NAME,
				false);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(
				OLD_FILE_NAME, 
				mFullPath2OldFile, 
				"<" + NEW_FILE_NAME, 
				false);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(
				OLD_FILE_NAME, 
				mFullPath2OldFile, 
				">" + NEW_FILE_NAME,
				false);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(
				OLD_FILE_NAME, 
				mFullPath2OldFile, 
				":" + NEW_FILE_NAME,
				false);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(
				OLD_FILE_NAME,
				mFullPath2OldFile, 
				"\"" + NEW_FILE_NAME,
				false);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(
				OLD_FILE_NAME,
				mFullPath2OldFile, 
				"|" + NEW_FILE_NAME,
				false);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(
				OLD_FILE_NAME,
				mFullPath2OldFile, 
				"?" + NEW_FILE_NAME,
				false);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(
				OLD_FILE_NAME,
				mFullPath2OldFile, 
				"*" + NEW_FILE_NAME, false);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
	}
	
	
	@Override
	protected void tearDown() throws Exception {
		if (mToCleanUpInServer != null) {
			RemoteOperationResult removeResult = mActivity.removeFile(mToCleanUpInServer);
			if (!removeResult.isSuccess()) {
				Utils.logAndThrow(LOG_TAG, removeResult);
			}
		}
		super.tearDown();
	}
	
}
