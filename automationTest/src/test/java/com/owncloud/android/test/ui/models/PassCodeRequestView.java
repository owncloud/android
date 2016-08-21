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

import static org.junit.Assert.assertTrue;

import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.PageFactory;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

public class PassCodeRequestView {
final AndroidDriver driver;
	
	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".className(\"android.widget.EditText\").index(0)")
	private AndroidElement codeElement1;
	
	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".className(\"android.widget.EditText\").index(1)")
	private AndroidElement codeElement2;
	
	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".className(\"android.widget.EditText\").index(2)")
	private AndroidElement codeElement3;
	
	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".className(\"android.widget.EditText\").index(3)")
	private AndroidElement codeElement4;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".resourceId(\"android:id/action_bar_title\")")
	private AndroidElement titleText;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".text(\"Please, insert your pass code\")")
	private AndroidElement insertMessage;
	
	public PassCodeRequestView (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}
	
	public void enterPasscode(String codeNumber1, String codeNumber2, 
			String codeNumber3, String codeNumber4){
		codeElement1
		   .sendKeys(codeNumber1 + codeNumber2 + codeNumber3 + codeNumber4);
	}
	
	public AndroidElement getTitleTextElement () {
		return titleText;
	}
	
	public AndroidElement getInsertMessage () {
		return insertMessage;
	}

}
