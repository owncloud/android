/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 * @author David Crespo Ríos
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

package com.owncloud.android.dependecyinjection

import com.owncloud.android.MainApp
import com.owncloud.android.presentation.ui.files.filelist.MainFileListViewModel
import com.owncloud.android.presentation.ui.files.operations.FileOperationsViewModel
import com.owncloud.android.presentation.ui.security.passcode.PasscodeAction
import com.owncloud.android.presentation.viewmodels.authentication.OCAuthenticationViewModel
import com.owncloud.android.presentation.viewmodels.capabilities.OCCapabilityViewModel
import com.owncloud.android.presentation.viewmodels.drawer.DrawerViewModel
import com.owncloud.android.presentation.viewmodels.files.FileDetailsViewModel
import com.owncloud.android.presentation.viewmodels.logging.LogListViewModel
import com.owncloud.android.presentation.viewmodels.migration.MigrationViewModel
import com.owncloud.android.presentation.viewmodels.oauth.OAuthViewModel
import com.owncloud.android.presentation.viewmodels.releasenotes.ReleaseNotesViewModel
import com.owncloud.android.presentation.viewmodels.security.BiometricViewModel
import com.owncloud.android.presentation.viewmodels.security.PassCodeViewModel
import com.owncloud.android.presentation.viewmodels.security.PatternViewModel
import com.owncloud.android.presentation.viewmodels.settings.SettingsAdvancedViewModel
import com.owncloud.android.presentation.viewmodels.settings.SettingsLogsViewModel
import com.owncloud.android.presentation.viewmodels.settings.SettingsMoreViewModel
import com.owncloud.android.presentation.viewmodels.settings.SettingsPictureUploadsViewModel
import com.owncloud.android.presentation.viewmodels.settings.SettingsSecurityViewModel
import com.owncloud.android.presentation.viewmodels.settings.SettingsVideoUploadsViewModel
import com.owncloud.android.presentation.viewmodels.settings.SettingsViewModel
import com.owncloud.android.presentation.viewmodels.sharing.OCShareViewModel
import com.owncloud.android.presentation.viewmodels.transfers.TransfersViewModel
import com.owncloud.android.ui.dialog.RemoveAccountDialogViewModel
import com.owncloud.android.ui.preview.PreviewImageViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {

    viewModel { DrawerViewModel(get(), get()) }

    viewModel { (accountName: String) ->
        OCCapabilityViewModel(accountName, get(), get(), get())
    }

    viewModel { (filePath: String, accountName: String) ->
        OCShareViewModel(filePath, accountName, get(), get(), get(), get(), get(), get(), get(), get(), get())
    }

    viewModel { (action: PasscodeAction) ->
        PassCodeViewModel(get(), get(), action)
    }

    viewModel { OCAuthenticationViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { OAuthViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { SettingsSecurityViewModel(get(), get()) }
    viewModel { SettingsLogsViewModel(get(), get(), get()) }
    viewModel { SettingsMoreViewModel(get()) }
    viewModel { SettingsPictureUploadsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { SettingsVideoUploadsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { SettingsAdvancedViewModel(get()) }
    viewModel { RemoveAccountDialogViewModel(get(), get(), get(), get()) }
    viewModel { LogListViewModel(get()) }
    viewModel { MigrationViewModel(MainApp.dataFolder, get(), get(), get(), get(), get(), get()) }
    viewModel { PatternViewModel(get()) }
    viewModel { BiometricViewModel(get(), get()) }
    viewModel { ReleaseNotesViewModel(get(), get()) }

    viewModel { PreviewImageViewModel(get(), get(), get()) }
    viewModel { FileDetailsViewModel(get(), get(), get(), get(), get()) }
    viewModel { FileOperationsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { MainFileListViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { TransfersViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}
