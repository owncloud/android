/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2026 ownCloud GmbH.
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
import com.owncloud.android.databinding.MembersActivityBinding
import com.owncloud.android.domain.roles.model.OCRole
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.spaces.model.SpaceMember
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.utils.DisplayUtils

class SpaceMembersActivity: FileActivity(), SpaceMembersFragment.SpaceMemberFragmentListener {

    private lateinit var binding: MembersActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MembersActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupStandardToolbar(title = null, displayHomeAsUpEnabled = true, homeButtonEnabled = true, displayShowTitleEnabled = true)

        supportActionBar?.setHomeActionContentDescription(R.string.common_back)

        val currentSpace = intent.getParcelableExtra<OCSpace>(EXTRA_SPACE) ?: return
        binding.apply {
            itemName.text = currentSpace.name
            currentSpace.quota?.let { quota ->
                val usedQuota = quota.used
                val totalQuota = quota.total
                itemSize.text = when {
                    usedQuota == null -> getString(R.string.drawer_unavailable_used_storage)
                    totalQuota == 0L -> DisplayUtils.bytesToHumanReadable(usedQuota, baseContext, true)
                    else -> getString(
                        R.string.drawer_quota,
                        DisplayUtils.bytesToHumanReadable(usedQuota, baseContext, true),
                        DisplayUtils.bytesToHumanReadable(totalQuota, baseContext, true),
                        quota.getRelative().toString())
                }
            }
        }

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

    override fun addMember(space: OCSpace, spaceMembers: List<SpaceMember>, roles: List<OCRole>, editMode: Boolean, selectedMember: SpaceMember?) {
        val addMemberFragment = AddMemberFragment.newInstance(account.name, space, spaceMembers, roles, editMode, selectedMember)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.apply {
            replace(R.id.members_fragment_container, addMemberFragment, TAG_ADD_MEMBER_FRAGMENT)
            addToBackStack(null)
            commit()
        }
    }

    companion object {
        private const val TAG_SPACE_MEMBERS_FRAGMENT = "SPACE_MEMBERS_FRAGMENT"
        private const val TAG_ADD_MEMBER_FRAGMENT ="ADD_MEMBER_FRAGMENT"
        const val EXTRA_SPACE = "EXTRA_SPACE"
    }

}
