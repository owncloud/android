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

import io.appium.java_client.android.AndroidDriver;

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


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CreateFolderTestSuite{

	AndroidDriver driver;
	Common common;
	FilesView filesView;

	@Rule public TestName name = new TestName();

	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();

		filesView = Actions.login(Config.URL, 
				Config.user,Config.password, Config.isTrusted, driver);
		common.assertIsInFilesView(filesView);
	}

	@Test
	@Category({RegresionTestCategory.class, SmokeTestCategory.class})
	public void testCreateFolder () throws Exception {
		Actions.createFolder(Config.folderToCreate, filesView, driver);
	}
	
	public static void createFolderWithSpecialCharactersMethod (AndroidDriver driver, 
			Common common, FilesView filesView) throws Exception {
		AndroidDriver.ImeHandler ime = driver.manage().ime();
		
		//set unicode keyboard for special character
		ime.activateEngine("io.appium.android.ime/.UnicodeIME");
		Actions.createFolder(Config.folderToCreateSpecialCharacters, filesView,
				driver);
		
		//set normal keyboard
		ime.activateEngine("com.google.android.inputmethod.latin/"
				+ "com.android.inputmethod.latin.LatinIME");
	}

	@Test
	@Category({RegresionTestCategory.class, SmokeTestCategory.class})
	public void testCreateFolderWithSpecialCharacters () throws Exception {
		createFolderWithSpecialCharactersMethod (driver, common, filesView);
	}

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());

		FilesView filesView = new FilesView(driver);
		Actions.deleteElement(Config.folderToCreateSpecialCharacters, filesView
				,driver);
		Actions.deleteElement(Config.folderToCreate, filesView, driver);

		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}
