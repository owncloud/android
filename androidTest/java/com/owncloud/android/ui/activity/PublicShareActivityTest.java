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
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.v4.content.ContextCompat;
import android.test.suitebuilder.annotation.LargeTest;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.utils.AccountsManager;

import org.junit.After;
import org.junit.Before;
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
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
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

    private static final String ERROR_MESSAGE = "BAD LINK";
    private static final String RESULT_CODE = "mResultCode";
    private static final String LOG_TAG = "PublicShareSuite";
    private static final int VERSION_10 = 10;

    private Context targetContext = null;
    private static String folder = "Photos";
    private static String file = "ownCloud.pdf";
    private static final String nameShare = "$%@rter";
    private static final String nameShareEdited = "publicnameverylongtotest";
    private static final int GRANT_BUTTON_INDEX = 1;
    private int version = -1;

    private String testUser = null;
    private String testPassword = null;
    private String testServerURL = null;
    private enum ServerType {
        /*
         * Server with http
         */
        HTTP(1),

        /*
         * Server with https, but non-secure certificate
         */
        HTTPS_NON_SECURE(2),

        /*
         * Server with https
         */
        HTTPS_SECURE(3),

        /*
         * Server redirected to a non-secure server
         */
        REDIRECTED_NON_SECURE(4);

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
                    return HTTP;
                case 2:
                    return HTTPS_NON_SECURE;
                case 3:
                    return HTTPS_SECURE;
                case 4:
                    return REDIRECTED_NON_SECURE;
            }
            return null;
        }
    }
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

                    //Get Server Capabilities
                    capabilities = AccountsManager.getCapabilities(testServerURL, testUser, testPassword);

                }
            };


    @Before

    public void init() {

        // UiDevice available form API level 17

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

                SystemClock.sleep(WAIT_CONNECTION_MS);
                //Accept the untrusted certificate
                if (servertype == ServerType.HTTPS_NON_SECURE) {
                    onView(withId(R.id.ok)).perform(click());
                }
            }

        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }

        mActivityRule.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }


    /**
     *  Share publicly a folder (default options)
     */
    @Test
    public void test1_create_public_link()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Share Public Start");
        SystemClock.sleep(WAIT_INITIAL_MS);

        //Select share option
        onView(withText(folder)).perform(longClick());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        selectOptionActionsMenu(R.string.action_share);

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

        Log_OC.i(LOG_TAG, "Test Public Share Passed");

    }

    @Test
    public void test2_get_link()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Get Link Start");
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Select share option
        onView(withText(folder)).perform(longClick());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        selectOptionActionsMenu(R.string.action_share);

        //Get public link
        onView(withId(R.id.getPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check that the sharing panel is displayed
        onView(withId(R.id.parentPanel)).check(matches(isDisplayed()));
        onView(withId(R.id.alertTitle)).check(matches(isDisplayed()));

        //Get text copied in the clipboard
        onView(withText("Copy to clipboard")).perform(click());
        String text = getTextFromClipboard();
        SystemClock.sleep(WAIT_CLIPBOARD_MS);
        //check if the copied link is correct
        assertTrue(ERROR_MESSAGE, text.startsWith(testServerURL));

        Log_OC.i(LOG_TAG, "Test Get Link Passed");

    }

    @Test
    public void test3_edit_name() {

        Log_OC.i(LOG_TAG, "Test Edit Link Name Start");
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //if ownCloud >= 10, we can handle the link name. If not... skipping test.
        if (capabilities.getVersionMayor() >= VERSION_10) {

            //Select share option
            onView(withText(folder)).perform(longClick());
            SystemClock.sleep(WAIT_CONNECTION_MS);
            selectOptionActionsMenu(R.string.action_share);

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

            //Check the name,only in the case of ownCloud >= 10
            if (capabilities.getVersionMayor() >= VERSION_10) {
                onView(withText(nameShareEdited)).check(matches(isDisplayed()));
            }
            SystemClock.sleep(WAIT_CONNECTION_MS);
        }

        Log_OC.i(LOG_TAG, "Test Edit Link Name Passed");
    }

    @Test
    public void test4_enable_allow_edit()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Enable Allow Edit Start");
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Select share option
        onView(withText(folder)).perform(longClick());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        selectOptionActionsMenu(R.string.action_share);

        //Edit the link enabling the "allow edit option"
        onView(withId(R.id.editPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check file listing disabled + checked (default) if ownCloud >= 10
        if (capabilities.getVersionMayor() >= VERSION_10) {
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
        if (capabilities.getVersionMayor() >= VERSION_10) {
            onView(withId(R.id.shareViaShowFileListingSwitch)).check(matches(isEnabled()));
            onView(withId(R.id.shareViaShowFileListingSwitch)).check(matches(isChecked()));
        }
        onView(withId(R.id.shareViaLinkPasswordSwitch)).check(matches(not(isChecked())));
        onView(withId(R.id.shareViaLinkExpirationSwitch)).check(matches(not(isChecked())));

        onView(withId(R.id.cancelAddPublicLinkButton)).perform(scrollTo(), click());

        Log_OC.i(LOG_TAG, "Test Enable Allow Edit Passed");

    }

    @Test
    public void test5_filelisting()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test File Listing Start");

        //Select share option
        onView(withText(folder)).perform(longClick());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        selectOptionActionsMenu(R.string.action_share);

        //Edit the link disabling the "show file listing"
        onView(withId(R.id.editPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Only makes sense if ownCloud >= 10 and not 10.0.0
        if (capabilities.getVersionMayor() >= VERSION_10 &&
                (!(capabilities.getVersionMayor() == VERSION_10 &&
                capabilities.getVersionMinor() == 0 &&
                capabilities.getVersionMicro() == 0))) {

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

            onView(withId(R.id.cancelAddPublicLinkButton)).perform(scrollTo(), click());
            SystemClock.sleep(WAIT_CONNECTION_MS);

            onView(withId(R.id.editPublicLinkButton)).perform(click());
            SystemClock.sleep(WAIT_CONNECTION_MS);

            //Switching off to check "Show file listing" is checked and disabled
            onView(withId(R.id.shareViaLinkEditPermissionSwitch)).perform(click());

            //Check options status
            onView(withId(R.id.shareViaLinkEditPermissionSwitch)).check(matches(not(isChecked())));
            onView(withId(R.id.shareViaShowFileListingSwitch)).check(matches(not(isEnabled())));
            onView(withId(R.id.shareViaShowFileListingSwitch)).check(matches(isChecked()));
            onView(withId(R.id.shareViaLinkPasswordSwitch)).check(matches(not(isChecked())));
            onView(withId(R.id.shareViaLinkExpirationSwitch)).check(matches(not(isChecked())));

            //Switch on to following tests
            onView(withId(R.id.shareViaLinkEditPermissionSwitch)).perform(click());

        } else {
            onView(withId(R.id.shareViaShowFileListingSwitch)).check(matches(not(isDisplayed())));
        }


        Log_OC.i(LOG_TAG, "Test File Listing Passed");

    }

    @Test
    public void test6_enable_password()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Enable Password Start");
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Select share option
        onView(withText(folder)).perform(longClick());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        selectOptionActionsMenu(R.string.action_share);

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

    @Test
    public void test7_enable_expiration()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Enable Expiration Start");
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Select share option
        onView(withText(folder)).perform(longClick());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        selectOptionActionsMenu(R.string.action_share);

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

    @Test
    public void test8_unshare_public()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Unshare Public Start");
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Select share option
        onView(withText(folder)).perform(longClick());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        selectOptionActionsMenu(R.string.action_share);

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

        Log_OC.i(LOG_TAG, "Test Unshare Public Passed");

    }

    @Test
    public void test9_share_multiple()
            throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Log_OC.i(LOG_TAG, "Test Share Multiple Public Start");
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Select share option
        onView(withText(folder)).perform(longClick());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        selectOptionActionsMenu(R.string.action_share);

        if (capabilities.getVersionMayor() >= VERSION_10) {
            onView(withId(R.id.addPublicLinkButton)).check(matches(isDisplayed()));
            for (int i = 0; i < 5 ; i++) {
                publicShareCreationDefault(null);
            }

        } else {  //Servers < 10 hide the button
            publicShareCreationDefault(null);
            onView(withId(R.id.addPublicLinkButton))
                    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE)));
        }

        Log_OC.i(LOG_TAG, "Test Unshare Public Passed");

    }



    //To create a new public link
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


    //Returns the permission of writing in device storage
    private int grantedPermission () {
        return ContextCompat.checkSelfPermission(targetContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    //Get copied link from clipboard
    private String getTextFromClipboard(){
        //Clipboard can not be handled in thread without Looper
        Looper.prepare();
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

    //To select an option in files view
    private void selectOptionActionsMenu (int option) {
        String optionSelected = targetContext.getResources().getString(option);
        if (!new UiObject(new UiSelector().description(optionSelected)).exists()) {
            onView(allOf(withContentDescription("More options"),
                    isDescendantOfA(withId(R.id.toolbar)))).perform(click());
            switch (option) {
                case R.string.action_share:
                    onView(withId(R.id.action_share_file)).perform(click());
                    break;
                default:
                    break;
            }

        } else {
            switch (option) {
                case R.string.action_share:
                    onView(withId(R.id.action_share_file)).perform(click());
                    break;
                default:
                    break;
            }
        }
    }

    @After
    public void tearDown() throws Exception {
        AccountsManager.deleteAllAccounts(targetContext);
    }

}