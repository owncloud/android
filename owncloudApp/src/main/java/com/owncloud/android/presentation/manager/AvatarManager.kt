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

package com.owncloud.android.presentation.manager

import android.accounts.Account
import android.graphics.drawable.Drawable
import com.owncloud.android.MainApp.Companion.appContext
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.exceptions.FileNotFoundException
import com.owncloud.android.domain.user.model.UserAvatar
import com.owncloud.android.domain.user.model.UserQuota
import com.owncloud.android.ui.DefaultAvatarTextDrawable
import com.owncloud.android.utils.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * The avatar is loaded if available in the cache and bound to the received UI element. The avatar is not
 * fetched from the server if not available, unless the parameter 'fetchFromServer' is set to 'true'.
 *
 * If there is no avatar stored and cannot be fetched, a colored icon is generated with the first
 * letter of the account username.
 *
 * If this is not possible either, a predefined user icon is bound instead.
 */
class AvatarManager {

    suspend fun getAvatarForAccount(
        account: Account,
        displayRadius: Float
    ): Drawable? {
            var avatarDrawable: Drawable? = null
            val imageKey = "a_${account.name}"

            // Check disk cache in background thread
            val avatarBitmap = ThumbnailsCacheManager.getBitmapFromDiskCache(imageKey)

            if (avatarBitmap != null) {
                Timber.i("Avatar retrieved from cache with imageKey: $imageKey")
                avatarDrawable = BitmapUtils.bitmapToCircularBitmapDrawable(appContext.resources, avatarBitmap)
            } else {
                // generate placeholder from user name
                try {
                    Timber.i("Avatar with imageKey $imageKey is not available in cache. Generating one...")
                    avatarDrawable = DefaultAvatarTextDrawable.createAvatar(account.name, displayRadius)
                } catch (e: Exception) {
                    // nothing to do, return null to apply default icon
                    Timber.e(e, "Error calculating RGB value for active account icon.")
                }
            }
            return avatarDrawable
        }
}
