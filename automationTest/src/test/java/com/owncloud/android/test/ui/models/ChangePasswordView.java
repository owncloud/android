
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


import org.openqa.selenium.support.PageFactory;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;



public class ChangePasswordView {

	final AndroidDriver driver;
	
	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".text(\"Change password\")")
	private AndroidElement changePassword;
	
	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".text(\"Remove account\")")
	private AndroidElement removeAccount;
	
	public ChangePasswordView (AndroidDriver driver) {

		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);

	}

	

	public LoginForm changePasswordButton () {

		changePassword.click();
		LoginForm loginform = new LoginForm(driver);
		return loginform;

	}
	
	public SettingsView removeAccountButton () {

		changePassword.click();
		SettingsView settingsview = new SettingsView (driver);
		return settingsview;

	}

}