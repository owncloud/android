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
	public static final String URL = "docker.oc.solidgear.es:56219";
	public static boolean isTrusted = false;

	//without http or https
	public static final String URL2 = "docker.oc.solidgear.es:56066";
	public static boolean isTrusted2 = false;

	public static final String user = "user1";
	public static final String password = "a";
	public static final String user2 = "user2";
	public static final String password2 = "a";
	public static final String userAccount = user + "@"+ URL;
	public static final String userAccount2 = user2 + "@"+ URL2;

	public static final String gmailAccount = "gmailAccountVar";
	
	public static final String fileWhichIsInTheServer1 ="ok.txt";
	public static final String fileWhichIsInTheServer2 ="algo.txt";
	
	public static final String fileToTestName = "test";
	public static final String fileToTestSendByEmailName = "test";
	public static final String bigFileToTestName = "test";
	
	public static final String passcode1 = "passcode1";
	public static final String passcode2 = "passcode2";
	public static final String passcode3 = "passcode3";
	public static final String passcode4 = "passcode4";

}
