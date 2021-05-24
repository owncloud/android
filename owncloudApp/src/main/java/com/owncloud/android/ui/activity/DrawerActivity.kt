/*
 * ownCloud Android client application
 *
 * @author Andy Scherzinger
 * @author Christian Schabesberger
 * @author David González Verdugo
 * @author Shashvat Kedia
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.ui.activity

import android.accounts.Account
import android.accounts.AccountManagerFuture
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.owncloud.android.BuildConfig
import com.owncloud.android.MainApp.Companion.initDependencyInjection
import com.owncloud.android.R
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.extensions.goToUrl
import com.owncloud.android.extensions.sendEmail
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.ui.settings.SettingsActivity
import com.owncloud.android.presentation.viewmodels.drawer.DrawerViewModel
import com.owncloud.android.utils.AvatarUtils
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.PreferenceUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import kotlin.math.ceil

/**
 * Base class to handle setup of the drawer implementation including user switching and avatar fetching and fallback
 * generation.
 */
abstract class DrawerActivity : ToolbarActivity() {

    private val drawerViewModel by viewModel<DrawerViewModel>()

    private var menuAccountAvatarRadiusDimension = 0f
    private var currentAccountAvatarRadiusDimension = 0f
    private var otherAccountAvatarRadiusDimension = 0f

    private var drawerToggle: ActionBarDrawerToggle? = null

    private var isAccountChooserActive = false
    private var checkedMenuItem = Menu.NONE

    /**
     * accounts for the (max) three displayed accounts in the drawer header.
     */
    private var accountsWithAvatars = arrayOfNulls<Account>(3)

