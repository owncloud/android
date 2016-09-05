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
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.test.suitebuilder.annotation.LargeTest;

import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertTrue;
import com.owncloud.android.R;
import com.owncloud.android.utils.AccountsManager;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.junit.Before;
import org.junit.After;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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

    private static final int WAIT_LOGIN = 2000;
    private static final int WAIT_CONNECTION = 1500;

    private static final String ERROR_MESSAGE = "Activity not finished";
    private static final String ALREADY_EXISTING_ACCOUNT_ERROR =
            "An account for the same user and server already exists in the device";
    private static final String WRONG_ACCOUNT_ERROR = "Wrong username or password";
    private static final String SECURE_CONNECTION_ESTABLISHED = "Secure connection established";
    private static final String SECURE_CONNECTION_DISMISSED = "Couldn't verify SSL server's identity";

    private static final String SUFFIX_BROWSER = "/index.php/apps/files/";
    private static final String RESULT_CODE = "mResultCode";
    private static final String LOG_TAG = "LoginSuite";

    private Context targetContext = null;

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

    @Test
    public void test1_check_login()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Bundle arguments = InstrumentationRegistry.getArguments();

        Log_OC.i(LOG_TAG, "Test Check Login Correct Start");

        // Get values passed
        String testUser = arguments.getString("TEST_USER");
        String testPassword = arguments.getString("TEST_PASSWORD");
        String testServerURL = arguments.getString("TEST_SERVER_URL");
        String testServerPort = arguments.getString("TEST_SERVER_PORT");

        String connectionString = getConnectionString(testServerURL, testServerPort);

        // Check that login button is disabled
        onView(withId(R.id.buttonOK))
                .check(matches(not(isEnabled())));

        setFields(connectionString, testUser, testPassword);

        // Check that the Activity ends after clicking

        Thread.sleep(WAIT_LOGIN);
        Field f = Activity.class.getDeclaredField(RESULT_CODE);
        f.setAccessible(true);
        int mResultCode = f.getInt(mActivityRule.getActivity());

        assertTrue(ERROR_MESSAGE, mResultCode == Activity.RESULT_OK);

        Log_OC.i(LOG_TAG, "Test Check Login Correct Passed");

    }

    @Test
    public void test2_check_login_special_characters()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Bundle arguments = InstrumentationRegistry.getArguments();

        Log_OC.i(LOG_TAG, "Test Check Login Special Characters Start");

        // Get values passed
        String testUser = arguments.getString("TEST_USER2");
        String testPassword = arguments.getString("TEST_PASSWORD2");
        String testServerURL = arguments.getString("TEST_SERVER_URL");
        String testServerPort = arguments.getString("TEST_SERVER_PORT");

        String connectionString = getConnectionString(testServerURL, testServerPort);

        // Check that login button is disabled
        onView(withId(R.id.buttonOK))
                .check(matches(not(isEnabled())));

        setFields(connectionString, testUser, testPassword);

        // Check that the Activity ends after clicking

        Thread.sleep(WAIT_LOGIN);
        Field f = Activity.class.getDeclaredField(RESULT_CODE);
        f.setAccessible(true);
        int mResultCode = f.getInt(mActivityRule.getActivity());

        assertTrue(ERROR_MESSAGE, mResultCode == Activity.RESULT_OK);

        Log_OC.i(LOG_TAG, "Test Check Login Special Characters Passed");

    }

    @Test
    public void test3_check_login_incorrect()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Bundle arguments = InstrumentationRegistry.getArguments();

        Log_OC.i(LOG_TAG, "Test Check Login Incorrect Start");

        // Get values passed
        String testUser = arguments.getString("TEST_USER") + arguments.getString("TEST_USER");
        String testPassword = arguments.getString("TEST_PASSWORD");
        String testServerURL = arguments.getString("TEST_SERVER_URL");
        String testServerPort = arguments.getString("TEST_SERVER_PORT");

        String connectionString = getConnectionString(testServerURL, testServerPort);

        // Check that login button is disabled
        onView(withId(R.id.buttonOK))
                .check(matches(not(isEnabled())));

        setFields(connectionString, testUser, testPassword);

        //check that the credentials are not correct
        onView(withId(R.id.auth_status_text)).check(matches(withText(WRONG_ACCOUNT_ERROR)));

        Log_OC.i(LOG_TAG, "Test Check Login Incorrect Passed");


    }

    @Test
    public void test4_check_existing_account()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Bundle arguments = InstrumentationRegistry.getArguments();

        Log_OC.i(LOG_TAG, "Test Check Existing Account Start");

        // Get values passed
        String testUser = arguments.getString("TEST_USER");
        String testPassword = arguments.getString("TEST_PASSWORD");
        String testServerURL = arguments.getString("TEST_SERVER_URL");
        String testServerPort = arguments.getString("TEST_SERVER_PORT");

        String connectionString = getConnectionString(testServerURL, testServerPort);

        // Check that login button is disabled
        onView(withId(R.id.buttonOK))
                .check(matches(not(isEnabled())));

        setFields(connectionString, testUser, testPassword);

        //check that the credentials are already stored
        onView(withId(R.id.auth_status_text)).check(matches(withText(ALREADY_EXISTING_ACCOUNT_ERROR)));

        Log_OC.i(LOG_TAG, "Test Check Existing Account Passed");


    }

    @Test
    public void test5_check_login_blanks()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Bundle arguments = InstrumentationRegistry.getArguments();

        //At this point, we can clean all the existing accounts
        AccountsManager.deleteAllAccounts(targetContext);

        Log_OC.i(LOG_TAG, "Test Check Blanks Login Start");

        // Get values passed
        String testUser = "";
        String testPassword = "";
        String testServerURL = arguments.getString("TEST_SERVER_URL");
        String testServerPort = arguments.getString("TEST_SERVER_PORT");

        String connectionString = getConnectionString(testServerURL, testServerPort);

        // Check that login button is disabled
        onView(withId(R.id.buttonOK))
                .check(matches(not(isEnabled())));

        setFields(connectionString, testUser, testPassword);

        //check that the credentials are not correct
        onView(withId(R.id.auth_status_text)).check(matches(withText(WRONG_ACCOUNT_ERROR)));

        Log_OC.i(LOG_TAG, "Test Check Blanks Login Passed");

    }

    @Test
    public void test6_check_login_trimmed_blanks()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Bundle arguments = InstrumentationRegistry.getArguments();

        Log_OC.i(LOG_TAG, "Test Check Trimmed Blanks Start");

        // Get values passed
        String testUser = "    " + arguments.getString("TEST_USER") + "         ";
        String testPassword = arguments.getString("TEST_PASSWORD");
        String testServerURL = arguments.getString("TEST_SERVER_URL");
        String testServerPort = arguments.getString("TEST_SERVER_PORT");

        String connectionString = getConnectionString(testServerURL, testServerPort);

        // Check that login button is disabled
        onView(withId(R.id.buttonOK))
                .check(matches(not(isEnabled())));

        setFields(connectionString, testUser, testPassword);

        // Check that the Activity ends after clicking
        Thread.sleep(WAIT_LOGIN);
        Field f = Activity.class.getDeclaredField(RESULT_CODE);
        f.setAccessible(true);
        int mResultCode = f.getInt(mActivityRule.getActivity());

        assertTrue(ERROR_MESSAGE, mResultCode == Activity.RESULT_OK);

        Log_OC.i(LOG_TAG, "Test Check Trimmed Blanks Start");

    }

    @Test
    public void test7_check_url_from_browser()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Bundle arguments = InstrumentationRegistry.getArguments();

        Log_OC.i(LOG_TAG, "Test Check URL Browser Start");

        // Get values passed
        String testUser = arguments.getString("TEST_USER2");
        String testPassword = arguments.getString("TEST_PASSWORD2");
        String testServerPort = arguments.getString("TEST_SERVER_PORT");
        String testServerURL = arguments.getString("TEST_SERVER_URL");

        String connectionString = getConnectionString(testServerURL, testServerPort);

        connectionString += SUFFIX_BROWSER;

        // Check that login button is disabled
        onView(withId(R.id.buttonOK))
                .check(matches(not(isEnabled())));

        setFields(connectionString, testUser, testPassword);

        // Check that the Activity ends after clicking

        Thread.sleep(WAIT_LOGIN);
        Field f = Activity.class.getDeclaredField(RESULT_CODE);
        f.setAccessible(true);
        int mResultCode = f.getInt(mActivityRule.getActivity());

        assertTrue(ERROR_MESSAGE, mResultCode == Activity.RESULT_OK);

        Log_OC.i(LOG_TAG, "Test Check URL Browser Passed");

        //At this point, we can clean all the existing accounts
        AccountsManager.deleteAllAccounts(targetContext);

    }

    @Test
    public void test8_check_certif_not_secure_no_accept()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Bundle arguments = InstrumentationRegistry.getArguments();

        Log_OC.i(LOG_TAG, "Test not accept not secure start");

        // Get values passed
        String testUser = arguments.getString("TEST_USER2");
        String testPassword = arguments.getString("TEST_PASSWORD2");
        String testServerPort = arguments.getString("TEST_SERVER_PORT_SECURE");
        String testServerURL = arguments.getString("TEST_SERVER_URL");
        int trusted = arguments.getInt("TRUSTED");

        if (testServerPort != null && trusted == 0) {

            String connectionString = getConnectionString(testServerURL, testServerPort);

            // Check that login button is disabled
            onView(withId(R.id.buttonOK))
                    .check(matches(not(isEnabled())));

            // Type server url
            onView(withId(R.id.hostUrlInput))
                    .perform(typeText(connectionString), closeSoftKeyboard());
            onView(withId(R.id.account_username)).perform(click());

            SystemClock.sleep(WAIT_CONNECTION);

            //certif not accepted
            onView(withId(R.id.cancel)).perform(click());

            SystemClock.sleep(WAIT_CONNECTION);

            // Check that login button keeps on being disabled
            onView(withId(R.id.buttonOK))
                    .check(matches(not(isEnabled())));

            // Check that login button keeps on being disabled
            onView(withId(R.id.server_status_text))
                    .check(matches(withText(SECURE_CONNECTION_DISMISSED)));

            Log_OC.i(LOG_TAG, "Test not accept not secure passed");

        }



    }

    @Test
    public void test9_check_certif_not_secure()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Bundle arguments = InstrumentationRegistry.getArguments();

        Log_OC.i(LOG_TAG, "Test accept not secure start");

        // Get values passed

        String testUser = arguments.getString("TEST_USER");
        String testPassword = arguments.getString("TEST_PASSWORD");
        String testServerPort = arguments.getString("TEST_SERVER_PORT_SECURE");
        String testServerURL = arguments.getString("TEST_SERVER_URL");
        int trusted = arguments.getInt("TRUSTED");

        if (testServerPort != null && trusted == 0) {

            String connectionString = getConnectionString(testServerURL, testServerPort);


            // Check that login button is disabled
            onView(withId(R.id.buttonOK))
                    .check(matches(not(isEnabled())));

            // Type server url
            onView(withId(R.id.hostUrlInput))
                    .perform(typeText(connectionString), closeSoftKeyboard());
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
                    .check(matches(withText(SECURE_CONNECTION_ESTABLISHED)));

            // Type user
            onView(withId(R.id.account_username))
                    .perform(typeText(testUser), closeSoftKeyboard());

            // Type user pass
            onView(withId(R.id.account_password))
                    .perform(typeText(testPassword), closeSoftKeyboard());
            onView(withId(R.id.buttonOK)).perform(click());

            // Check that the Activity ends after clicking
            Thread.sleep(WAIT_LOGIN);
            Field f = Activity.class.getDeclaredField(RESULT_CODE);
            f.setAccessible(true);
            int mResultCode = f.getInt(mActivityRule.getActivity());

            assertTrue(ERROR_MESSAGE, mResultCode == Activity.RESULT_OK);

            Log_OC.i(LOG_TAG, "Test accept not secure passed");

        }

    }


    private String getConnectionString (String url, String port){

        return url+":"+port;
    }

    private void setFields (String connectionString, String username, String password){

        // Type server url
        onView(withId(R.id.hostUrlInput))
                .perform(typeText(connectionString), closeSoftKeyboard());
        onView(withId(R.id.account_username)).perform(click());

        // Type user
        onView(withId(R.id.account_username))
                .perform(typeText(username), closeSoftKeyboard());

        // Type user pass
        onView(withId(R.id.account_password))
                .perform(typeText(password), closeSoftKeyboard());
        onView(withId(R.id.buttonOK)).perform(click());
    }

    @After
    public void tearDown() throws Exception {

    }
}
