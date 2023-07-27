/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * <p>
 * Copyright (C) 2021 ownCloud GmbH.
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
package com.owncloud.android.presentation.migration

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.data.providers.implementation.OCSharedPreferencesProvider
import com.owncloud.android.data.providers.LegacyStorageProvider
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.File

class StorageMigrationActivity : AppCompatActivity() {

    private val migrationViewModel: MigrationViewModel by viewModel()

    private val fragmentMigrationIntro = MigrationIntroFragment()
    private val fragmentMigrationChoice = MigrationChoiceFragment()
    private val fragmentMigrationProgress = MigrationProgressFragment()
    private val fragmentMigrationCompleted = MigrationCompletedFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_storage_migration)

        migrationViewModel.migrationState.observe(this) {
            navigateToNextMigrationScreen(it.peekContent())
        }
    }

    override fun onBackPressed() {}

    private fun navigateToNextMigrationScreen(migrationState: MigrationState) {

        val targetFragment: Fragment = when (migrationState) {
            is MigrationState.MigrationIntroState -> fragmentMigrationIntro
            is MigrationState.MigrationChoiceState -> fragmentMigrationChoice
            is MigrationState.MigrationProgressState -> fragmentMigrationProgress
            is MigrationState.MigrationCompletedState -> fragmentMigrationCompleted
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.migration_frame_layout, targetFragment)
            .commit()
    }

    companion object {

        private val legacyStorageFolder = File(LegacyStorageProvider(MainApp.dataFolder).getRootFolderPath())

        const val PREFERENCE_ALREADY_MIGRATED_TO_SCOPED_STORAGE = "MIGRATED_TO_SCOPED_STORAGE"

        private fun hasDataInLegacyStorage(): Boolean =
            legacyStorageFolder.exists() && !legacyStorageFolder.listFiles().isNullOrEmpty()

        private fun hasAccessToLegacyStorage(): Boolean =
            legacyStorageFolder.canRead() && legacyStorageFolder.canWrite()

        private fun hasAlreadyMigratedToScopedStorage(context: Context): Boolean {
            val preferenceProvider = OCSharedPreferencesProvider(context = context)
            return preferenceProvider.getBoolean(key = PREFERENCE_ALREADY_MIGRATED_TO_SCOPED_STORAGE, defaultValue = false)
        }

        fun runIfNeeded(context: Context) {
            if (context is StorageMigrationActivity) {
                return
            }
            if (shouldShow(context)) {
                Timber.i("Migration to scoped storage should be done. Showing wizard to migrate...")
                context.startActivity(Intent(context, StorageMigrationActivity::class.java))
            }
        }

        private fun shouldShow(context: Context): Boolean {
            return !hasAlreadyMigratedToScopedStorage(context) && hasDataInLegacyStorage() && hasAccessToLegacyStorage()
        }
    }
}