    /**
     * Initializes the drawer and its content.
     * This method needs to be called after the content view has been set.
     */
    protected open fun setupDrawer() {

        // Allow or disallow touches with other visible windows
        getDrawerLayout()?.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)
        getNavView()?.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)

        // Set background header image and logo, if any
        if (resources.getBoolean(R.bool.use_drawer_background_header)) {
            getDrawerHeaderBackground()?.setImageResource(R.drawable.drawer_header_background)
        }
        if (resources.getBoolean(R.bool.use_drawer_logo)) {
            getDrawerLogo()?.setImageResource(R.drawable.drawer_logo)
        }

        getDrawerAccountChooserToogle()?.setImageResource(R.drawable.ic_down)
        isAccountChooserActive = false

        //Notch support
        getNavView()?.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    v.rootWindowInsets.displayCutout?.let {
                        getDrawerActiveUser()?.layoutParams?.height =
                            DisplayUtils.getDrawerHeaderHeight(it.safeInsetTop, resources)
                    }
                }
            }

            override fun onViewDetachedFromWindow(v: View) {}
        })
        setupDrawerContent()
        getDrawerActiveUser()?.setOnClickListener { toggleAccountList() }

        drawerToggle =
            object : ActionBarDrawerToggle(this, getDrawerLayout(), R.string.drawer_open, R.string.drawer_close) {
                /** Called when a drawer has settled in a completely closed state.  */
                override fun onDrawerClosed(view: View) {
                    super.onDrawerClosed(view)
                    // standard behavior of drawer is to switch to the standard menu on closing
                    if (isAccountChooserActive) {
                        toggleAccountList()
                    }
                    drawerToggle?.isDrawerIndicatorEnabled = false
                    invalidateOptionsMenu()
                }

                /** Called when a drawer has settled in a completely open state.  */
                override fun onDrawerOpened(drawerView: View) {
                    super.onDrawerOpened(drawerView)
                    drawerToggle?.isDrawerIndicatorEnabled = true
                    invalidateOptionsMenu()
                }
            }

        // Set the drawer toggle as the DrawerListener
        getDrawerLayout()?.addDrawerListener(drawerToggle as ActionBarDrawerToggle)
        drawerToggle?.isDrawerIndicatorEnabled = false
    }

    /**
     * setup drawer content, basically setting the item selected listener.
     *
     */
    protected open fun setupDrawerContent() {
        val navigationView: NavigationView = getNavView() ?: return
        // Disable help or feedback on customization
        if (!resources.getBoolean(R.bool.help_enabled)) {
            navigationView.menu.removeItem(R.id.drawer_menu_help)
        }
        if (!resources.getBoolean(R.bool.feedback_enabled)) {
            navigationView.menu.removeItem(R.id.drawer_menu_feedback)
        }
        navigationView.setNavigationItemSelectedListener { menuItem: MenuItem ->
            getDrawerLayout()?.closeDrawers()
            when (menuItem.itemId) {
                R.id.nav_settings -> {
                    val settingsIntent = Intent(applicationContext, SettingsActivity::class.java)
                    startActivity(settingsIntent)
                }
                R.id.drawer_menu_account_add -> createAccount(false)
                R.id.drawer_menu_account_manage -> {
                    val manageAccountsIntent = Intent(applicationContext, ManageAccountsActivity::class.java)
                    startActivityForResult(manageAccountsIntent, ACTION_MANAGE_ACCOUNTS)
                }
                R.id.drawer_menu_feedback -> openFeedback()
                R.id.drawer_menu_help -> openHelp()
                Menu.NONE -> {
                    accountClicked(menuItem.title.toString())
                }
                else -> Timber.i("Unknown drawer menu item clicked: %s", menuItem.title)
            }
            true
        }

        // handle correct state
        navigationView.menu.setGroupVisible(R.id.drawer_menu_accounts, isAccountChooserActive)
    }

    fun setCheckedItemAtBottomBar(checkedMenuItem: Int) {
        getBottomNavigationView()?.menu?.findItem(checkedMenuItem)?.isChecked = true
    }

    /**
     * Initializes the bottom navigation bar, its content and highlights the menu item with the given id.
     * This method needs to be called after the content view has been set.
     *
     * @param menuItemId the menu item to be checked/highlighted
     */
    open fun setupNavigationBottomBar(menuItemId: Int) {
        // Allow or disallow touches with other visible windows
        getBottomNavigationView()?.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)
        setCheckedItemAtBottomBar(menuItemId)
        getBottomNavigationView()?.setOnNavigationItemSelectedListener { menuItem: MenuItem ->
            bottomBarNavigationTo(menuItem.itemId, getBottomNavigationView()?.selectedItemId == menuItem.itemId)
            true
        }
    }

    private fun bottomBarNavigationTo(menuItemId: Int, isCurrentOptionActive: Boolean) {
        when (menuItemId) {
            R.id.nav_all_files -> navigateToOption(FileListOption.ALL_FILES)
            R.id.nav_uploads -> if (!isCurrentOptionActive) {
                val uploadListIntent = Intent(applicationContext, UploadListActivity::class.java)
                uploadListIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(uploadListIntent)
            }
            R.id.nav_available_offline_files -> navigateToOption(FileListOption.AV_OFFLINE)
            R.id.nav_shared_by_link_files -> navigateToOption(FileListOption.SHARED_BY_LINK)
        }
    }

    private fun openHelp() {
        goToUrl(url = getString(R.string.url_help))
    }

    private fun openFeedback() {
        val feedbackMail = getString(R.string.mail_feedback)
        val feedback = "Android v" + BuildConfig.VERSION_NAME + " - " + getString(R.string.drawer_feedback)
        sendEmail(email = feedbackMail, subject = feedback)
    }

    /**
     * sets the new/current account and restarts. In case the given account equals the actual/current account the
     * call will be ignored.
     *
     * @param accountName The account name to be set
     */
    private fun accountClicked(accountName: String) {
        if (drawerViewModel.getCurrentAccount(this)?.name != accountName) {
            if (drawerViewModel.setCurrentAccount(applicationContext, accountName)) {
                // Refresh dependencies to be used in selected account
                initDependencyInjection()
                restart()
            } else {
                Timber.d("Was not able to change account")
                // TODO: Handle this error (?)
            }
        }
    }

    /**
     * click method for mini avatars in drawer header.
     *
     * @param view the clicked ImageView
     */
    open fun onAccountDrawerClick(view: View) {
        accountClicked(view.contentDescription.toString())
    }

    /**
     * checks if the drawer exists and is opened.
     *
     * @return `true` if the drawer is open, else `false`
     */
    open fun isDrawerOpen(): Boolean = getDrawerLayout()?.isDrawerOpen(GravityCompat.START) ?: false

    /**
     * closes the drawer.
     */
    open fun closeDrawer() {
        getDrawerLayout()?.closeDrawer(GravityCompat.START)
    }

    /**
     * opens the drawer.
     */
    open fun openDrawer() {
        getDrawerLayout()?.openDrawer(GravityCompat.START)
    }

    /**
     * Enable or disable interaction with all drawers.
     *
     * @param lockMode The new lock mode for the given drawer. One of [DrawerLayout.LOCK_MODE_UNLOCKED],
     * [DrawerLayout.LOCK_MODE_LOCKED_CLOSED] or [DrawerLayout.LOCK_MODE_LOCKED_OPEN].
     */
    open fun setDrawerLockMode(lockMode: Int) {
        getDrawerLayout()?.setDrawerLockMode(lockMode)
    }

    /**
     * updates the account list in the drawer.
     */
    private fun updateAccountList() {
        val accounts = drawerViewModel.getAccounts(this)
        if (getNavView() != null && getDrawerLayout() != null) {
            if (accounts.isNotEmpty()) {
                repopulateAccountList(accounts)
                setAccountInDrawer(drawerViewModel.getCurrentAccount(this) ?: accounts.first())
                populateDrawerOwnCloudAccounts()

                // activate second/end account avatar
                accountsWithAvatars[1]?.let { account ->
                    getDrawerAccountEnd()?.let {
                        AvatarUtils().loadAvatarForAccount(
                            imageView = it,
                            account = account,
                            displayRadius = otherAccountAvatarRadiusDimension
                        )
                    }
                }
                if (accountsWithAvatars[1] == null) {
                    getDrawerAccountEnd()?.isVisible = false
                }

                // activate third/middle account avatar
                accountsWithAvatars[2]?.let { account ->
                    getDrawerAccountMiddle()?.let {
                        AvatarUtils().loadAvatarForAccount(
                            imageView = it,
                            account = account,
                            displayRadius = otherAccountAvatarRadiusDimension
                        )
                    }
                }
                if (accountsWithAvatars[2] == null) {
                    getDrawerAccountMiddle()?.isVisible = false
                }
            } else {
                getDrawerAccountEnd()?.isVisible = false
                getDrawerAccountMiddle()?.isVisible = false
            }
        }
    }

    /**
     * re-populates the account list.
     *
     * @param accounts list of accounts
     */
    private fun repopulateAccountList(accounts: List<Account>) {
        val navigationView = getNavView() ?: return
        val navigationMenu = navigationView.menu
        // remove all accounts from list
        navigationMenu.removeGroup(R.id.drawer_menu_accounts)

        // add all accounts to list except current one
        accounts.filter { it.name != account.name }.forEach {
            val accountMenuItem: MenuItem =
                navigationMenu.add(R.id.drawer_menu_accounts, Menu.NONE, MENU_ORDER_ACCOUNT, it.name)
            AvatarUtils().loadAvatarForAccount(
                menuItem = accountMenuItem,
                account = it,
                fetchIfNotCached = false,
                displayRadius = menuAccountAvatarRadiusDimension
            )
        }

        // re-add add-account and manage-accounts
        navigationMenu.add(
            R.id.drawer_menu_accounts, R.id.drawer_menu_account_add,
            MENU_ORDER_ACCOUNT_FUNCTION,
            resources.getString(R.string.prefs_add_account)
        ).setIcon(R.drawable.ic_plus_grey)
        navigationMenu.add(
            R.id.drawer_menu_accounts, R.id.drawer_menu_account_manage,
            MENU_ORDER_ACCOUNT_FUNCTION,
            resources.getString(R.string.drawer_manage_accounts)
        ).setIcon(R.drawable.ic_group)

        // adding sets menu group back to visible, so safety check and setting invisible
        showMenu()
    }

    /**
     * Updates the quota in the drawer
     */
    private fun updateQuota() {
        Timber.d("Update Quota")
        val account = drawerViewModel.getCurrentAccount(this) ?: return
        drawerViewModel.getStoredQuota(account.name)
        drawerViewModel.userQuota.observe(this, { event ->
            when (event.peekContent()) {
                is UIResult.Success -> {
                    event.peekContent().getStoredData()?.let { userQuota ->
                        when {
                            userQuota.available < 0 -> { // Pending, unknown or unlimited free storage
                                getAccountQuotaBar()?.run {
                                    isVisible = true
                                    progress = 0
                                }
                                getAccountQuotaText()?.text = String.format(
                                    getString(R.string.drawer_unavailable_free_storage),
                                    DisplayUtils.bytesToHumanReadable(userQuota.used, this)
                                )

                            }
                            userQuota.available == 0L -> { // Quota 0, guest users
                                getAccountQuotaBar()?.isVisible = false
                                getAccountQuotaText()?.text = getString(R.string.drawer_unavailable_used_storage)
                            }
                            else -> { // Limited quota
                                // Update progress bar rounding up to next int. Example: quota is 0.54 => 1
                                getAccountQuotaBar()?.run {
                                    progress = ceil(userQuota.getRelative()).toInt()
                                    isVisible = true
                                }
                                getAccountQuotaText()?.text = String.format(
                                    getString(R.string.drawer_quota),
                                    DisplayUtils.bytesToHumanReadable(userQuota.used, this),
                                    DisplayUtils.bytesToHumanReadable(userQuota.getTotal(), this),
                                    userQuota.getRelative()
                                )
                            }
                        }
                    }
                }
                is UIResult.Loading -> getAccountQuotaText()?.text = getString(R.string.drawer_loading_quota)
                is UIResult.Error -> getAccountQuotaText()?.text = getString(R.string.drawer_unavailable_used_storage)
            }
        })
    }

    override fun setupRootToolbar(title: String, isSearchEnabled: Boolean) {
        super.setupRootToolbar(title, isSearchEnabled)

        val toolbarLeftIcon = findViewById<ImageView>(R.id.root_toolbar_left_icon)
        toolbarLeftIcon.setOnClickListener { openDrawer() }
    }

    /**
     * Sets the given account data in the drawer in case the drawer is available. The account name is shortened
     * beginning from the @-sign in the username.
     *
     * @param account the account to be set in the drawer
     */
    protected fun setAccountInDrawer(account: Account) {
        if (getDrawerLayout() != null) {
            getDrawerUserNameFull()?.text = account.name
            try {
                val ocAccount = OwnCloudAccount(account, this)
                getDrawerUserName()?.text = ocAccount.displayName
            } catch (e: Exception) {
                Timber.w("Couldn't read display name of account; using account name instead")
                getDrawerUserName()?.text = AccountUtils.getUsernameOfAccount(account.name)
            }

            getDrawerCurrentAccount()?.let {
                AvatarUtils().loadAvatarForAccount(
                    imageView = it,
                    account = account,
                    displayRadius = currentAccountAvatarRadiusDimension
                )
                updateQuota()
            }
        }
    }

    /**
     * Toggle between standard menu and account list including saving the state.
     */
    private fun toggleAccountList() {
        isAccountChooserActive = !isAccountChooserActive
        showMenu()
    }

    /**
     * depending on the #mIsAccountChooserActive flag shows the account chooser or the standard menu.
     */
    private fun showMenu() {
        val navigationView = getNavView() ?: return

        val accountCount = drawerViewModel.getAccounts(this).size
        getDrawerAccountChooserToogle()?.setImageResource(if (isAccountChooserActive) R.drawable.ic_up else R.drawable.ic_down)
        navigationView.menu.setGroupVisible(R.id.drawer_menu_accounts, isAccountChooserActive)
        navigationView.menu.setGroupVisible(R.id.drawer_menu_settings_etc, !isAccountChooserActive)
        getDrawerLogo()?.isVisible = !isAccountChooserActive || accountCount < USER_ITEMS_ALLOWED_BEFORE_REMOVING_CLOUD
    }

    /**
     * checks/highlights the provided menu item if the drawer has been initialized and the menu item exists.
     *
     * @param menuItemId the menu item to be highlighted
     */
    protected open fun setDrawerMenuItemChecked(menuItemId: Int) {
        val navigationView = getNavView()
        if (navigationView != null && navigationView.menu.findItem(menuItemId) != null) {
            navigationView.menu.findItem(menuItemId).isChecked = true
            checkedMenuItem = menuItemId
        } else {
            Timber.w("setDrawerMenuItemChecked has been called with invalid menu-item-ID")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            isAccountChooserActive = savedInstanceState.getBoolean(KEY_IS_ACCOUNT_CHOOSER_ACTIVE, false)
            checkedMenuItem = savedInstanceState.getInt(KEY_CHECKED_MENU_ITEM, Menu.NONE)
        }

        currentAccountAvatarRadiusDimension = resources.getDimension(R.dimen.nav_drawer_header_avatar_radius)
        otherAccountAvatarRadiusDimension =
            resources.getDimension(R.dimen.nav_drawer_header_avatar_other_accounts_radius)
        menuAccountAvatarRadiusDimension = resources.getDimension(R.dimen.nav_drawer_menu_avatar_radius)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_ACCOUNT_CHOOSER_ACTIVE, isAccountChooserActive)
        outState.putInt(KEY_CHECKED_MENU_ITEM, checkedMenuItem)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isAccountChooserActive = savedInstanceState.getBoolean(KEY_IS_ACCOUNT_CHOOSER_ACTIVE, false)
        checkedMenuItem = savedInstanceState.getInt(KEY_CHECKED_MENU_ITEM, Menu.NONE)

        // (re-)setup drawer state
        showMenu()

        // check/highlight the menu item if present
        if (checkedMenuItem != Menu.NONE) {
            setDrawerMenuItemChecked(checkedMenuItem)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle?.let {
            it.syncState()
            if (isDrawerOpen()) {
                it.isDrawerIndicatorEnabled = true
            }
        }
        updateAccountList()
        updateQuota()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle?.onConfigurationChanged(newConfig)
    }

    override fun onBackPressed() {
        if (isDrawerOpen()) {
            closeDrawer()
            return
        }
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        setDrawerMenuItemChecked(checkedMenuItem)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // update Account list and active account if Manage Account activity replies with
        // - ACCOUNT_LIST_CHANGED = true
        // - RESULT_OK
        if (requestCode == ACTION_MANAGE_ACCOUNTS && resultCode == Activity.RESULT_OK && data!!.getBooleanExtra(
                ManageAccountsActivity.KEY_ACCOUNT_LIST_CHANGED,
                false
            )
        ) {

            // current account has changed
            if (data.getBooleanExtra(ManageAccountsActivity.KEY_CURRENT_ACCOUNT_CHANGED, false)) {
                account = AccountUtils.getCurrentOwnCloudAccount(this)
                // Refresh dependencies to be used in selected account
                initDependencyInjection()
                restart()
            } else {
                updateAccountList()
                updateQuota()
            }
        }
    }

    override fun onAccountCreationSuccessful(future: AccountManagerFuture<Bundle?>?) {
        super.onAccountCreationSuccessful(future)
        updateAccountList()
        updateQuota()
        // Refresh dependencies to be used in selected account
        initDependencyInjection()
        restart()
    }

    /**
     * populates the avatar drawer array with the first three ownCloud [Account]s while the first element is
     * always the current account.
     */
    private fun populateDrawerOwnCloudAccounts() {
        accountsWithAvatars = arrayOfNulls(3)
        val accountsAll = drawerViewModel.getAccounts(this)
        val currentAccount = drawerViewModel.getCurrentAccount(this)
        val otherAccounts = accountsAll.filter { it != currentAccount }
        accountsWithAvatars[0] = currentAccount
        accountsWithAvatars[1] = otherAccounts.getOrNull(0)
        accountsWithAvatars[2] = otherAccounts.getOrNull(1)
    }

    private fun getDrawerLayout(): DrawerLayout? = findViewById(R.id.drawer_layout)
    private fun getNavView(): NavigationView? = findViewById(R.id.nav_view)
    private fun getDrawerLogo(): AppCompatImageView? = findViewById(R.id.drawer_logo)
    private fun getBottomNavigationView(): BottomNavigationView? = findViewById(R.id.bottom_nav_view)
    private fun getAccountQuotaText(): TextView? = findViewById(R.id.account_quota_text)
    private fun getAccountQuotaBar(): ProgressBar? = findViewById(R.id.account_quota_bar)
    private fun getDrawerAccountChooserToogle() = findNavigationViewChildById(R.id.drawer_account_chooser_toogle) as ImageView?
    private fun getDrawerAccountEnd() = findNavigationViewChildById(R.id.drawer_account_end) as ImageView?
    private fun getDrawerAccountMiddle() = findNavigationViewChildById(R.id.drawer_account_middle) as ImageView?
    private fun getDrawerActiveUser() = findNavigationViewChildById(R.id.drawer_active_user) as ConstraintLayout?
    private fun getDrawerCurrentAccount() = findNavigationViewChildById(R.id.drawer_current_account) as AppCompatImageView?
    private fun getDrawerHeaderBackground() = findNavigationViewChildById(R.id.drawer_header_background) as ImageView?
    private fun getDrawerUserName(): TextView? = findNavigationViewChildById(R.id.drawer_username) as TextView?
    private fun getDrawerUserNameFull(): TextView? = findNavigationViewChildById(R.id.drawer_username_full) as TextView?

    /**
     * Finds a view that was identified by the id attribute from the drawer header.
     *
     * @param id the view's id
     * @return The view if found or `null` otherwise.
     */
    private fun findNavigationViewChildById(id: Int): View {
        return (findViewById<View>(R.id.nav_view) as NavigationView).getHeaderView(0).findViewById(id)
    }

    /**
     * Adds other listeners to react on changes of the drawer layout.
     *
     * @param listener Object interested in changes of the drawer layout.
     */
    open fun addDrawerListener(listener: DrawerListener) {
        getDrawerLayout()?.addDrawerListener(listener)
    }

    abstract fun navigateToOption(fileListOption: FileListOption)

    /**
     * restart helper method which is called after a changing the current account.
     */
    protected abstract fun restart()

    companion object {
        private const val KEY_IS_ACCOUNT_CHOOSER_ACTIVE = "IS_ACCOUNT_CHOOSER_ACTIVE"
        private const val KEY_CHECKED_MENU_ITEM = "CHECKED_MENU_ITEM"
        private const val ACTION_MANAGE_ACCOUNTS = 101
        private const val MENU_ORDER_ACCOUNT = 1
        private const val MENU_ORDER_ACCOUNT_FUNCTION = 2
        private const val USER_ITEMS_ALLOWED_BEFORE_REMOVING_CLOUD = 4
    }
}
