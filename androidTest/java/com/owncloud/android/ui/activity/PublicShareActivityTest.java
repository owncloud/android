/**
 *   ownCloud Android client application
 *
 *   @author Jesús Recio @jesmrec
 *   Copyright (C) 2017 ownCloud GmbH.
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


package com.owncloud.android.ui.activity;


import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.FailureHandler;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.v4.content.ContextCompat;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.CapabilityBooleanType;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.utils.AccountsManager;
import com.owncloud.android.utils.FileManager;
import com.owncloud.android.utils.ServerType;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@LargeTest

public class PublicShareActivityTest {

    private static final int WAIT_INITIAL_MS = 4000;
    private static final int WAIT_CONNECTION_MS = 1500;
    private static final int WAIT_CLIPBOARD_MS = 3000;
    private static final int WAIT_CERTIF_MS = 4000;

    private static final String ERROR_MESSAGE = "BAD LINK";
    private static final String RESULT_CODE = "mResultCode";
    private static final String LOG_TAG = "PublicShareSuite";
    private static final int VERSION_10 = 10;
    private static final int MULTIPLE_LINKS = 5;

    private Context targetContext = null;
    private static String folder = "Photos";
    private static String folder2 = "Documents";
    private static String file = "ownCloud Manual.pdf";
    private static final String nameShare = "$%@rter";
    private static final String nameShareEdited = "publicnameverylongtotest";
    private static final String nameShareMultiple = "linkN";
    private static final int GRANT_BUTTON_INDEX = 1;
    private int version = -1;

    private String testUser = null;
    private String testPassword = null;
    private String testServerURL = null;
    private ServerType servertype;
    private OCCapability capabilities;

    @Rule
    public ActivityTestRule<FileDisplayActivity> mActivityRule = new
            ActivityTestRule<FileDisplayActivity>(
                    FileDisplayActivity.class) {

                @Override
                public void beforeActivityLaunched() {
                    targetContext = InstrumentationRegistry.getInstrumentation()
                            .getTargetContext();
                    Bundle arguments = InstrumentationRegistry.getArguments();

                    testServerURL = arguments.getString("TEST_SERVER_URL");
                    testUser = arguments.getString("TEST_USER");
                    testPassword = arguments.getString("TEST_PASSWORD");
                    servertype = ServerType.fromValue(Integer.parseInt(arguments.getString("TRUSTED")));

                    //Add an account to the device in order to avoid login view
                    AccountsManager.addAccount(targetContext, testServerURL, testUser, testPassword);

                }
            };


    @Before
    public void init() {

        // UiDevice available from API level 17

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            UiDevice uiDevice =
                    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            try {
                if (!uiDevice.isScreenOn())
                    uiDevice.wakeUp();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        try {
            // From API level 23, permissions have to be accepted explicitly if they haven't before
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    grantedPermission() != PackageManager.PERMISSION_GRANTED) {

                //Accept to allow app to manage files on the device
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                        .findObject(new UiSelector().clickable(true).index(GRANT_BUTTON_INDEX)).click();

                SystemClock.sleep(WAIT_CERTIF_MS);
                //Accept the untrusted certificate
                if (servertype == ServerType.HTTPS_NON_SECURE) {
                    onView(withId(R.id.ok)).perform(click());
                }
            }

        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }

        //Get Server Capabilities
        capabilities = AccountsManager.getCapabilities(testServerURL, testUser, testPassword);

        mActivityRule.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }


    @BeforeClass
    public static void before(){
        //Needed to use clipboard
        Looper.prepare();
    }

    /**
     *  TEST CASE: Share publicly a folder (default options)
     *  PASSED IF: Link created and visible in share view (message of "no links" does not appear)
     */
    @Test
    public void test_01_create_public_link_folder_defaults()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Share Public Defaults Start");
        SystemClock.sleep(WAIT_INITIAL_MS);

        //Select share option
        selectShare(folder);

        //Check that no links are already created
        onView(withId(R.id.shareNoPublicLinks)).check(matches(isDisplayed()));

        //Depending the server version, send a name or not.
        if (capabilities.getVersionMayor() >= VERSION_10) {
            publicShareCreationDefault(nameShare);
        } else {
            publicShareCreationDefault(null);
        }

        //Check the name,only in the case of ownCloud >= 10
        if (capabilities.getVersionMayor() >= VERSION_10) {
            onView(withText(nameShare)).check(matches(isDisplayed()));
        }
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //The message of "not links created yet" is gone
        onView(withId(R.id.shareNoPublicLinks))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        Log_OC.i(LOG_TAG, "Test Share Public Defaults Passed");

    }

    /**
     *  TEST CASE: Share publicly a folder (all options enabled)
     *  PASSED IF:
     *              - Link created and visible in share view
     *              - "Allow editing", "Show file listing" (oC >= 10.0.1), "Password" and "Expiration" are enabled
     */
    @Test
    public void test_02_create_public_link_folder_all_enabled()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Share Public All Enabled Start");
        SystemClock.sleep(WAIT_INITIAL_MS);

        //Select share option
        selectShare(folder2);

        //Check that no links are already created
        onView(withId(R.id.shareNoPublicLinks)).check(matches(isDisplayed()));

        //Depending the server version, send a name or not.
        if (capabilities.getVersionMayor() >= VERSION_10) {
            publicShareCreationAllEnabled(nameShare);
        } else {
            publicShareCreationAllEnabled(null);
        }

        //Check the name,only in the case of ownCloud >= 10
        if (capabilities.getVersionMayor() >= VERSION_10) {
            onView(withText(nameShare)).check(matches(isDisplayed()));
        }
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //The message of "not links created yet" is gone
        onView(withId(R.id.shareNoPublicLinks))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        onView(withId(R.id.editPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check the status of the sharing options
        onView(withId(R.id.shareViaLinkEditPermissionSwitch)).check(matches(isChecked()));
        if (isSupportedFileListing()) {
            onView(withId(R.id.shareViaShowFileListingSwitch)).check(matches(isEnabled()));
            onView(withId(R.id.shareViaShowFileListingSwitch)).check(matches(isChecked()));
        }
        onView(withId(R.id.shareViaLinkPasswordSwitch)).check(matches(isChecked()));
        onView(withId(R.id.shareViaLinkExpirationSwitch)).check(matches(isChecked()));

        onView(withId(R.id.cancelAddPublicLinkButton)).perform(scrollTo(), click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Remove the link
        deleteLink();

        Log_OC.i(LOG_TAG, "Test Share Public All Enabled Passed");

    }

    /**
     *  TEST CASE: Share public a folder and gets the link
     *  PASSED IF: Link correctly copied in clipboard
     */
    @Test
    public void test_03_get_link()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Get Link Start");
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Select share option
        selectShare(folder);

        //Get public link
        onView(withId(R.id.getPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check that the sharing panel is displayed
        onView(withId(R.id.parentPanel)).check(matches(isDisplayed()));
        onView(withId(R.id.alertTitle)).check(matches(isDisplayed()));

        //Get text copied in the clipboard
        onView(withText(R.string.copy_link)).perform(click());

        String text = getTextFromClipboard();
        SystemClock.sleep(WAIT_CLIPBOARD_MS);
        //check if the copied link is correct
        assertTrue(ERROR_MESSAGE, text.startsWith(testServerURL));

        Log_OC.i(LOG_TAG, "Test Get Link Passed");

    }

    /**
     *  TEST CASE: Edit the name of a public folder. Only for oC >= 10
     *  PASSED IF: Link has the new name in share view
     */
    @Test
    public void test_04_edit_name() {

        Log_OC.i(LOG_TAG, "Test Edit Link Name Start");
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //if ownCloud >= 10, we can handle the link name. If not... skipping test.
        if (capabilities.getVersionMayor() >= VERSION_10) {

            //Select share option
            selectShare(folder);

            //Edit the link name
            onView(withId(R.id.editPublicLinkButton)).perform(click());
            SystemClock.sleep(WAIT_CONNECTION_MS);
            onView(withId(R.id.shareViaLinkNameValue)).perform(replaceText(nameShareEdited));

            SystemClock.sleep(WAIT_CONNECTION_MS);
            onView(withId(R.id.confirmAddPublicLinkButton)).perform(scrollTo(),click());
            SystemClock.sleep(WAIT_CONNECTION_MS);

            //Check that the sharing panel is displayed
            onView(withId(R.id.parentPanel)).check(matches(isDisplayed()));
            onView(withId(R.id.alertTitle)).check(matches(isDisplayed()));
            pressBack();

            //Check the name
            onView(withText(nameShareEdited)).check(matches(isDisplayed()));

            SystemClock.sleep(WAIT_CONNECTION_MS);
        }

        Log_OC.i(LOG_TAG, "Test Edit Link Name Passed");
    }

    /**
     *  TEST CASE: Edit the public folder by enabling "Allow Editing"
     *  PASSED IF:
     *             - "Allow editing" and "Show file listing" (oC >= 10) are enabled.
     *             - "Password" and "Expiration date" are disabled.
     */
    @Test
    public void test_05_enable_allow_edit()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Enable Allow Edit Start");
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Select share option
        selectShare(folder);

        //Edit the link enabling the "allow edit option"
        onView(withId(R.id.editPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check file listing disabled + checked (default) if ownCloud >= 10
        if (isSupportedFileListing()) {
            onView(withId(R.id.shareViaShowFileListingSwitch)).check(matches(not(isEnabled())));
            onView(withId(R.id.shareViaShowFileListingSwitch)).check(matches(isChecked()));
        }

        onView(withId(R.id.shareViaLinkEditPermissionSwitch)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        onView(withId(R.id.confirmAddPublicLinkButton)).perform(scrollTo(), click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Skip the sharing panel
        pressBack();
        SystemClock.sleep(WAIT_CONNECTION_MS);

        onView(withId(R.id.editPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check the status of the sharing options
        onView(withId(R.id.shareViaLinkEditPermissionSwitch)).check(matches(isChecked()));
        if (isSupportedFileListing()) {
            onView(withId(R.id.shareViaShowFileListingSwitch)).check(matches(isEnabled()));
            onView(withId(R.id.shareViaShowFileListingSwitch)).check(matches(isChecked()));
        }
        onView(withId(R.id.shareViaLinkPasswordSwitch)).check(matches(not(isChecked())));
        onView(withId(R.id.shareViaLinkExpirationSwitch)).check(matches(not(isChecked())));

        onView(withId(R.id.cancelAddPublicLinkButton)).perform(scrollTo(), click());

        Log_OC.i(LOG_TAG, "Test Enable Allow Edit Passed");

    }

    /**
     *  TEST CASE: Edit the public folder by switching "Show File Listing" ( oC >= 10.0.1 )
     *  PASSED IF:
     *          - if oC >= 10.0.1
     *              * "Allow editing" is enabled and "Show file listing" (oC >= 10) is enabled.
     *              * "Password" and "Expiration date" are disabled.
     *          - if oC < 10.0.1
     *              * "Show file listing" does not exist
     */
    @Test
    public void test_06_file_listing_disabled()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test File Listing Disable Start");

        //Select share option
        selectShare(folder);

        //Edit the link
        onView(withId(R.id.editPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Only makes sense if "Show file listing" is supported
        if (isSupportedFileListing()) {

            onView(withId(R.id.shareViaShowFileListingSwitch)).check(matches(isEnabled()));
            onView(withId(R.id.shareViaShowFileListingSwitch)).check(matches(isChecked()));
            //Switching off
            onView(withId(R.id.shareViaShowFileListingSwitch)).perform(click());
            SystemClock.sleep(WAIT_CONNECTION_MS);
            onView(withId(R.id.confirmAddPublicLinkButton)).perform(scrollTo(), click());
            SystemClock.sleep(WAIT_CONNECTION_MS);

            //Skip the sharing panel
            pressBack();
            SystemClock.sleep(WAIT_CONNECTION_MS);

            onView(withId(R.id.editPublicLinkButton)).perform(click());
            SystemClock.sleep(WAIT_CONNECTION_MS);

            //Check options status
            onView(withId(R.id.shareViaLinkEditPermissionSwitch)).check(matches(isChecked()));
            onView(withId(R.id.shareViaShowFileListingSwitch)).check(matches(isEnabled()));
            onView(withId(R.id.shareViaShowFileListingSwitch)).check(matches(not(isChecked())));
            onView(withId(R.id.shareViaLinkPasswordSwitch)).check(matches(not(isChecked())));
            onView(withId(R.id.shareViaLinkExpirationSwitch)).check(matches(not(isChecked())));

        } else {  //Server with no support for "Show file listing"
            onView(withId(R.id.shareViaShowFileListingSwitch)).check(matches(not(isDisplayed())));
        }


        Log_OC.i(LOG_TAG, "Test File Listing Disabled Passed");

    }

    /**
     *  TEST CASE: Edit the public folder by switching "Allow editing" off ( oC >= 10.0.1 )
     *  PASSED IF:
     *          - if oC >= 10.0.1
     *              * "Allow editing" is disabled and "Show file listing" (oC >= 10) is disabled and checked
     *              * "Password" and "Expiration date" are disabled.
     *          - if oC < 10.0.1
     *              * Test skipped
     */
    @Test
    public void test_07_file_listing_enabled()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test File Listing Enabled Start");

        //Select share option
        selectShare(folder);

        onView(withId(R.id.editPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Only makes sense if ownCloud >= 10 and not 10.0.0
        if (isSupportedFileListing()) {

            //Switching "Allow editing" off to check "Show file listing" is checked and disabled
            onView(withId(R.id.shareViaLinkEditPermissionSwitch)).perform(click());

            //Check options status
            onView(withId(R.id.shareViaLinkEditPermissionSwitch)).check(matches(not(isChecked())));
            onView(withId(R.id.shareViaShowFileListingSwitch)).check(matches(not(isEnabled())));
            onView(withId(R.id.shareViaShowFileListingSwitch)).check(matches(isChecked()));
            onView(withId(R.id.shareViaLinkPasswordSwitch)).check(matches(not(isChecked())));
            onView(withId(R.id.shareViaLinkExpirationSwitch)).check(matches(not(isChecked())));
        }

        //Switch on to following tests
        onView(withId(R.id.shareViaLinkEditPermissionSwitch)).perform(click());

        Log_OC.i(LOG_TAG, "Test File Listing Enabled Passed");
    }

    /**
     *  TEST CASE: Edit the public folder by switching "Password" on and "Allow editing" off
     *  PASSED IF:
     *          - "Password" enabled
     *          - "Allow editing" and "Expiration" disabled
     */
    @Test
    public void test_08_enable_password()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Enable Password Start");
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Select share option
        selectShare(folder);

        //Edit the link enabling the "enabling password"
        onView(withId(R.id.editPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        onView(withId(R.id.shareViaLinkEditPermissionSwitch)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        onView(withId(R.id.shareViaLinkPasswordSwitch)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Setting a password... no matter which one
        onView(withId(R.id.shareViaLinkPasswordValue)).perform(scrollTo(), replaceText("a"));
        onView(withId(R.id.confirmAddPublicLinkButton)).perform(scrollTo(), click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Skipping the panel
        pressBack();
        SystemClock.sleep(WAIT_CONNECTION_MS);

        onView(withId(R.id.editPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check the status of the sharing options
        onView(withId(R.id.shareViaLinkEditPermissionSwitch)).check(matches(not(isChecked())));
        onView(withId(R.id.shareViaLinkPasswordSwitch)).check(matches(isChecked()));
        onView(withId(R.id.shareViaLinkExpirationSwitch)).check(matches(not(isChecked())));

        onView(withId(R.id.cancelAddPublicLinkButton)).perform(scrollTo(), click());

        Log_OC.i(LOG_TAG, "Test Enable Password Passed");

    }

    /**
     *  TEST CASE: Edit the public folder by switching "Expiration Date" on and "Password" off
     *  PASSED IF:
     *          - "Expiration" enabled
     *          - "Allow editing" and "Password" disabled
     */
    @Test
    public void test_09_enable_expiration()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Enable Expiration Start");
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Select share option
        selectShare(folder);

        //Edit the link enabling the "expiration date"
        onView(withId(R.id.editPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        onView(withId(R.id.shareViaLinkPasswordSwitch)).perform(scrollTo(), click());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        onView(withId(R.id.shareViaLinkExpirationSwitch)).perform(scrollTo(), click());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.confirmAddPublicLinkButton)).perform(scrollTo(), click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Skip the sharing panel
        pressBack();
        SystemClock.sleep(WAIT_CONNECTION_MS);

        onView(withId(R.id.editPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check the status of the sharing options
        onView(withId(R.id.shareViaLinkEditPermissionSwitch)).check(matches(not(isChecked())));
        onView(withId(R.id.shareViaLinkPasswordSwitch)).check(matches(not(isChecked())));
        onView(withId(R.id.shareViaLinkExpirationSwitch)).check(matches(isChecked()));

        onView(withId(R.id.cancelAddPublicLinkButton)).perform(scrollTo(),click());

        Log_OC.i(LOG_TAG, "Test Enable Expiration Passed");

    }

    /**
     *  TEST CASE: Remove public link
     *  PASSED IF: No links in share view
     */
    @Test
    public void test_10_unshare_public()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Unshare Public Start");
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Select share option
        selectShare(folder);

        //Check that a public link exists
        onView(withId(R.id.shareNoPublicLinks))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        //Delete link
        deleteLink();

        //Check that there are no public links
        onView(withId(R.id.shareNoPublicLinks)).check(matches(isDisplayed()));

        Log_OC.i(LOG_TAG, "Test Unshare Public Passed");

    }

    /**
     *  TEST CASE: Check the multiple sharing
     *  PASSED IF:
     *          - if oc >= 10.0.1 = X public links created correctly
     *          - if oc < 10.0.1 = No option available for creating more than one link
     */
    @Test
    public void test_11_share_multiple_links()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Share Multiple Public Start");
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Select share option
        selectShare(folder);

        if (isSupportedMultipleLinks()) {
            onView(withId(R.id.addPublicLinkButton)).check(matches(isDisplayed()));
            for (int i = 0; i < MULTIPLE_LINKS ; i++) {
                publicShareCreationDefault(nameShareMultiple+i);
            }

        } else {  //Servers < 10 hide the button
            publicShareCreationDefault(null);
            onView(withId(R.id.addPublicLinkButton))
                    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE)));
        }

        Log_OC.i(LOG_TAG, "Test Unshare Public Passed");

    }

    @Test
    public void test_12_remove_multiple_links()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Remove Multiple Public Start");
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Select share option
        selectShare(folder);

        if (isSupportedMultipleLinks()) {
            for (int i = 0; i < MULTIPLE_LINKS ; i++) {
                deleteLink(nameShareMultiple+i);
            }

        } else {  //Servers < 10, only one link
            deleteLink();
            onView(withId(R.id.addPublicLinkButton))
                    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        }

        Log_OC.i(LOG_TAG, "Test Remove Multiple Public Passed");

    }

    /**
     *  TEST CASE: Check permalink
     *  PASSED IF: Link to the item in clipboard
     */
    @Test
    public void test_13_permalink()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Permalink Start");

        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Select share option
        selectShare(folder);

        onView(withId(R.id.getPrivateLinkButton)).check(matches(isDisplayed()));
        onView(withId(R.id.getPrivateLinkButton)).perform(click());

        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check that the sharing panel is displayed
        onView(withId(R.id.parentPanel)).check(matches(isDisplayed()));
        onView(withId(R.id.alertTitle)).check(matches(isDisplayed()));

        //Get text copied in the clipboard
        onView(withText(R.string.copy_link)).perform(click());
        String text = getTextFromClipboard();

        SystemClock.sleep(WAIT_CLIPBOARD_MS);

        assertTrue(ERROR_MESSAGE, text.startsWith(testServerURL+"/index.php/f"));

        Log_OC.i(LOG_TAG, "Test Permalink Passed");

    }


    /**
     *  TEST CASE: Capability "Allow public links" disabled
     *  PASSED IF: No option to public in share view
     */
    @Test
    public void test_14_capability_allow_public_links()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Capability Public Links Start");

        //Disable capability "Allow users share via link"
        capabilities.setFilesSharingPublicEnabled(CapabilityBooleanType.FALSE);

        SystemClock.sleep(WAIT_CONNECTION_MS);

        AccountsManager.saveCapabilities(capabilities, testServerURL, testUser);

        //Select share option
        selectShare(folder2);

        SystemClock.sleep(WAIT_CONNECTION_MS);

        onView(withId(R.id.addPublicLinkButton)).check(matches(not(isDisplayed())));

        Log_OC.i(LOG_TAG, "Test Capability Public Links Passed");

    }

    /**
     *  TEST CASE: Capability "Allow editing" disabled
     *  PASSED IF: No option in public links to edit the content
     */
    @Test
    public void test_15_capability_allow_public_uploads()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Capability Public Uploads Start");

        //Disable capability "Allow users share via link"
        capabilities.setFilesSharingPublicUpload(CapabilityBooleanType.FALSE);

        SystemClock.sleep(WAIT_CONNECTION_MS);

        AccountsManager.saveCapabilities(capabilities, testServerURL, testUser);

        //Select share option
        selectShare(folder2);

        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Creation of the share link.
        onView(withId(R.id.addPublicLinkButton)).perform(click());

        onView(withId(R.id.shareViaLinkEditPermissionSwitch)).check(matches(not(isDisplayed())));
        if (isSupportedFileListing()) {
            onView(withId(R.id.shareViaShowFileListingSwitch)).check(matches(not(isDisplayed())));
        }

        Log_OC.i(LOG_TAG, "Test Capability Public Uploads Passed");

    }

    /**
     *  TEST CASE: Share public a file (default options)
     *  PASSED IF: Link created and visible in share view (message of "no links" does not appear)
     */
    @Test
    public void test_16_create_public_link_file()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Share Public File Start");
        SystemClock.sleep(WAIT_INITIAL_MS);

        //Select share option
        selectShare(file);

        //Check that no links are already created
        onView(withId(R.id.shareNoPublicLinks)).check(matches(isDisplayed()));

        //Depending the server version, send a name or not.
        if (capabilities.getVersionMayor() >= VERSION_10) {
            publicShareCreationDefault(nameShare);
        } else {
            publicShareCreationDefault(null);
        }

        //Check the name,only in the case of ownCloud >= 10
        if (capabilities.getVersionMayor() >= VERSION_10) {
            onView(withText(nameShare)).check(matches(isDisplayed()));
        }
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //The message of "not links created yet" is gone
        onView(withId(R.id.shareNoPublicLinks))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        Log_OC.i(LOG_TAG, "Test Public Share File Passed");

    }

    /**
     *  TEST CASE: Edit the public folder by enabling "Password" and "Expiration"
     *  PASSED IF: "Password" and "Expiration" are enabled
     *
     */
    @Test
    public void test_17_edit_options_file()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Enable Edit Options File Start");
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Select share option
        selectShare(file);

        //Edit the link
        onView(withId(R.id.editPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //By default, disabled
        onView(withId(R.id.shareViaLinkPasswordSwitch)).check(matches(not(isChecked())));
        onView(withId(R.id.shareViaLinkExpirationSwitch)).check(matches(not(isChecked())));

        //Allow editing not available for files
        onView(withId(R.id.shareViaLinkEditPermissionSwitch)).check(matches(not(isDisplayed())));
        onView(withId(R.id.shareViaShowFileListingSwitch)).check(matches(not(isDisplayed())));

        //Setting a password... no matter which one
        onView(withId(R.id.shareViaLinkPasswordSwitch)).perform(click());
        onView(withId(R.id.shareViaLinkPasswordValue)).perform(scrollTo(), replaceText("a"));

        //Enable "expiration"
        onView(withId(R.id.shareViaLinkExpirationSwitch)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.confirmAddPublicLinkButton)).perform(scrollTo(), click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Skip the sharing panel
        pressBack();
        SystemClock.sleep(WAIT_CONNECTION_MS);

        onView(withId(R.id.editPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check the status of the sharing options
        onView(withId(R.id.shareViaLinkPasswordSwitch)).check(matches(isChecked()));
        onView(withId(R.id.shareViaLinkExpirationSwitch)).check(matches(isChecked()));

        onView(withId(R.id.cancelAddPublicLinkButton)).perform(scrollTo(), click());

        Log_OC.i(LOG_TAG, "Test Enable Edit Options File Passed");

    }

    /**
     *  TEST CASE: Remove public link on a file
     *  PASSED IF: No links in share view
     */
    @Test
    public void test_18_unshare_public_file()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Unshare Public File Start");
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Select share option
        selectShare(file);

        //Check that a public link exists
        onView(withId(R.id.shareNoPublicLinks))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        //Delete link
        onView(withId(R.id.deletePublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        onView(withId(android.R.id.button1)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check that there are no public links
        onView(withId(R.id.shareNoPublicLinks)).check(matches(isDisplayed()));

        Log_OC.i(LOG_TAG, "Test Unshare Public File Passed");

    }


    //To create a new public link with defaults
    private void publicShareCreationDefault (String name) {

        //Creation of the share link. Name only for servers >= 10
        onView(withId(R.id.addPublicLinkButton)).perform(click());

        //Check server version and parameter null (or not) to handle the link name
        if (capabilities.getVersionMayor() >= VERSION_10 && name!=null) {
            onView(withId(R.id.shareViaLinkNameValue)).perform(replaceText(name));
        }

        SystemClock.sleep(WAIT_CONNECTION_MS);
        onView(withId(R.id.confirmAddPublicLinkButton)).perform(scrollTo(),click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check that the sharing panel is displayed
        onView(withId(R.id.parentPanel)).check(matches(isDisplayed()));
        onView(withId(R.id.alertTitle)).check(matches(isDisplayed()));
        pressBack();

        SystemClock.sleep(WAIT_CONNECTION_MS);

    }

    //To create a new public link with all options enabled
    private void publicShareCreationAllEnabled (String name) {

        //Creation of the share link. Name only for servers >= 10
        onView(withId(R.id.addPublicLinkButton)).perform(click());

        //Check server version and parameter null (or not) to handle the link name
        if (capabilities.getVersionMayor() >= VERSION_10 && name!=null) {
            onView(withId(R.id.shareViaLinkNameValue)).perform(replaceText(name));
        }

        //Enable all options
        onView(withId(R.id.shareViaLinkEditPermissionSwitch)).perform(click());
        onView(withId(R.id.shareViaLinkPasswordSwitch)).perform(click());
        onView(withId(R.id.shareViaLinkPasswordValue)).perform(scrollTo(), replaceText("a"));
        onView(withId(R.id.shareViaLinkExpirationSwitch)).perform(scrollTo(), click());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.confirmAddPublicLinkButton)).perform(scrollTo(), click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check that the sharing panel is displayed
        onView(withId(R.id.parentPanel)).check(matches(isDisplayed()));
        onView(withId(R.id.alertTitle)).check(matches(isDisplayed()));
        pressBack();

        SystemClock.sleep(WAIT_CONNECTION_MS);

    }


    //Returns the permission of writing in device storage
    private int grantedPermission () {
        return ContextCompat.checkSelfPermission(targetContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }


    //Get copied link from clipboard
    private String getTextFromClipboard(){
        //Clipboard can not be handled in thread without Looper
        ClipboardManager clipboard = (ClipboardManager)
                targetContext.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null) {
            // Gets the first item from the clipboard data
            ClipData.Item item = clip.getItemAt(0);
            return item.getText().toString();
        } else
            return null;
    }


    //True if server supports File Listing option
    private boolean isSupportedFileListing (){
            return capabilities.getFilesSharingPublicSupportsUploadOnly() == CapabilityBooleanType.TRUE ? true : false;
    }


    //True if server supports multiple public links
    private boolean isSupportedMultipleLinks (){
        return capabilities.getFilesSharingPublicMultiple()  == CapabilityBooleanType.TRUE ? true : false;
    }

    //Delete a link. For non multiple servers.
    private void deleteLink(){
        onView(withId(R.id.deletePublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        onView(withId(android.R.id.button1)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);
    }

    //Delete a link which name is parameter
    private void deleteLink(String linkName){
        onView(allOf(withId(R.id.deletePublicLinkButton), hasSibling(withText(linkName)))).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        onView(withId(android.R.id.button1)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);
    }

    //Select "Share" option on a item in file list. Repeats until long click works.
    private void selectShare(String item){
        boolean longClicked = false;
        while (!longClicked) {
            onView(withText(item)).perform(longClick());
            SystemClock.sleep(WAIT_CONNECTION_MS);
            if (!viewIsDisplayed(R.id.action_share_file)) {
                onView(withContentDescription("Navigate up")).perform(click());
            } else {
                longClicked = true;
            }
        }
        FileManager.selectOptionActionsMenu(targetContext, R.string.action_share);
    }

    //Check if a view is displayed
    public static boolean viewIsDisplayed(int viewId)  {
        final boolean[] isDisplayed = {true};
        onView(withId(viewId)).withFailureHandler(new FailureHandler()
        {
            @Override
            public void handle(Throwable error, Matcher<View> viewMatcher)
            {
                isDisplayed[0] = false;
            }
        }).check(matches(isDisplayed()));
        return isDisplayed[0];
    }

    @After
    public void tearDown() throws Exception {
        AccountsManager.deleteAllAccounts(targetContext);
    }

}
