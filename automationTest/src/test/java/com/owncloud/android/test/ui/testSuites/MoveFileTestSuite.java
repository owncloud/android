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
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;

import com.owncloud.android.test.ui.actions.Actions;
import com.owncloud.android.test.ui.groups.*;
import com.owncloud.android.test.ui.models.ElementMenuOptions;
import com.owncloud.android.test.ui.models.FilesView;
import com.owncloud.android.test.ui.models.MoveView;
import com.owncloud.android.test.ui.models.WaitAMomentPopUp;



@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MoveFileTestSuite{
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

	public static void moveDownloadedFileMethod (AndroidDriver driver, 
			Common common, FilesView filesView) throws Exception {
		WaitAMomentPopUp waitAMomentPopUp;
		AndroidDriver.ImeHandler ime = driver.manage().ime();

		//check if the folder already exists and if true, delete them
		Actions.deleteElement(Config.folderWhereMove, filesView, driver);
		Actions.deleteElement(Config.fileToTest2, filesView, driver);

		assertNull(filesView.getElement(Config.folderWhereMove));
		assertNull(filesView.getElement(Config.fileToTest2));

		//set unicode keyboard for special character
		ime.activateEngine("io.appium.android.ime/.UnicodeIME");

		//Create the folder where the other is gone to be moved
		Actions.createFolder(Config.folderWhereMove, filesView, driver);

		//set normal keyboard
		ime.activateEngine("com.google.android.inputmethod.latin/"
				+ "com.android.inputmethod.latin.LatinIME");
		
		filesView = Actions.uploadFile(Config.fileToTest2, filesView);
		assertTrue(filesView.getElement(Config.fileToTest2)
				.isDisplayed());

		//select to move the file
		ElementMenuOptions menuOptions = filesView
				.longPressOnElement(Config.fileToTest2);
		MoveView moveView = menuOptions.clickOnMove();

		//to move to a folder
		moveView.tapOnElement(Config.folderWhereMove);
		waitAMomentPopUp = moveView.clickOnChoose();
		Common.waitTillElementIsNotPresentWithoutTimeout(
				waitAMomentPopUp.getWaitAMomentTextElement(), 100);

		//check that the element moved is inside the other
		filesView.tapOnElement(Config.folderWhereMove);
		Common.waitTillElementIsNotPresentWithoutTimeout(
				filesView.getProgressCircular(),1000);

		Thread.sleep(1000);
		assertTrue(filesView.getElement(Config.fileToTest2)
				.isDisplayed());

	}

	@Test
	@Category({RegresionTestCategory.class, SmokeTestCategory.class})
	public void testMoveDownloadedFile () throws Exception {
		moveDownloadedFileMethod(driver, common, filesView);
	}

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		FilesView filesView = new FilesView(driver);
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_BACK);
		Actions.deleteElement(Config.folderWhereMove, filesView, driver);
		Actions.deleteElement(Config.fileToTest2, filesView, driver);
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}
