/**
 * ownCloud Android client application
 *
 * @author Andy Scherzinger
 * @author Christian Schabesberger
 * @author David González Verdugo
 * @author Shashvat Kedia
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 * @author Aitor Ballesteros Pavon
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2024 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerFuture
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.owncloud.android.R
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.user.model.UserQuota
import com.owncloud.android.domain.user.model.UserQuotaState
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.collectLatestLifecycleFlow
import com.owncloud.android.extensions.goToUrl
import com.owncloud.android.extensions.openPrivacyPolicy
import com.owncloud.android.extensions.sendEmailOrOpenFeedbackDialogAction
import com.owncloud.android.extensions.setAccessibilityRole
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.presentation.authentication.AccountUtils
import com.owncloud.android.presentation.avatar.AvatarUtils
import com.owncloud.android.presentation.capabilities.CapabilityViewModel
import com.owncloud.android.presentation.common.DrawerViewModel
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.presentation.settings.SettingsActivity
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.PreferenceUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

/**
 * Base class to handle setup of the drawer implementation including avatar fetching and fallback
 * generation.
 */
abstract class DrawerActivity : ToolbarActivity() {

    private val drawerViewModel by viewModel<DrawerViewModel>()
    private val capabilitiesViewModel by viewModel<CapabilityViewModel> {
        parametersOf(
            account?.name
        )
    }

    private var currentAccountAvatarRadiusDimension = 0f

    private var drawerToggle: ActionBarDrawerToggle? = null

    private var checkedMenuItem = Menu.NONE

