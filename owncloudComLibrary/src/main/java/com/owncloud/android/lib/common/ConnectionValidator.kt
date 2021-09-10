package com.owncloud.android.lib.common

import com.owncloud.android.lib.common.authentication.OwnCloudCredentials
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.HttpBaseMethod
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.CheckPathExistenceRemoteOperation
import com.owncloud.android.lib.resources.status.GetRemoteStatusOperation
import com.owncloud.android.lib.resources.status.RemoteServerInfo
import org.apache.commons.lang3.exception.ExceptionUtils
import timber.log.Timber
import java.lang.Exception

class ConnectionValidator (
    val clearCookiesOnValidation: Boolean
){

    fun validate(baseClient: OwnCloudClient): Boolean {
        try {
            var validationRetryCount = 0
            val client = OwnCloudClient(baseClient.baseUri, null, false)
            if (clearCookiesOnValidation) {
                client.clearCookies()
            } else {
                client.cookiesForBaseUri = baseClient.cookiesForBaseUri
            }

            client.credentials = baseClient.credentials
            while (validationRetryCount < 5) {
                Timber.d("+++++++++++++++++++++++++++++++++++++ validationRetryCout %d", validationRetryCount)
                var successCounter = 0
                var failCounter = 0

                client.setFollowRedirects(true)
                if (isOnwCloudStatusOk(client)) {
                    successCounter++
                } else {
                    failCounter++
                }

                // Skip the part where we try to check if we can access the parts where we have to be logged in... if we are not logged in
                if(baseClient.credentials !is OwnCloudCredentialsFactory.OwnCloudAnonymousCredentials) {
                    client.setFollowRedirects(false)
                    val contentReply = canAccessRootFolder(client)
                    if (contentReply.httpCode == HttpConstants.HTTP_OK) {
                        if (contentReply.data == true) { //if data is true it means that the content reply was ok
                            successCounter++
                        } else {
                            failCounter++
                        }
                    } else {
                        failCounter++
                        if (contentReply.hashCode() == HttpConstants.HTTP_UNAUTHORIZED) {
                            triggerAuthRefresh()
                        }
                    }
                }
                if (successCounter >= failCounter) {
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
        val reply = getOwnCloudStatus(client)
        // dont check status code. It currently relais on the broken redirect code of the owncloud client
        // TODO: Use okhttp redirect and add this check again
        // return reply.httpCode == HttpConstants.HTTP_OK &&
        return !reply.isException &&
                reply.data != null
    }

    private fun getOwnCloudStatus(client: OwnCloudClient): RemoteOperationResult<RemoteServerInfo> {
        val remoteStatusOperation = GetRemoteStatusOperation()
        return remoteStatusOperation.execute(client)
    }

    private fun triggerAuthRefresh(): OwnCloudCredentials {
        //TODO: Implement me
        return OwnCloudCredentialsFactory.getAnonymousCredentials()
    }

    private fun canAccessRootFolder(client: OwnCloudClient): RemoteOperationResult<Boolean> {
        val checkPathExistenceRemoteOperation = CheckPathExistenceRemoteOperation("/", true)
        return checkPathExistenceRemoteOperation.execute(client)
    }
}