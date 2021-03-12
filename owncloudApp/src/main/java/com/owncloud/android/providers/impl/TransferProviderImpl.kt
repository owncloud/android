/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.providers.impl

import android.accounts.Account
import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.presentation.manager.TRANSFER_TAG_DOWNLOAD
import com.owncloud.android.providers.TransferProvider
import com.owncloud.android.workers.DownloadFileWorker
import timber.log.Timber
import java.util.UUID

class TransferProviderImpl(
    private val context: Context
) : TransferProvider {

    override fun downloadFile(account: Account, file: OCFile): UUID {
        val inputData = workDataOf(
            DownloadFileWorker.KEY_PARAM_ACCOUNT to account.name,
            DownloadFileWorker.KEY_PARAM_FILE_ID to file.id
        )

        val downloadFileWork = OneTimeWorkRequestBuilder<DownloadFileWorker>()
            .setInputData(inputData)
            .addTag(file.id.toString())
            .addTag(account.name)
            .addTag(TRANSFER_TAG_DOWNLOAD)
            .build()

        WorkManager.getInstance(context).enqueue(downloadFileWork)
        Timber.i("Download of ${file.fileName} has been enqueued.")

        return downloadFileWork.id
    }
}
