/**
 *   ownCloud Android client application
 *

 *   Copyright (C) 2016 ownCloud GmbH.
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

package com.owncloud.android.authentication;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Build;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.test.suitebuilder.annotation.LargeTest;

import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertTrue;
import com.owncloud.android.R;
import com.owncloud.android.utils.AccountsManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.authentication.AccountAuthenticator.AuthenticatorException;

import org.junit.Before;
import org.junit.After;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.lang.reflect.Field;

import android.app.Activity;

import android.util.Log;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;

import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static org.hamcrest.Matchers.not;


@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@LargeTest
public class AuthenticatorActivityTest {

    public static final String EXTRA_ACTION = "ACTION";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";

    private static final int WAIT_INITIAL = 500;
    private static final int WAIT_LOGIN = 2000;
    private static final int WAIT_CONNECTION = 2000;

    private static final String ERROR_MESSAGE = "Activity not finished";
    private static final String SUFFIX_BROWSER = "/index.php/apps/files/";
    private static final String RESULT_CODE = "mResultCode";
    private static final String LOG_TAG = "LoginSuite";
    private static final String USER_INEXISTENT = "userinexistent";

    private Context targetContext = null;

    private String testUser = null;
    private String testUser2 = null;
    private String testPassword = null;
    private String testPassword2 = null;
    private String testServerURL = null;
    private String testServerPort = null;
    private String testServerPortSecure = null;
    int trusted = -1;

    @Rule
    public ActivityTestRule<AuthenticatorActivity> mActivityRule = new ActivityTestRule<AuthenticatorActivity>(
            AuthenticatorActivity.class) {
        @Override
        protected Intent getActivityIntent() {

            targetContext = InstrumentationRegistry.getInstrumentation()
                    .getTargetContext();
            Intent result = new Intent(targetContext, AuthenticatorActivity.class);
            result.putExtra(EXTRA_ACTION, AuthenticatorActivity.ACTION_CREATE);
            result.putExtra(EXTRA_ACCOUNT, "");
            return result;
        }
    };

    @Before
    public void init() {
        Bundle arguments = InstrumentationRegistry.getArguments();

        testUser = arguments.getString("TEST_USER");
        testUser2 = arguments.getString("TEST_USER2");
        testPassword = arguments.getString("TEST_PASSWORD");
        testPassword2 = arguments.getString("TEST_PASSWORD2");
        testServerURL = arguments.getString("TEST_SERVER_URL");
        testServerPort = arguments.getString("TEST_SERVER_PORT");
        testServerPortSecure = arguments.getString("TEST_SERVER_PORT_SECURE");
        trusted = Integer.parseInt(arguments.getString("TRUSTED"));

         // UiDevice available form API level 17
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            /*Point[] coordinates = new Point[4];
            coordinates[0] = new Point(248, 1020);
            coordinates[1] = new Point(248, 429);
            coordinates[2] = new Point(796, 1020);
            coordinates[3] = new Point(796, 429);*/
            try {
                if (!uiDevice.isScreenOn()) {
                    uiDevice.wakeUp();
                    //uiDevice.swipe(coordinates, 10);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *  Login with correct credentials
     */
    @Test
    public void test1_check_login()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Check Login Correct Start");

        String connectionString = getConnectionString(testServerURL, testServerPort);

        //To avoid the short delay when the activity starts
        SystemClock.sleep(WAIT_INITIAL);

        // Check that login button is disabled
        onView(withId(R.id.buttonOK)).check(matches(not(isEnabled())));

        setFields(connectionString, testUser, testPassword);

        // Check that the Activity ends after clicking
        SystemClock.sleep(WAIT_LOGIN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            assertTrue(ERROR_MESSAGE, mActivityRule.getActivity().isDestroyed());
        else {

            Field f = Activity.class.getDeclaredField(RESULT_CODE);
            f.setAccessible(true);
            int mResultCode = f.getInt(mActivityRule.getActivity());
            assertTrue(ERROR_MESSAGE, mResultCode == Activity.RESULT_OK);

        }

        Log_OC.i(LOG_TAG, "Test Check Login Correct Passed");

    }

    /**
     *  Login with correct credentials changing device orientation
     */
    @Test
    public void test2_login_orientation_changes()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Check Login Orientation Changes Start");

        String connectionString = getConnectionString(testServerURL, testServerPort);

        //Set landscape
        mActivityRule.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        onView(withId(R.id.hostUrlInput)).perform(closeSoftKeyboard(),
                replaceText(connectionString), closeSoftKeyboard());
        onView(withId(R.id.account_username)).perform(click(),
                replaceText(testUser), closeSoftKeyboard());
        onView(withId(R.id.account_password)).perform(click(),
                replaceText(testPassword), closeSoftKeyboard());

        //Set portrait
        mActivityRule.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        onView(withId(R.id.buttonOK)).perform(closeSoftKeyboard(), click());

        // Check that the Activity ends after clicking
        SystemClock.sleep(WAIT_LOGIN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            assertTrue(ERROR_MESSAGE, mActivityRule.getActivity().isDestroyed());
        else {
            Field f = Activity.class.getDeclaredField(RESULT_CODE);
            f.setAccessible(true);
            int mResultCode = f.getInt(mActivityRule.getActivity());
            assertTrue(ERROR_MESSAGE, mResultCode != Activity.RESULT_OK);

        }

        Log_OC.i(LOG_TAG, "Test Check Login Orientation Changes Passed");
    }

    /**
     *  Login with correct credentials, that contains special characters
     */
    @Test
    public void test3_check_login_special_characters()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Check Login Special Characters Start");

        String connectionString = getConnectionString(testServerURL, testServerPort);

        // Check that login button is disabled
        onView(withId(R.id.buttonOK)).check(matches(not(isEnabled())));

        setFields(connectionString, testUser2, testPassword2);

        // Check that the Activity ends after clicking
        SystemClock.sleep(WAIT_LOGIN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            assertTrue(ERROR_MESSAGE, mActivityRule.getActivity().isDestroyed());
        else {

            Field f = Activity.class.getDeclaredField(RESULT_CODE);
            f.setAccessible(true);
            int mResultCode = f.getInt(mActivityRule.getActivity());
            assertTrue(ERROR_MESSAGE, mResultCode == Activity.RESULT_OK);

        }

        Log_OC.i(LOG_TAG, "Test Check Login Special Characters Passed");

    }

    /**
     *  Login with incorrect credentials (Negative test)
     */
    @Test
    public void test4_check_login_incorrect()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Check Login Incorrect Start");

        String connectionString = getConnectionString(testServerURL, testServerPort);

        // Check that login button is disabled
        onView(withId(R.id.buttonOK)).check(matches(not(isEnabled())));

        setFields(connectionString, USER_INEXISTENT, testPassword);

        //check that the credentials are not correct
        onView(withId(R.id.auth_status_text)).check(matches(withText(R.string.auth_unauthorized)));

        Log_OC.i(LOG_TAG, "Test Check Login Incorrect Passed");


    }

    /**
     *  Login with in an exiting account (Negative test)
     */
    @Test
    public void test5_check_existing_account()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Check Existing Account Start");

        String connectionString = getConnectionString(testServerURL, testServerPort);

        //Add an account to the device
        AccountsManager.addAccount(targetContext, connectionString, testUser, testPassword);

        // Check that login button is disabled
        onView(withId(R.id.buttonOK)).check(matches(not(isEnabled())));

        setFields(connectionString, testUser, testPassword);

        //check that the credentials are already stored
        onView(withId(R.id.auth_status_text)).check(matches(withText(R.string.auth_account_not_new)));

        Log_OC.i(LOG_TAG, "Test Check Existing Account Passed");


    }

    /**
     *  Login without credentials (Negative test)
     */
    @Test
    public void test6_check_login_blanks()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Check Blanks Login Start");

        String connectionString = getConnectionString(testServerURL, testServerPort);

        // Check that login button is disabled
        onView(withId(R.id.buttonOK)).check(matches(not(isEnabled())));

        setFields(connectionString, "", "");

        //check that the credentials are not correct
        onView(withId(R.id.auth_status_text)).check(matches(withText(R.string.auth_unauthorized)));

        Log_OC.i(LOG_TAG, "Test Check Blanks Login Passed");

    }

    /**
     *  Login with an username that contains blanks before and after.
     */
    @Test
    public void test7_check_login_trimmed_blanks()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Check Trimmed Blanks Start");

        String UserBlanks = "    " + testUser + "         ";
        String connectionString = getConnectionString(testServerURL, testServerPort);

        // Check that login button is disabled
        onView(withId(R.id.buttonOK)).check(matches(not(isEnabled())));

        setFields(connectionString, UserBlanks, testPassword);

        // Check that the Activity ends after clicking
        SystemClock.sleep(WAIT_LOGIN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            assertTrue(ERROR_MESSAGE, mActivityRule.getActivity().isDestroyed());
        else {
            Field f = Activity.class.getDeclaredField(RESULT_CODE);
            f.setAccessible(true);
            int mResultCode = f.getInt(mActivityRule.getActivity());
            assertTrue(ERROR_MESSAGE, mResultCode == Activity.RESULT_OK);

        }

        Log_OC.i(LOG_TAG, "Test Check Trimmed Blanks Start");

    }

    /**
     *  Login with server URL copied and pasted from web browser
     */
    @Test
    public void test8_check_url_from_browser()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Check URL Browser Start");

        String connectionString = getConnectionString(testServerURL, testServerPort);

        connectionString += SUFFIX_BROWSER;

        // Check that login button is disabled
        onView(withId(R.id.buttonOK)).check(matches(not(isEnabled())));

        setFields(connectionString, testUser2, testPassword2);

        // Check that the Activity ends after clicking
        SystemClock.sleep(WAIT_LOGIN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            assertTrue(ERROR_MESSAGE, mActivityRule.getActivity().isDestroyed());
        else {
            Field f = Activity.class.getDeclaredField(RESULT_CODE);
            f.setAccessible(true);
            int mResultCode = f.getInt(mActivityRule.getActivity());
            assertTrue(ERROR_MESSAGE, mResultCode == Activity.RESULT_OK);

        }

        Log_OC.i(LOG_TAG, "Test Check URL Browser Passed");

    }

    /**
     *  Login in https non-secure (self signed or expired certificate).
     *  Certified is not accepted (Negative test).
     */
    @Test
    public void test9_check_certif_not_secure_no_accept()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test not accept not secure start");

        if (testServerPortSecure != null && trusted == 0) {

            String connectionString = getConnectionString(testServerURL, testServerPortSecure);

            // Check that login button is disabled
            onView(withId(R.id.buttonOK))
                    .check(matches(not(isEnabled())));

            // Type server url
            onView(withId(R.id.hostUrlInput))
                    .perform(replaceText(connectionString), closeSoftKeyboard());
            onView(withId(R.id.account_username)).perform(click());

            SystemClock.sleep(WAIT_CONNECTION);

            //certif not accepted
            onView(withId(R.id.cancel)).perform(click());

            SystemClock.sleep(WAIT_CONNECTION);

            // Check that login button keeps on being disabled
            onView(withId(R.id.buttonOK))
                    .check(matches(not(isEnabled())));

            // Check that SSL server is not trusted
            onView(withId(R.id.server_status_text))
                    .check(matches(withText(R.string.auth_ssl_unverified_server_title)));

            Log_OC.i(LOG_TAG, "Test not accept not secure passed");

        }
    }

    /**
     *  Login in https non-secure (self signed or expired certificate).
     *  Certified is accepted.
     */
    @Test
    public void test_10_check_certif_not_secure()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test accept not secure start");

        // Get values passed

        if (testServerPort != null && trusted == 0) {

            String connectionString = getConnectionString(testServerURL, testServerPortSecure);

            // Check that login button is disabled
            onView(withId(R.id.buttonOK))
                    .check(matches(not(isEnabled())));

            // Type server url
            onView(withId(R.id.hostUrlInput))
                    .perform(replaceText(connectionString), closeSoftKeyboard());
            onView(withId(R.id.account_username)).perform(click());

            SystemClock.sleep(WAIT_CONNECTION);

            //Check untrusted certificate, opening the details
            onView(withId(R.id.details_btn)).perform(click());
            //Check that the details view is present after opening
            onView(withId(R.id.details_view)).check(matches(isDisplayed()));
            //Close the details
            onView(withId(R.id.details_btn)).perform(click());
            //Check that the details view is already not present
            onView(withId(R.id.details_view)).check(matches(not((isDisplayed()))));

            //Closing the view
            onView(withId(R.id.ok)).perform(click());

            SystemClock.sleep(WAIT_CONNECTION);
            //Check correct connection message
            onView(withId(R.id.server_status_text))
                    .check(matches(withText(R.string.auth_secure_connection)));

            // Type user
            onView(withId(R.id.account_username))
                    .perform(replaceText(testUser), closeSoftKeyboard());

            // Type user pass
            onView(withId(R.id.account_password))
                    .perform(replaceText(testPassword), closeSoftKeyboard());

            onView(withId(R.id.buttonOK)).perform(click());

            // Check that the Activity ends after clicking
            SystemClock.sleep(WAIT_LOGIN);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                assertTrue(ERROR_MESSAGE, mActivityRule.getActivity().isDestroyed());
            else {
                Field f = Activity.class.getDeclaredField(RESULT_CODE);
                f.setAccessible(true);
                int mResultCode = f.getInt(mActivityRule.getActivity());
                assertTrue(ERROR_MESSAGE, mResultCode == Activity.RESULT_OK);

            }

            Log_OC.i(LOG_TAG, "Test accept not secure passed");

        }

    }


    private String getConnectionString (String url, String port){

        if (port != null)
            return url+":"+port;
        else
            return url;
    }

    private void setFields (String connectionString, String username, String password){

        // Type server url
        onView(withId(R.id.hostUrlInput))
                .perform(replaceText(connectionString), closeSoftKeyboard());
        onView(withId(R.id.account_username)).perform(click());

        SystemClock.sleep(WAIT_CONNECTION);

        // Type user
        onView(withId(R.id.account_username))
                .perform(replaceText(username), closeSoftKeyboard());

        // Type user pass
        onView(withId(R.id.account_password))
                .perform(replaceText(password), closeSoftKeyboard());
        onView(withId(R.id.buttonOK)).perform(click());
    }

    @After
    public void tearDown() throws Exception {
        AccountsManager.deleteAllAccounts(targetContext);
    }
}
