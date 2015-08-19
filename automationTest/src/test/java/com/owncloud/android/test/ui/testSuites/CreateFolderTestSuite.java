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
import com.owncloud.android.test.ui.models.FilesView;
import com.owncloud.android.test.ui.models.WaitAMomentPopUp;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CreateFolderTestSuite{

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
	public void testCreateFolder () throws Exception {

		FilesView filesView = Actions.login(Config.URL, 
				Config.user,Config.password, Config.isTrusted, driver);
		common.assertIsInFilesView(filesView);

		Actions.createFolder(filesView, Config.folderToCreate, driver);
	}

	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testCreateFolderWithSpecialCharacters () throws Exception {
		FilesView filesView = Actions.login(Config.URL, 
				Config.user,Config.password, Config.isTrusted, driver);
		common.assertIsInFilesView(filesView);
		
		AndroidDriver.ImeHandler ime = driver.manage().ime();
	    //ime.getAvailableEngines();
	    //for (String engine : ime.getAvailableEngines()) {
	      //System.out.println(engine);
	    //}
	    ime.activateEngine("io.appium.android.ime/.UnicodeIME");
	    createFolder(filesView, Config.folderToCreateSpecialCharacters);
		
		ime.activateEngine("com.google.android.inputmethod.latin/"
				+ "com.android.inputmethod.latin.LatinIME");
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
