/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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

package com.owncloud.android.presentation.thumbnails

import android.accounts.Account
import android.net.Uri
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import com.owncloud.android.MainApp.Companion.appContext
import com.owncloud.android.R
import com.owncloud.android.data.ClientManager
import com.owncloud.android.domain.files.model.OCFileWithSyncInfo
import com.owncloud.android.domain.spaces.model.SpaceSpecial
import com.owncloud.android.lib.common.SingleSessionManager
import com.owncloud.android.lib.common.http.HttpConstants.ACCEPT_ENCODING_HEADER
import com.owncloud.android.lib.common.http.HttpConstants.ACCEPT_ENCODING_IDENTITY
import com.owncloud.android.lib.common.http.HttpConstants.AUTHORIZATION_HEADER
import com.owncloud.android.lib.common.http.HttpConstants.OC_X_REQUEST_ID
import com.owncloud.android.lib.common.http.HttpConstants.USER_AGENT_HEADER
import com.owncloud.android.lib.common.utils.RandomUtils
import com.owncloud.android.presentation.authentication.AccountUtils
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.Locale
import kotlin.math.roundToInt

object ThumbnailsRequester : KoinComponent {
    private val clientManager: ClientManager by inject()

    private const val SPACE_SPECIAL_PREVIEW_URI = "%s?scalingup=0&a=1&x=%d&y=%d&c=%s&preview=1"
    private const val FILE_PREVIEW_URI = "%s%s?x=%d&y=%d&c=%s&preview=1&id=%s"

    private const val DISK_CACHE_SIZE: Long = 1024 * 1024 * 10 // 10MB

    fun getCoilImageLoader(): ImageLoader {
        val ownCloudClient = getOwnCloudClient()

        val coilRequestHeaderInterceptor = CoilRequestHeaderInterceptor(
            requestHeaders = hashMapOf(
                AUTHORIZATION_HEADER to ownCloudClient.credentials.headerAuth,
                ACCEPT_ENCODING_HEADER to ACCEPT_ENCODING_IDENTITY,
                USER_AGENT_HEADER to SingleSessionManager.getUserAgent(),
                OC_X_REQUEST_ID to RandomUtils.generateRandomUUID(),
            )
        )

        return ImageLoader(appContext).newBuilder().okHttpClient(
            okHttpClient = ownCloudClient.okHttpClient.newBuilder().addNetworkInterceptor(coilRequestHeaderInterceptor).build()
        ).logger(DebugLogger())
            .memoryCache {
                MemoryCache.Builder(appContext)
                    .maxSizePercent(0.1)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(appContext.cacheDir.resolve("thumbnails_coil_cache"))
                    .maxSizeBytes(DISK_CACHE_SIZE)
                    .build()
            }
            .build()
    }

    fun getPreviewUriForSpaceSpecial(spaceSpecial: SpaceSpecial): String {
        // Converts dp to pixel
        val spacesThumbnailSize = appContext.resources.getDimension(R.dimen.spaces_thumbnail_height).roundToInt()
        return String.format(
            Locale.ROOT,
            SPACE_SPECIAL_PREVIEW_URI,
            spaceSpecial.webDavUrl,
            spacesThumbnailSize,
            spacesThumbnailSize,
            spaceSpecial.eTag
        )
    }

    fun getPreviewUriForFile(ocFile: OCFileWithSyncInfo, account: Account): String {
        var baseUrl = getOwnCloudClient().baseUri.toString() + "/remote.php/dav/files/" + account.name.split("@".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()[0]
        ocFile.space?.getSpaceSpecialImage()?.let {
            baseUrl = it.webDavUrl
        }

        // Converts dp to pixel
        val fileThumbnailSize = appContext.resources.getDimension(R.dimen.file_icon_size_grid).roundToInt()
        return String.format(
            Locale.ROOT,
            FILE_PREVIEW_URI,
            baseUrl,
            Uri.encode(ocFile.file.remotePath, "/"),
            fileThumbnailSize,
            fileThumbnailSize,
            ocFile.file.etag,
            "${ocFile.file.remoteId}${ocFile.file.modificationTimestamp}",
        )
    }

    private fun getOwnCloudClient() = clientManager.getClientForCoilThumbnails(
        accountName = AccountUtils.getCurrentOwnCloudAccount(appContext).name
    )

    private class CoilRequestHeaderInterceptor(
        private val requestHeaders: HashMap<String, String>
    ) : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request().newBuilder()
            requestHeaders.toHeaders().forEach { request.addHeader(it.first, it.second) }
            return chain.proceed(request.build()).newBuilder().removeHeader("Cache-Control")
                .addHeader("Cache-Control", "max-age=5000 , must-revalidate, value").build().also { Timber.d("Header :" + it.headers) }
        }
    }
}
