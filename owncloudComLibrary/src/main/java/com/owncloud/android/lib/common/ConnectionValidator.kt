package com.owncloud.android.lib.common

import com.owncloud.android.lib.common.authentication.OwnCloudCredentials
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.HttpBaseMethod
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.status.GetRemoteStatusOperation
import com.owncloud.android.lib.resources.status.RemoteServerInfo
import org.apache.commons.lang3.exception.ExceptionUtils
import timber.log.Timber
import java.lang.Exception

class ConnectionValidator (
    val clearCookiesOnValidation: Boolean
        ){

    fun validate(method: HttpBaseMethod, baseClient: OwnCloudClient): Boolean {
        try {
            var validationRetryCount = 0
            val client = OwnCloudClient(baseClient.baseUri, null, false)
            if (clearCookiesOnValidation) {
                client.clearCookies()
            } else {
                client.cookiesForBaseUri = baseClient.cookiesForBaseUri
            }

            client.credentials = baseClient.credentials
            client.setFollowRedirects(true)
            while (validationRetryCount < 5) {
                var successCounter = 0
                var failCounter = 0

                if (isOnwCloudStatusOk(client)) {
                    successCounter++
                } else {
                    failCounter++
                }

                val contentReply = accessRootFolder()
                if (contentReply == HttpConstants.HTTP_OK) {
                    if (isRootFolderOk(contentReply)) {
                        successCounter++
                    } else {
                        failCounter++
                    }
                } else {
                    failCounter++
                    if (contentReply == HttpConstants.HTTP_UNAUTHORIZED) {
                        triggerAuthRefresh()
                    }
                }
                if(successCounter >= failCounter) {
                    //update credentials in client
                    return true
                }
                validationRetryCount++
            }
            Timber.d("Could not authenticate or get valid data from owncloud")
        } catch (e: Exception) {
            Timber.d(ExceptionUtils.getStackTrace(e))
        }
        return false
    }

    private fun isOnwCloudStatusOk(client: OwnCloudClient): Boolean {
        //TODO: Implement me
        val reply = getOwnCloudStatus(client)
        return if (reply.httpCode == HttpConstants.HTTP_OK) {
            isOCStatusReplyValid(reply.data)
        } else {
            false
        }
    }

    private fun getOwnCloudStatus(client: OwnCloudClient): RemoteOperationResult<RemoteServerInfo> {
        val remoteStatusOperation = GetRemoteStatusOperation()
        //TODO: follow redirects only 5 times
        return remoteStatusOperation.execute(client)
    }

    private fun isOCStatusReplyValid(info: RemoteServerInfo): Boolean {
        //TODO: Implement me
        Timber.d("owncloud version %s", info.ownCloudVersion.version)
        return true
    }

    private fun triggerAuthRefresh(): OwnCloudCredentials {
        //TODO: Implement me
        return OwnCloudCredentialsFactory.getAnonymousCredentials()
    }

    private fun accessRootFolder(): Int {
        //TODO: Implement me
        return 55
    }

    private fun isRootFolderOk(content: Int): Boolean {
        //TODO: Implement me
        return true
    }
}