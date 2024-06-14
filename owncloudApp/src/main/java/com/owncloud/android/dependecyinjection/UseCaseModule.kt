/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.dependecyinjection

import com.owncloud.android.domain.appregistry.usecases.CreateFileWithAppProviderUseCase
import com.owncloud.android.domain.appregistry.usecases.GetAppRegistryForMimeTypeAsStreamUseCase
import com.owncloud.android.domain.appregistry.usecases.GetAppRegistryWhichAllowCreationAsStreamUseCase
import com.owncloud.android.domain.appregistry.usecases.GetUrlToOpenInWebUseCase
import com.owncloud.android.domain.authentication.oauth.OIDCDiscoveryUseCase
import com.owncloud.android.domain.authentication.oauth.RegisterClientUseCase
import com.owncloud.android.domain.authentication.oauth.RequestTokenUseCase
import com.owncloud.android.domain.authentication.usecases.GetBaseUrlUseCase
import com.owncloud.android.domain.authentication.usecases.LoginBasicAsyncUseCase
import com.owncloud.android.domain.authentication.usecases.LoginOAuthAsyncUseCase
import com.owncloud.android.domain.authentication.usecases.SupportsOAuth2UseCase
import com.owncloud.android.domain.availableoffline.usecases.GetFilesAvailableOfflineFromAccountAsStreamUseCase
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
import com.owncloud.android.domain.files.usecases.IsAnyFileAvailableLocallyAndNotAvailableOfflineUseCase
import com.owncloud.android.domain.files.usecases.CleanConflictUseCase
import com.owncloud.android.domain.files.usecases.CleanWorkersUUIDUseCase
import com.owncloud.android.domain.files.usecases.CopyFileUseCase
import com.owncloud.android.domain.files.usecases.CreateFolderAsyncUseCase
import com.owncloud.android.domain.files.usecases.DisableThumbnailsForFileUseCase
import com.owncloud.android.domain.files.usecases.GetFileByIdAsStreamUseCase
import com.owncloud.android.domain.files.usecases.GetFileByIdUseCase
import com.owncloud.android.domain.files.usecases.GetFileByRemotePathUseCase
import com.owncloud.android.domain.files.usecases.GetFileWithSyncInfoByIdUseCase
import com.owncloud.android.domain.files.usecases.GetFolderContentAsStreamUseCase
import com.owncloud.android.domain.files.usecases.GetFolderContentUseCase
import com.owncloud.android.domain.files.usecases.GetFolderImagesUseCase
import com.owncloud.android.domain.files.usecases.GetPersonalRootFolderForAccountUseCase
import com.owncloud.android.domain.files.usecases.GetSearchFolderContentUseCase
import com.owncloud.android.domain.files.usecases.GetSharedByLinkForAccountAsStreamUseCase
import com.owncloud.android.domain.files.usecases.GetSharesRootFolderForAccount
import com.owncloud.android.domain.files.usecases.GetWebDavUrlForSpaceUseCase
import com.owncloud.android.domain.files.usecases.ManageDeepLinkUseCase
import com.owncloud.android.domain.files.usecases.MoveFileUseCase
import com.owncloud.android.domain.files.usecases.RemoveFileUseCase
import com.owncloud.android.domain.files.usecases.RenameFileUseCase
import com.owncloud.android.domain.files.usecases.SaveConflictUseCase
import com.owncloud.android.domain.files.usecases.SaveDownloadWorkerUUIDUseCase
import com.owncloud.android.domain.files.usecases.SaveFileOrFolderUseCase
import com.owncloud.android.domain.files.usecases.SetLastUsageFileUseCase
import com.owncloud.android.domain.files.usecases.SortFilesUseCase
import com.owncloud.android.domain.files.usecases.SortFilesWithSyncInfoUseCase
import com.owncloud.android.domain.files.usecases.UpdateAlreadyDownloadedFilesPathUseCase
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
import com.owncloud.android.domain.spaces.usecases.GetPersonalAndProjectSpacesForAccountUseCase
import com.owncloud.android.domain.spaces.usecases.GetPersonalAndProjectSpacesWithSpecialsForAccountAsStreamUseCase
import com.owncloud.android.domain.spaces.usecases.GetPersonalSpaceForAccountUseCase
import com.owncloud.android.domain.spaces.usecases.GetProjectSpacesWithSpecialsForAccountAsStreamUseCase
import com.owncloud.android.domain.spaces.usecases.GetSpaceByIdForAccountUseCase
import com.owncloud.android.domain.spaces.usecases.GetSpaceWithSpecialsByIdForAccountUseCase
import com.owncloud.android.domain.spaces.usecases.GetSpacesFromEveryAccountUseCaseAsStream
import com.owncloud.android.domain.spaces.usecases.RefreshSpacesFromServerAsyncUseCase
import com.owncloud.android.domain.transfers.usecases.ClearSuccessfulTransfersUseCase
import com.owncloud.android.domain.transfers.usecases.GetAllTransfersAsStreamUseCase
import com.owncloud.android.domain.transfers.usecases.GetAllTransfersUseCase
import com.owncloud.android.domain.transfers.usecases.UpdatePendingUploadsPathUseCase
import com.owncloud.android.domain.user.usecases.GetStoredQuotaUseCase
import com.owncloud.android.domain.user.usecases.GetUserAvatarAsyncUseCase
import com.owncloud.android.domain.user.usecases.GetUserInfoAsyncUseCase
import com.owncloud.android.domain.user.usecases.GetUserQuotasUseCase
import com.owncloud.android.domain.user.usecases.RefreshUserQuotaFromServerAsyncUseCase
import com.owncloud.android.domain.webfinger.usecases.GetOwnCloudInstanceFromWebFingerUseCase
import com.owncloud.android.domain.webfinger.usecases.GetOwnCloudInstancesFromAuthenticatedWebFingerUseCase
import com.owncloud.android.usecases.accounts.RemoveAccountUseCase
import com.owncloud.android.usecases.files.FilterFileMenuOptionsUseCase
import com.owncloud.android.usecases.files.RemoveLocalFilesForAccountUseCase
import com.owncloud.android.usecases.files.RemoveLocallyFilesWithLastUsageOlderThanGivenTimeUseCase
import com.owncloud.android.usecases.synchronization.SynchronizeFileUseCase
import com.owncloud.android.usecases.synchronization.SynchronizeFolderUseCase
import com.owncloud.android.usecases.transfers.downloads.CancelDownloadForFileUseCase
import com.owncloud.android.usecases.transfers.downloads.CancelDownloadsRecursivelyUseCase
import com.owncloud.android.usecases.transfers.downloads.DownloadFileUseCase
import com.owncloud.android.usecases.transfers.downloads.GetLiveDataForDownloadingFileUseCase
import com.owncloud.android.usecases.transfers.downloads.GetLiveDataForFinishedDownloadsFromAccountUseCase
import com.owncloud.android.usecases.transfers.uploads.CancelTransfersFromAccountUseCase
import com.owncloud.android.usecases.transfers.uploads.CancelUploadForFileUseCase
import com.owncloud.android.usecases.transfers.uploads.CancelUploadUseCase
import com.owncloud.android.usecases.transfers.uploads.CancelUploadsRecursivelyUseCase
import com.owncloud.android.usecases.transfers.uploads.ClearFailedTransfersUseCase
import com.owncloud.android.usecases.transfers.uploads.RetryFailedUploadsForAccountUseCase
import com.owncloud.android.usecases.transfers.uploads.RetryFailedUploadsUseCase
import com.owncloud.android.usecases.transfers.uploads.RetryUploadFromContentUriUseCase
import com.owncloud.android.usecases.transfers.uploads.RetryUploadFromSystemUseCase
import com.owncloud.android.usecases.transfers.uploads.UploadFileFromContentUriUseCase
import com.owncloud.android.usecases.transfers.uploads.UploadFileFromSystemUseCase
import com.owncloud.android.usecases.transfers.uploads.UploadFileInConflictUseCase
import com.owncloud.android.usecases.transfers.uploads.UploadFilesFromContentUriUseCase
import com.owncloud.android.usecases.transfers.uploads.UploadFilesFromSystemUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val useCaseModule = module {
    // Authentication
    factoryOf(::GetBaseUrlUseCase)
    factoryOf(::GetOwnCloudInstanceFromWebFingerUseCase)
    factoryOf(::GetOwnCloudInstancesFromAuthenticatedWebFingerUseCase)
    factoryOf(::LoginBasicAsyncUseCase)
    factoryOf(::LoginOAuthAsyncUseCase)
    factoryOf(::SupportsOAuth2UseCase)

    // OAuth
    factoryOf(::OIDCDiscoveryUseCase)
    factoryOf(::RegisterClientUseCase)
    factoryOf(::RequestTokenUseCase)

    // Capabilities
    factoryOf(::GetCapabilitiesAsLiveDataUseCase)
    factoryOf(::GetStoredCapabilitiesUseCase)
    factoryOf(::RefreshCapabilitiesFromServerAsyncUseCase)

    // Files
    factoryOf(::CleanConflictUseCase)
    factoryOf(::CleanWorkersUUIDUseCase)
    factoryOf(::CopyFileUseCase)
    factoryOf(::CreateFolderAsyncUseCase)
    factoryOf(::DisableThumbnailsForFileUseCase)
    factoryOf(::FilterFileMenuOptionsUseCase)
    factoryOf(::GetFileByIdAsStreamUseCase)
    factoryOf(::GetFileByIdUseCase)
    factoryOf(::GetFileByRemotePathUseCase)
    factoryOf(::GetFileWithSyncInfoByIdUseCase)
    factoryOf(::GetFolderContentAsStreamUseCase)
    factoryOf(::GetFolderContentUseCase)
    factoryOf(::GetFolderImagesUseCase)
    factoryOf(::IsAnyFileAvailableLocallyAndNotAvailableOfflineUseCase)
    factoryOf(::GetPersonalRootFolderForAccountUseCase)
    factoryOf(::GetSearchFolderContentUseCase)
    factoryOf(::GetSharedByLinkForAccountAsStreamUseCase)
    factoryOf(::GetSharesRootFolderForAccount)
    factoryOf(::GetUrlToOpenInWebUseCase)
    factoryOf(::ManageDeepLinkUseCase)
    factoryOf(::MoveFileUseCase)
    factoryOf(::RemoveFileUseCase)
    factoryOf(::RemoveLocalFilesForAccountUseCase)
    factoryOf(::RemoveLocallyFilesWithLastUsageOlderThanGivenTimeUseCase)
    factoryOf(::RenameFileUseCase)
    factoryOf(::SaveConflictUseCase)
    factoryOf(::SaveDownloadWorkerUUIDUseCase)
    factoryOf(::SaveFileOrFolderUseCase)
    factoryOf(::SetLastUsageFileUseCase)
    factoryOf(::SortFilesUseCase)
    factoryOf(::SortFilesWithSyncInfoUseCase)
    factoryOf(::SynchronizeFileUseCase)
    factoryOf(::SynchronizeFolderUseCase)

    // Open in web
    factoryOf(::CreateFileWithAppProviderUseCase)
    factoryOf(::GetAppRegistryForMimeTypeAsStreamUseCase)
    factoryOf(::GetAppRegistryWhichAllowCreationAsStreamUseCase)
    factoryOf(::GetUrlToOpenInWebUseCase)

    // Av Offline
    factoryOf(::GetFilesAvailableOfflineFromAccountAsStreamUseCase)
    factoryOf(::GetFilesAvailableOfflineFromAccountUseCase)
    factoryOf(::GetFilesAvailableOfflineFromEveryAccountUseCase)
    factoryOf(::SetFilesAsAvailableOfflineUseCase)
    factoryOf(::UnsetFilesAsAvailableOfflineUseCase)

    // Sharing
    factoryOf(::CreatePrivateShareAsyncUseCase)
    factoryOf(::CreatePublicShareAsyncUseCase)
    factoryOf(::DeleteShareAsyncUseCase)
    factoryOf(::EditPrivateShareAsyncUseCase)
    factoryOf(::EditPublicShareAsyncUseCase)
    factoryOf(::GetShareAsLiveDataUseCase)
    factoryOf(::GetShareesAsyncUseCase)
    factoryOf(::GetSharesAsLiveDataUseCase)
    factoryOf(::RefreshSharesFromServerAsyncUseCase)

    // Spaces
    factoryOf(::GetPersonalAndProjectSpacesForAccountUseCase)
    factoryOf(::GetPersonalAndProjectSpacesWithSpecialsForAccountAsStreamUseCase)
    factoryOf(::GetPersonalSpaceForAccountUseCase)
    factoryOf(::GetProjectSpacesWithSpecialsForAccountAsStreamUseCase)
    factoryOf(::GetSpaceWithSpecialsByIdForAccountUseCase)
    factoryOf(::GetSpacesFromEveryAccountUseCaseAsStream)
    factoryOf(::GetWebDavUrlForSpaceUseCase)
    factoryOf(::RefreshSpacesFromServerAsyncUseCase)
    factoryOf(::GetSpaceByIdForAccountUseCase)

    // Transfers
    factoryOf(::CancelDownloadForFileUseCase)
    factoryOf(::CancelDownloadsRecursivelyUseCase)
    factoryOf(::CancelTransfersFromAccountUseCase)
    factoryOf(::CancelUploadForFileUseCase)
    factoryOf(::CancelUploadUseCase)
    factoryOf(::CancelUploadsRecursivelyUseCase)
    factoryOf(::ClearFailedTransfersUseCase)
    factoryOf(::ClearSuccessfulTransfersUseCase)
    factoryOf(::DownloadFileUseCase)
    factoryOf(::GetAllTransfersAsStreamUseCase)
    factoryOf(::GetAllTransfersUseCase)
    factoryOf(::GetLiveDataForDownloadingFileUseCase)
    factoryOf(::GetLiveDataForFinishedDownloadsFromAccountUseCase)
    factoryOf(::RetryFailedUploadsForAccountUseCase)
    factoryOf(::RetryFailedUploadsUseCase)
    factoryOf(::RetryUploadFromContentUriUseCase)
    factoryOf(::RetryUploadFromSystemUseCase)
    factoryOf(::UpdateAlreadyDownloadedFilesPathUseCase)
    factoryOf(::UpdatePendingUploadsPathUseCase)
    factoryOf(::UploadFileFromContentUriUseCase)
    factoryOf(::UploadFileFromSystemUseCase)
    factoryOf(::UploadFileInConflictUseCase)
    factoryOf(::UploadFilesFromContentUriUseCase)
    factoryOf(::UploadFilesFromSystemUseCase)

    // User
    factoryOf(::GetStoredQuotaUseCase)
    factoryOf(::GetUserAvatarAsyncUseCase)
    factoryOf(::GetUserInfoAsyncUseCase)
    factoryOf(::GetUserQuotasUseCase)
    factoryOf(::RefreshUserQuotaFromServerAsyncUseCase)

    // Server
    factoryOf(::GetServerInfoAsyncUseCase)

    // Camera Uploads
    factoryOf(::GetCameraUploadsConfigurationUseCase)
    factoryOf(::GetPictureUploadsConfigurationStreamUseCase)
    factoryOf(::GetVideoUploadsConfigurationStreamUseCase)
    factoryOf(::ResetPictureUploadsUseCase)
    factoryOf(::ResetVideoUploadsUseCase)
    factoryOf(::SavePictureUploadsConfigurationUseCase)
    factoryOf(::SaveVideoUploadsConfigurationUseCase)

    // Accounts
    factoryOf(::RemoveAccountUseCase)
}
