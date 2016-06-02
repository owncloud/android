
/**
 *   ownCloud Android client application
 *
 *   @author jesmrec
 *   Copyright (C) 2016 ownCloud Inc.
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

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

import org.openqa.selenium.support.PageFactory;

import com.owncloud.android.test.ui.testSuites.Config;

public class AllowDenyView {

	final AndroidDriver driver;	
	
	/*@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".resourceId(\"com.android.packageinstaller:id/permission_allow_button\")")*/
	@AndroidFindBy(name = "Allow")
	private AndroidElement allowButton;
	
	/*@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".resourceId(\"com.android.packageinstaller:id/permission_deny_button\")")*/
	@AndroidFindBy(name = "Deny")
	private AndroidElement denyButton;
	
	public AllowDenyView (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}
	
	public void clickOnAcceptButton () {
		allowButton.click();
		//FileListView fileListView = new FileListView(driver);
		//return fileListView;
	}
	
	public void clickOnDenyButton () {
		denyButton.click();
		//FileListView fileListView = new FileListView(driver);
		//return fileListView;
	}
	
	
	public AndroidElement getAllowButtonElement () {
		return allowButton;
	}
	
	public AndroidElement getDenyButtonElement () {
		return denyButton;
	}
	
}