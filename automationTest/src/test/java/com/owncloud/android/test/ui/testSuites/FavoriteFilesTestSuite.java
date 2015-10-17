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

import com.owncloud.android.test.ui.actions.Actions;
import com.owncloud.android.test.ui.groups.*;
import com.owncloud.android.test.ui.models.FileDetailsView;
import com.owncloud.android.test.ui.models.ElementMenuOptions;
import com.owncloud.android.test.ui.models.FilesView;



@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Category({RegresionTestCategory.class})
public class FavoriteFilesTestSuite{

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
	@Category({FailingTestCategory.class})
	public void testFavoriteFile () throws Exception {
		Common.waitTillElementIsNotPresentWithoutTimeout(filesView
				.getProgressCircular(), 1000);

		//if the file already exists, it is not necessary to upload it
		AndroidElement file = filesView.getElement(Config.fileToTest2);
		if(file==null){
			filesView = Actions.uploadFile(Config.fileToTest2, filesView);
			file = filesView.getElement(Config.fileToTest2);
		}
		assertTrue(file.isDisplayed());

		ElementMenuOptions menuOptions = filesView
				.longPressOnElement(Config.fileToTest2);
		FileDetailsView fileDetailsView = menuOptions.clickOnDetails();
		fileDetailsView.checkFavoriteCheckbox();
		Thread.sleep(3000);
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_BACK);

		assertTrue(filesView.getElement(Config.fileToTest2)
				.findElement(By.id(FilesView.getFavoriteFileIndicator()))
				.isDisplayed());
	}

	public static void favoriteFileAndRefreshMethod (AndroidDriver driver, 
			Common common, FilesView filesView) throws Exception {
		Common.waitTillElementIsNotPresentWithoutTimeout(
				filesView.getProgressCircular(), 1000);

		//if the file already exists, it is not necessary to upload it
		AndroidElement file = filesView.getElement(Config.fileToTest2);
		if(file==null){
			filesView = Actions.uploadFile(Config.fileToTest2, filesView);
			file = filesView.getElement(Config.fileToTest2);
		}
		assertTrue(file.isDisplayed());

		ElementMenuOptions menuOptions = filesView
				.longPressOnElement(Config.fileToTest2);
		FileDetailsView fileDetailsView = menuOptions.clickOnDetails();
		fileDetailsView.checkFavoriteCheckbox();
		Thread.sleep(3000);
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_BACK);

		filesView.pulldownToRefresh();
		//assertTrue(filesView.getProgressCircular().isDisplayed());
		Common.waitTillElementIsNotPresentWithoutTimeout(filesView
				.getProgressCircular(), 100);

		assertTrue(filesView.getElement(Config.fileToTest2)
				.findElement(By.id(FilesView.getFavoriteFileIndicator()))
				.isDisplayed());
	}
	
	@Test	
	@Category({RegresionTestCategory.class, SmokeTestCategory.class})
	public void testFavoriteFileAndRefresh () throws Exception {
		favoriteFileAndRefreshMethod (driver,common,filesView);
	}


	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		FilesView filesView = new FilesView(driver);
		Actions.deleteElement(Config.fileToTest2,filesView, driver);

		driver.removeApp("com.owncloud.android");
		driver.quit();
	}


}

