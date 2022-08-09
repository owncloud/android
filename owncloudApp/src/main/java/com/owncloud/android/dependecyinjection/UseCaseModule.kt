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

package com.owncloud.android.dependecyinjection

import com.owncloud.android.domain.authentication.oauth.OIDCDiscoveryUseCase
import com.owncloud.android.domain.authentication.oauth.RegisterClientUseCase
import com.owncloud.android.domain.authentication.oauth.RequestTokenUseCase
import com.owncloud.android.domain.authentication.usecases.GetBaseUrlUseCase
import com.owncloud.android.domain.authentication.usecases.LoginBasicAsyncUseCase
import com.owncloud.android.domain.authentication.usecases.LoginOAuthAsyncUseCase
import com.owncloud.android.domain.authentication.usecases.SupportsOAuth2UseCase
import com.owncloud.android.domain.availableoffline.usecases.GetFilesAvailableOfflineFromAccountUseCase
import com.owncloud.android.domain.availableoffline.usecases.GetFilesAvailableOfflineFromEveryAccountUseCase
import com.owncloud.android.domain.availableoffline.usecases.SetFilesAsAvailableOfflineUseCase
import com.owncloud.android.domain.availableoffline.usecases.UnsetFilesAsAvailableOfflineUseCase
import com.owncloud.android.domain.camerauploads.usecases.GetCameraUploadsConfigurationUseCase
import com.owncloud.android.domain.camerauploads.usecases.GetPictureUploadsConfigurationStreamUseCase
import com.owncloud.android.domain.camerauploads.usecases.GetVideoUploadsConfigurationStreamUseCase
import com.owncloud.android.domain.camerauploads.usecases.ResetPictureUploadsUseCase
import com.owncloud.android.domain.camerauploads.usecases.ResetVideoUploadsUseCase
import com.owncloud.android.domain.camerauploads.usecases.SavePictureUploadsConfigurationUseCase
import com.owncloud.android.domain.camerauploads.usecases.SaveVideoUploadsConfigurationUseCase
import com.owncloud.android.domain.capabilities.usecases.GetCapabilitiesAsLiveDataUseCase
import com.owncloud.android.domain.capabilities.usecases.GetStoredCapabilitiesUseCase
import com.owncloud.android.domain.capabilities.usecases.RefreshCapabilitiesFromServerAsyncUseCase
import com.owncloud.android.domain.files.usecases.CopyFileUseCase
import com.owncloud.android.domain.files.usecases.CreateFolderAsyncUseCase
import com.owncloud.android.domain.files.usecases.GetFileByIdUseCase
import com.owncloud.android.domain.files.usecases.GetFileByRemotePathUseCase
import com.owncloud.android.domain.files.usecases.GetFilesSharedByLinkUseCase
import com.owncloud.android.domain.files.usecases.GetFolderContentAsLiveDataUseCase
import com.owncloud.android.domain.files.usecases.GetFolderContentUseCase
import com.owncloud.android.domain.files.usecases.GetFolderImagesUseCase
import com.owncloud.android.domain.files.usecases.GetSearchFolderContentUseCase
import com.owncloud.android.domain.files.usecases.MoveFileUseCase
import com.owncloud.android.domain.files.usecases.RemoveFileUseCase
import com.owncloud.android.domain.files.usecases.RenameFileUseCase
import com.owncloud.android.domain.files.usecases.SaveFileOrFolderUseCase
import com.owncloud.android.domain.files.usecases.SortFilesUseCase
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
import com.owncloud.android.domain.transfers.usecases.ClearFailedTransfersUseCase
import com.owncloud.android.domain.transfers.usecases.ClearSuccessfulTransfersUseCase
import com.owncloud.android.domain.transfers.usecases.DeleteTransferWithIdUseCase
import com.owncloud.android.domain.transfers.usecases.GetAllTransfersUseCase
import com.owncloud.android.domain.user.usecases.GetStoredQuotaUseCase
import com.owncloud.android.domain.user.usecases.GetUserAvatarAsyncUseCase
import com.owncloud.android.domain.user.usecases.GetUserInfoAsyncUseCase
import com.owncloud.android.domain.user.usecases.RefreshUserQuotaFromServerAsyncUseCase
import com.owncloud.android.usecases.synchronization.SynchronizeFileUseCase
import com.owncloud.android.usecases.synchronization.SynchronizeFolderUseCase
import com.owncloud.android.usecases.transfers.downloads.CancelDownloadForFileUseCase
import com.owncloud.android.usecases.transfers.downloads.CancelDownloadsForAccountUseCase
import com.owncloud.android.usecases.transfers.downloads.DownloadFileUseCase
import com.owncloud.android.usecases.transfers.downloads.GetLiveDataForDownloadingFileUseCase
import com.owncloud.android.usecases.transfers.downloads.GetLiveDataForFinishedDownloadsFromAccountUseCase
import com.owncloud.android.usecases.transfers.uploads.CancelUploadForFileUseCase
import com.owncloud.android.usecases.transfers.uploads.CancelUploadWithIdUseCase
import com.owncloud.android.usecases.transfers.uploads.RetryFailedUploadsUseCase
import com.owncloud.android.usecases.transfers.uploads.RetryUploadFromContentUriUseCase
import com.owncloud.android.usecases.transfers.uploads.RetryUploadFromSystemUseCase
import com.owncloud.android.usecases.transfers.uploads.UploadFileFromContentUriUseCase
import com.owncloud.android.usecases.transfers.uploads.UploadFileFromSystemUseCase
import com.owncloud.android.usecases.transfers.uploads.UploadFileInConflictUseCase
import com.owncloud.android.usecases.transfers.uploads.UploadFilesFromContentUriUseCase
import com.owncloud.android.usecases.transfers.uploads.UploadFilesFromSystemUseCase
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
    factory { CopyFileUseCase(get()) }
    factory { GetFileByIdUseCase(get()) }
    factory { GetFileByRemotePathUseCase(get()) }
    factory { GetFolderContentUseCase(get()) }
    factory { GetFolderContentAsLiveDataUseCase(get()) }
    factory { GetFolderImagesUseCase(get()) }
    factory { MoveFileUseCase(get()) }
    factory { RemoveFileUseCase(get()) }
    factory { RenameFileUseCase(get()) }
    factory { SaveFileOrFolderUseCase(get()) }
    factory { GetFilesSharedByLinkUseCase(get()) }
    factory { GetSearchFolderContentUseCase(get()) }
    factory { SynchronizeFileUseCase(get(), get(), get(), get()) }
    factory { SynchronizeFolderUseCase(get(), get()) }
    factory { SortFilesUseCase() }

    // Av Offline
    factory { GetFilesAvailableOfflineFromAccountUseCase(get()) }
    factory { GetFilesAvailableOfflineFromEveryAccountUseCase(get()) }
    factory { SetFilesAsAvailableOfflineUseCase(get()) }
    factory { UnsetFilesAsAvailableOfflineUseCase(get()) }

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

    // Transfers
    factory { CancelDownloadForFileUseCase(get()) }
    factory { CancelDownloadsForAccountUseCase(get()) }
    factory { DownloadFileUseCase(get()) }
    factory { GetLiveDataForDownloadingFileUseCase(get()) }
    factory { GetLiveDataForFinishedDownloadsFromAccountUseCase(get()) }
    factory { UploadFileFromSystemUseCase(get()) }
    factory { UploadFileFromContentUriUseCase(get()) }
    factory { UploadFilesFromContentUriUseCase(get(), get()) }
    factory { UploadFilesFromSystemUseCase(get(), get()) }
    factory { UploadFileInConflictUseCase(get(), get()) }
    factory { CancelUploadForFileUseCase(get(), get()) }
    factory { RetryUploadFromSystemUseCase(get(), get(), get()) }
    factory { RetryUploadFromContentUriUseCase(get(), get(), get()) }
    factory { GetAllTransfersUseCase(get()) }
    factory { CancelUploadWithIdUseCase(get(), get()) }
    factory { DeleteTransferWithIdUseCase(get()) }
    factory { ClearFailedTransfersUseCase(get()) }
    factory { RetryFailedUploadsUseCase(get(), get(), get(), get()) }
    factory { ClearSuccessfulTransfersUseCase(get()) }

    // User
    factory { GetStoredQuotaUseCase(get()) }
    factory { GetUserAvatarAsyncUseCase(get()) }
    factory { GetUserInfoAsyncUseCase(get()) }
    factory { RefreshUserQuotaFromServerAsyncUseCase(get()) }

    // Server
    factory { GetServerInfoAsyncUseCase(get()) }

    // Camera Uploads
    factory { GetCameraUploadsConfigurationUseCase(get()) }
    factory { SavePictureUploadsConfigurationUseCase(get()) }
    factory { SaveVideoUploadsConfigurationUseCase(get()) }
    factory { ResetPictureUploadsUseCase(get()) }
    factory { ResetVideoUploadsUseCase(get()) }
    factory { GetPictureUploadsConfigurationStreamUseCase(get()) }
    factory { GetVideoUploadsConfigurationStreamUseCase(get()) }
}
