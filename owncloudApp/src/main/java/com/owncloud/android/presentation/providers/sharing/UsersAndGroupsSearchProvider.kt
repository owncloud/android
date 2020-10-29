/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Juan Carlos González Cabrero
 * @author David González Verdugo
 * Copyright (C) 2020 ownCloud GmbH.
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package com.owncloud.android.presentation.providers.sharing

import android.app.SearchManager
import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.BaseColumns
import android.widget.Toast
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.domain.capabilities.usecases.GetStoredCapabilitiesUseCase
import com.owncloud.android.domain.sharing.sharees.GetShareesAsyncUseCase
import com.owncloud.android.domain.sharing.sharees.model.OCSharee
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.extensions.parseError
import org.json.JSONException
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.HashMap
import java.util.Locale

/**
 * Content provider for search suggestions, to search for users and groups existing in an ownCloud server.
 */
class UsersAndGroupsSearchProvider : ContentProvider() {
    private lateinit var uriMatcher: UriMatcher

    override fun getType(uri: Uri): String? {
        // TODO implement
        return null
    }

    override fun onCreate(): Boolean {
        try {
            suggestAuthority = context?.resources?.getString(R.string.search_suggest_authority)

            // init share types
            shareTypes[suggestAuthority!! + DATA_USER_SUFFIX] = ShareType.USER
            shareTypes[suggestAuthority!! + DATA_GROUP_SUFFIX] = ShareType.GROUP
            shareTypes[suggestAuthority!! + DATA_REMOTE_SUFFIX] = ShareType.FEDERATED

            // init URI matcher
            uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
            uriMatcher.addURI(
                suggestAuthority,
                SearchManager.SUGGEST_URI_PATH_QUERY + "/*",
                SEARCH
            )

            // init intent action
            suggestIntentAction = context?.resources?.getString(R.string.search_suggest_intent_action)

            return true

        } catch (t: Throwable) {
            Timber.e(t, "Fail creating provider")
            return false
        }
    }

