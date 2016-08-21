/**
 *   ownCloud Android client application
 *
 *   @author purigarcia
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.test.ui.testSuites;

import static org.junit.Assert.*;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.openqa.selenium.By;

import com.owncloud.android.test.ui.actions.Actions;
import com.owncloud.android.test.ui.groups.*;
import com.owncloud.android.test.ui.models.FileListView;;

public class ShareLinkFileTestSuite{

	AndroidDriver driver;
	Common common;
	private Boolean fileHasBeenCreated = false;

	@Rule public TestName name = new TestName();

	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
	}

	@Test
	@Category({NoIgnoreTestCategory.class})
	public void testShareLinkFileByGmail () throws Exception {	
		AndroidElement sharedElementIndicator;
		FileListView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView(fileListView);

		//TODO. if the file already exists, do not upload
		FileListView fileListViewAfterUploadFile = Actions
				.uploadFile(Config.fileToTest, fileListView);

		assertTrue(fileHasBeenCreated = fileListViewAfterUploadFile
				.getFileElement(Config.fileToTest).isDisplayed());

		sharedElementIndicator = Actions.shareLinkElementByGmail(
				Config.fileToTest,fileListViewAfterUploadFile,driver,common);

		assertTrue(fileListViewAfterUploadFile
				.getFileElementLayout(Config.fileToTest)
				.findElement(By.id(FileListView.getSharedElementIndicator()))
				.isDisplayed());
		//assertTrue(sharedElementIndicator.isDisplayed());
	}

	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testShareLinkFileByCopyLink () throws Exception {	
		AndroidElement sharedElementIndicator;
		FileListView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView(fileListView);

		//TODO. if the file already exists, do not upload
		FileListView fileListViewAfterUploadFile = Actions
				.uploadFile(Config.fileToTest, fileListView);

		assertTrue(fileHasBeenCreated = fileListViewAfterUploadFile
				.getFileElement(Config.fileToTest).isDisplayed());

		sharedElementIndicator = Actions.shareLinkElementByCopyLink(
				Config.fileToTest,
				fileListViewAfterUploadFile,driver,common);
		
		assertTrue(sharedElementIndicator.isDisplayed());
	}

	@Test
	@Category({IgnoreTestCategory.class, SmokeTestCategory.class})
	public void testUnshareLinkFile () throws Exception {	
		AndroidElement sharedElementIndicator;
		FileListView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView(fileListView);

		//TODO. if the file already exists, do not upload
		FileListView fileListViewAfterUploadFile = Actions
				.uploadFile(Config.fileToTest, fileListView);

		assertTrue(fileHasBeenCreated = fileListViewAfterUploadFile
				.getFileElement(Config.fileToTest).isDisplayed());

		sharedElementIndicator = Actions.shareLinkElementByCopyLink(
				Config.fileToTest,fileListViewAfterUploadFile,driver,common);
		
		assertTrue(sharedElementIndicator.isDisplayed());
		Actions.unshareLinkElement(Config.fileToTest,
				fileListViewAfterUploadFile,driver,common);
		assertFalse(sharedElementIndicator.isDisplayed());

	}

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		if (fileHasBeenCreated) {
			FileListView fileListView = new FileListView(driver);
			Actions.deleteElement(Config.fileToTest,fileListView, driver);
		}
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}
}