    /**
     * Initializes the drawer and its content.
     * This method needs to be called after the content view has been set.
     */
    protected open fun setupDrawer() {
        // Allow or disallow touches with other visible windows
        getDrawerLayout()?.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)
        getNavView()?.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)

        // Set background header image, if any
        if (resources.getBoolean(R.bool.use_drawer_background_header)) {
            getDrawerHeaderBackground()?.setImageResource(R.drawable.drawer_header_background)
        }

        // Set logo and text for drawer link, if any
        if (resources.getBoolean(R.bool.use_drawer_logo)) {
            if (isDrawerLinkEnabled()) {
                getDrawerLinkIcon()?.apply {
                    isVisible = true
                    setOnClickListener { openDrawerLink() }
                    setAccessibilityRole(roleDescription = context.getString(R.string.link_role_accessibility))
                }
                getDrawerLinkText()?.apply {
                    isVisible = true
                    setOnClickListener { openDrawerLink() }
                    setAccessibilityRole(roleDescription = context.getString(R.string.link_role_accessibility))
                }
            } else {
                getDrawerLogo()?.setImageResource(R.drawable.drawer_logo)
            }
        }

        // Notch support
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

        drawerToggle =
            object : ActionBarDrawerToggle(this, getDrawerLayout(), R.string.drawer_open, R.string.drawer_close) {
                /** Called when a drawer has settled in a completely closed state.  */
                override fun onDrawerClosed(view: View) {
                    super.onDrawerClosed(view)
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
    private fun setupDrawerContent() {
        val navigationView: NavigationView = getNavView() ?: return
        // Disable help or feedback on customization
        if (!resources.getBoolean(R.bool.help_enabled)) {
            navigationView.menu.removeItem(R.id.drawer_menu_help)
        }
        if (!resources.getBoolean(R.bool.feedback_enabled)) {
            navigationView.menu.removeItem(R.id.drawer_menu_feedback)
        }
        if (!resources.getBoolean(R.bool.privacy_policy_enabled)) {
            navigationView.menu.removeItem(R.id.drawer_menu_privacy_policy)
        }
        navigationView.setNavigationItemSelectedListener { menuItem: MenuItem ->
            getDrawerLayout()?.closeDrawers()
            when (menuItem.itemId) {
                R.id.nav_settings -> {
                    val settingsIntent = Intent(applicationContext, SettingsActivity::class.java)
                    startActivity(settingsIntent)
                }
                R.id.drawer_menu_feedback -> { openFeedback() }
                R.id.drawer_menu_help -> { openHelp() }
                R.id.drawer_menu_privacy_policy -> { openPrivacyPolicy() }
                else -> { Timber.i("Unknown drawer menu item clicked: %s", menuItem.title) }
            }
            true
        }
        setRolesAccessibilityToMenuItems()
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
        if (account != null) {
            capabilitiesViewModel.capabilities.observe(this) { event: Event<UIResult<OCCapability>> ->
                setSpacesVisibilityBottomBar(event.peekContent())
            }
        }
        setCheckedItemAtBottomBar(menuItemId)
        getBottomNavigationView()?.setOnNavigationItemSelectedListener { menuItem: MenuItem ->
            bottomBarNavigationTo(menuItem.itemId, getBottomNavigationView()?.selectedItemId == menuItem.itemId)
            true
        }
    }

    private fun setSpacesVisibilityBottomBar(uiResult: UIResult<OCCapability>) {
        if (uiResult is UIResult.Success) {
            val capabilities = uiResult.data
            if (AccountUtils.isSpacesFeatureAllowedForAccount(baseContext, account, capabilities)) {
                getBottomNavigationView()?.menu?.get(0)?.title = getString(R.string.bottom_nav_personal)
                getBottomNavigationView()?.menu?.get(1)?.title = getString(R.string.bottom_nav_shares)
                getBottomNavigationView()?.menu?.get(1)?.icon = AppCompatResources.getDrawable(this, R.drawable.ic_ocis_shares)
                getBottomNavigationView()?.menu?.get(2)?.isVisible = capabilities?.isSpacesProjectsAllowed() == true
            } else {
                getBottomNavigationView()?.menu?.get(0)?.title = getString(R.string.bottom_nav_files)
                getBottomNavigationView()?.menu?.get(2)?.isVisible = false
            }
        }
    }

    private fun bottomBarNavigationTo(menuItemId: Int, isCurrentOptionActive: Boolean) {
        when (menuItemId) {
            R.id.nav_all_files -> navigateToOption(FileListOption.ALL_FILES)
            R.id.nav_spaces -> navigateToOption(FileListOption.SPACES_LIST)
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
        sendEmailOrOpenFeedbackDialogAction(drawerViewModel.getFeedbackMail())
    }

    private fun openDrawerLink() {
        goToUrl(url = resources.getString(R.string.drawer_link))
    }

    private fun isDrawerLinkEnabled() =
        resources.getString(R.string.drawer_link_label).isNotBlank() && resources.getString(R.string.drawer_link).isNotBlank()

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
        findViewById<View>(R.id.nav_view).requestFocus()
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
     * Updates the quota in the drawer
     */
    private fun updateQuota() {
        Timber.d("Update Quota")
        collectLatestLifecycleFlow(drawerViewModel.userQuota) { event ->
            event?.let {
                when (val uiResult = event.peekContent()) {
                    is UIResult.Success -> {
                        uiResult.data?.let { userQuotaData ->
                            collectLatestLifecycleFlow(userQuotaData) { quota ->
                                quota?.let { onUpdateQuotaIsSuccessful(it) }
                            }
                        }
                    }
                    is UIResult.Loading -> { getAccountQuotaText()?.text = getString(R.string.drawer_loading_quota) }
                    is UIResult.Error -> { getAccountQuotaText()?.text = getString(R.string.drawer_unavailable_used_storage) }
                }
            }
        }
    }

    private fun onUpdateQuotaIsSuccessful(userQuota: UserQuota) {
        when {
            userQuota.available == -4L -> { // Light users (oCIS)
                getAccountQuotaText()?.text = getString(R.string.drawer_unavailable_used_storage)
                getAccountQuotaBar()?.isVisible = false
                getAccountQuotaStatusText()?.isVisible = false
            }

            userQuota.available < 0 -> { // Pending, unknown or unlimited free storage
                getAccountQuotaBar()?.visibility = View.GONE
                getAccountQuotaText()?.text = String.format(
                    getString(R.string.drawer_unavailable_free_storage),
                    DisplayUtils.bytesToHumanReadable(userQuota.used, this, true)
                )
                getAccountQuotaStatusText()?.visibility = View.GONE
            }

            userQuota.available == 0L -> { // Exceeded storage. The value is over 100%.
                getAccountQuotaBar()?.apply {
                    isVisible = true
                    progress = 100
                    progressTintList = ColorStateList.valueOf(resources.getColor(R.color.quota_exceeded))
                }

                if (userQuota.state == UserQuotaState.EXCEEDED) { // oCIS
                    getAccountQuotaText()?.apply {
                        text = String.format(
                            getString(R.string.drawer_quota),
                            DisplayUtils.bytesToHumanReadable(userQuota.used, context, true),
                            DisplayUtils.bytesToHumanReadable(userQuota.getTotal(), context, true),
                            userQuota.getRelative()
                        )
                    }
                    getAccountQuotaStatusText()?.apply {
                        visibility = View.VISIBLE
                        text = getString(R.string.drawer_exceeded_quota)
                    }
                } else { // oC10
                    getAccountQuotaText()?.text = getString(R.string.drawer_exceeded_quota)
                    getAccountQuotaStatusText()?.visibility = View.GONE
                }
            }

            else -> { // Limited storage. Value under 100%
                if (userQuota.state == UserQuotaState.NEARING) { // Nearing storage. Value between 75% and 90%
                    getAccountQuotaBar()?.apply {
                        isVisible = true
                        progress = userQuota.getRelative().toInt()
                        progressTintList = ColorStateList.valueOf(resources.getColor(R.color.quota_exceeded))
                    }
                    getAccountQuotaText()?.apply {
                        text = String.format(
                            getString(R.string.drawer_quota),
                            DisplayUtils.bytesToHumanReadable(userQuota.used, context, true),
                            DisplayUtils.bytesToHumanReadable(userQuota.getTotal(), context, true),
                            userQuota.getRelative()
                        )
                    }
                    getAccountQuotaStatusText()?.apply {
                        visibility = View.VISIBLE
                        text = getString(R.string.drawer_nearing_quota)
                    }
                } else if (userQuota.state == UserQuotaState.CRITICAL ||
                    userQuota.state == UserQuotaState.EXCEEDED) { // Critical storage. Value over 90%
                    getAccountQuotaBar()?.apply {
                        isVisible = true
                        progress = userQuota.getRelative().toInt()
                        progressTintList = ColorStateList.valueOf(resources.getColor(R.color.quota_exceeded))
                    }
                    getAccountQuotaText()?.apply {
                        text = String.format(
                            getString(R.string.drawer_quota),
                            DisplayUtils.bytesToHumanReadable(userQuota.used, context, true),
                            DisplayUtils.bytesToHumanReadable(userQuota.getTotal(), context, true),
                            userQuota.getRelative()
                        )
                    }
                    getAccountQuotaStatusText()?.apply {
                        visibility = View.VISIBLE
                        text = getString(R.string.drawer_critical_quota)
                    }
                } else { // Normal storage. Value under 75%
                    getAccountQuotaBar()?.apply {
                        progress = userQuota.getRelative().toInt()
                        isVisible = true
                        progressTintList = ColorStateList.valueOf(resources.getColor(R.color.color_accent))
                    }
                    getAccountQuotaText()?.text = String.format(
                        getString(R.string.drawer_quota),
                        DisplayUtils.bytesToHumanReadable(userQuota.used, this, true),
                        DisplayUtils.bytesToHumanReadable(userQuota.getTotal(), this, true),
                        userQuota.getRelative()
                    )
                    getAccountQuotaStatusText()?.visibility = View.GONE
                }
            }
        }
    }

    override fun setupRootToolbar(title: String, isSearchEnabled: Boolean, isAvatarRequested: Boolean) {
        super.setupRootToolbar(
            title = title,
            isSearchEnabled = isSearchEnabled,
            isAvatarRequested = isAvatarRequested,
        )

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
                Timber.w(e, "Couldn't read display name of account; using account name instead")
                getDrawerUserName()?.text = drawerViewModel.getUsernameOfAccount(account.name)
            }

            getDrawerCurrentAccount()?.let {
                AvatarUtils().loadAvatarForAccount(
                    imageView = it,
                    account = account,
                    displayRadius = currentAccountAvatarRadiusDimension
                )
                drawerViewModel.getUserQuota(account.name)
                updateQuota()
            }
        }
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

    private fun setOnAccountsUpdatedListener() {
        val accountManager = AccountManager.get(this)
        accountManager.addOnAccountsUpdatedListener({
            drawerViewModel.removeAccount(this)

            // Notify removal to Document Provider
            val authority = getString(R.string.document_provider_authority)
            val rootsUri = DocumentsContract.buildRootsUri(authority)
            contentResolver.notifyChange(rootsUri, null)

            if (drawerViewModel.getAccounts(this).isEmpty()) {
                mAccountWasSet = false
            }
        }, Handler(), false)
    }

    private fun setRolesAccessibilityToMenuItems() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val navViewMenu = getNavView()?.menu ?: return
            val roleAccessibilityDescription = getString(R.string.button_role_accessibility)
            navViewMenu.apply {
                findItem(R.id.nav_settings)?.contentDescription = "${getString(R.string.actionbar_settings)} $roleAccessibilityDescription"
                findItem(R.id.drawer_menu_feedback)?.contentDescription = "${getString(R.string.drawer_feedback)} $roleAccessibilityDescription"
                findItem(R.id.drawer_menu_help)?.contentDescription = "${getString(R.string.prefs_help)} $roleAccessibilityDescription"
                findItem(R.id.drawer_menu_privacy_policy)?.contentDescription =
                    "${getString(R.string.prefs_privacy_policy)} $roleAccessibilityDescription"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            checkedMenuItem = savedInstanceState.getInt(KEY_CHECKED_MENU_ITEM, Menu.NONE)
        }

        currentAccountAvatarRadiusDimension = resources.getDimension(R.dimen.nav_drawer_header_avatar_radius)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CHECKED_MENU_ITEM, checkedMenuItem)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        checkedMenuItem = savedInstanceState.getInt(KEY_CHECKED_MENU_ITEM, Menu.NONE)

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
        setOnAccountsUpdatedListener()
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

    override fun onAccountCreationSuccessful(future: AccountManagerFuture<Bundle?>?) {
        super.onAccountCreationSuccessful(future)
        updateQuota()
        restart()
    }

    private fun getDrawerLayout(): DrawerLayout? = findViewById(R.id.drawer_layout)
    private fun getNavView(): NavigationView? = findViewById(R.id.nav_view)
    private fun getBottomNavigationView(): BottomNavigationView? = findViewById(R.id.bottom_nav_view)
    private fun getDrawerLogo(): AppCompatImageView? = findViewById(R.id.drawer_logo)
    private fun getDrawerLinkIcon(): ImageView? = findViewById(R.id.drawer_link_icon)
    private fun getDrawerLinkText(): TextView? = findViewById(R.id.drawer_link_text)
    private fun getAccountQuotaText(): TextView? = findViewById(R.id.account_quota_text)
    private fun getAccountQuotaStatusText(): TextView? = findViewById(R.id.account_quota_status_text)
    private fun getAccountQuotaBar(): ProgressBar? = findViewById(R.id.account_quota_bar)
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
    private fun findNavigationViewChildById(id: Int): View =
        (findViewById<View>(R.id.nav_view) as NavigationView).getHeaderView(0).findViewById(id)

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
        const val CENTRAL_URL = "https://central.owncloud.org/"
        const val GITHUB_URL = "https://github.com/owncloud/android/issues/new/choose"
        private const val KEY_CHECKED_MENU_ITEM = "CHECKED_MENU_ITEM"
    }
}
