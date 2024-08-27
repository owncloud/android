/**
 * ownCloud Android client application
 *
 * @author Andy Scherzinger
 * @author Christian Schabesberger
 * @author Jorge Aguado Recio
 * @author Juan Carlos Garrote Gascón
 * @author Aitor Ballesteros Pavón
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
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity

import android.view.Menu
import android.view.View
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.owncloud.android.R
import com.owncloud.android.extensions.setAccessibilityRole
import com.owncloud.android.presentation.accounts.ManageAccountsDialogFragment
import com.owncloud.android.presentation.accounts.ManageAccountsDialogFragment.Companion.MANAGE_ACCOUNTS_DIALOG
import com.owncloud.android.presentation.authentication.AccountUtils
import com.owncloud.android.presentation.avatar.AvatarUtils

/**
 * Base class providing toolbar registration functionality, see [.setupToolbar].
 */
abstract class ToolbarActivity : BaseActivity() {

    /**
     * Toolbar setup that must be called in implementer's [.onCreate] after [.setContentView] if they
     * want to use the toolbar.
     */
    open fun setupStandardToolbar(
        title: String?,
        displayHomeAsUpEnabled: Boolean,
        homeButtonEnabled: Boolean,
        displayShowTitleEnabled: Boolean
    ) {
        useStandardToolbar(true)

        val standardToolbar = getStandardToolbar()

        title?.let { standardToolbar.title = it }
        setSupportActionBar(standardToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(displayHomeAsUpEnabled)
        supportActionBar?.setHomeButtonEnabled(homeButtonEnabled)
        supportActionBar?.setDisplayShowTitleEnabled(displayShowTitleEnabled)
    }

    open fun setupRootToolbar(
        title: String,
        isSearchEnabled: Boolean,
        isAvatarRequested: Boolean = false,
    ) {
        useStandardToolbar(false)

        val toolbarTitle = findViewById<TextView>(R.id.root_toolbar_title)
        val searchView = findViewById<SearchView>(R.id.root_toolbar_search_view)
        val avatarView = findViewById<ImageView>(R.id.root_toolbar_avatar)

        toolbarTitle.apply {
            isVisible = true
            text = title
            if (isSearchEnabled) {
                setOnClickListener {
                    toolbarTitle.isVisible = false
                    searchView.isVisible = true
                    searchView.isIconified = false
                }
                toolbarTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_search, 0)
            } else {
                setOnClickListener(null)
                toolbarTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
        }
        toolbarTitle.setAccessibilityRole(className = Button::class.java)

        searchView.apply {
            isVisible = false
            setOnCloseListener {
                searchView.visibility = View.GONE
                toolbarTitle.visibility = VISIBLE
                false
            }
            val textSearchView = findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
            val closeButton = findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
            textSearchView.setHintTextColor(ContextCompat.getColor(applicationContext, R.color.search_view_hint_text))
            closeButton.setColorFilter(ContextCompat.getColor(applicationContext, R.color.white))
        }

        AccountUtils.getCurrentOwnCloudAccount(baseContext) ?: return
        if (isAvatarRequested) {
            AvatarUtils().loadAvatarForAccount(
                avatarView,
                AccountUtils.getCurrentOwnCloudAccount(baseContext),
                true,
                baseContext.resources.getDimension(R.dimen.toolbar_avatar_radius)
            )
        }
        avatarView.setOnClickListener {
            val dialog = ManageAccountsDialogFragment.newInstance(AccountUtils.getCurrentOwnCloudAccount(applicationContext))
            dialog.show(supportFragmentManager, MANAGE_ACCOUNTS_DIALOG)
        }
    }

    private fun useStandardToolbar(isToolbarStandard: Boolean) {
        getRootToolbar().isVisible = !isToolbarStandard
        getStandardToolbar().isVisible = isToolbarStandard
    }

    open fun updateStandardToolbar(
        title: String = getString(R.string.default_display_name_for_root_folder),
        displayHomeAsUpEnabled: Boolean = true,
        homeButtonEnabled: Boolean = true
    ) {

        if (getStandardToolbar().isVisible) {
            supportActionBar?.title = title
            supportActionBar?.setDisplayHomeAsUpEnabled(displayHomeAsUpEnabled)
            supportActionBar?.setHomeButtonEnabled(homeButtonEnabled)
        } else {
            setupStandardToolbar(title, displayHomeAsUpEnabled, displayHomeAsUpEnabled, true)
        }
    }

    private fun getRootToolbar(): ConstraintLayout = findViewById(R.id.root_toolbar)

    private fun getStandardToolbar(): Toolbar = findViewById(R.id.standard_toolbar)

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        (menu.findItem(R.id.action_search).actionView as SearchView).run {
            val searchText = findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
            val closeButton = findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
            val searchButton = findViewById<ImageView>(androidx.appcompat.R.id.search_button)

            maxWidth = Int.MAX_VALUE

            searchButton.setBackgroundColor(getColor(R.color.actionbar_start_color))
            searchText.setHintTextColor(getColor(R.color.search_view_hint_text))
            closeButton.setColorFilter(getColor(R.color.white))
            background = getDrawable(R.drawable.rounded_search_view)
            isFocusable = false
        }
        return true
    }
}
