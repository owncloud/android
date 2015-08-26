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

public final class Config {

	//without http or https
	public static final String URL = "owncloudServerVar";
	public static boolean isTrusted = true;

	//without http or https
	public static final String URL2 = "owncloudServer2Var";
	public static boolean isTrusted2 = true;

	public static final String user = "owncloudUserVar";
	public static final String password = "owncloudPasswordVar";
	public static final String user2 = "owncloudUser2Var";
	public static final String password2 = "owncloudPassword2Var";
	public static final String userAccount = user + "@"+ URL;
	public static final String userAccount2 = user2 + "@"+ URL2;

	public static final String gmailAccount = "automationOwncloud@gmail.com";

	public static final String fileWhichIsInTheServer1 ="ownCloudUserManual.pdf";
	public static final String fileWhichIsInTheServer2 ="ownCloudUserManual.pdf";

	public static final String folderWhereFilesToUploadAre = "ocAutomation";
	public static final String fileToTest = "doc.txt";
	public static final String fileToTest2 = "docümento.txt";
	public static final String fileToTest3 = "año.pdf";
	public static final String fileToTestSendByEmail = "test.jpg";
	public static final String bigFileToTest = "video.mp4";
	public static final String fileToRename = "newNÁme";

	public static final String folderToCreate="testCreateFolder";
    public static final String folderToCreateSpecialCharacters="a%&@()ño";
    public static final String folderWhereMove="foldérWhereMove";
    public static final String folderToMove="folderTöMove";
    public static final String folderBeforeRename="folderBeforeRèname";
    public static final String folderToRename="folderToRenáme";

	public static final String passcode1 = "1";
	public static final String passcode2 = "1";
	public static final String passcode3 = "2";
	public static final String passcode4 = "2";
}
