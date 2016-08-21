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
@Category({NoIgnoreTestCategory.class})
public class FavoriteFilesTestSuite{

	AndroidDriver driver;
	Common common;

	@Rule public TestName name = new TestName();


	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
	}


	@Test	
	@Category({FailingTestCategory.class})
	public void testKeepFileUpToDate () throws Exception {

		FilesView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView(fileListView);

		Common.waitTillElementIsNotPresentWithoutTimeout(fileListView
				.getProgressCircular(), 1000);

		//if the file already exists, it is not necessary to upload it
		AndroidElement file = fileListView.getElement(Config.fileToTest);
		if(file==null){
			fileListView = Actions.uploadFile(Config.fileToTest, fileListView);
			file = fileListView.getElement(Config.fileToTest);
		}
		assertTrue(file.isDisplayed());

		ElementMenuOptions menuOptions = fileListView
				.longPressOnElement(Config.fileToTest);
		FileDetailsView fileDetailsView = menuOptions.clickOnDetails();
		fileDetailsView.checkKeepFileUpToDateCheckbox();
		Thread.sleep(3000);
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_BACK);

		assertTrue(fileListView.getElement(Config.fileToTest)
				.findElement(By.id(FilesView.getFavoriteFileIndicator()))
				.isDisplayed());
	}

	@Test	
	@Category({NoIgnoreTestCategory.class})
	public void testKeepFileUpToDateAndRefresh () throws Exception {

		FilesView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView(fileListView);

		Common.waitTillElementIsNotPresentWithoutTimeout(
				fileListView.getProgressCircular(), 1000);

		//if the file already exists, it is not necessary to upload it
		AndroidElement file = fileListView.getElement(Config.fileToTest);
		if(file==null){
			fileListView = Actions.uploadFile(Config.fileToTest, fileListView);
			file = fileListView.getElement(Config.fileToTest);
		}
		assertTrue(file.isDisplayed());

		ElementMenuOptions menuOptions = fileListView
				.longPressOnElement(Config.fileToTest);
		FileDetailsView fileDetailsView = menuOptions.clickOnDetails();
		fileDetailsView.checkKeepFileUpToDateCheckbox();
		Thread.sleep(3000);
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_BACK);

		fileListView.pulldownToRefresh();
		//assertTrue(fileListView.getProgressCircular().isDisplayed());
		Common.waitTillElementIsNotPresentWithoutTimeout(fileListView
				.getProgressCircular(), 100);

		assertTrue(fileListView.getElement(Config.fileToTest)
				.findElement(By.id(FilesView.getFavoriteFileIndicator()))
				.isDisplayed());
	}


	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		FilesView fileListView = new FilesView(driver);
		Actions.deleteElement(Config.fileToTest,fileListView, driver);

		driver.removeApp("com.owncloud.android");
		driver.quit();
	}


}

