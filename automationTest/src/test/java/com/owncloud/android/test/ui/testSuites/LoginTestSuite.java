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
import com.owncloud.android.test.ui.models.Drawer;
import com.owncloud.android.test.ui.models.LoginForm;
import com.owncloud.android.test.ui.models.FilesView;
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
	@Category({RegresionTestCategory.class})
	public void testLoginPortrait () throws Exception {
		driver.rotate(ScreenOrientation.PORTRAIT);

		FilesView filesView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFilesView(filesView);
	}

	
	
	@Test
	@Category({RegresionTestCategory.class})
	public void testLoginLandscape () throws Exception {
		driver.rotate(ScreenOrientation.LANDSCAPE);
		FilesView filesView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFilesView(filesView);
	}

	
	
	@Test
	@Category({RegresionTestCategory.class})
	public void testLoginWrongPassword () throws Exception {
		LoginForm loginForm = new LoginForm(driver);
		Actions.login(Config.URL, 
				Config.user,Config.password+"wrong", Config.isTrusted, driver);
		assertTrue(common.waitForTextPresent("Wrong username or password", 
				loginForm.getAuthStatusText()));
	}
	
	
	
	public static FilesView loginAndShowFilesMethod (AndroidDriver driver, 
			Common common) throws Exception {
		//driver.rotate(ScreenOrientation.PORTRAIT);

		FilesView filesView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFilesView(filesView);

		//assertTrue(filesView.getElement(Config.fileWhichIsInTheServer1)
			//	.isDisplayed());
		return filesView;
	}

	@Test
	@Category({RegresionTestCategory.class, SmokeTestCategory.class})
	public void testLoginAndShowFiles () throws Exception {
		loginAndShowFilesMethod (driver, common);
	}

	

	@Test
	@Category({RegresionTestCategory.class, SmokeTestCategory.class})
	public void testMultiAccountRotate () throws Exception {
		driver.rotate(ScreenOrientation.LANDSCAPE);
		FilesView filesView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFilesView(filesView);

		driver.rotate(ScreenOrientation.PORTRAIT);

		//filesView.clickOnHomeButton();
		Drawer drawer = filesView.swipeToShowDrawer();
		SettingsView settingsView = drawer.clickOnSettingsButton();

		settingsView.tapOnAddAccount(1, 1000);
		Actions.login(Config.URL2, Config.user2,
				Config.password2, Config.isTrusted2, driver);
		common.assertIsInSettingsView(settingsView);
	}
	

	
	public static FilesView multiAccountAndShowFilesMethod(AndroidDriver driver, 
			Common common) throws InterruptedException{
		driver.rotate(ScreenOrientation.LANDSCAPE);
		FilesView filesView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFilesView(filesView);
		assertTrue(filesView
				.getElement(Config.fileWhichIsInTheServer1).isDisplayed());

		//driver.rotate(ScreenOrientation.PORTRAIT);
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
		return filesView;
	}

	@Test
	@Category({RegresionTestCategory.class, SmokeTestCategory.class})
	public void testMultiAccountAndShowFiles () throws Exception {
		multiAccountAndShowFilesMethod(driver, common); 
	}

	
	
	@Test
	@Category({RegresionTestCategory.class})
	public void testExistingAccountRotate () throws Exception {
		driver.rotate(ScreenOrientation.PORTRAIT);
		FilesView filesView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFilesView(filesView);

		driver.rotate(ScreenOrientation.LANDSCAPE);
		Drawer drawer = filesView.swipeToShowDrawer();
		SettingsView settingsView = drawer.clickOnSettingsButton();
		settingsView.tapOnAddAccount(1, 1000);

		LoginForm loginForm = new LoginForm(driver);
		filesView = Actions.login(Config.URL, Config.user,Config.password, 
				Config.isTrusted, driver);	
		assertTrue(common.waitForTextPresent("An account for the same user and"
				+ " server already exists in the device", 
				loginForm.getAuthStatusText()));
	}

	
	
	@Test
	@Category({RegresionTestCategory.class})
	public void testChangePasswordWrong () throws Exception {
		driver.rotate(ScreenOrientation.PORTRAIT);
		FilesView filesView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFilesView(filesView);
		Drawer drawer = filesView.swipeToShowDrawer();
		SettingsView settingsView = drawer.clickOnSettingsButton();
		settingsView.tapOnAccountElement(1,1, 1000);
		LoginForm changePasswordForm = settingsView
				.clickOnChangePasswordElement();
		changePasswordForm.typePassword("WrongPassword");
		changePasswordForm.clickOnConnectButton();
		assertTrue(common.waitForTextPresent("Wrong username or password", 
				changePasswordForm.getAuthStatusText()));
	}
	
	
	
	public void loginLogoutAndLoginMethod (AndroidDriver driver, 
			Common common) throws Exception {
		FilesView filesView = loginAndShowFilesMethod (driver, common);
		LogoutTestSuite.logoutMethod (driver, common, filesView);
		loginAndShowFilesMethod (driver, common);
	}
	
	
	@Test
	@Category({UnfinishedTestCategory.class})
	public void testLoginLogoutAndLogin () throws Exception {
		//TODO. The second time it is gone directly to settings
		loginLogoutAndLoginMethod (driver, common);
	}


	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}


}
