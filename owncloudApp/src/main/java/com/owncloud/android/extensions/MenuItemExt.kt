/**
 * ownCloud Android client application
 *
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
package com.owncloud.android.extensions

import android.accounts.Account
import android.view.MenuItem
import com.owncloud.android.R
import com.owncloud.android.presentation.manager.AvatarManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject

object MenuItemExt : KoinComponent {

    private val avatarManager: AvatarManager by inject()

    fun MenuItem.loadAvatarForAccount(
        account: Account,
        fetchIfNotCached: Boolean,
        displayRadius: Float
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val drawable = avatarManager.getAvatarForAccount(
                account = account,
                fetchIfNotCached = fetchIfNotCached,
                displayRadius = displayRadius
            )
            if (drawable != null) {
                this@loadAvatarForAccount.icon = drawable
            } else {
                this@loadAvatarForAccount.setIcon(R.drawable.ic_account_circle)
            }
        }
    }
}