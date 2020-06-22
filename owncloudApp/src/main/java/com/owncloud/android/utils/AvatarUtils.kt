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

package com.owncloud.android.utils

import android.accounts.Account
import android.view.MenuItem
import android.widget.ImageView
import com.owncloud.android.R
import com.owncloud.android.presentation.manager.AvatarManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject

class AvatarUtils : KoinComponent {

    private val avatarManager: AvatarManager by inject()

    /**
     * Show the avatar corresponding to the received account in an {@ImageView}.
     * <p>
     * The avatar is shown if available locally in {@link ThumbnailsCacheManager}. The avatar is not
     * fetched from the server if not available.
     * <p>
     * If there is no avatar stored, a colored icon is generated with the first letter of the account username.
     * <p>
     * If this is not possible either, a predefined user icon is shown instead.
     *
     * @param account         OC account which avatar will be shown.
     * @param displayRadius   The radius of the circle where the avatar will be clipped into.
     * @param fetchIfNotCached When 'true', if there is no avatar stored in the cache, it's fetched from
     *                        the server. When 'false', server is not accessed, the fallback avatar is
     *                        generated instead.
     */
    fun loadAvatarForAccount(
        imageView: ImageView,
        account: Account,
        fetchIfNotCached: Boolean = false,
        displayRadius: Float
    ) {
        //TODO: Tech debt: Move this to a viewModel and use its viewModelScope instead
        CoroutineScope(Dispatchers.IO).launch {
            val drawable = avatarManager.getAvatarForAccount(
                account = account,
                fetchIfNotCached = fetchIfNotCached,
                displayRadius = displayRadius
            )
            withContext(Dispatchers.Main) {
                // Not just accessibility support, used to know what account is bound to each imageView
                imageView.contentDescription = account.name
                if (drawable != null) {
                    imageView.setImageDrawable(drawable)
                } else {
                    imageView.setImageResource(R.drawable.ic_account_circle)
                }
            }
        }
    }

    fun loadAvatarForAccount(
        menuItem: MenuItem,
        account: Account,
        fetchIfNotCached: Boolean = false,
        displayRadius: Float
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val drawable = avatarManager.getAvatarForAccount(
                account = account,
                fetchIfNotCached = fetchIfNotCached,
                displayRadius = displayRadius
            )
            withContext(Dispatchers.Main) {
                if (drawable != null) {
                    menuItem.icon = drawable
                } else {
                    menuItem.setIcon(R.drawable.ic_account_circle)
                }
            }
        }
    }
}
