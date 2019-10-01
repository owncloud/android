/**
 * ownCloud Android client application
 *
 * @author Andy Scherzinger
 * @author Christian Schabesberger
 * @author David González Verdugo
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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.DisplayCutout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.owncloud.android.BuildConfig;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.datamodel.UserProfile;
import com.owncloud.android.datamodel.UserProfilesRepository;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.PreferenceUtils;

/**
 * Base class to handle setup of the drawer implementation including user switching and avatar fetching and fallback
 * generation.
 */
public abstract class DrawerActivity extends ToolbarActivity {

    private static final String TAG = DrawerActivity.class.getSimpleName();
    private static final String KEY_IS_ACCOUNT_CHOOSER_ACTIVE = "IS_ACCOUNT_CHOOSER_ACTIVE";
    private static final String KEY_CHECKED_MENU_ITEM = "CHECKED_MENU_ITEM";
    private static final int ACTION_MANAGE_ACCOUNTS = 101;
    private static final int MENU_ORDER_ACCOUNT = 1;
    private static final int MENU_ORDER_ACCOUNT_FUNCTION = 2;
    private static final int USER_ITEMS_ALLOWED_BEFORE_REMOVING_CLOUD = 4;

    private float mMenuAccountAvatarRadiusDimension;
    private float mCurrentAccountAvatarRadiusDimension;
    private float mOtherAccountAvatarRadiusDimension;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView mNavigationView;
    private ImageView mAccountChooserToggle;
    private ImageView mAccountMiddleAccountAvatar;
    private ImageView mAccountEndAccountAvatar;
    private ImageView mDrawerLogo;

    private boolean mIsAccountChooserActive;
    private int mCheckedMenuItem = Menu.NONE;

    /**
     * accounts for the (max) three displayed accounts in the drawer header.
     */
    private Account[] mAccountsWithAvatars = new Account[3];

    private TextView mAccountQuotaText;

    /**
     * Initializes the drawer, its content and highlights the menu item with the given id.
     * This method needs to be called after the content view has been set.
     *
     * @param menuItemId the menu item to be checked/highlighted
     */
    protected void setupDrawer(int menuItemId) {
        setupDrawer();
        setDrawerMenuItemChecked(menuItemId);
    }

