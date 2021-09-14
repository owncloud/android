package com.owncloud.android.lib.common

import android.accounts.AccountManager
import android.accounts.AccountsException
import android.content.Context
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory.OwnCloudAnonymousCredentials
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.CheckPathExistenceRemoteOperation
import com.owncloud.android.lib.resources.status.GetRemoteStatusOperation
import com.owncloud.android.lib.resources.status.RemoteServerInfo
import org.apache.commons.lang3.exception.ExceptionUtils
import timber.log.Timber
import java.io.IOException
import java.lang.Exception

class ConnectionValidator (
    val context: Context,
    val clearCookiesOnValidation: Boolean
){

    fun validate(baseClient: OwnCloudClient, singleSessionManager: SingleSessionManager): Boolean {
        try {
            var validationRetryCount = 0
            val client = OwnCloudClient(baseClient.baseUri, null, false, singleSessionManager)
            if (clearCookiesOnValidation) {
                client.clearCookies();
            } else {
                client.cookiesForBaseUri = baseClient.cookiesForBaseUri
            }

            client.account = baseClient.account
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
                if(baseClient.credentials !is OwnCloudAnonymousCredentials) {
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
                        if (contentReply.httpCode == HttpConstants.HTTP_UNAUTHORIZED) {
                            checkUnauthorizedAccess(client, singleSessionManager, contentReply.httpCode)
                        }
                    }
                }
                if (successCounter >= failCounter) {
                    baseClient.credentials = client.credentials
                    baseClient.cookiesForBaseUri = client.cookiesForBaseUri
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

    private fun canAccessRootFolder(client: OwnCloudClient): RemoteOperationResult<Boolean> {
        val checkPathExistenceRemoteOperation = CheckPathExistenceRemoteOperation("/", true)
        return checkPathExistenceRemoteOperation.execute(client)
    }

    /**
     * Determines if credentials should be invalidated according the to the HTTPS status
     * of a network request just performed.
     *
     * @param httpStatusCode Result of the last request ran with the 'credentials' belows.
     * @return 'True' if credentials should and might be invalidated, 'false' if shouldn't or
     * cannot be invalidated with the given arguments.
     */
    private fun shouldInvalidateAccountCredentials(credentials: OwnCloudCredentials, account: OwnCloudAccount, httpStatusCode: Int): Boolean {
        var shouldInvalidateAccountCredentials = httpStatusCode == HttpConstants.HTTP_UNAUTHORIZED
        shouldInvalidateAccountCredentials = shouldInvalidateAccountCredentials and  // real credentials
                (credentials !is OwnCloudAnonymousCredentials)

        // test if have all the needed to effectively invalidate ...
        shouldInvalidateAccountCredentials =
            shouldInvalidateAccountCredentials and (account.savedAccount != null)
        return shouldInvalidateAccountCredentials
    }

    /**
     * Invalidates credentials stored for the given account in the system  [AccountManager] and in
     * current [SingleSessionManager.getDefaultSingleton] instance.
     *
     *
     * [.shouldInvalidateAccountCredentials] should be called first.
     *
     */
    private fun invalidateAccountCredentials(account: OwnCloudAccount, credentials: OwnCloudCredentials) {
        val am = AccountManager.get(context)
        am.invalidateAuthToken(
            account.savedAccount.type,
            credentials.authToken
        )
        am.clearPassword(account.savedAccount) // being strict, only needed for Basic Auth credentials
    }

    /**
     * Checks the status code of an execution and decides if should be repeated with fresh credentials.
     *
     *
     * Invalidates current credentials if the request failed as anauthorized.
     *
     *
     * Refresh current credentials if possible, and marks a retry.
     *
     * @param status
     * @param repeatCounter
     * @return
     */
    private fun checkUnauthorizedAccess(client: OwnCloudClient, singleSessionManager: SingleSessionManager, status: Int): Boolean {
        var credentialsWereRefreshed = false
        val account = client.account
        val credentials = account.credentials
        if (shouldInvalidateAccountCredentials(credentials, account, status)) {
            invalidateAccountCredentials(account, credentials)

            if (credentials.authTokenCanBeRefreshed()) {
                try {
                    // This command does the actual refresh
                    account.loadCredentials(context)
                    // if mAccount.getCredentials().length() == 0 --> refresh failed
                    client.credentials = account.credentials
                    credentialsWereRefreshed = true
                } catch (e: AccountsException) {
                    Timber.e(
                        e, "Error while trying to refresh auth token for %s\ntrace: %s",
                        account.savedAccount.name,
                        ExceptionUtils.getStackTrace(e)
                    )
                } catch (e: IOException) {
                    Timber.e(
                        e, "Error while trying to refresh auth token for %s\ntrace: %s",
                        account.savedAccount.name,
                        ExceptionUtils.getStackTrace(e)
                    )
                }
                if (!credentialsWereRefreshed) {
                    // if credentials are not refreshed, client must be removed
                    // from the OwnCloudClientManager to prevent it is reused once and again
                    singleSessionManager.removeClientFor(account)
                }
            }
            // else: onExecute will finish with status 401
        }
        return credentialsWereRefreshed
    }
}