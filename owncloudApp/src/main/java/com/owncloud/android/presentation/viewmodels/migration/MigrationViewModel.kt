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

package com.owncloud.android.presentation.viewmodels.migration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.owncloud.android.data.storage.LocalStorageProvider
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import org.apache.commons.io.FileUtils
import java.io.File

/**
 * View Model to keep a reference to the capability repository and an up-to-date capability
 */
class MigrationViewModel(
        private val rootFolder: String,
        private val scopedStorageProvider: LocalStorageProvider.ScopedStorageProvider,
        private val coroutineDispatcherProvider: CoroutinesDispatcherProvider
) : ViewModel() {

    private val _migrationState = MediatorLiveData<Event<MigrationState>>()
    val migrationState: LiveData<Event<MigrationState>> = _migrationState

    init {
        _migrationState.postValue(Event(MigrationState.MigrationIntroState))
    }
    fun getLegacyStorageSizeInBytes(): Long {
        val legacyStorageDirectory = LocalStorageProvider.LegacyStorageProvider(rootFolder).getPrimaryStorageDirectory()
        return FileUtils.sizeOfDirectory(legacyStorageDirectory)
    }

    fun migrateLegacyStorageToScopedStorage() {
        scopedStorageProvider.migrateLegacyToScopedStorage()
    }
}
