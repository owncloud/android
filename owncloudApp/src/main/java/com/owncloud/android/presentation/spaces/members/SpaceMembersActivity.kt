/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2025 ownCloud GmbH.
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

package com.owncloud.android.presentation.spaces.members

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.transaction
import com.owncloud.android.R
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.ui.activity.FileActivity

class SpaceMembersActivity: FileActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.members_activity)

        setupStandardToolbar(title = null, displayHomeAsUpEnabled = true, homeButtonEnabled = true, displayShowTitleEnabled = true)

        supportActionBar?.setHomeActionContentDescription(R.string.common_back)

        val currentSpace = intent.getParcelableExtra<OCSpace>(EXTRA_SPACE)

        supportFragmentManager.transaction {
            if (savedInstanceState == null && currentSpace != null) {
                val fragment = SpaceMembersFragment.newInstance(account.name, currentSpace)
                replace(R.id.members_fragment_container, fragment, TAG_SPACE_MEMBERS_FRAGMENT)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean = false

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        if (item.itemId == android.R.id.home && !supportFragmentManager.popBackStackImmediate()) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }

    companion object {
        private const val TAG_SPACE_MEMBERS_FRAGMENT = "SPACE_MEMBERS_FRAGMENT"
        const val EXTRA_SPACE = "EXTRA_SPACE"
    }

}
