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

package com.owncloud.android.test.ui.actions;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.remote.RemoteWebElement;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.owncloud.android.test.ui.models.CertificatePopUp;
import com.owncloud.android.test.ui.models.Drawer;
import com.owncloud.android.test.ui.models.ElementMenuOptions;
import com.owncloud.android.test.ui.models.GmailSendMailView;
import com.owncloud.android.test.ui.models.ShareView;
import com.owncloud.android.test.ui.models.UploadFilesView;
import com.owncloud.android.test.ui.models.LoginForm;
import com.owncloud.android.test.ui.models.FilesView;
import com.owncloud.android.test.ui.models.FolderPopUp;
import com.owncloud.android.test.ui.models.RemoveConfirmationView;
import com.owncloud.android.test.ui.models.SettingsView;
import com.owncloud.android.test.ui.models.WaitAMomentPopUp;
import com.owncloud.android.test.ui.testSuites.Common;
import com.owncloud.android.test.ui.testSuites.Config;

public class Actions {

	public static FilesView login(String url, String user, String password,
			Boolean isTrusted, AndroidDriver driver) 
					throws InterruptedException {
		LoginForm loginForm = new LoginForm(driver);
		CertificatePopUp certificatePopUp = loginForm.typeHostUrl(url);	
		if(!isTrusted){
			WebDriverWait wait = new WebDriverWait(driver, 30);
			//sometimes the certificate has been already accept 
			//and it doesn't appear again
			try {
				wait.until(ExpectedConditions
						.visibilityOf(certificatePopUp.getOkButtonElement()));
				//we need to repaint the screen 
				//because of some element are misplaced
				driver.rotate(ScreenOrientation.LANDSCAPE);
				driver.rotate(ScreenOrientation.PORTRAIT);
				certificatePopUp.clickOnOkButton();
			}catch (NoSuchElementException e) {

			}

		}
		loginForm.typeUserName(user);
		loginForm.typePassword(password);
		//TODO. Assert related to check the connection?
		return loginForm.clickOnConnectButton();
	}

	public static void createFolder(String folderName,
			FilesView filesView, AndroidDriver driver) throws Exception{
		deleteElement(folderName, filesView, driver);
		assertNull(filesView.getElement(folderName));
		FolderPopUp newFolderPopUp = filesView.clickOnNewFolderButton();
		newFolderPopUp.typeNewFolderName(folderName);
		WaitAMomentPopUp waitAMomentPopUp = newFolderPopUp
				.clickOnNewFolderOkButton();
		Common.waitTillElementIsNotPresentWithoutTimeout(waitAMomentPopUp
				.getWaitAMomentTextElement(), 100);
		AndroidElement folder = filesView.getElement(folderName);
		assertNotNull(folder);
		assertTrue(folder.isDisplayed());
	}


	public static AndroidElement scrollTillFindElement (String elementName,
			AndroidElement element, AndroidDriver driver) {
		AndroidElement fileElement;

		if(element.getAttribute("scrollable").equals("true")){
			if(element.getId()=="com.owncloud.android:id/grid_root"){
				driver.scrollTo(elementName);
			}else{
				HashMap<String, String> scrollObject = new HashMap<String,String>();
				scrollObject.put("text", elementName);
				scrollObject.put("element", ( (RemoteWebElement) element).getId());
				driver.executeScript("mobile: scrollTo", scrollObject);
			}
		}
		try {
			fileElement = (AndroidElement) driver
					.findElementByAndroidUIAutomator("new UiSelector()"
							+ ".description(\"LinearLayout-"+ elementName +"\")");
		} catch (NoSuchElementException e) {
			try {
				//if the description is not LinearLayout
				fileElement = (AndroidElement) driver
						.findElementByName(elementName);
			} catch (NoSuchElementException e1) {
				fileElement = null;
			}
		}
		return fileElement;
	}

	public static AndroidElement getElementInFilesView (String elementName, AndroidDriver driver) {
		AndroidElement layout=null, element;
		try {
			layout = (AndroidElement) driver
					.findElementById("com.owncloud.android:id/list_root");

		} catch (NoSuchElementException e) {
			layout = (AndroidElement) driver
					.findElementById("com.owncloud.android:id/grid_root");
		}
		element = Actions.scrollTillFindElement (elementName,layout,driver);
		return element;
	}


