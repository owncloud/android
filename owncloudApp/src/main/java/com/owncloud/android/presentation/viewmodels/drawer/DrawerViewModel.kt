/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.presentation.viewmodels.drawer

import android.accounts.Account
import android.graphics.drawable.Drawable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.MainApp.Companion.appContext
import com.owncloud.android.R
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.domain.exceptions.FileNotFoundException
import com.owncloud.android.domain.user.model.UserAvatar
import com.owncloud.android.domain.user.model.UserQuota
import com.owncloud.android.domain.user.usecases.GetStoredQuotaUseCase
import com.owncloud.android.domain.user.usecases.GetUserAvatarAsyncUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResult
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.manager.AvatarManager
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import kotlinx.coroutines.launch
import timber.log.Timber

class DrawerViewModel(
    private val getStoredQuotaUseCase: GetStoredQuotaUseCase,
    private val getUserAvatarAsyncUseCase: GetUserAvatarAsyncUseCase,
    private val avatarManager: AvatarManager,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider
) : ViewModel() {

    private val _userQuota = MediatorLiveData<Event<UIResult<UserQuota?>>>()
    val userQuota: LiveData<Event<UIResult<UserQuota?>>> = _userQuota

    fun getStoredQuota(
        accountName: String
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutinesDispatcherProvider.io,
        showLoading = true,
        liveData = _userQuota,
        useCase = getStoredQuotaUseCase,
        useCaseParams = GetStoredQuotaUseCase.Params(accountName = accountName)
    )

    suspend fun getStoredAvatar(account: Account, displayRadius: Float): Drawable? {
        return avatarManager.getAvatarForAccount(
            account = account,
            displayRadius = displayRadius
        )
    }

    suspend fun fetchAvatar(
        accountName: String
    ) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val useCaseResult =
                getUserAvatarAsyncUseCase.execute(GetUserAvatarAsyncUseCase.Params(accountName = accountName))

            Timber.d(useCaseResult.toString())

            if (useCaseResult.isSuccess) {
                val userAvatar = useCaseResult.getDataOrNull()
                userAvatar?.let {
                    val avatarKey = ThumbnailsCacheManager.addAvatarToCache(
                        accountName,
                        it.avatarData,
                        getAvatarDimension()
                    )
                    Timber.d("User avatar saved into cache -> %s", avatarKey)
                }

            } else if (useCaseResult.getThrowableOrNull() is FileNotFoundException) {
                Timber.i("No avatar available, removing cached copy")
                ThumbnailsCacheManager.removeAvatarFromCache(accountName)
            }
        }
    }

    /**
     * Converts size of file icon from dp to pixel
     *
     * @return int
     */
    private fun getAvatarDimension(): Int {
        // Converts dp to pixel
        val r = appContext.resources
        return Math.round(r.getDimension(R.dimen.file_avatar_size))
    }

}
