/**
 * ownCloud Android client application
 * <p>
 * Copyright (C) 2019 ownCloud GmbH.
 *
 * @author Jesús Recio (@jesmrec)
 * @author Christian Schabesberger
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.authentication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;

import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.AccountsManager;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.lang.reflect.Field;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webKeys;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@LargeTest
public class SAMLAuthenticatorActivityTest {

    public static final String EXTRA_ACTION = "ACTION";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";

    private static final int WAIT_INITIAL_MS = 1000;
    private static final int WAIT_LOGIN_MS = 5000;
    private static final int WAIT_CONNECTION_MS = 2500;
    private static final int WAIT_CHANGE_MS = 1000;

    private static final String ERROR_MESSAGE = "Activity not finished";
    private static final String RESULT_CODE = "mResultCode";
    private static final String LOG_TAG = "LoginSuiteSAML";

    private Context targetContext = null;
    private String testServerURL = null;
    private String testUser = null;
    private String testPassword = null;
    private String webViewUsernameId = null;
    private String webViewPasswordId = null;
    private String webViewSubmitXPath = null;

    private enum ServerType {
        /*
         *  Server with trusted certificate
         */
        TRUSTED(1),

        /*
         * Sever with non-trusted certificate
         */
        NON_TRUSTED(2);

        private final int status;

        ServerType(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }

        public static ServerType fromValue(int value) {
            switch (value) {
                case 1:
                    return TRUSTED;
                case 2:
                    return NON_TRUSTED;
            }
            return null;
        }

    }

    private ServerType servertype;

    @Rule
    public ActivityTestRule<AuthenticatorActivity> mActivityRule = new ActivityTestRule<AuthenticatorActivity>(
            AuthenticatorActivity.class) {
        @Override
        protected Intent getActivityIntent() {

            targetContext = InstrumentationRegistry.getInstrumentation()
                    .getTargetContext();
            Intent result = new Intent(targetContext, AuthenticatorActivity.class);
            result.putExtra(EXTRA_ACTION, AuthenticatorActivity.ACTION_CREATE);
            return result;
        }
    };

    @Before
    public void init() {
        Bundle arguments = InstrumentationRegistry.getArguments();

        testUser = arguments.getString("TEST_USER");
        testPassword = arguments.getString("TEST_PASSWORD");
        testServerURL = arguments.getString("TEST_SERVER_URL");
        servertype = ServerType.fromValue(Integer.parseInt(arguments.getString("TRUSTED")));
        webViewUsernameId = arguments.getString("TEST_USERNAME_ID");
        webViewPasswordId = arguments.getString("TEST_PASSWORD_ID");
        webViewSubmitXPath = arguments.getString("TEST_SUBMIT_XPATH");

        // UiDevice available from API level 17
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            /* Code to unlock with a swipe. In local tests is not used, but with scheduled tests
               maybe necessary */
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
     *  Login with SAML. Supported with non-secured servers under https
     */
    @Test
    public void test1_check_login_saml()
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Check Login SAML Start");

        SystemClock.sleep(WAIT_INITIAL_MS);

        // Check that login button is hidden
        onView(withId(R.id.loginButton)).check(matches(not(isDisplayed())));

        onView(withId(R.id.hostUrlInput)).perform(replaceText(testServerURL));

        //Needed to click on the screen to validate the URL
        onView(withId(R.id.thumbnail)).perform(click());

        //Certificate acceptance in case of non-trusted or expirated
        if (servertype == ServerType.NON_TRUSTED) {

            SystemClock.sleep(WAIT_CONNECTION_MS);
            onView(withId(R.id.ok)).perform(click());
        }

        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check that the URL is valid
        onView(withId(R.id.server_status_text)).check(matches(withText(R.string.auth_secure_connection)));

        //Go to idp webview
        onView(withId(R.id.loginButton)).perform(click());

        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Fill credentials on the WebView.
        onWebView().withElement(findElement(Locator.NAME, webViewUsernameId)).perform(webKeys(testUser));
        onWebView().withElement(findElement(Locator.NAME, webViewPasswordId)).perform(webKeys(testPassword));
        onWebView().withElement(findElement(Locator.XPATH, webViewSubmitXPath)).perform(webClick());

        // Check that the Activity ends after clicking
        SystemClock.sleep(WAIT_LOGIN_MS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            assertTrue(ERROR_MESSAGE, mActivityRule.getActivity().isDestroyed());
        } else {
            Field f = Activity.class.getDeclaredField(RESULT_CODE);
            f.setAccessible(true);
            int mResultCode = f.getInt(mActivityRule.getActivity());
            assertTrue(ERROR_MESSAGE, mResultCode == Activity.RESULT_OK);
        }

        Log_OC.i(LOG_TAG, "Test Check Login SAML Passed");

    }

    @Test
    public void test2_check_login_saml_orientation_changes()
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Check Login SAML Orientation Changes Start");

        // Check that login button is hidden
        onView(withId(R.id.loginButton)).check(matches(not(isDisplayed())));

        onView(withId(R.id.hostUrlInput)).perform(replaceText(testServerURL));

        //Set landscape
        mActivityRule.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        SystemClock.sleep(WAIT_CHANGE_MS);

        //Needed to click on the screen to validate the URL
        onView(withId(R.id.thumbnail)).perform(closeSoftKeyboard(), click());

        //Here we guess that the certificate was accepted in first test
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check that the URL is valid
        onView(withId(R.id.server_status_text)).check(matches(withText(R.string.auth_secure_connection)));

        //Go to idp webview
        onView(withId(R.id.loginButton)).perform(click());

        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Fill credentials on the WebView.
        onWebView().withElement(findElement(Locator.NAME, webViewUsernameId)).perform(webKeys(testUser));
        onWebView().withElement(findElement(Locator.NAME, webViewPasswordId)).perform(webKeys(testPassword));

        //Set portrait
        mActivityRule.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        SystemClock.sleep(WAIT_CHANGE_MS);

        onWebView().withElement(findElement(Locator.XPATH, webViewSubmitXPath)).perform(webClick());

        // Check that the Activity ends after clicking
        SystemClock.sleep(WAIT_LOGIN_MS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            assertTrue(ERROR_MESSAGE, mActivityRule.getActivity().isDestroyed());
        } else {
            Field f = Activity.class.getDeclaredField(RESULT_CODE);
            f.setAccessible(true);
            int mResultCode = f.getInt(mActivityRule.getActivity());
            assertTrue(ERROR_MESSAGE, mResultCode == Activity.RESULT_OK);
        }

        Log_OC.i(LOG_TAG, "Test Check Login SAML Orientation Changes Passed");

    }

    @After
    public void tearDown() {
        AccountsManager.deleteAllAccounts(targetContext);
    }
}