    /**
     * TODO description
     *
     *
     * Reference: http://developer.android.com/guide/topics/search/adding-custom-suggestions.html#CustomContentProvider
     *
     * @param uri           Content [Uri], formattted as
     * "content://com.owncloud.android.providers.UsersAndGroupsSearchProvider/" +
     * [android.app.SearchManager.SUGGEST_URI_PATH_QUERY] + "/" + 'userQuery'
     * @param projection    Expected to be NULL.
     * @param selection     Expected to be NULL.
     * @param selectionArgs Expected to be NULL.
     * @param sortOrder     Expected to be NULL.
     * @return Cursor with users and groups in the ownCloud server that match 'userQuery'.
     */
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        Timber.d("query received in thread ${Thread.currentThread().name}")
        return when (uriMatcher.match(uri)) {
            SEARCH -> searchForUsersOrGroups(uri)
            else -> null
        }
    }

    private fun searchForUsersOrGroups(uri: Uri): Cursor? {
        var response: MatrixCursor? = null

        val userQuery = uri.lastPathSegment!!.toLowerCase(Locale.getDefault())

        /// need to trust on the AccountUtils to get the current account since the query in the client side is not
        /// directly started by our code, but from SearchView implementation
        val account = AccountUtils.getCurrentOwnCloudAccount(context)

        val getStoredCapabilitiesUseCase: GetStoredCapabilitiesUseCase by inject()

        val capabilities = getStoredCapabilitiesUseCase.execute(
            GetStoredCapabilitiesUseCase.Params(
                accountName = account.name
            )
        )

        val getShareesAsyncUseCase: GetShareesAsyncUseCase by inject()

        val getShareesResult = getShareesAsyncUseCase.execute(
            GetShareesAsyncUseCase.Params(
                userQuery,
                REQUESTED_PAGE,
                RESULTS_PER_PAGE
            )
        )

        if (getShareesResult.isError) {
            context?.let {
                showErrorMessage(
                    it.resources.getString(R.string.get_sharees_error),
                    getShareesResult.getThrowableOrNull()
                )
            }
        }

        val names = getShareesResult.getDataOrNull()

        // convert the responses from the OC server to the expected format
        if (!names.isNullOrEmpty()) {
            response = MatrixCursor(COLUMNS)
            val namesIt = names.iterator()
            var item: OCSharee
            var displayName: String? = null
            var icon = 0
            var dataUri: Uri? = null
            var count = 0

            val userBaseUri = Uri.Builder().scheme(CONTENT).authority(
                suggestAuthority!! + DATA_USER_SUFFIX
            ).build()
            val groupBaseUri = Uri.Builder().scheme(CONTENT).authority(
                suggestAuthority!! + DATA_GROUP_SUFFIX
            ).build()
            val remoteBaseUri = Uri.Builder().scheme(CONTENT).authority(
                suggestAuthority!! + DATA_REMOTE_SUFFIX
            ).build()

            val federatedShareAllowed = capabilities?.filesSharingFederationOutgoing?.isTrue ?: false

            try {
                while (namesIt.hasNext()) {
                    item = namesIt.next()
                    var userName = item.label
                    val type = item.shareType
                    val shareWith = item.shareWith

                    try {
                        val shareWithAdditionalInfo = item.additionalInfo

                        userName = if (shareWithAdditionalInfo.isEmpty())
                            userName
                        else
                            "$userName ($shareWithAdditionalInfo)"

                    } catch (e: JSONException) {
                        Timber.e(e, "Exception while parsing shareWithAdditionalInfo")
                    }

                    when (type) {
                        ShareType.GROUP -> {
                            displayName = context?.getString(R.string.share_group_clarification, userName)
                            icon = R.drawable.ic_group
                            dataUri = Uri.withAppendedPath(groupBaseUri, shareWith)
                        }
                        ShareType.FEDERATED -> {
                            if (federatedShareAllowed) {
                                icon = R.drawable.ic_user
                                displayName = if (userName == shareWith) {
                                    context?.getString(R.string.share_remote_clarification, userName)
                                } else {
                                    val uriSplitted =
                                        shareWith.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                                    context?.getString(
                                        R.string.share_known_remote_clarification, userName,
                                        uriSplitted[uriSplitted.size - 1]
                                    )
                                }
                                dataUri = Uri.withAppendedPath(remoteBaseUri, shareWith)
                            }
                        }
                        ShareType.USER -> {
                            displayName = userName
                            icon = R.drawable.ic_user
                            dataUri = Uri.withAppendedPath(userBaseUri, shareWith)
                        }
                    }

                    if (displayName != null && dataUri != null) {
                        response.newRow()
                            .add(count++)             // BaseColumns._ID
                            .add(displayName)         // SearchManager.SUGGEST_COLUMN_TEXT_1
                            .add(icon)                // SearchManager.SUGGEST_COLUMN_ICON_1
                            .add(dataUri)
                    }
                }
            } catch (e: JSONException) {
                Timber.e(e, "Exception while parsing data of users/groups")
            }
        }

        return response
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        // TODO implementation
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        // TODO implementation
        return 0
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        // TODO implementation
        return 0
    }

    /**
     * Show error genericErrorMessage
     */
    private fun showErrorMessage(genericErrorMessage: String, throwable: Throwable?) {
        val errorMessage = throwable?.parseError(genericErrorMessage, MainApp.appContext.resources)
        val handler = Handler(Looper.getMainLooper())
        // The Toast must be shown in the main thread to grant that will be hidden correctly; otherwise
        // the thread may die before, an exception will occur, and the genericErrorMessage will be left on the screen
        // until the app dies
        handler.post {
            Toast.makeText(
                context?.applicationContext,
                errorMessage,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {

        private val COLUMNS = arrayOf(
            BaseColumns._ID,
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_ICON_1,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA
        )

        private const val SEARCH = 1

        private const val DEFAULT_MIN_CHARACTERS_TO_SEARCH = 4
        private const val RESULTS_PER_PAGE = 30
        private const val REQUESTED_PAGE = 1

        const val CONTENT = "content"

        const val DATA_USER_SUFFIX = ".data.user"
        const val DATA_GROUP_SUFFIX = ".data.group"
        const val DATA_REMOTE_SUFFIX = ".data.remote"

        private var suggestAuthority: String? = null
        var suggestIntentAction: String? = null
            private set
        private val shareTypes = HashMap<String, ShareType>()

        fun getShareType(authority: String?): ShareType? {
            return shareTypes[authority]
        }
    }
}