	public static LoginForm openDrawerAndDeleteAccount (int accountPosition,FilesView filesView) 
			throws InterruptedException {	
		Drawer drawer = filesView.swipeToShowDrawer();
		SettingsView settingView = drawer.clickOnSettingsButton();
		return deleteAccount(accountPosition,settingView);
	}

	public static LoginForm deleteAccount (int accountPosition, SettingsView settingsView) {
		settingsView.tapOnAccountElement(accountPosition,1, 1000);
		return settingsView.clickOnDeleteAccountElement();
	}

	public static void clickOnMainLayout(AndroidDriver driver){
		driver.tap(1, 0, 0, 1);
	}


	public static AndroidElement deleteElementRemoteAndLocal(String elementName,  
			FilesView filesView, AndroidDriver driver) throws Exception{
		AndroidElement fileElement;
		WaitAMomentPopUp waitAMomentPopUp;
		try{
			fileElement = getElementInFilesView(elementName, driver);
			if(fileElement!=null){
				ElementMenuOptions menuOptions = filesView
						.longPressOnElement(elementName);
				RemoveConfirmationView removeConfirmationView = menuOptions
						.clickOnRemove();
				waitAMomentPopUp = removeConfirmationView
						.clickOnRemoteAndLocalButton();
				Common.waitTillElementIsNotPresent(
						waitAMomentPopUp.getWaitAMomentTextElement(), 100);
			}
		}catch(NoSuchElementException e){
			fileElement=null;
		}
		return fileElement;
	}

	public static AndroidElement deleteElement(String elementName,  
			FilesView filesView, AndroidDriver driver) throws Exception{
		AndroidElement fileElement;
		WaitAMomentPopUp waitAMomentPopUp;
		try{
			fileElement = getElementInFilesView(elementName, driver);
			if(fileElement!=null){
				ElementMenuOptions menuOptions = filesView
						.longPressOnElement(elementName);
				RemoveConfirmationView removeConfirmationView = menuOptions
						.clickOnRemove();
				waitAMomentPopUp = removeConfirmationView
						.clickOnAnyRemoteButton();
				Common.waitTillElementIsNotPresent(
						waitAMomentPopUp.getWaitAMomentTextElement(), 100);
			}
		}catch(NoSuchElementException e){
			fileElement=null;
		}
		return fileElement;
	}

	public static AndroidElement shareLinkElementByGmail(String elementName,  
			FilesView filesView, AndroidDriver driver, Common common) 
					throws Exception{
		try{
			ElementMenuOptions menuOptions = filesView
					.longPressOnElement(elementName);
			ShareView shareView = menuOptions.clickOnShareLinkElement();
			Actions.scrollTillFindElement("Gmail", shareView
					.getListViewLayout(), driver).click();
			GmailSendMailView gmailSendMailView = new GmailSendMailView(driver);
			gmailSendMailView.typeToEmailAdress(Config.gmailAccount);
			gmailSendMailView.clickOnSendButton();
			Common.waitTillElementIsNotPresentWithoutTimeout(filesView
					.getProgressCircular(), 1000);
			common.wait.until(ExpectedConditions.visibilityOf(
					Actions.getElementInFilesView(elementName,driver)
					.findElement(By.id(FilesView
							.getSharedElementIndicator()))));

		}catch(NoSuchElementException e){
			return null;
		}
		return (AndroidElement) Actions
				.getElementInFilesView(elementName,driver)
				.findElement(By.id(FilesView.getSharedElementIndicator()));
	}

	public static AndroidElement shareLinkElementByCopyLink(String elementName,  
			FilesView filesView, AndroidDriver driver, Common common) 
					throws Exception{
		try{
			ElementMenuOptions menuOptions = filesView
					.longPressOnElement(elementName);
			ShareView shareView = menuOptions.clickOnShareLinkElement();
			Actions.scrollTillFindElement("Copy link", shareView.getListViewLayout(), 
					driver).click();
			WaitAMomentPopUp waitAMomentPopUp = new WaitAMomentPopUp(driver);
			Common.waitTillElementIsNotPresentWithoutTimeout(waitAMomentPopUp
					.getWaitAMomentTextElement(), 100);
			common.wait.until(ExpectedConditions.visibilityOf(
					Actions.getElementInFilesView(elementName,driver)
					.findElement(By.id(FilesView.getSharedElementIndicator()))));
		}catch(NoSuchElementException e){
			return null;
		}
		return (AndroidElement) Actions
				.getElementInFilesView(elementName,driver)
				.findElement(By.id(FilesView.getSharedElementIndicator()));
	}


