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