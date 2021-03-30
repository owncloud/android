/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.dependecyinjection

import com.owncloud.android.presentation.viewmodels.authentication.OCAuthenticationViewModel
import com.owncloud.android.presentation.viewmodels.capabilities.OCCapabilityViewModel
import com.owncloud.android.presentation.viewmodels.drawer.DrawerViewModel
import com.owncloud.android.presentation.viewmodels.files.FileDetailsViewModel
import com.owncloud.android.presentation.viewmodels.files.FilesViewModel
import com.owncloud.android.presentation.viewmodels.oauth.OAuthViewModel
import com.owncloud.android.presentation.viewmodels.sharing.OCShareViewModel
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

    viewModel { OCAuthenticationViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { OAuthViewModel(get(), get(), get(), get()) }
    viewModel { FilesViewModel(get(), get(), get()) }

    viewModel { PreviewImageViewModel(get(), get(), get()) }
    viewModel { FileDetailsViewModel(get()) }
}