    /**
     * Initializes the drawer and its content.
     * This method needs to be called after the content view has been set.
     */
    protected void setupDrawer() {
        mDrawerLayout = findViewById(R.id.drawer_layout);

        // Allow or disallow touches with other visible windows
        mDrawerLayout.setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)
        );

        mNavigationView = findViewById(R.id.nav_view);

        // Allow or disallow touches with other visible windows
        mNavigationView.setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)
        );

        if (mNavigationView != null) {
            mDrawerLogo = findViewById(R.id.drawer_logo);

            // Set background header image and logo, if any
            if (getResources().getBoolean(R.bool.use_drawer_background_header)) {
                ((ImageView) findNavigationViewChildById(R.id.drawer_header_background))
                        .setImageResource(R.drawable.drawer_header_background);
            }

            if (mDrawerLogo != null && getResources().getBoolean(R.bool.use_drawer_logo)) {
                mDrawerLogo.setImageResource(R.drawable.drawer_logo);
            }

            mAccountChooserToggle = (ImageView) findNavigationViewChildById(R.id.drawer_account_chooser_toogle);
            mAccountChooserToggle.setImageResource(R.drawable.ic_down);
            mIsAccountChooserActive = false;

            mAccountMiddleAccountAvatar = (ImageView) findNavigationViewChildById(R.id.drawer_account_middle);
            mAccountEndAccountAvatar = (ImageView) findNavigationViewChildById(R.id.drawer_account_end);

            // on pre lollipop the light theme adds a black tint to icons with white coloring
            // ruining the generic avatars, so tinting for icons is deactivated pre lollipop
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                mNavigationView.setItemIconTintList(null);
            }

            //Notch support
            mNavigationView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        DisplayCutout displayCutout = v.getRootWindowInsets().getDisplayCutout();

                        if (displayCutout != null) {
                            RelativeLayout rlDrawerActiveUser =
                                    (RelativeLayout) findNavigationViewChildById(R.id.drawer_active_user);

                            int orientation = getResources().getConfiguration().orientation;
                            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                                int displayCutoutDP = (displayCutout.getSafeInsetTop()) /
                                                (getResources().getDisplayMetrics().densityDpi /
                                                        DisplayMetrics.DENSITY_DEFAULT);
                                rlDrawerActiveUser.getLayoutParams().height =
                                        (int) getResources().getDimension(R.dimen.nav_drawer_header_height) +
                                                displayCutoutDP;
                            } else {
                                rlDrawerActiveUser.getLayoutParams().height =
                                        (int) getResources().getDimension(R.dimen.nav_drawer_header_height);
                            }
                        }
                    }
                }
                @Override
                public void onViewDetachedFromWindow(View v) {
                }
            });

            setupDrawerContent(mNavigationView);

            findNavigationViewChildById(R.id.drawer_active_user)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            toggleAccountList();
                        }
                    });
        }

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                // standard behavior of drawer is to switch to the standard menu on closing
                if (mIsAccountChooserActive) {
                    toggleAccountList();
                }
                invalidateOptionsMenu();
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mDrawerToggle.setDrawerIndicatorEnabled(true);
                invalidateOptionsMenu();
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * setup drawer content, basically setting the item selected listener.
     *
     * @param navigationView the drawers navigation view
     */
    protected void setupDrawerContent(NavigationView navigationView) {
        // Disable help or feedback on customization
        if (!getResources().getBoolean(R.bool.help_enabled)) {
            navigationView.getMenu().removeItem(R.id.drawer_menu_help);
        }

        if (!getResources().getBoolean(R.bool.feedback_enabled)) {
            navigationView.getMenu().removeItem(R.id.drawer_menu_feedback);
        }

        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        mDrawerLayout.closeDrawers();

                        switch (menuItem.getItemId()) {
                            case R.id.nav_all_files:
                                menuItem.setChecked(true);
                                mCheckedMenuItem = menuItem.getItemId();
                                allFilesOption();
                                break;
                            case R.id.nav_uploads:
                                Intent uploadListIntent = new Intent(getApplicationContext(),
                                        UploadListActivity.class);
                                uploadListIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(uploadListIntent);
                                break;
                            case R.id.nav_only_available_offline:
                                menuItem.setChecked(true);
                                mCheckedMenuItem = menuItem.getItemId();
                                onlyAvailableOfflineOption();
                                break;
                            case R.id.nav_settings:
                                Intent settingsIntent = new Intent(getApplicationContext(),
                                        Preferences.class);
                                startActivity(settingsIntent);
                                break;
                            case R.id.drawer_menu_account_add:
                                createAccount(false);
                                break;
                            case R.id.drawer_menu_account_manage:
                                Intent manageAccountsIntent = new Intent(getApplicationContext(),
                                        ManageAccountsActivity.class);
                                startActivityForResult(manageAccountsIntent, ACTION_MANAGE_ACCOUNTS);
                                break;
                            case R.id.drawer_menu_feedback:
                                openFeedback();
                                break;
                            case R.id.drawer_menu_help:
                                openHelp();
                                break;
                            case Menu.NONE:
                                // account clicked
                                accountClicked(menuItem.getTitle().toString());
                            default:
                                Log_OC.i(TAG, "Unknown drawer menu item clicked: " + menuItem.getTitle());
                        }

                        return true;
                    }
                });

        // handle correct state
        if (mIsAccountChooserActive) {
            navigationView.getMenu().setGroupVisible(R.id.drawer_menu_accounts, true);

        } else {
            navigationView.getMenu().setGroupVisible(R.id.drawer_menu_accounts, false);
        }
    }

    private void openHelp() {
        final String helpWeb = (String) getText(R.string.url_help);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(helpWeb));
        startActivity(intent);
    }

    private void openFeedback() {
        String feedbackMail = (String) getText(R.string.mail_feedback);
        String feedback = "Android v" + BuildConfig.VERSION_NAME + " - " + getText(R.string.drawer_feedback);
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, feedback);

        intent.setData(Uri.parse(feedbackMail));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * sets the new/current account and restarts. In case the given account equals the actual/current account the
     * call will be ignored.
     *
     * @param accountName The account name to be set
     */
    private void accountClicked(String accountName) {
        if (!AccountUtils.getCurrentOwnCloudAccount(getApplicationContext()).name.equals(accountName)) {
            AccountUtils.setCurrentOwnCloudAccount(getApplicationContext(), accountName);
            restart();
        }
    }

    /**
     * click method for mini avatars in drawer header.
     *
     * @param view the clicked ImageView
     */
    public void onAccountDrawerClick(View view) {
        accountClicked(view.getContentDescription().toString());
    }

    /**
     * checks if the drawer exists and is opened.
     *
     * @return <code>true</code> if the drawer is open, else <code>false</code>
     */
    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    /**
     * closes the drawer.
     */
    public void closeDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    /**
     * opens the drawer.
     */
    public void openDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
    }

    /**
     * Enable or disable interaction with all drawers.
     *
     * @param lockMode The new lock mode for the given drawer. One of {@link DrawerLayout#LOCK_MODE_UNLOCKED},
     *                 {@link DrawerLayout#LOCK_MODE_LOCKED_CLOSED} or {@link DrawerLayout#LOCK_MODE_LOCKED_OPEN}.
     */
    public void setDrawerLockMode(int lockMode) {
        if (mDrawerLayout != null) {
            mDrawerLayout.setDrawerLockMode(lockMode);
        }
    }

    /**
     * Enable or disable the drawer indicator.
     *
     * @param enable <code>true</code> to enable, <code>false</code> to disable
     */
    public void setDrawerIndicatorEnabled(boolean enable) {
        if (mDrawerToggle != null) {
            mDrawerToggle.setDrawerIndicatorEnabled(enable);
        }
    }

    /**
     * updates the account list in the drawer.
     */
    private void updateAccountList() {
        Account[] accounts = AccountManager.get(this).getAccountsByType(MainApp.Companion.getAccountType());
        if (mNavigationView != null && mDrawerLayout != null) {
            if (accounts.length > 0) {
                repopulateAccountList(accounts);
                setAccountInDrawer(AccountUtils.getCurrentOwnCloudAccount(this));
                populateDrawerOwnCloudAccounts();

                // activate second/end account avatar
                if (mAccountsWithAvatars[1] != null) {
                    DisplayUtils.showAccountAvatar(
                            mAccountsWithAvatars[1],
                            (ImageView) findNavigationViewChildById(R.id.drawer_account_end),
                            mOtherAccountAvatarRadiusDimension,
                            false
                    );
                    mAccountEndAccountAvatar.setVisibility(View.VISIBLE);
                } else {
                    mAccountEndAccountAvatar.setVisibility(View.GONE);
                }

                // activate third/middle account avatar
                if (mAccountsWithAvatars[2] != null) {
                    DisplayUtils.showAccountAvatar(
                            mAccountsWithAvatars[2],
                            (ImageView) findNavigationViewChildById(R.id.drawer_account_middle),
                            mOtherAccountAvatarRadiusDimension,
                            false
                    );
                    mAccountMiddleAccountAvatar.setVisibility(View.VISIBLE);
                } else {
                    mAccountMiddleAccountAvatar.setVisibility(View.GONE);
                }
            } else {
                mAccountEndAccountAvatar.setVisibility(View.GONE);
                mAccountMiddleAccountAvatar.setVisibility(View.GONE);
            }
        }
    }

    /**
     * re-populates the account list.
     *
     * @param accounts list of accounts
     */
    private void repopulateAccountList(Account[] accounts) {
        // remove all accounts from list
        mNavigationView.getMenu().removeGroup(R.id.drawer_menu_accounts);

        // add all accounts to list
        for (int i = 0; i < accounts.length; i++) {
            if (!getAccount().name.equals(accounts[i].name)) {

                MenuItem accountMenuItem = mNavigationView.getMenu().add(
                        R.id.drawer_menu_accounts,
                        Menu.NONE,
                        MENU_ORDER_ACCOUNT,
                        accounts[i].name
                );
                ThumbnailsCacheManager.GetAvatarTask task =
                        new ThumbnailsCacheManager.GetAvatarTask(
                                accountMenuItem,
                                accounts[i],
                                mMenuAccountAvatarRadiusDimension,
                                false
                        );
                task.execute();
            }
        }

        // re-add add-account and manage-accounts
        mNavigationView.getMenu().add(R.id.drawer_menu_accounts, R.id.drawer_menu_account_add,
                MENU_ORDER_ACCOUNT_FUNCTION,
                getResources().getString(R.string.prefs_add_account)).setIcon(R.drawable.ic_plus_grey);
        mNavigationView.getMenu().add(R.id.drawer_menu_accounts, R.id.drawer_menu_account_manage,
                MENU_ORDER_ACCOUNT_FUNCTION,
                getResources().getString(R.string.drawer_manage_accounts)).setIcon(R.drawable.ic_group);

        // adding sets menu group back to visible, so safety check and setting invisible
        showMenu();
    }

    /**
     * Updates the quota in the drawer
     */
    private void updateQuota() {
        Account account = AccountUtils.getCurrentOwnCloudAccount(this);

        if (account == null) {
            return;
        }

        UserProfile.UserQuota userQuota = UserProfilesRepository.getUserProfilesRepository().getQuota(account.name);

        if (userQuota == null) {
            return;
        }

        ProgressBar accountQuotaBar = findViewById(R.id.account_quota_bar);

        TextView accountQuotaText = findViewById(R.id.account_quota_text);

        if (accountQuotaBar != null && accountQuotaText != null) {

            if (userQuota.getFree() < 0) { // Pending, unknown or unlimited free storage

                accountQuotaBar.setVisibility(View.VISIBLE);

                accountQuotaBar.setProgress(0);

                accountQuotaText.setText(
                        String.format(getString(R.string.drawer_unavailable_free_storage),
                                DisplayUtils.bytesToHumanReadable(userQuota.getUsed(), this))
                );

            } else if (userQuota.getFree() == 0) { // Quota 0, guest users

                accountQuotaBar.setVisibility(View.GONE);

                accountQuotaText.setText(getString(R.string.drawer_unavailable_used_storage));

            } else { // Limited quota

                accountQuotaBar.setVisibility(View.VISIBLE);

                // Update progress bar rounding up to next int. Example: quota is 0.54 => 1
                accountQuotaBar.setProgress((int) Math.ceil(userQuota.getRelative()));

                accountQuotaText.setText(
                        String.format(getString(R.string.drawer_quota),
                                DisplayUtils.bytesToHumanReadable(userQuota.getUsed(), this),
                                DisplayUtils.bytesToHumanReadable(userQuota.getTotal(), this),
                                userQuota.getRelative()
                        )
                );

            }
        }
    }

    /**
     * Method that gets called on drawer menu click for 'All Files'.
     */
    public abstract void allFilesOption();

    /**
     * Method that gets called on drawer menu click for 'Available Offline'.
     */
    public abstract void onlyAvailableOfflineOption();

    /**
     * Updates title bar and home buttons (state and icon).
     * <p/>
     * Assumes that navigation drawer is NOT visible.
     */
    protected void updateActionBarTitleAndHomeButton(OCFile chosenFile) {
        super.updateActionBarTitleAndHomeButton(chosenFile);

        /// set home button properties
        if (mDrawerToggle != null) {
            mDrawerToggle.setDrawerIndicatorEnabled(isRoot(chosenFile));
        }
    }

    /**
     * Sets the given account data in the drawer in case the drawer is available. The account name is shortened
     * beginning from the @-sign in the username.
     *
     * @param account the account to be set in the drawer
     */
    protected void setAccountInDrawer(Account account) {
        if (mDrawerLayout != null && account != null) {
            TextView username = (TextView) findNavigationViewChildById(R.id.drawer_username);
            TextView usernameFull = (TextView) findNavigationViewChildById(R.id.drawer_username_full);
            usernameFull.setText(account.name);
            try {
                OwnCloudAccount oca = new OwnCloudAccount(account, this);
                username.setText(oca.getDisplayName());

            } catch (Exception e) {
                Log_OC.w(TAG, "Couldn't read display name of account; using account name instead");

                username.setText(AccountUtils.getUsernameOfAccount(account.name));
            }

            DisplayUtils.showAccountAvatar(
                    account,
                    (ImageView) findNavigationViewChildById(R.id.drawer_current_account),
                    mCurrentAccountAvatarRadiusDimension,
                    false
            );

            updateQuota();
        }
    }

    /**
     * Toggle between standard menu and account list including saving the state.
     */
    private void toggleAccountList() {
        mIsAccountChooserActive = !mIsAccountChooserActive;
        showMenu();
    }

    /**
     * depending on the #mIsAccountChooserActive flag shows the account chooser or the standard menu.
     */
    private void showMenu() {
        if (mNavigationView != null) {
            final int accountCount = AccountManager.get(this)
                    .getAccountsByType(MainApp.Companion.getAccountType()).length;

            if (mIsAccountChooserActive) {
                mAccountChooserToggle.setImageResource(R.drawable.ic_up);
                mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_accounts, true);
                mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_standard, false);
                mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_settings_etc, false);
                if (mDrawerLogo != null && accountCount > USER_ITEMS_ALLOWED_BEFORE_REMOVING_CLOUD) {
                    mDrawerLogo.setVisibility(View.GONE);
                }
            } else {
                mAccountChooserToggle.setImageResource(R.drawable.ic_down);
                mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_accounts, false);
                mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_standard, true);
                mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_settings_etc, true);
                if (mDrawerLogo != null) {
                    mDrawerLogo.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    /**
     * checks/highlights the provided menu item if the drawer has been initialized and the menu item exists.
     *
     * @param menuItemId the menu item to be highlighted
     */
    protected void setDrawerMenuItemChecked(int menuItemId) {
        if (mNavigationView != null && mNavigationView.getMenu() != null && mNavigationView.getMenu().findItem
                (menuItemId) != null) {
            mNavigationView.getMenu().findItem(menuItemId).setChecked(true);
            mCheckedMenuItem = menuItemId;
        } else {
            Log_OC.w(TAG, "setDrawerMenuItemChecked has been called with invalid menu-item-ID");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mIsAccountChooserActive = savedInstanceState.getBoolean(KEY_IS_ACCOUNT_CHOOSER_ACTIVE, false);
            mCheckedMenuItem = savedInstanceState.getInt(KEY_CHECKED_MENU_ITEM, Menu.NONE);
        }

        mCurrentAccountAvatarRadiusDimension = getResources()
                .getDimension(R.dimen.nav_drawer_header_avatar_radius);
        mOtherAccountAvatarRadiusDimension = getResources()
                .getDimension(R.dimen.nav_drawer_header_avatar_other_accounts_radius);
        mMenuAccountAvatarRadiusDimension = getResources()
                .getDimension(R.dimen.nav_drawer_menu_avatar_radius);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_IS_ACCOUNT_CHOOSER_ACTIVE, mIsAccountChooserActive);
        outState.putInt(KEY_CHECKED_MENU_ITEM, mCheckedMenuItem);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mIsAccountChooserActive = savedInstanceState.getBoolean(KEY_IS_ACCOUNT_CHOOSER_ACTIVE, false);
        mCheckedMenuItem = savedInstanceState.getInt(KEY_CHECKED_MENU_ITEM, Menu.NONE);

        // (re-)setup drawer state
        showMenu();

        // check/highlight the menu item if present
        if (mCheckedMenuItem > Menu.NONE || mCheckedMenuItem < Menu.NONE) {
            setDrawerMenuItemChecked(mCheckedMenuItem);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
            if (isDrawerOpen()) {
                mDrawerToggle.setDrawerIndicatorEnabled(true);
            }
        }
        updateAccountList();
        updateQuota();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onBackPressed() {
        if (isDrawerOpen()) {
            closeDrawer();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setDrawerMenuItemChecked(mCheckedMenuItem);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // update Account list and active account if Manage Account activity replies with
        // - ACCOUNT_LIST_CHANGED = true
        // - RESULT_OK
        if (requestCode == ACTION_MANAGE_ACCOUNTS
                && resultCode == RESULT_OK
                && data.getBooleanExtra(ManageAccountsActivity.KEY_ACCOUNT_LIST_CHANGED, false)) {

            // current account has changed
            if (data.getBooleanExtra(ManageAccountsActivity.KEY_CURRENT_ACCOUNT_CHANGED, false)) {
                setAccount(AccountUtils.getCurrentOwnCloudAccount(this));
                restart();
            } else {
                updateAccountList();
                updateQuota();
            }
        }
    }

    /**
     * Finds a view that was identified by the id attribute from the drawer header.
     *
     * @param id the view's id
     * @return The view if found or <code>null</code> otherwise.
     */
    private View findNavigationViewChildById(int id) {
        return ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0).findViewById(id);
    }

    /**
     * restart helper method which is called after a changing the current account.
     */
    protected abstract void restart();

    @Override
    protected void onAccountCreationSuccessful(AccountManagerFuture<Bundle> future) {
        super.onAccountCreationSuccessful(future);
        updateAccountList();
        updateQuota();
        restart();
    }

    /**
     * populates the avatar drawer array with the first three ownCloud {@link Account}s while the first element is
     * always the current account.
     */
    private void populateDrawerOwnCloudAccounts() {
        mAccountsWithAvatars = new Account[3];
        Account[] accountsAll = AccountManager.get(this).getAccountsByType
                (MainApp.Companion.getAccountType());
        Account currentAccount = AccountUtils.getCurrentOwnCloudAccount(this);

        mAccountsWithAvatars[0] = currentAccount;
        int j = 0;
        for (int i = 1; i <= 2 && i < accountsAll.length && j < accountsAll.length; j++) {
            if (!currentAccount.equals(accountsAll[j])) {
                mAccountsWithAvatars[i] = accountsAll[j];
                i++;
            }
        }
    }

    /**
     * Adds other listeners to react on changes of the drawer layout.
     *
     * @param listener      Object interested in changes of the drawer layout.
     */
    public void addDrawerListener(DrawerLayout.DrawerListener listener) {
        if (mDrawerLayout != null) {
            mDrawerLayout.addDrawerListener(listener);
        } else {
            Log_OC.e(TAG, "Drawer layout not ready to add drawer listener");
        }
    }
}