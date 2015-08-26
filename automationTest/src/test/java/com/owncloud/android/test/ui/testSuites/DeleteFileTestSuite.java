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
import com.owncloud.android.test.ui.models.FilesView;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DeleteFileTestSuite{

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
	
	public static void deleteFileRemoteAndLocalMethod (AndroidDriver driver, 
			Common common, FilesView filesView) throws Exception {		
		//if the file already exists, delete it remote
		AndroidElement file = filesView.getElement(Config.fileToTest);
		if(file!=null){
			Actions.deleteElement(Config.fileToTest,filesView, driver);
			assertNull(filesView.getElement(Config.fileToTest));
		}
		//now we are sure that we are going to delete it remote and locally
		filesView = Actions.uploadFile(Config.fileToTest, filesView);

		Common.waitTillElementIsNotPresentWithoutTimeout(
				filesView.getProgressCircular(), 1000);

		common.wait.until(ExpectedConditions.visibilityOf(
				filesView.getElement(Config.fileToTest)
				.findElement(By.id(FilesView.getLocalFileIndicator()))));
		
		file = filesView.getElement(Config.fileToTest);
		
		assertTrue(file.isDisplayed());

		Actions.deleteElementRemoteAndLocal(Config.fileToTest,filesView,
				driver);
		
		common.assertIsInFilesView(filesView);
		assertNull(filesView.getElement(Config.fileToTest));
	}

	@Test
	@Category({RegresionTestCategory.class, SmokeTestCategory.class})
	public void testDeleteFileRemoteAndLocal () throws Exception {		
		deleteFileRemoteAndLocalMethod (driver, common, filesView);
	}
	
	//TODO. Delete local and delete remote

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}