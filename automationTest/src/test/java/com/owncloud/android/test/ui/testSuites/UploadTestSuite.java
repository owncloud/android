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
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.owncloud.android.test.ui.actions.Actions;
import com.owncloud.android.test.ui.groups.*;
import com.owncloud.android.test.ui.models.GmailEmailListView;
import com.owncloud.android.test.ui.models.GmailEmailView;
import com.owncloud.android.test.ui.models.ImageViewFromOtherApp;
import com.owncloud.android.test.ui.models.FilesView;
import com.owncloud.android.test.ui.models.NotificationView;
import com.owncloud.android.test.ui.models.UploadView;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Category({RegresionTestCategory.class})
public class UploadTestSuite{

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


	public void uploadFile(FilesView filesView, String file) 
			throws Exception{
		//check if the file already exists and if true, delete it
		Actions.deleteElement(file, filesView, driver);

		filesView = Actions.uploadFile(file, filesView);

		assertTrue(filesView.getElement(file).isDisplayed());

		Common.waitTillElementIsNotPresentWithoutTimeout(
				filesView.getProgressCircular(), 1000);

		common.wait.until(ExpectedConditions.visibilityOf(
				filesView.getElement(file)
				.findElement(By.id(FilesView.getLocalFileIndicator()))));

		assertTrue(filesView.getElement(file)
				.findElement(By.id(FilesView.getLocalFileIndicator()))
				.isDisplayed());
	}

	@Test
	@Category({RegresionTestCategory.class, SmokeTestCategory.class})
	public void testUploadFile () throws Exception {
		uploadFile(filesView, Config.fileToTest);
	}

	@Test
	@Category({RegresionTestCategory.class, SmokeTestCategory.class})
	public void testUploadFileWithSpecialCharacters () throws Exception {
		uploadFile(filesView, Config.fileToTest2);
	}

	@Test
	@Category({RegresionTestCategory.class, SmokeTestCategory.class})
	public void testUploadSeveralFile () throws Exception {
		//check if the file already exists and if true, delete it
		Actions.deleteElement(Config.fileToTest, filesView, driver);
		Actions.deleteElement(Config.fileToTest2, filesView, driver);
		Actions.deleteElement(Config.fileToTest3, filesView, driver);

		filesView = Actions.uploadSeveralFile(Config.fileToTest,
				Config.fileToTest2, Config.fileToTest3, filesView);

		assertTrue(filesView.getElement(Config.fileToTest).isDisplayed());
		assertTrue(filesView.getElement(Config.fileToTest2).isDisplayed());
		assertTrue(filesView.getElement(Config.fileToTest3).isDisplayed());

		Common.waitTillElementIsNotPresentWithoutTimeout(
				filesView.getProgressCircular(), 1000);

		common.wait.until(ExpectedConditions.visibilityOf(
				filesView.getElement(Config.fileToTest)
				.findElement(By.id(FilesView.getLocalFileIndicator()))));

		common.wait.until(ExpectedConditions.visibilityOf(
				filesView.getElement(Config.fileToTest2)
				.findElement(By.id(FilesView.getLocalFileIndicator()))));

		common.wait.until(ExpectedConditions.visibilityOf(
				filesView.getElement(Config.fileToTest3)
				.findElement(By.id(FilesView.getLocalFileIndicator()))));

		assertTrue(filesView.getElement(Config.fileToTest)
				.findElement(By.id(FilesView.getLocalFileIndicator()))
				.isDisplayed());

		assertTrue(filesView.getElement(Config.fileToTest2)
				.findElement(By.id(FilesView.getLocalFileIndicator()))
				.isDisplayed());

		assertTrue(filesView.getElement(Config.fileToTest3)
				.findElement(By.id(FilesView.getLocalFileIndicator()))
				.isDisplayed());

	}

	@Test
	@Category({UnfinishedTestCategory.class})
	public void testUploadBigFile () throws Exception {

		//check if the file already exists and if true, delete it
		Actions.deleteElement(Config.bigFileToTest, filesView, driver);

		filesView = Actions.uploadFile(Config.bigFileToTest, filesView);


		driver.openNotifications();
		NotificationView notificationView = new NotificationView(driver);

		try{
			if(notificationView.getUploadingNotification().isDisplayed()){
				Common.waitTillElementIsPresent(
						notificationView.getUploadSucceededNotification(),
						300000);

				driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_HOME);
				driver.startActivity("com.owncloud.android", 
						".ui.activity.FileDisplayActivity");

			}
		} catch (NoSuchElementException e) {
			driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_HOME);
			driver.startActivity("com.owncloud.android", 
					".ui.activity.FileDisplayActivity");
		}

		assertTrue(filesView.getElement(Config.bigFileToTest).isDisplayed());

		Common.waitTillElementIsNotPresentWithoutTimeout(
				filesView.getProgressCircular(), 1000);
		common.wait.until(ExpectedConditions.visibilityOf(
				filesView.getElement(Config.bigFileToTest)
				.findElement(By.id(FilesView.getLocalFileIndicator()))));
		assertTrue(filesView.getElement(Config.bigFileToTest)
				.findElement(By.id(FilesView.getLocalFileIndicator()))
				.isDisplayed());
		filesView = new FilesView(driver);
		assertTrue(filesView.getElement(Config.bigFileToTest)
				.isDisplayed());
	}


	@Test
	@Category({RegresionTestCategory.class})
	public void testUploadFromGmail () throws Exception {

		driver.startActivity("com.google.android.gm",
				".ConversationListActivityGmail");
		GmailEmailListView gmailEmailListView = new GmailEmailListView(driver);
		Thread.sleep(5000);
		GmailEmailView gmailEmailView = gmailEmailListView.clickOnEmail();
		ImageViewFromOtherApp imageView = gmailEmailView.clickOnfileButton();
		Thread.sleep(2000);
		imageView.clickOnOptionsButton();
		imageView.clickOnShareButton();
		imageView.clickOnOwnCloudButton();
		//justonce button do not appear always
		try{
			imageView.clickOnJustOnceButton();
		}catch (NoSuchElementException e) {
		}
		UploadView uploadView = new UploadView(driver);
		uploadView.clickOUploadButton();
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_HOME);
		driver.startActivity("com.owncloud.android",
				".ui.activity.FileDisplayActivity");

		filesView = new FilesView(driver);
		assertTrue(filesView
				.getElement(Config.fileToTestSendByEmail).isDisplayed());
	}


	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		FilesView filesView = new FilesView(driver);

		Actions.deleteElement(Config.fileToTestSendByEmail,filesView,
				driver);
		Actions.deleteElement(Config.fileToTest,filesView, driver);
		Actions.deleteElement(Config.fileToTest2,filesView, driver);
		Actions.deleteElement(Config.fileToTest3,filesView, driver);
		Actions.deleteElement(Config.bigFileToTest,filesView, driver);

		driver.removeApp("com.owncloud.android");
		driver.quit();
	}


}

