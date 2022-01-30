package com.owncloud.android.ui.preview

import android.accounts.Account
import android.accounts.AuthenticatorException
import android.accounts.OperationCanceledException
import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.util.Base64
import com.google.android.exoplayer2.MediaItem
import com.owncloud.android.MainApp.Companion.appContext
import com.owncloud.android.MainApp.Companion.userAgent
import com.owncloud.android.datamodel.OCFile
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import timber.log.Timber
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.authentication.OwnCloudBasicCredentials
import com.owncloud.android.lib.common.authentication.OwnCloudBearerCredentials
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.HashMap

/**
 * Task for prepare video player asynchronously
 */
class PrepareVideoPlayerAsyncTask(
    private val context: Context,
    listener: OnPrepareVideoPlayerTaskListener, private val ocFile: OCFile, private val account: Account
) : AsyncTask<Any, Void?, MediaSource>() {
    private val weakListener: WeakReference<OnPrepareVideoPlayerTaskListener> = WeakReference(listener)

    override fun doInBackground(vararg params: Any): MediaSource? {
        var mediaSource: MediaSource? = null
        val uri: Uri
        try {
            // If the file is already downloaded, reproduce it locally, if not, do streaming
            uri = if (ocFile.isDown)
                ocFile.storageUri
            else
                Uri.parse(AccountUtils.getWebDavUrlForAccount(context, account) + Uri.encode(ocFile.remotePath, "/"))
            val useBandwidthMeter = true
            val bandwidthMeter = if (useBandwidthMeter) BANDWIDTH_METER else null
            val httpDataSourceFactory = buildHttpDataSourceFactory(bandwidthMeter, ocFile, account)

            // Produces DataSource instances through which media data is loaded.
            val mediaDataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(context, bandwidthMeter, httpDataSourceFactory!!)

            // This represents the media to be played.
            mediaSource = buildMediaSource(mediaDataSourceFactory, uri)
        } catch (e: AccountUtils.AccountNotFoundException) {
            Timber.e(e)
        }
        return mediaSource
    }

    /**
     * Build the media source needed to play the video
     *
     * @param mediaDataSourceFactory
     * @param uri
     * @return media to be played
     */
    private fun buildMediaSource(mediaDataSourceFactory: DataSource.Factory, uri: Uri): MediaSource {
        return ProgressiveMediaSource.Factory(mediaDataSourceFactory, DefaultExtractorsFactory()).createMediaSource(
            MediaItem.fromUri(uri)
        )
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @param bandwidthMeter Whether to set [.BANDWIDTH_METER] as a listener to the new
     * DataSource factory.
     * @return A new HttpDataSource factory.
     */
    private fun buildHttpDataSourceFactory(bandwidthMeter: DefaultBandwidthMeter?, file: OCFile, account: Account): HttpDataSource.Factory? {
        if (file.isDown) {
            return DefaultHttpDataSource.Factory()
        } else {
            try {
                val credentials = AccountUtils.getCredentialsForAccount(appContext, account)
                val login = credentials.username
                val password = credentials.authToken
                val params: MutableMap<String, String> = HashMap(1)
                if (credentials is OwnCloudBasicCredentials) { // Basic auth
                    val cred = "$login:$password"
                    val auth = "Basic " + Base64.encodeToString(cred.toByteArray(), Base64.URL_SAFE)
                    params["Authorization"] = auth
                } else if (credentials is OwnCloudBearerCredentials) { // OAuth
                    val bearerToken = credentials.getAuthToken()
                    val auth = "Bearer $bearerToken"
                    params["Authorization"] = auth
                }
                return CustomHttpDataSourceFactory(
                    userAgent,
                    bandwidthMeter, params
                )
            } catch (e: AuthenticatorException) {
                Timber.e(e)
            } catch (e: IOException) {
                Timber.e(e)
            } catch (e: OperationCanceledException) {
                Timber.e(e)
            }
        }
        return null
    }

    override fun onPostExecute(mediaSource: MediaSource?) {
        super.onPostExecute(mediaSource)
        if (mediaSource != null) {
            val listener = weakListener.get()
            listener?.OnPrepareVideoPlayerTaskCallback(mediaSource)
        }
    }

    /*
     * Interface to retrieve data from prepare video player task
     */
    interface OnPrepareVideoPlayerTaskListener {
        fun OnPrepareVideoPlayerTaskCallback(mediaSource: MediaSource?)
    }

    companion object {
        private val BANDWIDTH_METER = DefaultBandwidthMeter()
    }

}
