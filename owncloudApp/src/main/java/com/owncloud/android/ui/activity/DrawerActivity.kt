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
import android.accounts.AccountManager
import android.accounts.AccountManagerFuture
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.lifecycle.Observer
import com.google.android.material.navigation.NavigationView
import com.owncloud.android.BuildConfig
import com.owncloud.android.MainApp.Companion.accountType
import com.owncloud.android.MainApp.Companion.initDependencyInjection
import com.owncloud.android.R
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.viewmodels.drawer.DrawerViewModel
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.PreferenceUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_coordinator_layout.*
import kotlinx.android.synthetic.main.nav_drawer_content.*
import kotlinx.android.synthetic.main.nav_drawer_footer.*
import kotlinx.android.synthetic.main.nav_drawer_header.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private var accountChooserToggle: ImageView? = null
    private var accountEndAccountAvatar: ImageView? = null
    private var accountMiddleAccountAvatar: ImageView? = null

    private var drawerLayout: DrawerLayout? = null
    private var drawerLogo: ImageView? = null
    private var drawerToggle: ActionBarDrawerToggle? = null

    private var navigationView: NavigationView? = null

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
        drawer_layout?.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)
        // Allow or disallow touches with other visible windows
        nav_view?.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)
        if (nav_view != null) {
            accountChooserToggle = findNavigationViewChildById(R.id.drawer_account_chooser_toogle) as ImageView?
            accountMiddleAccountAvatar = findNavigationViewChildById(R.id.drawer_account_middle) as ImageView?
            accountEndAccountAvatar = findNavigationViewChildById(R.id.drawer_account_end) as ImageView?
            // Set background header image and logo, if any
            if (resources.getBoolean(R.bool.use_drawer_background_header)) {
                (findNavigationViewChildById(R.id.drawer_header_background) as ImageView?)?.setImageResource(R.drawable.drawer_header_background)
            }
            if (resources.getBoolean(R.bool.use_drawer_logo)) {
                drawer_logo?.setImageResource(R.drawable.drawer_logo)
            }

            accountChooserToggle?.setImageResource(R.drawable.ic_down)
            isAccountChooserActive = false

            //Notch support
            nav_view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val displayCutout = v.rootWindowInsets.displayCutout
                        if (displayCutout != null) {
                            val rlDrawerActiveUser =
                                findNavigationViewChildById(R.id.drawer_active_user) as ConstraintLayout?
                            val orientation = resources.configuration.orientation
                            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                                val displayCutoutDP = displayCutout.safeInsetTop /
                                        (resources.displayMetrics.densityDpi /
                                                DisplayMetrics.DENSITY_DEFAULT)
                                rlDrawerActiveUser?.layoutParams?.height =
                                    resources.getDimension(R.dimen.nav_drawer_header_height).toInt() +
                                            displayCutoutDP
                            } else {
                                rlDrawerActiveUser?.layoutParams?.height =
                                    resources.getDimension(R.dimen.nav_drawer_header_height).toInt()
                            }
                        }
                    }
                }

                override fun onViewDetachedFromWindow(v: View) {}
            })
            setupDrawerContent(nav_view)
            findNavigationViewChildById(R.id.drawer_active_user).setOnClickListener { toggleAccountList() }
        }
        drawerToggle =
            object : ActionBarDrawerToggle(this, drawer_layout, R.string.drawer_open, R.string.drawer_close) {
                /** Called when a drawer has settled in a completely closed state.  */
                override fun onDrawerClosed(view: View) {
                    super.onDrawerClosed(view)
                    // standard behavior of drawer is to switch to the standard menu on closing
                    if (isAccountChooserActive) {
                        toggleAccountList()
                    }
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
        drawer_layout.addDrawerListener(drawerToggle as ActionBarDrawerToggle)
        drawerToggle?.isDrawerIndicatorEnabled = true
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * setup drawer content, basically setting the item selected listener.
     *
     * @param navigationView the drawers navigation view
     */
    protected open fun setupDrawerContent(navigationView: NavigationView) {
        // Disable help or feedback on customization
        if (!resources.getBoolean(R.bool.help_enabled)) {
            navigationView.menu.removeItem(R.id.drawer_menu_help)
        }
        if (!resources.getBoolean(R.bool.feedback_enabled)) {
            navigationView.menu.removeItem(R.id.drawer_menu_feedback)
        }
        navigationView.setNavigationItemSelectedListener { menuItem: MenuItem ->
            drawer_layout.closeDrawers()
            when (menuItem.itemId) {
                R.id.nav_settings -> {
                    val settingsIntent = Intent(applicationContext, Preferences::class.java)
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
                    // account clicked
                    accountClicked(menuItem.title.toString())
                    Timber.i("Unknown drawer menu item clicked: %s", menuItem.title)
                }
                else -> Timber.i("Unknown drawer menu item clicked: %s", menuItem.title)
            }
            true
        }

        // handle correct state
        if (isAccountChooserActive) {
            navigationView.menu.setGroupVisible(R.id.drawer_menu_accounts, true)
        } else {
            navigationView.menu.setGroupVisible(R.id.drawer_menu_accounts, false)
        }
    }

    fun setCheckedItemAtBottomBar(checkedMenuItem: Int) {
        bottom_nav_view.menu.findItem(checkedMenuItem).isChecked = true
    }

    /**
     * Initializes the bottom navigation bar, its content and highlights the menu item with the given id.
     * This method needs to be called after the content view has been set.
     *
     * @param menuItemId the menu item to be checked/highlighted
     */
    open fun setupNavigationBottomBar(menuItemId: Int) {
        // Allow or disallow touches with other visible windows
        bottom_nav_view.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)
        setCheckedItemAtBottomBar(menuItemId)
        bottom_nav_view.setOnNavigationItemSelectedListener { menuItem: MenuItem ->
            navBarNavigationTo(menuItem.itemId, bottom_nav_view.selectedItemId == menuItem.itemId)
            true
        }
    }

    private fun navBarNavigationTo(menuItemId: Int, isCurrentOptionActive: Boolean) {
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
        val helpWeb = getText(R.string.url_help) as String
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(helpWeb))
        startActivity(intent)
    }

    private fun openFeedback() {
        val feedbackMail = getString(R.string.mail_feedback)
        val feedback = "Android v" + BuildConfig.VERSION_NAME + " - " + getString(R.string.drawer_feedback)
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            putExtra(Intent.EXTRA_SUBJECT, feedback)
            data = Uri.parse(feedbackMail)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    /**
     * sets the new/current account and restarts. In case the given account equals the actual/current account the
     * call will be ignored.
     *
     * @param accountName The account name to be set
     */
    private fun accountClicked(accountName: String) {
        if (AccountUtils.getCurrentOwnCloudAccount(applicationContext).name != accountName) {
            AccountUtils.setCurrentOwnCloudAccount(
                applicationContext,
                accountName
            )
            // Refresh dependencies to be used in selected account
            initDependencyInjection()
            restart()
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
    open fun isDrawerOpen(): Boolean = drawer_layout?.isDrawerOpen(GravityCompat.START) ?: false

    /**
     * closes the drawer.
     */
    open fun closeDrawer() {
        drawer_layout.closeDrawer(GravityCompat.START)
    }

    /**
     * opens the drawer.
     */
    open fun openDrawer() {
        drawer_layout.openDrawer(GravityCompat.START)
    }

    /**
     * Enable or disable interaction with all drawers.
     *
     * @param lockMode The new lock mode for the given drawer. One of [DrawerLayout.LOCK_MODE_UNLOCKED],
     * [DrawerLayout.LOCK_MODE_LOCKED_CLOSED] or [DrawerLayout.LOCK_MODE_LOCKED_OPEN].
     */
    open fun setDrawerLockMode(lockMode: Int) {
        drawer_layout.setDrawerLockMode(lockMode)
    }

    /**
     * Enable or disable the drawer indicator.
     *
     * @param enable `true` to enable, `false` to disable
     */
    open fun setDrawerIndicatorEnabled(enable: Boolean) {
        drawerToggle?.isDrawerIndicatorEnabled = enable
    }

    /**
     * updates the account list in the drawer.
     */
    private fun updateAccountList() {
        val accounts = AccountManager.get(this).getAccountsByType(accountType)
        if (nav_view != null && drawer_layout != null) {
            if (accounts.isNotEmpty()) {
                repopulateAccountList(accounts)
                setAccountInDrawer(AccountUtils.getCurrentOwnCloudAccount(this))
                populateDrawerOwnCloudAccounts()

                // activate second/end account avatar
                accountsWithAvatars[1]?.let {
                    CoroutineScope(Dispatchers.Main).launch {
                        // not just accessibility support, used to know what account is bound to each imageView
                        accountEndAccountAvatar?.contentDescription = it.name
                        val drawable = drawerViewModel.getStoredAvatar(
                            account = it,
                            displayRadius = otherAccountAvatarRadiusDimension
                        )
                        if (drawable != null) {
                            accountEndAccountAvatar?.setImageDrawable(drawable)
                        } else {
                            accountEndAccountAvatar?.setImageResource(R.drawable.ic_account_circle)
                        }
                    }
                }
                if (accountsWithAvatars[1] == null) {
                    accountEndAccountAvatar?.visibility = View.GONE
                }

                // activate third/middle account avatar
                accountsWithAvatars[2]?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        // not just accessibility support, used to know what account is bound to each imageView
                        accountMiddleAccountAvatar?.contentDescription = it.name
                        val drawable = drawerViewModel.getStoredAvatar(
                            account = it,
                            displayRadius = otherAccountAvatarRadiusDimension
                        )
                        if (drawable != null) {
                            accountMiddleAccountAvatar?.setImageDrawable(drawable)
                        } else {
                            accountMiddleAccountAvatar?.setImageResource(R.drawable.ic_account_circle)
                        }
                    }
                }
                if (accountsWithAvatars[2] == null) {
                    accountMiddleAccountAvatar?.visibility = View.GONE
                }
            } else {
                accountEndAccountAvatar?.visibility = View.GONE
                accountMiddleAccountAvatar?.visibility = View.GONE
            }
        }
    }

    /**
     * re-populates the account list.
     *
     * @param accounts list of accounts
     */
    private fun repopulateAccountList(accounts: Array<Account>) {
        // remove all accounts from list
        nav_view.menu.removeGroup(R.id.drawer_menu_accounts)

        // add all accounts to list
        for (account in accounts) {
            if (getAccount().name != account.name) {
                val accountMenuItem: MenuItem =
                    nav_view.menu.add(R.id.drawer_menu_accounts, Menu.NONE, MENU_ORDER_ACCOUNT, account.name)
                CoroutineScope(Dispatchers.IO).launch {
                    val drawable = drawerViewModel.getStoredAvatar(
                        account = account,
                        displayRadius = menuAccountAvatarRadiusDimension
                    )
                    if (drawable != null) {
                        accountMenuItem.icon = drawable
                    } else {
                        accountMenuItem.setIcon(R.drawable.ic_account_circle)
                    }
                }
            }
        }

        // re-add add-account and manage-accounts
        nav_view.menu.add(
            R.id.drawer_menu_accounts, R.id.drawer_menu_account_add,
            MENU_ORDER_ACCOUNT_FUNCTION,
            resources.getString(R.string.prefs_add_account)
        ).setIcon(R.drawable.ic_plus_grey)
        nav_view.menu.add(
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
        val account = AccountUtils.getCurrentOwnCloudAccount(this) ?: return
        drawerViewModel.getStoredQuota(account.name)
        drawerViewModel.userQuota.observe(this, Observer { event ->
            when (event.peekContent()) {
                is UIResult.Success -> {
                    event.peekContent().getStoredData()?.let { userQuota ->
                        when {
                            userQuota.available < 0 -> { // Pending, unknown or unlimited free storage
                                account_quota_bar?.visibility = View.VISIBLE
                                account_quota_bar?.progress = 0
                                account_quota_text?.text = String.format(
                                    getString(R.string.drawer_unavailable_free_storage),
                                    DisplayUtils.bytesToHumanReadable(userQuota.used, this)
                                )
                            }
                            userQuota.available == 0L -> { // Quota 0, guest users
                                account_quota_bar?.visibility = View.GONE
                                account_quota_text?.text = getString(R.string.drawer_unavailable_used_storage)
                            }
                            else -> { // Limited quota
                                account_quota_bar?.visibility = View.VISIBLE

                                // Update progress bar rounding up to next int. Example: quota is 0.54 => 1
                                account_quota_bar?.progress = ceil(userQuota.getRelative()).toInt()
                                account_quota_text?.text = String.format(
                                    getString(R.string.drawer_quota),
                                    DisplayUtils.bytesToHumanReadable(userQuota.used, this),
                                    DisplayUtils.bytesToHumanReadable(userQuota.getTotal(), this),
                                    java.lang.String.valueOf(userQuota.getRelative())
                                )
                            }
                        }
                    }
                }
                is UIResult.Loading -> account_quota_text?.text = getString(R.string.drawer_loading_quota)
                is UIResult.Error -> account_quota_text?.text = getString(R.string.drawer_unavailable_used_storage)
            }
        })
    }

    /**
     * Updates title bar and home buttons (state and icon).
     *
     *
     * Assumes that navigation drawer is NOT visible.
     */
    override fun updateActionBarTitleAndHomeButton(chosenFile: OCFile?) {
        super.updateActionBarTitleAndHomeButton(chosenFile)

        /// set home button properties
        drawerToggle?.isDrawerIndicatorEnabled = isRoot(chosenFile)
    }

    /**
     * Sets the given account data in the drawer in case the drawer is available. The account name is shortened
     * beginning from the @-sign in the username.
     *
     * @param account the account to be set in the drawer
     */
    protected fun setAccountInDrawer(account: Account) {
        if (drawer_layout != null) {
            drawer_username_full?.text = account.name
            try {
                val oca = OwnCloudAccount(account, this)
                drawer_username.text = oca.displayName
            } catch (e: Exception) {
                Timber.w("Couldn't read display name of account; using account name instead")
                drawer_username?.text = AccountUtils.getUsernameOfAccount(account.name)
            }
            CoroutineScope(Dispatchers.IO).launch {
                val drawable = drawerViewModel.getStoredAvatar(
                    account = account,
                    displayRadius = currentAccountAvatarRadiusDimension
                )
                withContext(Dispatchers.Main) {
                    val currentAccount: ImageView? =
                        findNavigationViewChildById(R.id.drawer_current_account) as ImageView?
                    if (drawable != null) {
                        currentAccount?.setImageDrawable(drawable)
                    } else {
                        currentAccount?.setImageResource(R.drawable.ic_account_circle)
                    }
                }
            }
            updateQuota()
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
        if (nav_view != null) {
            val accountCount = AccountManager.get(this).getAccountsByType(accountType).size
            if (isAccountChooserActive) {
                accountChooserToggle?.setImageResource(R.drawable.ic_up)
                nav_view.menu.setGroupVisible(R.id.drawer_menu_accounts, true)
                nav_view.menu.setGroupVisible(R.id.drawer_menu_settings_etc, false)
                if (accountCount > USER_ITEMS_ALLOWED_BEFORE_REMOVING_CLOUD) {
                    drawer_logo?.visibility = View.GONE
                }
            } else {
                accountChooserToggle?.setImageResource(R.drawable.ic_down)
                nav_view.menu.setGroupVisible(R.id.drawer_menu_accounts, false)
                nav_view.menu.setGroupVisible(R.id.drawer_menu_settings_etc, true)
                drawer_logo?.visibility = View.VISIBLE
            }
        }
    }

    /**
     * checks/highlights the provided menu item if the drawer has been initialized and the menu item exists.
     *
     * @param menuItemId the menu item to be highlighted
     */
    protected open fun setDrawerMenuItemChecked(menuItemId: Int) {
        if (nav_view != null && nav_view.menu != null && nav_view.menu.findItem(menuItemId) != null) {
            nav_view.menu.findItem(menuItemId).isChecked = true
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

    /**
     * Finds a view that was identified by the id attribute from the drawer header.
     *
     * @param id the view's id
     * @return The view if found or `null` otherwise.
     */
    private fun findNavigationViewChildById(id: Int): View {
        return (findViewById<View>(R.id.nav_view) as NavigationView).getHeaderView(0).findViewById(id)
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
        val accountsAll = AccountManager.get(this).getAccountsByType(accountType)
        val currentAccount = AccountUtils.getCurrentOwnCloudAccount(this)
        accountsWithAvatars[0] = currentAccount
        var j = 0
        var i = 1
        while (i <= 2 && i < accountsAll.size && j < accountsAll.size) {
            if (currentAccount != accountsAll[j]) {
                accountsWithAvatars[i] = accountsAll[j]
                i++
            }
            j++
        }
    }

    /**
     * Adds other listeners to react on changes of the drawer layout.
     *
     * @param listener Object interested in changes of the drawer layout.
     */
    open fun addDrawerListener(listener: DrawerListener) {
        drawer_layout?.addDrawerListener(listener)
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

