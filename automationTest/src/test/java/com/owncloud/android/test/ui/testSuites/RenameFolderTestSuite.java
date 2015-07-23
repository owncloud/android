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
import com.owncloud.android.test.ui.models.ElementMenuOptions;
import com.owncloud.android.test.ui.models.FileListView;
import com.owncloud.android.test.ui.models.FolderPopUp;
import com.owncloud.android.test.ui.models.WaitAMomentPopUp;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RenameFolderTestSuite{

	AndroidDriver driver;
	Common common;
	private String CurrentCreatedFolder = "";

	@Rule public TestName name = new TestName();


	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
	}

	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class, InProgressCategory.class})
	public void testRenameFolder () throws Exception {
		WaitAMomentPopUp waitAMomentPopUp = null;
		FileListView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView(fileListView);

		//if the folder already exists, do no created
		AndroidElement folder = fileListView.getFileElement(Config.folderBeforeRename);
		if(folder==null){
			//create the folder to rename
			waitAMomentPopUp = Actions
					.createFolder(Config.folderBeforeRename, fileListView);
			Common.waitTillElementIsNotPresentWithoutTimeout(
					waitAMomentPopUp.getWaitAMomentTextElement(), 100);
			folder = fileListView.getFileElement(Config.folderBeforeRename);
		}

		assertTrue(folder.isDisplayed());
		CurrentCreatedFolder = Config.folderBeforeRename;
		
		//check if the folder with the new name already exists 
		//and if true, delete them
		Actions.deleteElement(Config.folderToRename, fileListView, driver);

		ElementMenuOptions menuOptions = fileListView
				.longPressOnElement(Config.folderBeforeRename);
		FolderPopUp FolderPopUp = menuOptions.clickOnRename();
		FolderPopUp.typeNewFolderName(Config.folderToRename);
		FolderPopUp.clickOnNewFolderOkButton();
		Common.waitTillElementIsNotPresentWithoutTimeout(waitAMomentPopUp
				.getWaitAMomentTextElement(), 100);
		folder = fileListView.getFileElement(Config.folderToRename);
		assertNotNull(folder);
		assertTrue(folder.isDisplayed());	
		assertEquals(Config.folderToRename , folder.getText());
		CurrentCreatedFolder = Config.folderBeforeRename;
	}

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		
		FileListView fileListView = new FileListView(driver);
		Actions.deleteElement(CurrentCreatedFolder, fileListView, driver);
		
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}
