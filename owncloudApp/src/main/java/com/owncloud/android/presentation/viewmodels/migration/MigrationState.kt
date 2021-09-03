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
package com.owncloud.android.presentation.viewmodels.migration

sealed class MigrationState {

    object MigrationIntroState : MigrationState()

    data class MigrationChoiceState(
        val legacyStorageSpaceInBytes: Long,
        val availableBytesInScopedStorage: Long,
    ) : MigrationState()

    data class MigrationProgressState(
        val migrationType: MigrationType,
        val progress: Int = 0,
    ) : MigrationState()

    object MigrationCompletedState : MigrationState()

    object MigrationDone : MigrationState()

    fun nextState(): MigrationState {
        return when (this) {
            is MigrationIntroState -> MigrationChoiceState(legacyStorageSpaceInBytes = 0, availableBytesInScopedStorage = 0)
            is MigrationChoiceState -> MigrationProgressState(progress = 0, migrationType = MigrationType.MIGRATE_AND_KEEP)
            is MigrationProgressState -> MigrationCompletedState
            is MigrationCompletedState, MigrationDone -> MigrationDone
        }
    }

    enum class MigrationType { MIGRATE_AND_KEEP, MIGRATE_AND_CLEAN }
}
