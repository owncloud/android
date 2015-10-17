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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.openqa.selenium.By;

import com.owncloud.android.test.ui.actions.Actions;
import com.owncloud.android.test.ui.groups.*;
import com.owncloud.android.test.ui.models.ElementMenuOptions;
import com.owncloud.android.test.ui.models.FilesView;
import com.owncloud.android.test.ui.models.MoveView;
import com.owncloud.android.test.ui.models.WaitAMomentPopUp;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MoveFolderTestSuite{
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

	@Test
	@Category({RegresionTestCategory.class, SmokeTestCategory.class})
	public void testMoveFolder () throws Exception {
		WaitAMomentPopUp waitAMomentPopUp;
		AndroidDriver.ImeHandler ime = driver.manage().ime();

		//Common.waitTillElementIsNotPresentWithoutTimeout(
		//fileListView.getProgressCircular(), 1000);

		//check if the folder already exists and if true, delete them
		Actions.deleteElement(Config.folderWhereMove, filesView, driver);
		Actions.deleteElement(Config.folderToMove, filesView, driver);

		assertNull(filesView.getElement(Config.folderWhereMove));
		assertNull(filesView.getElement(Config.folderToMove));

		//set unicode keyboard for special character
		ime.activateEngine("io.appium.android.ime/.UnicodeIME");

		//Create the folder where the other is gone to be moved
		Actions.createFolder(Config.folderWhereMove, filesView, driver);

		//Create the folder which is going to be moved
		Actions.createFolder(Config.folderToMove, filesView, driver);

		//set normal keyboard
		ime.activateEngine("com.google.android.inputmethod.latin/"
				+ "com.android.inputmethod.latin.LatinIME");
		
		//select to move the folder
		ElementMenuOptions menuOptions = filesView
				.longPressOnElement(Config.folderToMove);
		MoveView moveView = menuOptions.clickOnMove();

		//to move to a folder
		moveView.tapOnElement(Config.folderWhereMove);
		waitAMomentPopUp = moveView.clickOnChoose();
		Common.waitTillElementIsNotPresentWithoutTimeout(waitAMomentPopUp
				.getWaitAMomentTextElement(), 100);

		//check that the folder moved is inside the other
		filesView.tapOnElement(Config.folderWhereMove);
		Common.waitTillElementIsNotPresentWithoutTimeout(
				filesView.getProgressCircular(), 1000);
		Thread.sleep(1000);
		assertTrue(filesView.getElement(Config.folderToMove)
				.isDisplayed());
	}

	public static void moveFolderWithDownloadedFilesMethod (AndroidDriver driver, 
			Common common, FilesView filesView) throws Exception {
		WaitAMomentPopUp waitAMomentPopUp;
		AndroidDriver.ImeHandler ime = driver.manage().ime();

		//Common.waitTillElementIsNotPresentWithoutTimeout(
		//fileListView.getProgressCircular(), 1000);

		//check if the folder already exists and if true, delete them
		Actions.deleteElement(Config.folderWhereMove, filesView, driver);
		Actions.deleteElement(Config.folderToMove, filesView, driver);

		assertNull(filesView.getElement(Config.folderWhereMove));
		assertNull(filesView.getElement(Config.folderToMove));

		//set unicode keyboard for special character
		ime.activateEngine("io.appium.android.ime/.UnicodeIME");

		//Create the folder where the other is gone to be moved
		Actions.createFolder(Config.folderWhereMove, filesView, driver);

		//Create the folder which is going to be moved
		Actions.createFolder(Config.folderToMove, filesView, driver);

		//set normal keyboard
		ime.activateEngine("com.google.android.inputmethod.latin/"
				+ "com.android.inputmethod.latin.LatinIME");

		//Create content inside the folder
		Actions.createContentInsideFolder(Config.folderToMove, 
				Config.fileToTest,  Config.fileToTest2,  Config.fileToTest3,
				Config.folderToCreateSpecialCharacters, filesView, driver);

		//select to move the folder
		ElementMenuOptions menuOptions = filesView
				.longPressOnElement(Config.folderToMove);
		MoveView moveView = menuOptions.clickOnMove();

		//to move to a folder
		moveView.tapOnElement(Config.folderWhereMove);
		waitAMomentPopUp = moveView.clickOnChoose();
		Common.waitTillElementIsNotPresentWithoutTimeout(waitAMomentPopUp
				.getWaitAMomentTextElement(), 100);

		//check that the folder moved is inside the other
		filesView.tapOnElement(Config.folderWhereMove);
		Common.waitTillElementIsNotPresentWithoutTimeout(
				filesView.getProgressCircular(), 1000);
		Thread.sleep(1000);
		assertTrue(filesView.getElement(Config.folderToMove)
				.isDisplayed());

		//check that the files inside are there and still downloaded
		filesView.tapOnElement(Config.folderToMove);
		Common.waitTillElementIsNotPresentWithoutTimeout(
				filesView.getProgressCircular(), 1000);
		Thread.sleep(1000);
		assertTrue(filesView.getElement(Config.fileToTest).isDisplayed());
		assertTrue(filesView.getElement(Config.fileToTest2).isDisplayed());
		assertTrue(filesView.getElement(Config.fileToTest3).isDisplayed());

		assertTrue(filesView.getElement(Config.fileToTest)
				.findElement(By.id(FilesView.getLocalFileIndicator()))
				.isDisplayed());
		assertTrue(filesView.getElement(Config.fileToTest2)
				.findElement(By.id(FilesView.getLocalFileIndicator()))
				.isDisplayed());
		assertTrue(filesView.getElement(Config.fileToTest3)
				.findElement(By.id(FilesView.getLocalFileIndicator()))
				.isDisplayed());
		assertTrue(filesView.getElement(Config.folderToCreateSpecialCharacters)
				.isDisplayed());
		filesView.clickOnBackButton();
	}

	@Test
	@Category({RegresionTestCategory.class, SmokeTestCategory.class})
	public void testMoveFolderWithDownloadedFiles () throws Exception {
		moveFolderWithDownloadedFilesMethod(driver,common, filesView);
	}

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		FilesView filesView = new FilesView(driver);
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_BACK);
		Actions.deleteElement(Config.folderWhereMove, filesView, driver);
		Actions.deleteElement(Config.folderToMove, filesView, driver);
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}


}