	public static void unshareLinkElement(String elementName,  
			FilesView filesView, AndroidDriver driver, Common common) 
					throws Exception{
		try{
			ElementMenuOptions menuOptions = filesView
					.longPressOnElement(elementName);
			WaitAMomentPopUp waitAMomentPopUp = menuOptions
					.clickOnUnshareLinkElement();
			Common.waitTillElementIsNotPresentWithoutTimeout(waitAMomentPopUp
					.getWaitAMomentTextElement(), 100);
			Common.waitTillElementIsNotPresent((AndroidElement) Actions
					.getElementInFilesView(elementName,driver)
					.findElement(By.id(FilesView.getSharedElementIndicator())
							),100);
		}catch(NoSuchElementException e){

		}
	}


	public static FilesView uploadFile(String elementName,
			FilesView filesView) throws InterruptedException{
		filesView.clickOnUploadButton();
		UploadFilesView uploadFilesView = filesView
				.clickOnFilesElementUploadFile();
		uploadFilesView.tapOnElement(Config.folderWhereFilesToUploadAre);
		Thread.sleep(15000);
		uploadFilesView.clickOnElement(elementName);
		filesView = uploadFilesView.clickOnUploadButton();
		//TO DO. detect when the file is successfully uploaded
		Thread.sleep(15000);
		return filesView; 
	}

	public static FilesView uploadSeveralFile(String elementName,
			String elementName2, String elementName3,FilesView filesView)
					throws InterruptedException{

		filesView.clickOnUploadButton();
		UploadFilesView uploadFilesView = filesView
				.clickOnFilesElementUploadFile();
		uploadFilesView.tapOnElement(Config.folderWhereFilesToUploadAre);
		Thread.sleep(15000);
		uploadFilesView.clickOnElement(elementName);
		uploadFilesView.clickOnElement(elementName2);
		uploadFilesView.clickOnElement(elementName3);

		filesView = uploadFilesView.clickOnUploadButton();
		//TO DO. detect when the file is successfully uploaded
		Thread.sleep(15000);
		return filesView; 
	}

	public static void createContentInsideFolder(String folderWhereCreateContent, 
			String fileToUpload,  String fileToUpload2,  String fileToUpload3,
			String folderToCreate, FilesView filesView, AndroidDriver driver) 
					throws Exception{
		AndroidDriver.ImeHandler ime = driver.manage().ime();
		//access to the folder
		filesView.tapOnElement(folderWhereCreateContent);
		Thread.sleep(1000);

		//upload some files
		filesView = Actions.uploadSeveralFile(fileToUpload,
				fileToUpload2,fileToUpload3, filesView);

		assertTrue(filesView.getElement(fileToUpload).isDisplayed());
		assertTrue(filesView.getElement(fileToUpload2).isDisplayed());
		assertTrue(filesView.getElement(fileToUpload3).isDisplayed());

		assertTrue(filesView.getElement(fileToUpload)
				.findElement(By.id(FilesView.getLocalFileIndicator()))
				.isDisplayed());
		assertTrue(filesView.getElement(fileToUpload2)
				.findElement(By.id(FilesView.getLocalFileIndicator()))
				.isDisplayed());
		assertTrue(filesView.getElement(fileToUpload3)
				.findElement(By.id(FilesView.getLocalFileIndicator()))
				.isDisplayed());

		//create some folder
		ime.activateEngine("io.appium.android.ime/.UnicodeIME");
		createFolder(folderToCreate,filesView, driver);
		ime.activateEngine("com.google.android.inputmethod.latin/"
				+ "com.android.inputmethod.latin.LatinIME");

		filesView.clickOnBackButton();
		AndroidElement folder = filesView.getElement(folderWhereCreateContent);
		assertTrue(folder.isDisplayed());
	}

}
