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
import com.owncloud.android.test.ui.models.FilesView;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DeleteFolderTestSuite{
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
	public void testDeleteFolder () throws Exception {
		//if the folder already exists, do no created
		AndroidElement folder = filesView.getElement(Config.folderToCreate);
		if(folder==null){
			//create the folder
			Actions.createFolder(Config.folderToCreate, filesView, driver);
			folder = filesView.getElement(Config.folderToCreate);
		}
		assertTrue(folder.isDisplayed());

		//delete the folder
		Actions.deleteElement(Config.folderToCreate, filesView, driver);
		assertNull(filesView.getElement(Config.folderToCreate));
	}
	
	
	public static void deleteFolderWithContentsMethod (AndroidDriver driver, 
			Common common, FilesView filesView) throws Exception {
		//if the folder already exists, do no created
		AndroidElement folder = filesView.getElement(Config.folderToCreate);
		if(folder==null){
			Actions.createFolder(Config.folderToCreate, filesView, driver);
			folder = filesView.getElement(Config.folderToCreate);
		}
		assertTrue(folder.isDisplayed());
		filesView.tapOnElement(Config.folderToCreate);

		filesView = Actions.uploadSeveralFile(Config.fileToTest,
				Config.fileToTest2,Config.fileToTest3, filesView);

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

		filesView.clickOnBackButton();
		folder = filesView.getElement(Config.folderToCreate);
		assertTrue(folder.isDisplayed());

		//delete the folder
		Actions.deleteElement(Config.folderToCreate, filesView, driver);
		//assertFalse(folder.isDisplayed());
		assertNull(filesView.getElement(Config.folderToCreate));

		filesView.pulldownToRefresh();

		Common.waitTillElementIsNotPresentWithoutTimeout(filesView
				.getProgressCircular(), 100);
		//assertFalse(folder.isDisplayed());
		assertNull(filesView.getElement(Config.folderToCreate));

	}

	@Test
	@Category({RegresionTestCategory.class, SmokeTestCategory.class})
	public void testDeleteFolderWithContents () throws Exception {
		deleteFolderWithContentsMethod (driver, common, filesView);
	}

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		FilesView filesView = new FilesView(driver);
		Actions.deleteElement(Config.folderToCreate, filesView, driver);
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}
