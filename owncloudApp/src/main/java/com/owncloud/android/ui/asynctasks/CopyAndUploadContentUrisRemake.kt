/**
 * ownCloud Android client application
 *
 * @author Gibson Ruitiari
 *
 * Copyright (C) 2023 ownCloud GmbH.
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

package com.owncloud.android.ui.asynctasks

import android.accounts.Account
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.usecases.transfers.uploads.UploadFilesFromSystemUseCase
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class CopyAndUploadContentUrisRemake(
    private val account: Account,
    private val spaceId: String?,
    private val uploadPath: String,
) {

    private val uploadFilesFromSystemUseCase: UploadFilesFromSystemUseCase by inject(UploadFilesFromSystemUseCase::class.java)
    fun uploadFile(
        temporaryFilePaths: List<String>, temporaryFilePathInputStreams: List<InputStream>
    ): ResultCode {
        val filesToUpload = arrayListOf<String>()
        return try {
            for (it in temporaryFilePaths.zip(temporaryFilePathInputStreams)) {
                val (filePathInString, filePathInputStream) = it
                val createdTempFilePath = createTempFileFromContentUrisInputStream(
                    filePathInputStream, filePathInString
                )
                if (createdTempFilePath == null) {
                    ResultCode.FILE_NOT_FOUND
                } else {
                    filesToUpload.add(createdTempFilePath)
                }
            }
            val uploadParams = UploadFilesFromSystemUseCase.Params(
                account.name, filesToUpload, uploadPath, spaceId
            )
            uploadFilesFromSystemUseCase.execute(uploadParams)
            filesToUpload.clear()
            ResultCode.OK
        } catch (exception: Exception) {
            Timber.e(exception, "Exception while copying files from given input streams")
            ResultCode.LOCAL_STORAGE_NOT_COPIED
        }
    }

    private fun createTempFileFromContentUrisInputStream(
        inputStream: InputStream, temporaryFilePathInString: String
    ): String? {
        val temporaryFilePath = File(temporaryFilePathInString)
        temporaryFilePath.parentFile?.apply {
            if (!exists()) mkdirs()
        }
        temporaryFilePath.createNewFile()
        return try {
            FileOutputStream(temporaryFilePathInString).buffered().use { writer ->
                inputStream.buffered().use { is_ ->
                    val buffer = ByteArray(4096)
                    var bytesConsumed: Int
                    while (is_.read(buffer).also { bytesConsumed = it } != -1) {
                        writer.write(buffer, 0, bytesConsumed)
                    }
                }
            }
            return temporaryFilePathInString
        } catch (ex: IOException) {
            Timber.e("Error While Creating Temp File $ex")
            if (temporaryFilePath.exists()) {
                temporaryFilePath.delete()
            }
            null
        }
    }
}
