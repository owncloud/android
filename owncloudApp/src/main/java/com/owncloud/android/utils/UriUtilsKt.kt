package com.owncloud.android.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.owncloud.android.R
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.lib.common.network.WebdavUtils
import timber.log.Timber
import java.io.File

object UriUtilsKt {
    fun getExposedFileUriForOCFile(context: Context, ocFile: OCFile): Uri? {
        if (ocFile.storagePath == null || ocFile.storagePath.toString().isEmpty()) {
            return null
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return Uri.parse(
                ContentResolver.SCHEME_FILE + "://" + WebdavUtils.encodePath(ocFile.storagePath)
            )
        } else {
            // Use the FileProvider to get a content URI
            return try {
                FileProvider.getUriForFile(
                    context,
                    context.getString(R.string.file_provider_authority),
                    File(ocFile.storagePath)
                )
            } catch (e: IllegalArgumentException) {
                Timber.e(e, "File can't be exported")
                null
            }
        }
    }

    /**
     * The URI to the file contents, if stored locally
     *
     * @return A URI to the local copy of the file, or NULL if not stored in the device
     */
    fun getStorageUriForFile(file: OCFile): Uri? {
        if (file.storagePath == null || file.length == 0.toLong()) {
            return null
        }

        return Uri.Builder().apply {
            scheme(ContentResolver.SCHEME_FILE)
            path(file.storagePath)
        }.build()
    }
}
