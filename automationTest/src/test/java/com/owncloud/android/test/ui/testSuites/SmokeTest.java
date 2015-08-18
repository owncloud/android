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
import org.openqa.selenium.ScreenOrientation;

import com.owncloud.android.test.ui.actions.Actions;
import com.owncloud.android.test.ui.groups.*;
import com.owncloud.android.test.ui.models.Drawer;
import com.owncloud.android.test.ui.models.FilesView;
import com.owncloud.android.test.ui.models.SettingsView;
import com.owncloud.android.test.ui.models.WaitAMomentPopUp;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SmokeTest{

	AndroidDriver driver;
	Common common;
	private String CurrentCreatedFolder = "";

	@Rule public TestName name = new TestName();

	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
	}

	public void createFolder(FilesView filesView, String folderName) 
			throws Exception{
		//check if the folder already exists and if true, delete them
		Actions.deleteElement(folderName, filesView, driver);
		assertNull(filesView.getElement(folderName));

		WaitAMomentPopUp waitAMomentPopUp = Actions
				.createFolder(folderName, filesView);
		Common.waitTillElementIsNotPresentWithoutTimeout(waitAMomentPopUp
				.getWaitAMomentTextElement(), 100);
		AndroidElement folder = filesView.getElement(folderName);
		assertNotNull(folder);
		assertTrue(folder.isDisplayed());	
		CurrentCreatedFolder = folderName;
	}

	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testSmokeTest () throws Exception {

		driver.rotate(ScreenOrientation.LANDSCAPE);
		FilesView filesView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFilesView(filesView);
		assertTrue(filesView
				.getElement(Config.fileWhichIsInTheServer1).isDisplayed());
		
		driver.rotate(ScreenOrientation.PORTRAIT);
		Drawer drawer = filesView.swipeToShowDrawer();
		SettingsView settingsView = drawer.clickOnSettingsButton();
		
		settingsView.tapOnAddAccount(1, 1000);
		Actions.login(Config.URL2, Config.user2,
				Config.password2, Config.isTrusted2, driver);
		common.assertIsInSettingsView(settingsView);
		settingsView.tapOnAccountElement(2,1, 100);
		common.assertIsInFilesView(filesView);
		
		assertTrue(filesView.getElement(Config.fileWhichIsInTheServer2)
				.isDisplayed());
		
		AndroidDriver.ImeHandler ime = driver.manage().ime();
	    //ime.getAvailableEngines();
	    //for (String engine : ime.getAvailableEngines()) {
	      //System.out.println(engine);
	    //}
	    ime.activateEngine("io.appium.android.ime/.UnicodeIME");
	    createFolder(filesView, Config.folderToCreateSpecialCharacters);
		
		ime.activateEngine("com.google.android.inputmethod.latin/"
				+ "com.android.inputmethod.latin.LatinIME");
		
		filesView.tapOnElement(Config.folderToCreateSpecialCharacters);

		filesView = Actions.uploadSeveralFile(Config.fileToTest,
				Config.fileToTest2,Config.fileToTest3, filesView);

		assertTrue(filesView.getElement(Config.fileToTest).isDisplayed());
		assertTrue(filesView.getElement(Config.fileToTest2).isDisplayed());
		assertTrue(filesView.getElement(Config.fileToTest3).isDisplayed());

		filesView.clickOnBackButton();
		AndroidElement folder = filesView.getElement(Config.folderToCreateSpecialCharacters);
		assertTrue(folder.isDisplayed());

		//delete the folder
		Actions.deleteElement(Config.folderToCreateSpecialCharacters, filesView, driver);
		//assertFalse(folder.isDisplayed());
		assertNull(filesView.getElement(Config.folderToCreateSpecialCharacters));

		filesView.pulldownToRefresh();

		Common.waitTillElementIsNotPresentWithoutTimeout(filesView
				.getProgressCircular(), 100);
		//assertFalse(folder.isDisplayed());
		assertNull(filesView.getElement(Config.folderToCreateSpecialCharacters));
	}

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());

		FilesView filesView = new FilesView(driver);
		if(CurrentCreatedFolder != ""){
			Actions.deleteElement(CurrentCreatedFolder, filesView, driver);
		}

		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}
