/**
 * ownCloud Android client application
 *
 * @author Jesús Recio @jesmrec
 * @author Christian Schabesberger
 * Copyright (C) 2019 ownCloud GmbH.
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
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.FailureHandler;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.CapabilityBooleanType;
import com.owncloud.android.lib.resources.status.RemoteCapability;
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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
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
    private RemoteCapability capabilities;

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
                if (!uiDevice.isScreenOn()) {
                    uiDevice.wakeUp();
                }
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
    public static void before() {
        //Needed to use clipboard
        Looper.prepare();
    }

    /**
     *  TEST CASE: Share publicly a folder (default options)
     *  PASSED IF: Link created and visible in share view (message of "no links" does not appear)
     */
    @Test
    public void test_01_create_public_link_folder_defaults()
            throws IllegalArgumentException {

        Log_OC.i(LOG_TAG, "Test Share Public Defaults Start");
        SystemClock.sleep(WAIT_INITIAL_MS);

        //Select share option
        selectShare(folder);

        //Check that no links are already created
        onView(withId(R.id.shareNoPublicLinks)).check(matches(isDisplayed()));

        publicShareCreationDefault(nameShare);

        onView(withText(nameShare)).check(matches(isDisplayed()));
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //The message of "not links created yet" is gone
        onView(withId(R.id.shareNoPublicLinks))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        Log_OC.i(LOG_TAG, "Test Share Public Defaults Passed");

    }

    /**
     *  TEST CASE: Share publicly a folder with Download/View permission
     *  PASSED IF: Link created and visible in share view with Download/View option
     *
     */
    @Test
    public void test_02_create_public_link_download_view_permission()
            throws IllegalArgumentException {

        Log_OC.i(LOG_TAG, "Test Share Public with Download/View");
        SystemClock.sleep(WAIT_INITIAL_MS);

        //Select share option
        selectShare(folder2);

        //Check that no links are already created
        onView(withId(R.id.shareNoPublicLinks)).check(matches(isDisplayed()));
        SystemClock.sleep(WAIT_CONNECTION_MS);
        onView(withId(R.id.addPublicLinkButton)).perform(click());
        onView(withId(R.id.shareViaLinkNameValue)).perform(replaceText(nameShare));

        //Enable the option for Download/View permissions
        onView(withId(R.id.shareViaLinkEditPermissionReadOnly)).perform(click());

        onView(withId(R.id.saveButton)).perform(scrollTo(), click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check that the sharing panel is displayed
        onView(withId(R.id.parentPanel)).check(matches(isDisplayed()));
        onView(withId(R.id.alertTitle)).check(matches(isDisplayed()));
        pressBack();

        //Check the name
        onView(withText(nameShare)).check(matches(isDisplayed()));
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //The message of "not links created yet" is gone
        onView(withId(R.id.shareNoPublicLinks))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        onView(withId(R.id.editPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check the status of the sharing options
        onView(withId(R.id.shareViaLinkEditPermissionReadOnly)).check(matches(isChecked()));

        onView(withId(R.id.shareViaLinkPasswordSwitch)).check(matches(not(isChecked())));
        onView(withId(R.id.shareViaLinkExpirationSwitch)).check(matches(not(isChecked())));

        onView(withId(R.id.cancelButton)).perform(scrollTo(), click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Remove the link
        deleteLink();

        Log_OC.i(LOG_TAG, "Test Share Public with Download/View");

    }

    /**
     *  TEST CASE: Share publicly a folder with Download/View/Upload permission
     *  PASSED IF: Link created and visible in share view with Download/View/Upload option
     *
     */
    @Test
    public void test_03_create_public_link_download_view_upload_permission()
            throws IllegalArgumentException {

        Log_OC.i(LOG_TAG, "Test Share Public with Download/View/Upload");
        SystemClock.sleep(WAIT_INITIAL_MS);

        //Select share option
        selectShare(folder2);

        //Check that no links are already created
        onView(withId(R.id.shareNoPublicLinks)).check(matches(isDisplayed()));
        onView(withId(R.id.addPublicLinkButton)).perform(click());
        onView(withId(R.id.shareViaLinkNameValue)).perform(replaceText(nameShare));

        //Enable the option for Download/View/Upload permissions
        onView(withId(R.id.shareViaLinkEditPermissionReadAndWrite)).perform(click());

        onView(withId(R.id.saveButton)).perform(scrollTo(), click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check that the sharing panel is displayed
        onView(withId(R.id.parentPanel)).check(matches(isDisplayed()));
        onView(withId(R.id.alertTitle)).check(matches(isDisplayed()));
        pressBack();

        //Check the name
        onView(withText(nameShare)).check(matches(isDisplayed()));
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //The message of "not links created yet" is gone
        onView(withId(R.id.shareNoPublicLinks))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        onView(withId(R.id.editPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check the status of the sharing options
        onView(withId(R.id.shareViaLinkEditPermissionReadAndWrite)).check(matches(isChecked()));

        onView(withId(R.id.shareViaLinkPasswordSwitch)).check(matches(not(isChecked())));
        onView(withId(R.id.shareViaLinkExpirationSwitch)).check(matches(not(isChecked())));

        onView(withId(R.id.cancelButton)).perform(scrollTo(), click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Remove the link
        deleteLink();

        Log_OC.i(LOG_TAG, "Test Share Public with Download/View/Upload");

    }

    /**
     *  TEST CASE: Share publicly a folder with Upload only permission
     *  PASSED IF: Link created and visible in share view with Upload only option
     *
     */
    @Test
    public void test_04_create_public_link_upload_only()
            throws IllegalArgumentException {

        Log_OC.i(LOG_TAG, "Test Share Public with Upload only");
        SystemClock.sleep(WAIT_INITIAL_MS);

        //Select share option
        selectShare(folder2);

        //Check that no links are already created
        onView(withId(R.id.shareNoPublicLinks)).check(matches(isDisplayed()));
        onView(withId(R.id.addPublicLinkButton)).perform(click());
        onView(withId(R.id.shareViaLinkNameValue)).perform(replaceText(nameShare));

        //Enable the option for Upload only permissions
        onView(withId(R.id.shareViaLinkEditPermissionUploadFiles)).perform(click());

        onView(withId(R.id.saveButton)).perform(scrollTo(), click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check that the sharing panel is displayed
        onView(withId(R.id.parentPanel)).check(matches(isDisplayed()));
        onView(withId(R.id.alertTitle)).check(matches(isDisplayed()));
        pressBack();

        //Check the name
        onView(withText(nameShare)).check(matches(isDisplayed()));
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //The message of "not links created yet" is gone
        onView(withId(R.id.shareNoPublicLinks))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        onView(withId(R.id.editPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check the status of the sharing options
        onView(withId(R.id.shareViaLinkEditPermissionUploadFiles)).check(matches(isChecked()));

        onView(withId(R.id.shareViaLinkPasswordSwitch)).check(matches(not(isChecked())));
        onView(withId(R.id.shareViaLinkExpirationSwitch)).check(matches(not(isChecked())));

        onView(withId(R.id.cancelButton)).perform(scrollTo(), click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Remove the link
        deleteLink();

        Log_OC.i(LOG_TAG, "Test Share Public with Upload only");

    }

    /**
     *  TEST CASE: Share public a folder and gets the link
     *  PASSED IF: Link correctly copied in clipboard
     */
    @Test
    public void test_05_get_link()
            throws IllegalArgumentException {

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
     *  TEST CASE: Edit the name of a public folder.
     *  PASSED IF: Link has the new name in share view
     */
    @Test
    public void test_06_edit_name() {

        Log_OC.i(LOG_TAG, "Test Edit Link Name Start");
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Select share option
        selectShare(folder);

        //Edit the link name
        onView(withId(R.id.editPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        onView(withId(R.id.shareViaLinkNameValue)).perform(replaceText(nameShareEdited));

        SystemClock.sleep(WAIT_CONNECTION_MS);
        onView(withId(R.id.saveButton)).perform(scrollTo(), click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check that the sharing panel is displayed
        onView(withId(R.id.parentPanel)).check(matches(isDisplayed()));
        onView(withId(R.id.alertTitle)).check(matches(isDisplayed()));
        pressBack();

        //Check the name
        onView(withText(nameShareEdited)).check(matches(isDisplayed()));

        Log_OC.i(LOG_TAG, "Test Edit Link Name Passed");
    }

    /**
     *  TEST CASE: Edit the public folder by switching "Password" on
     *  PASSED IF:
     *          - "Password" enabled
     *          - "Expiration" disabled
     */
    @Test
    public void test_07_edit_link_enable_password()
            throws IllegalArgumentException {

        Log_OC.i(LOG_TAG, "Test Enable Password Start");
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Select share option
        selectShare(folder);

        //Edit the link enabling the "enabling password"
        onView(withId(R.id.editPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        onView(withId(R.id.shareViaLinkPasswordSwitch)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Setting a password... no matter which one
        onView(withId(R.id.shareViaLinkPasswordValue)).perform(scrollTo(), replaceText("a"));
        onView(withId(R.id.saveButton)).perform(scrollTo(), click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Skipping the panel
        pressBack();
        SystemClock.sleep(WAIT_CONNECTION_MS);

        onView(withId(R.id.editPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check the status of the sharing options
        onView(withId(R.id.shareViaLinkPasswordSwitch)).check(matches(isChecked()));
        onView(withId(R.id.shareViaLinkExpirationSwitch)).check(matches(not(isChecked())));

        onView(withId(R.id.cancelButton)).perform(scrollTo(), click());

        Log_OC.i(LOG_TAG, "Test Enable Password Passed");

    }

    /**
     *  TEST CASE: Edit the public folder by switching "Expiration Date" on and "Password" off
     *  PASSED IF:
     *          - "Expiration" enabled
     *          - "Password" disabled
     */
    @Test
    public void test_08_edit_link_enable_expiration()
            throws IllegalArgumentException {

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
        onView(withId(R.id.saveButton)).perform(scrollTo(), click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Skip the sharing panel
        pressBack();
        SystemClock.sleep(WAIT_CONNECTION_MS);

        onView(withId(R.id.editPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check the status of the sharing options
        onView(withId(R.id.shareViaLinkPasswordSwitch)).check(matches(not(isChecked())));
        onView(withId(R.id.shareViaLinkExpirationSwitch)).check(matches(isChecked()));

        onView(withId(R.id.cancelButton)).perform(scrollTo(), click());

        Log_OC.i(LOG_TAG, "Test Enable Expiration Passed");

    }

    /**
     *  TEST CASE: Remove public link
     *  PASSED IF: No links in share view
     */
    @Test
    public void test_09_unshare_public()
            throws IllegalArgumentException {

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
     *  PASSED IF: Public links created correctly
     */
    @Test
    public void test_10_share_multiple_links()
            throws IllegalArgumentException {

        Log_OC.i(LOG_TAG, "Test Share Multiple Public Start");
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Select share option
        selectShare(folder);

        onView(withId(R.id.addPublicLinkButton)).check(matches(isDisplayed()));
        for (int i = 0; i < MULTIPLE_LINKS; i++) {
            publicShareCreationDefault(nameShareMultiple + i);
        }

        Log_OC.i(LOG_TAG, "Test Unshare Public Passed");

    }

    /**
     *  TEST CASE: Remove all links
     *  PASSED IF: No links fot the item
     */
    @Test
    public void test_11_remove_multiple_links()
            throws IllegalArgumentException {

        Log_OC.i(LOG_TAG, "Test Remove Multiple Public Start");
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Select share option
        selectShare(folder);

        for (int i = 0; i < MULTIPLE_LINKS; i++) {
            deleteLink(nameShareMultiple + i);
        }

        Log_OC.i(LOG_TAG, "Test Remove Multiple Public Passed");

    }

    /**
     *  TEST CASE: Check permalink
     *  PASSED IF: Link to the item in clipboard
     */
    @Test
    public void test_12_permalink()
            throws IllegalArgumentException {

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

        assertTrue(ERROR_MESSAGE, text.startsWith(testServerURL + "/f"));

        Log_OC.i(LOG_TAG, "Test Permalink Passed");

    }

    /**
     *  TEST CASE: Capability "Allow public links" disabled
     *  PASSED IF: No option to public in share view
     */
    @Test
    public void test_13_capability_allow_public_links()
            throws IllegalArgumentException {

        Log_OC.i(LOG_TAG, "Test Capability Public Links Start");

        //Disable capability "Allow users share via link"
        capabilities.setFilesSharingPublicEnabled(CapabilityBooleanType.FALSE);

        SystemClock.sleep(WAIT_CONNECTION_MS);

        AccountsManager.saveCapabilities(targetContext, capabilities, testServerURL, testUser);

        //Select share option
        selectShare(folder2);

        SystemClock.sleep(WAIT_CONNECTION_MS);

        onView(withId(R.id.addPublicLinkButton)).check(matches(not(isDisplayed())));

        Log_OC.i(LOG_TAG, "Test Capability Public Links Passed");

    }

    /**
     *  TEST CASE: Capability "Allow uploads" disabled
     *  PASSED IF: Only Download/View option displayed
     */
    @Test
    public void test_14_capability_allow_public_uploads()
            throws IllegalArgumentException {

        Log_OC.i(LOG_TAG, "Test Capability Public Uploads Start");

        //Disable capability "Allow users share via link"
        capabilities.setFilesSharingPublicUpload(CapabilityBooleanType.FALSE);

        SystemClock.sleep(WAIT_CONNECTION_MS);

        AccountsManager.saveCapabilities(targetContext, capabilities, testServerURL, testUser);

        //Select share option
        selectShare(folder2);

        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Creation of the share link.
        onView(withId(R.id.addPublicLinkButton)).perform(click());

        onView(withId(R.id.shareViaLinkEditPermissionReadOnly)).check(matches(not(isDisplayed())));
        onView(withId(R.id.shareViaLinkEditPermissionReadAndWrite)).check(matches(not(isDisplayed())));
        onView(withId(R.id.shareViaLinkEditPermissionUploadFiles)).check(matches(not(isDisplayed())));

        Log_OC.i(LOG_TAG, "Test Capability Public Uploads Passed");

    }

    /**
     *  TEST CASE: Share public a file (default options)
     *  PASSED IF: Link created and visible in share view (message of "no links" does not appear)
     */
    @Test
    public void test_15_create_public_link_file()
            throws IllegalArgumentException {

        Log_OC.i(LOG_TAG, "Test Share Public File Start");
        SystemClock.sleep(WAIT_INITIAL_MS);

        //Select share option
        selectShare(file);

        //Check that no links are already created
        onView(withId(R.id.shareNoPublicLinks)).check(matches(isDisplayed()));

        publicShareCreationDefault(nameShare);

        onView(withText(nameShare)).check(matches(isDisplayed()));
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
    public void test_16_edit_options_file()
            throws IllegalArgumentException {

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

        //Setting a password... no matter which one
        onView(withId(R.id.shareViaLinkPasswordSwitch)).perform(click());
        onView(withId(R.id.shareViaLinkPasswordValue)).perform(scrollTo(), replaceText("a"));

        //Enable "expiration"
        onView(withId(R.id.shareViaLinkExpirationSwitch)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.saveButton)).perform(scrollTo(), click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Skip the sharing panel
        pressBack();
        SystemClock.sleep(WAIT_CONNECTION_MS);

        onView(withId(R.id.editPublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check the status of the sharing options
        onView(withId(R.id.shareViaLinkPasswordSwitch)).check(matches(isChecked()));
        onView(withId(R.id.shareViaLinkExpirationSwitch)).check(matches(isChecked()));

        onView(withId(R.id.cancelButton)).perform(scrollTo(), click());

        Log_OC.i(LOG_TAG, "Test Enable Edit Options File Passed");

    }

    /**
     *  TEST CASE: Remove public link on a file
     *  PASSED IF: No links in share view
     */
    @Test
    public void test_17_unshare_public_file()
            throws IllegalArgumentException {

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
    private void publicShareCreationDefault(String name) {

        //Creation of the share link. Name only for servers >= 10
        onView(withId(R.id.addPublicLinkButton)).perform(click());

        onView(withId(R.id.shareViaLinkNameValue)).perform(replaceText(name));

        SystemClock.sleep(WAIT_CONNECTION_MS);
        onView(withId(R.id.saveButton)).perform(scrollTo(), click());
        SystemClock.sleep(WAIT_CONNECTION_MS);

        //Check that the sharing panel is displayed
        onView(withId(R.id.parentPanel)).check(matches(isDisplayed()));
        onView(withId(R.id.alertTitle)).check(matches(isDisplayed()));
        pressBack();

        SystemClock.sleep(WAIT_CONNECTION_MS);

    }

    //Returns the permission of writing in device storage
    private int grantedPermission() {
        return ContextCompat.checkSelfPermission(targetContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    //Get copied link from clipboard
    private String getTextFromClipboard() {
        //Clipboard can not be handled in thread without Looper
        ClipboardManager clipboard = (ClipboardManager)
                targetContext.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null) {
            // Gets the first item from the clipboard data
            ClipData.Item item = clip.getItemAt(0);
            return item.getText().toString();
        } else {
            return null;
        }
    }

    //True if server supports File Listing option
    private boolean isSupportedFileListing() {
        return capabilities.getFilesSharingPublicSupportsUploadOnly() == CapabilityBooleanType.TRUE;
    }

    //Delete a link. For non multiple servers.
    private void deleteLink() {
        onView(withId(R.id.deletePublicLinkButton)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        onView(withId(android.R.id.button1)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);
    }

    //Delete a link which name is parameter
    private void deleteLink(String linkName) {
        onView(allOf(withId(R.id.deletePublicLinkButton), hasSibling(withText(linkName)))).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);
        onView(withId(android.R.id.button1)).perform(click());
        SystemClock.sleep(WAIT_CONNECTION_MS);
    }

    //Select "Share" option on a item in file list. Repeats until long click works.
    private void selectShare(String item) {
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
    public static boolean viewIsDisplayed(int viewId) {
        final boolean[] isDisplayed = {true};
        onView(withId(viewId)).withFailureHandler(new FailureHandler() {
            @Override
            public void handle(Throwable error, Matcher<View> viewMatcher) {
                isDisplayed[0] = false;
            }
        }).check(matches(isDisplayed()));
        return isDisplayed[0];
    }

    @After
    public void tearDown() {
        AccountsManager.deleteAllAccounts(targetContext);
    }

}
