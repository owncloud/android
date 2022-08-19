/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2022 ownCloud GmbH.
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

package com.owncloud.android.presentation.viewmodels.migration

import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.data.storage.LegacyStorageProvider
import com.owncloud.android.data.storage.LocalStorageProvider
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.domain.transfers.usecases.UpdatePendingUploadsPathUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.ui.migration.StorageMigrationActivity.Companion.PREFERENCE_ALREADY_MIGRATED_TO_SCOPED_STORAGE
import com.owncloud.android.providers.AccountProvider
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import kotlinx.coroutines.launch
import java.io.File

/**
 * View Model to keep a reference to the capability repository and an up-to-date capability
 */
class MigrationViewModel(
    rootFolder: String,
    private val localStorageProvider: LocalStorageProvider,
    private val preferencesProvider: SharedPreferencesProvider,
    private val contextProvider: ContextProvider,
    private val accountProvider: AccountProvider,
    private val updatePendingUploadsPathUseCase: UpdatePendingUploadsPathUseCase,
    private val coroutineDispatcherProvider: CoroutinesDispatcherProvider,
) : ViewModel() {

    private val _migrationState = MediatorLiveData<Event<MigrationState>>()
    val migrationState: LiveData<Event<MigrationState>> = _migrationState

    private val legacyStorageDirectoryPath = LegacyStorageProvider(rootFolder).getRootFolderPath()

    init {
        _migrationState.postValue(Event(MigrationState.MigrationIntroState))
    }

    private fun getLegacyStorageSizeInBytes(): Long {
        val legacyStorageDirectory = File(legacyStorageDirectoryPath)
        return localStorageProvider.sizeOfDirectory(legacyStorageDirectory)
    }

    fun moveLegacyStorageToScopedStorage() {
        viewModelScope.launch(coroutineDispatcherProvider.io) {
            localStorageProvider.moveLegacyToScopedStorage()
            updatePendingUploadsPath()
            updateAlreadyDownloadedFilesPath()
            moveToNextState()
        }
    }

    private fun saveAlreadyMigratedPreference() {
        preferencesProvider.putBoolean(key = PREFERENCE_ALREADY_MIGRATED_TO_SCOPED_STORAGE, value = true)
    }

    private fun updatePendingUploadsPath() {
        val temporalFoldersForAccounts = mutableListOf<File>()
        accountProvider.getLoggedAccounts().forEach { account ->
            temporalFoldersForAccounts.add(File(localStorageProvider.getTemporalPath(account.name)))
        }
        updatePendingUploadsPathUseCase.execute(
            UpdatePendingUploadsPathUseCase.Params(
                oldDirectory = legacyStorageDirectoryPath,
                newDirectory = localStorageProvider.getRootFolderPath(),
                temporalFoldersForAccounts = temporalFoldersForAccounts
            )
        )
    }

    private fun updateAlreadyDownloadedFilesPath() {
        val listOfAccounts = accountProvider.getLoggedAccounts()

        if (listOfAccounts.isEmpty()) return

        listOfAccounts.forEach { account ->
            val fileStorageManager = FileDataStorageManager(contextProvider.getContext(), account, contextProvider.getContext().contentResolver)

            fileStorageManager.migrateLegacyToScopedPath(legacyStorageDirectoryPath, localStorageProvider.getRootFolderPath())
        }
    }

    fun moveToNextState() {

        val nextState: MigrationState = when (_migrationState.value?.peekContent()) {
            is MigrationState.MigrationIntroState -> MigrationState.MigrationChoiceState(
                legacyStorageSpaceInBytes = getLegacyStorageSizeInBytes()
            )
            is MigrationState.MigrationChoiceState -> MigrationState.MigrationProgressState
            is MigrationState.MigrationProgressState -> MigrationState.MigrationCompletedState
            is MigrationState.MigrationCompletedState -> MigrationState.MigrationCompletedState
            null -> MigrationState.MigrationIntroState
        }

        if (nextState is MigrationState.MigrationCompletedState) {
            saveAlreadyMigratedPreference()
        }

        _migrationState.postValue(Event(nextState))
    }

    fun isThereEnoughSpaceInDevice(): Boolean {
        val stat = StatFs(Environment.getDataDirectory().path)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        return availableBytes > getLegacyStorageSizeInBytes()
    }
}
