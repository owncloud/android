/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.owncloud.android.domain.capabilities.usecases.GetStoredCapabilitiesUseCase
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PATH
import com.owncloud.android.domain.files.usecases.GetFileByRemotePathUseCase
import com.owncloud.android.domain.spaces.usecases.GetPersonalAndProjectSpacesForAccountUseCase
import com.owncloud.android.domain.spaces.usecases.RefreshSpacesFromServerAsyncUseCase
import com.owncloud.android.presentation.authentication.AccountUtils
import com.owncloud.android.usecases.synchronization.SynchronizeFolderUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class AccountDiscoveryWorker(
    private val appContext: Context,
    private val workerParameters: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParameters
), KoinComponent {

    private val getStoredCapabilitiesUseCase: GetStoredCapabilitiesUseCase by inject()
    private val refreshSpacesFromServerAsyncUseCase: RefreshSpacesFromServerAsyncUseCase by inject()
    private val getPersonalAndProjectSpacesForAccountUseCase: GetPersonalAndProjectSpacesForAccountUseCase by inject()
    private val getFileByRemotePathUseCase: GetFileByRemotePathUseCase by inject()
    private val synchronizeFolderUseCase: SynchronizeFolderUseCase by inject()

    override suspend fun doWork(): Result {
        val accountName = workerParameters.inputData.getString(KEY_PARAM_DISCOVERY_ACCOUNT)
        val account = AccountUtils.getOwnCloudAccountByName(appContext, accountName)
        Timber.d("Account Discovery for account: $accountName and accountName: ${account.name}")

        if (accountName.isNullOrBlank() || account == null) return Result.failure()

        // 1. Get capabilities for account
        val capabilities = getStoredCapabilitiesUseCase(GetStoredCapabilitiesUseCase.Params(accountName))

        val spacesAvailableForAccount = AccountUtils.isSpacesFeatureAllowedForAccount(appContext, account, capabilities)

        // 2.1 Account does not support spaces
        if (!spacesAvailableForAccount) {
            val rootLegacyFolder = getFileByRemotePathUseCase(GetFileByRemotePathUseCase.Params(accountName, ROOT_PATH, null)).getDataOrNull()
            rootLegacyFolder?.let {
                discoverRootFolder(it)
            }
        } else {
            val spacesRootFoldersToDiscover = mutableListOf<OCFile>()

            // 2.2 Account does support spaces
            refreshSpacesFromServerAsyncUseCase(RefreshSpacesFromServerAsyncUseCase.Params(accountName))
            val spaces = getPersonalAndProjectSpacesForAccountUseCase(GetPersonalAndProjectSpacesForAccountUseCase.Params(accountName))

            // First we discover the root of the personal space since it is the first thing seen after login
            val personalSpace = spaces.firstOrNull { it.isPersonal }
            personalSpace?.let { space ->
                val rootFolderForSpace =
                    getFileByRemotePathUseCase(GetFileByRemotePathUseCase.Params(accountName, ROOT_PATH, space.root.id)).getDataOrNull()
                rootFolderForSpace?.let {
                    discoverRootFolder(it)
                }
            }

            // Then we discover the root of the rest of spaces
            val spacesWithoutPersonal = spaces.filterNot { it.isPersonal }
            spacesWithoutPersonal.forEach { space ->
                // Create the root file for each space
                val rootFolderForSpace =
                    getFileByRemotePathUseCase(GetFileByRemotePathUseCase.Params(accountName, ROOT_PATH, space.root.id)).getDataOrNull()
                rootFolderForSpace?.let {
                    spacesRootFoldersToDiscover.add(it)
                }
            }
            spacesRootFoldersToDiscover.forEach {
                discoverRootFolder(it)
            }
        }

        return Result.success()
    }

    private fun discoverRootFolder(folder: OCFile) {
        synchronizeFolderUseCase(
            SynchronizeFolderUseCase.Params(
                accountName = folder.owner,
                remotePath = folder.remotePath,
                spaceId = folder.spaceId,
                syncMode = SynchronizeFolderUseCase.SyncFolderMode.REFRESH_FOLDER_RECURSIVELY
            )
        )
    }

    companion object {
        const val KEY_PARAM_DISCOVERY_ACCOUNT = "KEY_PARAM_DISCOVERY_ACCOUNT"
    }
}
