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

package com.owncloud.android.ui.helpers

import android.accounts.Account
import android.content.ContentResolver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.owncloud.android.ui.asynctasks.CopyAndUploadContentUrisRemake
import com.owncloud.android.ui.helpers.UriUploader.UriUploaderResultCode
import com.owncloud.android.utils.FileStorageUtils
import timber.log.Timber
import java.io.InputStream
import java.util.concurrent.CountDownLatch

class UriUploaderRemake(
    private val urisToUpload: ArrayList<Uri>,
    private val account: Account,
    private val spaceId: String?,
    private val uploadPath: String,
    private val showWaitingDialog: Boolean,
    handlerThreadLooper: Looper
) {

    private val copyAndUploadContentUriTask by lazy {
        CopyAndUploadContentUrisRemake(account = account, spaceId = spaceId, uploadPath = uploadPath)
    }

    private val ioThreadHandler = Handler(handlerThreadLooper)

    fun uploadUris(
        getDisplayNameForUri: (Uri) -> String, showLoadingDialog: () -> Unit, getInputStreamFromUri: (Uri) -> InputStream?
    ): UriUploaderResultCode {
        var uriUploaderResultCode = UriUploaderResultCode.OK
        val countdownLatch = CountDownLatch(1)
        ioThreadHandler.post {
            val validUris = urisToUpload.filter { uri ->
                uri.scheme.equals(ContentResolver.SCHEME_CONTENT)
            }
            uriUploaderResultCode = try {
                if (validUris.isEmpty()) {
                    UriUploaderResultCode.ERROR_NO_FILE_TO_UPLOAD
                } else {
                    if (showWaitingDialog) {
                        showLoadingDialog()
                    }
                    val fullTemporaryPaths = validUris.map { uri -> getFullTemporaryPath(getDisplayNameForUri(uri)) }

                    val currentFullTemporaryPathInputStreams = validUris.mapNotNull {
                        getInputStreamFromUri(it)
                    }
                    if (currentFullTemporaryPathInputStreams.size == fullTemporaryPaths.size) {
                        copyAndUploadContentUriTask.uploadFile(
                            fullTemporaryPaths, currentFullTemporaryPathInputStreams
                        )
                        UriUploaderResultCode.COPY_THEN_UPLOAD
                    } else {
                        // not equal
                        UriUploaderResultCode.ERROR_NO_FILE_TO_UPLOAD
                    }
                }

            } catch (securityException: SecurityException) {
                Timber.e(securityException, "Permissions fail")
                UriUploaderResultCode.ERROR_READ_PERMISSION_NOT_GRANTED
            } catch (exception: Exception) {
                Timber.e(exception, "Unknown error occurred")
                UriUploaderResultCode.ERROR_UNKNOWN
            }
            countdownLatch.countDown()
        }
        Timber.d("LOg upload: $uriUploaderResultCode")
        try {
            countdownLatch.await()
        } catch (_: Exception) {
            // swallow the interruption exception, don't crash the app
        }
        return uriUploaderResultCode
    }

    private fun getFullTemporaryPath(displayNameForUri: String): String {
        val currentRemotePath = "$uploadPath${displayNameForUri}"
        return FileStorageUtils.getTemporalPath(
            account.name, spaceId
        ) + currentRemotePath
    }
}
