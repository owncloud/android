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

import com.owncloud.android.domain.authentication.oauth.OIDCDiscoveryUseCase
import com.owncloud.android.domain.authentication.oauth.RegisterClientUseCase
import com.owncloud.android.domain.authentication.oauth.RequestTokenUseCase
import com.owncloud.android.domain.authentication.usecases.GetBaseUrlUseCase
import com.owncloud.android.domain.authentication.usecases.LoginBasicAsyncUseCase
import com.owncloud.android.domain.authentication.usecases.LoginOAuthAsyncUseCase
import com.owncloud.android.domain.authentication.usecases.SupportsOAuth2UseCase
import com.owncloud.android.domain.capabilities.usecases.GetCapabilitiesAsLiveDataUseCase
import com.owncloud.android.domain.capabilities.usecases.GetStoredCapabilitiesUseCase
import com.owncloud.android.domain.capabilities.usecases.RefreshCapabilitiesFromServerAsyncUseCase
import com.owncloud.android.domain.files.usecases.CreateFolderAsyncUseCase
import com.owncloud.android.domain.files.usecases.GetFileByIdUseCase
import com.owncloud.android.domain.files.usecases.RefreshFolderFromServerAsyncUseCase
import com.owncloud.android.domain.server.usecases.GetServerInfoAsyncUseCase
import com.owncloud.android.domain.sharing.sharees.GetShareesAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.CreatePrivateShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.CreatePublicShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.DeleteShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.EditPrivateShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.EditPublicShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.GetShareAsLiveDataUseCase
import com.owncloud.android.domain.sharing.shares.usecases.GetSharesAsLiveDataUseCase
import com.owncloud.android.domain.sharing.shares.usecases.RefreshSharesFromServerAsyncUseCase
import com.owncloud.android.domain.user.usecases.GetStoredQuotaUseCase
import com.owncloud.android.domain.user.usecases.GetUserAvatarAsyncUseCase
import com.owncloud.android.domain.user.usecases.GetUserInfoAsyncUseCase
import com.owncloud.android.domain.user.usecases.RefreshUserQuotaFromServerAsyncUseCase
import org.koin.dsl.module

val useCaseModule = module {
    // Authentication
    factory { GetBaseUrlUseCase(get()) }
    factory { LoginBasicAsyncUseCase(get()) }
    factory { LoginOAuthAsyncUseCase(get()) }
    factory { SupportsOAuth2UseCase(get()) }

    // OAuth
    factory { OIDCDiscoveryUseCase(get()) }
    factory { RequestTokenUseCase(get()) }
    factory { RegisterClientUseCase(get()) }

    // Capabilities
    factory { GetCapabilitiesAsLiveDataUseCase(get()) }
    factory { GetStoredCapabilitiesUseCase(get()) }
    factory { RefreshCapabilitiesFromServerAsyncUseCase(get()) }

    // Files
    factory { CreateFolderAsyncUseCase(get()) }
    factory { GetFileByIdUseCase(get()) }
    factory { RefreshFolderFromServerAsyncUseCase(get()) }

    // Sharing
    factory { CreatePrivateShareAsyncUseCase(get()) }
    factory { CreatePublicShareAsyncUseCase(get()) }
    factory { DeleteShareAsyncUseCase(get()) }
    factory { EditPrivateShareAsyncUseCase(get()) }
    factory { EditPublicShareAsyncUseCase(get()) }
    factory { GetShareAsLiveDataUseCase(get()) }
    factory { GetShareesAsyncUseCase(get()) }
    factory { GetSharesAsLiveDataUseCase(get()) }
    factory { RefreshSharesFromServerAsyncUseCase(get()) }

    // User
    factory { GetStoredQuotaUseCase(get()) }
    factory { GetUserAvatarAsyncUseCase(get()) }
    factory { GetUserInfoAsyncUseCase(get()) }
    factory { RefreshUserQuotaFromServerAsyncUseCase(get()) }

    // Server
    factory { GetServerInfoAsyncUseCase(get()) }
}
