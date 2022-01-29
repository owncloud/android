/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
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
package com.owncloud.android.operations

import android.accounts.Account
import android.accounts.AccountManager
import com.owncloud.android.MainApp.Companion.appContext
import com.owncloud.android.domain.capabilities.usecases.GetStoredCapabilitiesUseCase
import com.owncloud.android.domain.user.usecases.GetUserAvatarAsyncUseCase
import com.owncloud.android.domain.user.usecases.GetUserInfoAsyncUseCase
import com.owncloud.android.domain.user.usecases.RefreshUserQuotaFromServerAsyncUseCase
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.presentation.manager.AvatarManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * Performs the Profile synchronization for account step by step.
 *
 * First: Synchronize user info
 * Second: Synchronize user quota
 * Third: Synchronize user avatar
 *
 * If one step fails, next one is not performed since it may fail too.
 */
class SyncProfileOperation(
    private val account: Account
) : KoinComponent {
    fun syncUserProfile() {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                val getUserInfoAsyncUseCase: GetUserInfoAsyncUseCase by inject()
                val userInfoResult = getUserInfoAsyncUseCase.execute(GetUserInfoAsyncUseCase.Params(account.name))
                userInfoResult.getDataOrNull()?.let { userInfo ->
                    Timber.d("User info synchronized for account ${account.name}")

                    AccountManager.get(appContext).run {
                        setUserData(account, AccountUtils.Constants.KEY_DISPLAY_NAME, userInfo.displayName)
                        setUserData(account, AccountUtils.Constants.KEY_ID, userInfo.id)
                    }

                    val refreshUserQuotaFromServerAsyncUseCase: RefreshUserQuotaFromServerAsyncUseCase by inject()
                    val userQuotaResult =
                        refreshUserQuotaFromServerAsyncUseCase.execute(
                            RefreshUserQuotaFromServerAsyncUseCase.Params(
                                account.name
                            )
                        )
                    userQuotaResult.getDataOrNull()?.let {
                        Timber.d("User quota synchronized for account ${account.name}")

                        val getStoredCapabilitiesUseCase: GetStoredCapabilitiesUseCase by inject()

                        val storedCapabilities = getStoredCapabilitiesUseCase.execute(GetStoredCapabilitiesUseCase.Params(account.name))
                        val shouldFetchAvatar = storedCapabilities?.isFetchingAvatarAllowed() ?: true

                        if (shouldFetchAvatar) {
                            val getUserAvatarAsyncUseCase: GetUserAvatarAsyncUseCase by inject()
                            val userAvatarResult = getUserAvatarAsyncUseCase.execute(GetUserAvatarAsyncUseCase.Params(account.name))
                            AvatarManager().handleAvatarUseCaseResult(account, userAvatarResult)
                            if (userAvatarResult.isSuccess) {
                                Timber.d("Avatar synchronized for account ${account.name}")
                            }
                        } else {
                            Timber.d("Avatar for this account: ${account.name} won't be synced due to capabilities ")
                        }
                    }
                } ?: Timber.d("User profile was not synchronized")
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while getting user profile")
        }
    }
}
