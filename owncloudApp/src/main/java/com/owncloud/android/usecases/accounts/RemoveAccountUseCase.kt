/**
 * ownCloud Android client application
 *
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

package com.owncloud.android.usecases.accounts

import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.camerauploads.usecases.GetCameraUploadsConfigurationUseCase
import com.owncloud.android.domain.camerauploads.usecases.ResetPictureUploadsUseCase
import com.owncloud.android.domain.camerauploads.usecases.ResetVideoUploadsUseCase
import com.owncloud.android.domain.capabilities.CapabilityRepository
import com.owncloud.android.domain.files.FileRepository
import com.owncloud.android.domain.sharing.shares.ShareRepository
import com.owncloud.android.domain.user.UserRepository
import com.owncloud.android.usecases.transfers.uploads.CancelTransfersFromAccountUseCase

class RemoveAccountUseCase(
    private val getCameraUploadsConfigurationUseCase: GetCameraUploadsConfigurationUseCase,
    private val resetPictureUploadsUseCase: ResetPictureUploadsUseCase,
    private val resetVideoUploadsUseCase: ResetVideoUploadsUseCase,
    private val cancelTransfersFromAccountUseCase: CancelTransfersFromAccountUseCase,
    private val fileRepository: FileRepository,
    private val capabilityRepository: CapabilityRepository,
    private val shareRepository: ShareRepository,
    private val userRepository: UserRepository
) : BaseUseCase<Unit, RemoveAccountUseCase.Params>() {

    override fun run(params: Params) {
        // Reset camera uploads if they were enabled for the removed account
        val cameraUploadsConfiguration = getCameraUploadsConfigurationUseCase.execute(Unit)
        if (params.accountName == cameraUploadsConfiguration.getDataOrNull()?.pictureUploadsConfiguration?.accountName) {
            resetPictureUploadsUseCase.execute(Unit)
        }
        if (params.accountName == cameraUploadsConfiguration.getDataOrNull()?.videoUploadsConfiguration?.accountName) {
            resetVideoUploadsUseCase.execute(Unit)
        }

        // Cancel transfers of the removed account
        cancelTransfersFromAccountUseCase.execute(
            CancelTransfersFromAccountUseCase.Params(accountName = params.accountName)
        )

        // Delete files for the removed account in database
        fileRepository.removeFilesForAccount(params.accountName)

        // Delete capabilities for the removed account in database
        capabilityRepository.removeCapabilitiesForAccount(params.accountName)

        // Delete shares for the removed account in database
        shareRepository.removeSharesForAccount(params.accountName)

        // Delete quota for the removed account in database
        userRepository.removeUserQuota(params.accountName)
    }

    data class Params(
        val accountName: String
    )
}
