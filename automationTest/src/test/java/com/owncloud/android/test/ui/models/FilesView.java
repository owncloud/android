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

package com.owncloud.android.test.ui.models;

import java.util.List;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.android.AndroidKeyCode;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.Point;

import com.owncloud.android.test.ui.actions.Actions;

public class FilesView {
	final AndroidDriver driver;

	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".description(\"More options\")")
	private AndroidElement menuButton;


	//this description is shown when list or grid view. 
	//Maybe it should be change
	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".description(\"List Layout\")")
	private AndroidElement filesView;

	@CacheLookup
	@AndroidFindBy(id = "com.owncloud.android:id/list_root")
	private AndroidElement listLayout;

	@AndroidFindBy(id = "com.owncloud.android:id/grid_root")
	private AndroidElement gridLayout;
	
	@AndroidFindBy(id = "android:id/home")
	private AndroidElement home;

	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".resourceId(\"android:id/action_bar_title\")")
	private AndroidElement titleText;

	@AndroidFindBy(id = "android:id/progress_circular")
	private AndroidElement progressCircular;

	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".description(\"New folder\")")
	private AndroidElement newFolderButton;

	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"Upload\")")
	private AndroidElement uploadButton;

	private AndroidElement waitAMomentText;

	@AndroidFindBy(id = "com.owncloud.android:id/ListItemLayout")
	private List<AndroidElement> listItemLayout;

	@AndroidFindBy(id = "com.owncloud.android:id/list_root")
	private AndroidElement listRootLayout;

	@AndroidFindBy(name = "Files")
	private AndroidElement filesElementUploadFile;

	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".className(\"android.widget.FrameLayout\").index(0)")
	private AndroidElement deviceScreen;

	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".description(\"ownCloud, Navigate up\")")
	private AndroidElement backButton;


	//private AndroidElement fileElement;

	private AndroidElement fileElementLayout;


	private static String localFileIndicator = 
			"com.owncloud.android:id/localFileIndicator";
	private static String favoriteFileIndicator = 
			"com.owncloud.android:id/favoriteIcon";
	private static String sharedElementIndicator = 
			"com.owncloud.android:id/sharedIcon";


	public FilesView (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}

	public TopbarMenu clickOnTopbarMenuButton () {
		//if the menu option is not in the actionBar, it is opening again
		try {
			menuButton.click();
		} catch (NoSuchElementException e){
			driver.sendKeyEvent(AndroidKeyCode.MENU);
		}
		TopbarMenu topbarMenu = new TopbarMenu (driver);
		return topbarMenu;
	}

	public SettingsView getSettingsView () {
		SettingsView settingsView = new SettingsView(driver);
		return settingsView;
	}

	public FolderPopUp clickOnNewFolderButton () {
		newFolderButton.click();
		FolderPopUp newFolderPopUp = new FolderPopUp(driver);
		return newFolderPopUp;
	}

	public void clickOnUploadButton () {
		uploadButton.click();
	}

	public void clickOnBackButton () {
		backButton.click();
	}

	public UploadFilesView clickOnFilesElementUploadFile () {
		filesElementUploadFile.click();
		UploadFilesView uploadFilesView = new UploadFilesView(driver);
		return uploadFilesView;
	}

	public AndroidElement getTitleTextElement () {
		return titleText;
	}

	public AndroidElement getUploadButton () {
		return uploadButton;
	}

	public AndroidElement getWaitAMomentTextElement () {
		return waitAMomentText;
	}

	public AndroidElement getListRootElement () {
		return listRootLayout;
	}

	public List<AndroidElement> getListItemLayout () {
		return listItemLayout;
	}

	public ElementMenuOptions longPressOnElement (String elementName) {
		Actions.getElementInFilesView(elementName,driver).tap(1, 1000);
		ElementMenuOptions menuOptions = new ElementMenuOptions(driver);
		return menuOptions;
	}


	public void tapOnElement (String elementName) {
		Actions.getElementInFilesView(elementName,driver).tap(1, 1);
	}


	public AndroidElement getElement (String elementName) {
		fileElementLayout = Actions.getElementInFilesView(elementName, driver);
		return fileElementLayout;
	}

	public AndroidElement getProgressCircular () {
		return progressCircular;
	}

	public static String getLocalFileIndicator() {
		return localFileIndicator;
	}

	public static String getFavoriteFileIndicator() {
		return favoriteFileIndicator;
	}

	public static String getSharedElementIndicator() {
		return sharedElementIndicator;
	}
	
	public void pulldownToRefresh () throws InterruptedException {
		Point listLocation = filesView.getLocation();
		driver.swipe(listLocation.getX(),listLocation.getY(), 
				listLocation.getX(),listLocation.getY()+1000, 5000);
	}

	public void pulldownToSeeNotification () throws InterruptedException {
		Point listLocation = deviceScreen.getLocation();
		driver.swipe(listLocation.getX(),listLocation.getY(), 
				listLocation.getX(),listLocation.getY()+1000, 5000);
	}
	
	public void clickOnHomeButton () {
		home.click();
	}

	public Drawer swipeToShowDrawer () throws InterruptedException {
		Point listLocation = filesView.getLocation();
		Dimension dimensions = filesView.getSize(); 
		driver.swipe(listLocation.getX()+1,
				listLocation.getY() + dimensions.getHeight()/2,
				listLocation.getX()+(dimensions.getWidth()/2),
				listLocation.getY() + dimensions.getHeight()/2, 200);
		Drawer drawer = new Drawer(driver);
		return drawer;
	}
	

	
	
	

	public boolean isGridView () {

		try {
			gridLayout = (AndroidElement) driver
					.findElementById("com.owncloud.android:id/grid_root");
		} catch (NoSuchElementException e) {
			gridLayout = null;
		}

		if(gridLayout != null){
			return true;
		}else{
			return false;
		}

	}

}
