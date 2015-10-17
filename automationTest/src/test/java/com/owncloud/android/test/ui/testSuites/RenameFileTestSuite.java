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
import com.owncloud.android.test.ui.models.FilesView;
import com.owncloud.android.test.ui.models.FolderPopUp;
import com.owncloud.android.test.ui.models.WaitAMomentPopUp;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RenameFileTestSuite{

	AndroidDriver driver;
	Common common;
	FilesView filesView;

	@Rule public TestName name = new TestName();

	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
		//login
		filesView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFilesView(filesView);
	}

	public static void renameDownloadedFileMethod (AndroidDriver driver, 
			Common common, FilesView filesView) throws Exception {
		AndroidDriver.ImeHandler ime = driver.manage().ime();

		//check if the file with the new name already exists, if true delete it
		Actions.deleteElement(Config.fileToRename, filesView, driver);

		//if the file to rename already exists, delete it 
		AndroidElement file = filesView.getElement(Config.fileToTest);
		if(file!=null){
			Actions.deleteElement(Config.fileToTest,filesView, driver);
			common.assertIsInFilesView(filesView);
			assertNull(filesView.getElement(Config.fileToTest));
		}
		//now we are sure that we are going to rename a downloaded file
		filesView = Actions.uploadFile(Config.fileToTest, filesView);

		assertTrue(filesView.getElement(Config.fileToTest).isDisplayed());
		Common.waitTillElementIsNotPresentWithoutTimeout(
				filesView.getProgressCircular(), 1000);

		//check that it is downloaded
		common.wait.until(ExpectedConditions.visibilityOf(
				filesView.getElement(Config.fileToTest)
				.findElement(By.id(FilesView.getLocalFileIndicator()))));

		assertTrue(filesView.getElement(Config.fileToTest)
				.findElement(By.id(FilesView.getLocalFileIndicator()))
				.isDisplayed());
		assertNull(filesView.getElement(Config.fileToRename));

		ElementMenuOptions menuOptions = filesView
				.longPressOnElement(Config.fileToTest);

		FolderPopUp folderPopUp = menuOptions.clickOnRename();

		//set unicode keyboard for special character
		ime.activateEngine("io.appium.android.ime/.UnicodeIME");

		folderPopUp.typeNewFolderName(Config.fileToRename);

		//set normal keyboard
		ime.activateEngine("com.google.android.inputmethod.latin/"
				+ "com.android.inputmethod.latin.LatinIME");

		WaitAMomentPopUp waitAMomentPopUp = folderPopUp
				.clickOnNewFolderOkButton();

		Common.waitTillElementIsNotPresentWithoutTimeout(waitAMomentPopUp
				.getWaitAMomentTextElement(), 100);

		file = filesView.getElement(Config.fileToRename);

		assertNotNull(file);
		assertTrue(file.isDisplayed());	
	}

	@Test
	@Category({RegresionTestCategory.class, SmokeTestCategory.class})
	public void testRenameDownloadedFile () throws Exception {
		renameDownloadedFileMethod (driver, common, filesView);
	}

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		FilesView filesView = new FilesView(driver);
		Actions.deleteElement(Config.fileToTest,filesView, driver);
		Actions.deleteElement(Config.fileToRename,filesView, driver);
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}
