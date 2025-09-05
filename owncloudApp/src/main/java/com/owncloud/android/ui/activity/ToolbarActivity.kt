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
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import com.owncloud.android.R

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
        homeButtonEnabled: Boolean,
        displayShowTitleEnabled: Boolean,
    ) {
        val standardToolbar = getStandardToolbar()

        title?.let { standardToolbar.title = it }
        setSupportActionBar(standardToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(homeButtonEnabled)
        supportActionBar?.setHomeButtonEnabled(homeButtonEnabled)
        supportActionBar?.setDisplayShowTitleEnabled(displayShowTitleEnabled)
    }

    open fun updateStandardToolbar(
        title: String = getString(R.string.default_display_name_for_root_folder),
        homeButtonDisplayed: Boolean = true,
        showBackArrow: Boolean = false,
    ) {
        if (getStandardToolbar().isVisible) {
            supportActionBar?.title = title
            supportActionBar?.setDisplayHomeAsUpEnabled(homeButtonDisplayed)
            supportActionBar?.setHomeButtonEnabled(homeButtonDisplayed)
            supportActionBar?.setHomeAsUpIndicator(
                if (showBackArrow) androidx.appcompat.R.drawable.abc_ic_ab_back_material else R.drawable.ic_drawer_icon
            )
        } else {
            setupStandardToolbar(title = title, homeButtonEnabled = homeButtonDisplayed, displayShowTitleEnabled = true)
        }
    }

    protected fun getStandardToolbar(): Toolbar = findViewById(R.id.standard_toolbar)

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        
        searchView.run {
            val searchText = findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
            val closeButton = findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
            val searchButton = findViewById<ImageView>(androidx.appcompat.R.id.search_button)

            maxWidth = Int.MAX_VALUE

            //searchButton.setBackgroundColor(getColor(R.color.actionbar_start_color))
            //searchText.setHintTextColor(getColor(R.color.search_view_hint_text))
            //closeButton.setColorFilter(getColor(R.color.white))
            isFocusable = false
        }
        
        // Set up listener to handle expanded/collapsed states
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                // Set rounded background when expanded
                searchView.background = AppCompatResources.getDrawable(this@ToolbarActivity, R.drawable.rounded_search_view)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                // Remove background when collapsed
                searchView.background = null
                return true
            }
        })
        
        return true
    }
}
