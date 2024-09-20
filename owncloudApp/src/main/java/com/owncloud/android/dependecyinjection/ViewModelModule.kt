/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 * @author David Crespo Ríos
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

package com.owncloud.android.dependecyinjection

import com.owncloud.android.MainApp
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.presentation.accounts.ManageAccountsViewModel
import com.owncloud.android.presentation.authentication.AuthenticationViewModel
import com.owncloud.android.presentation.authentication.oauth.OAuthViewModel
import com.owncloud.android.presentation.capabilities.CapabilityViewModel
import com.owncloud.android.presentation.common.DrawerViewModel
import com.owncloud.android.presentation.conflicts.ConflictsResolveViewModel
import com.owncloud.android.presentation.files.details.FileDetailsViewModel
import com.owncloud.android.presentation.files.filelist.MainFileListViewModel
import com.owncloud.android.presentation.files.operations.FileOperationsViewModel
import com.owncloud.android.presentation.logging.LogListViewModel
import com.owncloud.android.presentation.migration.MigrationViewModel
import com.owncloud.android.presentation.previews.PreviewAudioViewModel
import com.owncloud.android.presentation.previews.PreviewTextViewModel
import com.owncloud.android.presentation.previews.PreviewVideoViewModel
import com.owncloud.android.presentation.releasenotes.ReleaseNotesViewModel
import com.owncloud.android.presentation.security.biometric.BiometricViewModel
import com.owncloud.android.presentation.security.passcode.PassCodeViewModel
import com.owncloud.android.presentation.security.passcode.PasscodeAction
import com.owncloud.android.presentation.security.pattern.PatternViewModel
import com.owncloud.android.presentation.settings.SettingsViewModel
import com.owncloud.android.presentation.settings.advanced.SettingsAdvancedViewModel
import com.owncloud.android.presentation.settings.autouploads.SettingsPictureUploadsViewModel
import com.owncloud.android.presentation.settings.autouploads.SettingsVideoUploadsViewModel
import com.owncloud.android.presentation.settings.logging.SettingsLogsViewModel
import com.owncloud.android.presentation.settings.more.SettingsMoreViewModel
import com.owncloud.android.presentation.settings.security.SettingsSecurityViewModel
import com.owncloud.android.presentation.sharing.ShareViewModel
import com.owncloud.android.presentation.spaces.SpacesListViewModel
import com.owncloud.android.presentation.transfers.TransfersViewModel
import com.owncloud.android.ui.ReceiveExternalFilesViewModel
import com.owncloud.android.ui.preview.PreviewImageViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::ManageAccountsViewModel)
    viewModelOf(::BiometricViewModel)
    viewModelOf(::DrawerViewModel)
    viewModelOf(::FileDetailsViewModel)
    viewModelOf(::FileOperationsViewModel)
    viewModelOf(::LogListViewModel)
    viewModelOf(::OAuthViewModel)
    viewModelOf(::PatternViewModel)
    viewModelOf(::PreviewAudioViewModel)
    viewModelOf(::PreviewImageViewModel)
    viewModelOf(::PreviewTextViewModel)
    viewModelOf(::PreviewVideoViewModel)
    viewModelOf(::ReceiveExternalFilesViewModel)
    viewModelOf(::ReleaseNotesViewModel)
    viewModelOf(::SettingsAdvancedViewModel)
    viewModelOf(::SettingsLogsViewModel)
    viewModelOf(::SettingsMoreViewModel)
    viewModelOf(::SettingsPictureUploadsViewModel)
    viewModelOf(::SettingsSecurityViewModel)
    viewModelOf(::SettingsVideoUploadsViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::FileOperationsViewModel)

    viewModel { (accountName: String) -> CapabilityViewModel(accountName, get(), get(), get()) }
    viewModel { (action: PasscodeAction) -> PassCodeViewModel(get(), get(), action) }
    viewModel { (filePath: String, accountName: String) ->
        ShareViewModel(filePath, accountName, get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
    viewModel { (initialFolderToDisplay: OCFile, fileListOption: FileListOption) ->
        MainFileListViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), initialFolderToDisplay, fileListOption)
    }
    viewModel { (ocFile: OCFile) -> ConflictsResolveViewModel(get(), get(), get(), get(), get(), ocFile) }
    viewModel { AuthenticationViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { MigrationViewModel(MainApp.dataFolder, get(), get(), get(), get(), get(), get(), get()) }
    viewModel { TransfersViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { ReceiveExternalFilesViewModel(get(), get(), get()) }
    viewModel { (accountName: String, showPersonalSpace: Boolean) ->
        SpacesListViewModel(get(), get(), get(), get(), get(), accountName, showPersonalSpace)
    }
}
