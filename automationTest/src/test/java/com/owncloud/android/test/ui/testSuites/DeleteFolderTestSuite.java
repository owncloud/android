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
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;
import org.junit.FixMethodOrder;
import org.junit.Test;

import com.owncloud.android.test.ui.actions.Actions;
import com.owncloud.android.test.ui.groups.*;
import com.owncloud.android.test.ui.models.FileListView;
import com.owncloud.android.test.ui.models.WaitAMomentPopUp;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DeleteFolderTestSuite{
	AndroidDriver driver;
	Common common;
	private Boolean folderHasBeenCreated = false;
	private final String FOLDER_NAME = "testCreateFolder";

	@Rule public TestName name = new TestName();


	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
	}

	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testDeleteFolder () throws Exception {
		FileListView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView(fileListView);

		//TODO. if the folder already exists, do no created
		//create the folder
		WaitAMomentPopUp waitAMomentPopUp = Actions
				.createFolder(FOLDER_NAME, fileListView);
		Common.waitTillElementIsNotPresentWithoutTimeout(
				waitAMomentPopUp.getWaitAMomentTextElement(), 100);
		AndroidElement folder = fileListView.getFileElement(FOLDER_NAME);
		assertTrue(folderHasBeenCreated = folder.isDisplayed());

		//delete the folder
		Actions.deleteElement(FOLDER_NAME, fileListView, driver);
		assertFalse(folderHasBeenCreated =folder.isDisplayed());
	}

	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testDeleteFolderWithContents () throws Exception {
		FileListView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView(fileListView);

		//TODO. if the folder already exists, do no created
		//create the folder
		WaitAMomentPopUp waitAMomentPopUp = Actions
				.createFolder(FOLDER_NAME, fileListView);
		Common.waitTillElementIsNotPresentWithoutTimeout(
				waitAMomentPopUp.getWaitAMomentTextElement(), 100);
		assertTrue(folderHasBeenCreated =
				fileListView.getFileElement(FOLDER_NAME).isDisplayed());
		fileListView.tapOnElement(FOLDER_NAME);

		fileListView = Actions.uploadSeveralFile(Config.fileToTest,
				Config.fileToTest2,Config.fileToTest3, fileListView);
		
		assertTrue(fileListView
				.getFileElement(Config.fileToTest).isDisplayed());
		assertTrue(fileListView
				.getFileElement(Config.fileToTest2).isDisplayed());
		assertTrue(fileListView
				.getFileElement(Config.fileToTest3).isDisplayed());

		fileListView.clickOnBackButton();
		AndroidElement folder = fileListView.getFileElement(FOLDER_NAME);
		assertTrue(folderHasBeenCreated =folder.isDisplayed());

		//delete the folder
		Actions.deleteElement(FOLDER_NAME, fileListView, driver);
		assertFalse(folderHasBeenCreated =folder.isDisplayed());
		
		fileListView.pulldownToRefresh();
		
		Common.waitTillElementIsNotPresentWithoutTimeout(fileListView
				.getProgressCircular(), 100);
		assertFalse(folderHasBeenCreated =folder.isDisplayed());

	}

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		if(folderHasBeenCreated){
			FileListView fileListView = new FileListView(driver);
			Actions.deleteElement(FOLDER_NAME, fileListView, driver);
		}
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}
