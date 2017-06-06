/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2017 ownCloud GmbH.
 *   @author Jesús Recio (@jesmrec)
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.test.suitebuilder.annotation.LargeTest;

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

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webClick;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webKeys;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@LargeTest
public class SAMLAuthenticatorActivityTest {

    public static final String EXTRA_ACTION = "ACTION";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";

    private static final int WAIT_INITIAL = 1000;
    private static final int WAIT_LOGIN = 5000;
    private static final int WAIT_CONNECTION = 2500;

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

    public enum ServerType {
        /*
         *  Server with trusted certificate
         */
        TRUSTED(1),

        /*
         * Sever with non-trusted certificate
         */
        NON_TRUSTED(2);

        private final int status;

        ServerType (int status) {
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

    public ServerType servertype;

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
        testPassword = arguments.getString("TEST_PASSWORD");
        testServerURL = arguments.getString("TEST_SERVER_URL");
        servertype = ServerType.fromValue(Integer.parseInt(arguments.getString("TRUSTED")));
        webViewUsernameId = arguments.getString("TEST_USERNAME_ID");
        webViewPasswordId = arguments.getString("TEST_PASSWORD_ID");
        webViewSubmitXPath = arguments.getString("TEST_SUBMIT_XPATH");

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
     *  Login with SAML. Supported with non-secured servers under https
     */
    @Test
    public void test1_check_login_saml()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Check Login SAML Start");

        SystemClock.sleep(WAIT_INITIAL);

        // Check that login button is disabled
        onView(withId(R.id.buttonOK)).check(matches(not(isEnabled())));

        onView(withId(R.id.hostUrlInput)).perform(replaceText(testServerURL));

        //Needed to click on the screen to validate the URL
        onView(withId(R.id.thumbnail)).perform(click());

        //Certificate acceptance in case of non-trusted or expirated
        if (servertype == ServerType.NON_TRUSTED) {

            SystemClock.sleep(WAIT_CONNECTION);
            onView(withId(R.id.ok)).perform(click());
        }

        SystemClock.sleep(WAIT_CONNECTION);

        //Check that the URL is valid
        onView(withId(R.id.server_status_text)).check(matches(withText(R.string.auth_secure_connection)));

        //Go to idp webview
        onView(withId(R.id.buttonOK)).perform(click());

        SystemClock.sleep(WAIT_CONNECTION);

        //Fill credentials on the WebView.
        onWebView().withElement(findElement(Locator.NAME, webViewUsernameId)).perform(webKeys(testUser));
        onWebView().withElement(findElement(Locator.NAME, webViewPasswordId)).perform(webKeys(testPassword));
        onWebView().withElement(findElement(Locator.XPATH, webViewSubmitXPath)).perform(webClick());

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

        Log_OC.i(LOG_TAG, "Test Check Login SAML Passed");

    }

    @Test
    public void test2_check_login_saml_orientation_changes()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Check Login SAML Orientation Changes Start");

        onView(withId(R.id.buttonOK)).check(matches(not(isEnabled())));

        onView(withId(R.id.hostUrlInput)).perform(replaceText(testServerURL));

        //Set landscape
        mActivityRule.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        //Needed to click on the screen to validate the URL
        onView(withId(R.id.thumbnail)).perform(closeSoftKeyboard(), click());

        //Certificate acceptance in case of non-trusted or expirated
        if (servertype == ServerType.NON_TRUSTED) {

            SystemClock.sleep(WAIT_CONNECTION);
            onView(withId(R.id.ok)).perform(click());
        }

        SystemClock.sleep(WAIT_CONNECTION);

        //Check that the URL is valid
        onView(withId(R.id.server_status_text)).check(matches(withText(R.string.auth_secure_connection)));

        //Go to idp webview
        onView(withId(R.id.buttonOK)).perform(click());

        SystemClock.sleep(WAIT_CONNECTION);

        //Fill credentials on the WebView.
        onWebView().withElement(findElement(Locator.NAME, webViewUsernameId)).perform(webKeys(testUser));
        onWebView().withElement(findElement(Locator.NAME, webViewPasswordId)).perform(webKeys(testPassword));

        //Set portrait
        mActivityRule.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        onWebView().withElement(findElement(Locator.XPATH, webViewSubmitXPath)).perform(webClick());

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

        Log_OC.i(LOG_TAG, "Test Check Login SAML Orientation Changes Passed");

    }

    @After
    public void tearDown() throws Exception {
        AccountsManager.deleteAllAccounts(targetContext);
    }
}