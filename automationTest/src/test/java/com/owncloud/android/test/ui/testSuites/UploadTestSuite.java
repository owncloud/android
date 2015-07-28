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
@Category({NoIgnoreTestCategory.class})
public class UploadTestSuite{

	AndroidDriver driver;
	Common common;

	@Rule public TestName name = new TestName();


	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
	}


	public void uploadFile(FilesView fileListView, String file) 
			throws Exception{
		//check if the file already exists and if true, delete it
		Actions.deleteElement(file, fileListView, driver);

		fileListView = Actions.uploadFile(file, fileListView);

		assertTrue(fileListView.getElement(file).isDisplayed());

		Common.waitTillElementIsNotPresentWithoutTimeout(
				fileListView.getProgressCircular(), 1000);

		common.wait.until(ExpectedConditions.visibilityOf(
				fileListView.getElement(file)
				.findElement(By.id(FilesView.getLocalFileIndicator()))));

		assertTrue(fileListView.getElement(file)
				.findElement(By.id(FilesView.getLocalFileIndicator()))
				.isDisplayed());
	}

	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testUploadFile () throws Exception {
		FilesView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView(fileListView);
		uploadFile(fileListView, Config.fileToTest);
	}

	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testUploadFileWithSpecialCharacters () throws Exception {
		FilesView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView(fileListView);
		uploadFile(fileListView, Config.fileToTest2);
	}

	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testUploadSeveralFile () throws Exception {
		FilesView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView(fileListView);

		//check if the file already exists and if true, delete it
		Actions.deleteElement(Config.fileToTest, fileListView, driver);
		Actions.deleteElement(Config.fileToTest2, fileListView, driver);
		Actions.deleteElement(Config.fileToTest3, fileListView, driver);

		fileListView = Actions.uploadSeveralFile(Config.fileToTest,
				Config.fileToTest2, Config.fileToTest3, fileListView);

		assertTrue(fileListView
				.getElement(Config.fileToTest).isDisplayed());
		assertTrue(fileListView
				.getElement(Config.fileToTest2).isDisplayed());
		assertTrue(fileListView.getElement(Config.fileToTest3).isDisplayed());

		Common.waitTillElementIsNotPresentWithoutTimeout(
				fileListView.getProgressCircular(), 1000);

		common.wait.until(ExpectedConditions.visibilityOf(
				fileListView.getElement(Config.fileToTest)
				.findElement(By.id(FilesView.getLocalFileIndicator()))));

		common.wait.until(ExpectedConditions.visibilityOf(
				fileListView.getElement(Config.fileToTest2)
				.findElement(By.id(FilesView.getLocalFileIndicator()))));

		common.wait.until(ExpectedConditions.visibilityOf(
				fileListView.getElement(Config.fileToTest3)
				.findElement(By.id(FilesView.getLocalFileIndicator()))));

		assertTrue(fileListView.getElement(Config.fileToTest)
				.findElement(By.id(FilesView.getLocalFileIndicator()))
				.isDisplayed());

		assertTrue(fileListView.getElement(Config.fileToTest2)
				.findElement(By.id(FilesView.getLocalFileIndicator()))
				.isDisplayed());

		assertTrue(fileListView.getElement(Config.fileToTest3)
				.findElement(By.id(FilesView.getLocalFileIndicator()))
				.isDisplayed());

		//assertTrue(fileListViewAfterUploadFile
		//	.getFileElement(Config.fileToTest).isDisplayed());
	}

	@Test
	@Category({UnfinishedTestCategory.class})
	public void testUploadBigFile () throws Exception {

		FilesView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView(fileListView);

		//check if the file already exists and if true, delete it
		Actions.deleteElement(Config.bigFileToTest, fileListView, driver);

		fileListView = Actions.uploadFile(Config.bigFileToTest, fileListView);


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

		assertTrue(fileListView.getElement(Config.bigFileToTest).isDisplayed());

		Common.waitTillElementIsNotPresentWithoutTimeout(
				fileListView.getProgressCircular(), 1000);
		common.wait.until(ExpectedConditions.visibilityOf(
				fileListView.getElement(Config.bigFileToTest)
				.findElement(By.id(FilesView.getLocalFileIndicator()))));
		assertTrue(fileListView.getElement(Config.bigFileToTest)
				.findElement(By.id(FilesView.getLocalFileIndicator()))
				.isDisplayed());
		fileListView = new FilesView(driver);
		assertTrue(fileListView.getElement(Config.bigFileToTest)
				.isDisplayed());
	}


	@Test
	@Category({NoIgnoreTestCategory.class})
	public void testUploadFromGmail () throws Exception {
		FilesView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView(fileListView);
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
		
		fileListView = new FilesView(driver);
		assertTrue(fileListView
				.getElement(Config.fileToTestSendByEmail).isDisplayed());
	}


	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		FilesView fileListView = new FilesView(driver);

		Actions.deleteElement(Config.fileToTestSendByEmail,fileListView,
				driver);
		Actions.deleteElement(Config.fileToTest,fileListView, driver);
		Actions.deleteElement(Config.fileToTest2,fileListView, driver);
		Actions.deleteElement(Config.fileToTest3,fileListView, driver);
		Actions.deleteElement(Config.bigFileToTest,fileListView, driver);

		driver.removeApp("com.owncloud.android");
		driver.quit();
	}


}

