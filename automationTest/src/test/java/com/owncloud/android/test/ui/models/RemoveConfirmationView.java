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

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.PageFactory;


public class RemoveConfirmationView {
	final AndroidDriver driver;

	@AndroidFindBy(name = "Remote and local")
	private AndroidElement remoteAndLocalButton;

	@AndroidFindBy(name = "Remove from server")
	private AndroidElement remoteButton;

	@AndroidFindBy(name = "Local only")
	private AndroidElement localButton;

	public RemoveConfirmationView (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}

	public WaitAMomentPopUp clickOnAnyRemoteButton () {
		
		try {
			remoteAndLocalButton = (AndroidElement) driver
					.findElementByName("Remote and local");
		} catch (NoSuchElementException e) {
			remoteAndLocalButton = null;
		}
		try {
			remoteButton = (AndroidElement) driver
					.findElementByName("Remove from server");
		} catch (NoSuchElementException e) {
			remoteButton = null;
		}
		
		if(remoteAndLocalButton != null){
			remoteAndLocalButton.click();
		}else if(remoteButton!= null){
			remoteButton.click();
		}
		
		WaitAMomentPopUp waitAMomentPopUp = new WaitAMomentPopUp(driver);
		return waitAMomentPopUp;
	}

	public WaitAMomentPopUp clickOnRemoteAndLocalButton () {
		remoteAndLocalButton.click();
		WaitAMomentPopUp waitAMomentPopUp = new WaitAMomentPopUp(driver);
		return waitAMomentPopUp;
	}

	public WaitAMomentPopUp clickOnRemoteButton () {
		remoteButton.click();
		WaitAMomentPopUp waitAMomentPopUp = new WaitAMomentPopUp(driver);
		return waitAMomentPopUp;
	}

	public WaitAMomentPopUp clickOnLocalButton () {
		localButton.click();
		WaitAMomentPopUp waitAMomentPopUp = new WaitAMomentPopUp(driver);
		return waitAMomentPopUp;
	}
}
