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
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.openqa.selenium.ScreenOrientation;

import com.owncloud.android.test.ui.actions.Actions;
import com.owncloud.android.test.ui.groups.*;
import com.owncloud.android.test.ui.models.LoginForm;
import com.owncloud.android.test.ui.models.FilesView;
import com.owncloud.android.test.ui.models.MenuList;
import com.owncloud.android.test.ui.models.SettingsView;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LoginTestSuite{
	AndroidDriver driver;
	Common common;
	
	@Rule public TestName name = new TestName();
	
	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
	}
	
	@Test
	@Category({NoIgnoreTestCategory.class})
	public void testLoginPortrait () throws Exception {
		driver.rotate(ScreenOrientation.PORTRAIT);
		
		FilesView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView(fileListView);
	}
	
	@Test
	@Category({NoIgnoreTestCategory.class})
	public void testLoginLandscape () throws Exception {
		driver.rotate(ScreenOrientation.LANDSCAPE);
		FilesView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView(fileListView);
	}
	
	@Test
	@Category({NoIgnoreTestCategory.class})
	public void testLoginWrongPassword () throws Exception {
		LoginForm loginForm = new LoginForm(driver);
		Actions.login(Config.URL, 
				Config.user,Config.password+"wrong", Config.isTrusted, driver);
		assertTrue(common.waitForTextPresent("Wrong username or password", 
				loginForm.getAuthStatusText()));
	}
	
	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testLoginAndShowFiles () throws Exception {
		driver.rotate(ScreenOrientation.PORTRAIT);
		
		FilesView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView(fileListView);
		
		assertTrue(fileListView.getElement(Config.fileWhichIsInTheServer1)
				.isDisplayed());
	}
	
	
	
	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testMultiAccountRotate () throws Exception {
		driver.rotate(ScreenOrientation.LANDSCAPE);
		FilesView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView(fileListView);
		
		driver.rotate(ScreenOrientation.PORTRAIT);
		MenuList menu = fileListView.clickOnMenuButton();
		SettingsView settingsView = menu.clickOnSettingsButton();
		
		settingsView.tapOnAddAccount(1, 1000);
		Actions.login(Config.URL2, Config.user2,
				Config.password2, Config.isTrusted2, driver);
		common.assertIsInSettingsView(settingsView);
	}
	
	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class })
	public void testMultiAccountAndShowFiles () throws Exception {
		driver.rotate(ScreenOrientation.LANDSCAPE);
		FilesView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView(fileListView);
		assertTrue(fileListView
				.getElement(Config.fileWhichIsInTheServer1).isDisplayed());
		
		driver.rotate(ScreenOrientation.PORTRAIT);
		MenuList menu = fileListView.clickOnMenuButton();
		SettingsView settingsView = menu.clickOnSettingsButton();
		
		settingsView.tapOnAddAccount(1, 1000);
		Actions.login(Config.URL2, Config.user2,
				Config.password2, Config.isTrusted2, driver);
		common.assertIsInSettingsView(settingsView);
		settingsView.tapOnAccountElement(2,1, 100);
		common.assertIsInFileListView(fileListView);
		
		assertTrue(fileListView.getElement(Config.fileWhichIsInTheServer2)
				.isDisplayed());
	}
	
	@Test
	@Category({NoIgnoreTestCategory.class})
	public void testExistingAccountRotate () throws Exception {
		driver.rotate(ScreenOrientation.PORTRAIT);
		FilesView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView(fileListView);
		
		driver.rotate(ScreenOrientation.LANDSCAPE);
		MenuList menu = fileListView.clickOnMenuButton();
		SettingsView settingsView = menu.clickOnSettingsButton();
		settingsView.tapOnAddAccount(1, 1000);
		
		LoginForm loginForm = new LoginForm(driver);
		fileListView = Actions.login(Config.URL, Config.user,Config.password, 
				Config.isTrusted, driver);	
		assertTrue(common.waitForTextPresent("An account for the same user and"
				+ " server already exists in the device", 
				loginForm.getAuthStatusText()));
	}
	
	@Test
	@Category({NoIgnoreTestCategory.class})
	public void testChangePasswordWrong () throws Exception {
		driver.rotate(ScreenOrientation.PORTRAIT);
		FilesView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView(fileListView);
		MenuList menu = fileListView.clickOnMenuButton();
		SettingsView settingsView = menu.clickOnSettingsButton();
		settingsView.tapOnAccountElement(1,1, 1000);
		LoginForm changePasswordForm = settingsView
				.clickOnChangePasswordElement();
		changePasswordForm.typePassword("WrongPassword");
		changePasswordForm.clickOnConnectButton();
		assertTrue(common.waitForTextPresent("Wrong username or password", 
				changePasswordForm.getAuthStatusText()));
	}
	

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}
	
	
}
