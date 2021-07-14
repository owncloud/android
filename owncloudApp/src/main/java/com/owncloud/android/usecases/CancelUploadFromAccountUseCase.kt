/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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
package com.owncloud.android.usecases

import androidx.work.WorkManager
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.domain.BaseUseCase
import timber.log.Timber

class CancelUploadFromAccountUseCase(
    private val workManager: WorkManager
) : BaseUseCase<Unit, CancelUploadFromAccountUseCase.Params>() {

    override fun run(params: Params) {
        workManager.cancelAllWorkByTag(params.accountName)

        val uploadsStorageManager = UploadsStorageManager(MainApp.appContext.contentResolver)
        uploadsStorageManager.removeUploads(params.accountName)

        Timber.i("Uploads of ${params.accountName} has been cancelled.")
    }

    data class Params(
        val accountName: String,
    )
}
