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
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.owncloud.android.test.ui.actions.Actions;
import com.owncloud.android.test.ui.groups.*;
import com.owncloud.android.test.ui.models.ElementMenuOptions;
import com.owncloud.android.test.ui.models.FileListView;
import com.owncloud.android.test.ui.models.FolderPopUp;
import com.owncloud.android.test.ui.models.WaitAMomentPopUp;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RenameFileTestSuite{

	AndroidDriver driver;
	Common common;
	private String CurrentCreatedFile = "";

	@Rule public TestName name = new TestName();

	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
	}

	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testRenameDownloadedFile () throws Exception {
		FileListView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView(fileListView);

		//if the file already exists, delete it 
		AndroidElement file = fileListView.getFileElement(Config.fileToTest);
		if(file!=null){
			Actions.deleteElement(Config.fileToTest,fileListView, driver);
			common.assertIsInFileListView(fileListView);
			assertFalse(file.isDisplayed());
		}
		//now we are sure that we are going to rename a downloaded file
		fileListView = Actions.uploadFile(Config.fileToTest, fileListView);
		
		assertTrue(fileListView
				.getFileElement(Config.fileToTest).isDisplayed());
		CurrentCreatedFile = Config.fileToTest;
		Common.waitTillElementIsNotPresentWithoutTimeout(
				fileListView.getProgressCircular(), 1000);
		
		//check that it is downloaded
		common.wait.until(ExpectedConditions.visibilityOf(
				fileListView.getFileElementLayout(Config.fileToTest)
				.findElement(By.id(FileListView.getLocalFileIndicator()))));
		
		assertTrue(fileListView.getFileElementLayout(Config.fileToTest)
				.findElement(By.id(FileListView.getLocalFileIndicator()))
				.isDisplayed());

		//check if the file with the new name already exists, if true delete it
		Actions.deleteElement(Config.fileToRename, fileListView, driver);

		ElementMenuOptions menuOptions = fileListView
				.longPressOnElement(Config.fileToTest);

		FolderPopUp folderPopUp = menuOptions.clickOnRename();

		folderPopUp.typeNewFolderName(Config.fileToRename);

		WaitAMomentPopUp waitAMomentPopUp = folderPopUp
				.clickOnNewFolderOkButton();

		Common.waitTillElementIsNotPresentWithoutTimeout(waitAMomentPopUp
				.getWaitAMomentTextElement(), 100);

		file = fileListView.getFileElement(Config.fileToRename);

		assertNotNull(file);
		assertTrue(file.isDisplayed());	
		assertEquals(Config.fileToRename , file.getText());
		CurrentCreatedFile = Config.fileToRename;
	}

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		FileListView fileListView = new FileListView(driver);
		Actions.deleteElement(CurrentCreatedFile,fileListView, driver);
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}
