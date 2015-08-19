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
import org.openqa.selenium.ScreenOrientation;

import com.owncloud.android.test.ui.actions.Actions;
import com.owncloud.android.test.ui.groups.*;
import com.owncloud.android.test.ui.models.FilesView;



@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SmokeTestSuite{

	AndroidDriver driver;
	Common common;

	@Rule public TestName name = new TestName();

	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
	}

	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testSmokeTest () throws Exception {

		LoginTestSuite loginTS = new LoginTestSuite();
		loginTS.loginAndShowFilesMethod (driver, common);
		
		LogoutTestSuite logoutTS = new LogoutTestSuite();
		logoutTS.logoutMethod (driver, common);
		
		loginTS.multiAccountAndShowFilesMethod(driver, common); 
		
		driver.rotate(ScreenOrientation.LANDSCAPE);
		
		CreateFolderTestSuite createFolderTS = new CreateFolderTestSuite();
		createFolderTS.createFolderWithSpecialCharactersMethod(driver, common);
		
		driver.rotate(ScreenOrientation.PORTRAIT);
		DeleteFileTestSuite deleteFileTS = new DeleteFileTestSuite();
		deleteFileTS.deleteFileRemoteAndLocalMethod (driver, common);
		
		DeleteFolderTestSuite deleteFolderTS = new DeleteFolderTestSuite();
		deleteFolderTS.deleteFolderWithContentsMethod (driver, common);
		driver.rotate(ScreenOrientation.LANDSCAPE);
		
		FavoriteFilesTestSuite  favoriteFilesTS = new FavoriteFilesTestSuite();
		favoriteFilesTS.favoriteFileAndRefreshMethod (driver,common);
		
		
	

	}

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		
		FilesView filesView = new FilesView(driver);
		Actions.deleteElement(Config.folderToCreateSpecialCharacters, filesView
				,driver);


		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}
