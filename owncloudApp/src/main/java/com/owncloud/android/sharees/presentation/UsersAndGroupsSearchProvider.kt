/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Juan Carlos González Cabrero
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.sharees.presentation

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
import com.owncloud.android.R
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.capabilities.viewmodel.OCCapabilityViewModel
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.GetRemoteShareesOperation
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.operations.common.OperationType
import com.owncloud.android.ui.errorhandling.ErrorMessageAdapter
import com.owncloud.android.vo.Resource
import org.json.JSONException
import org.json.JSONObject
import java.util.ArrayList
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
            suggestAuthority = context!!.resources.getString(R.string.search_suggest_authority)

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
            suggestIntentAction = context!!.resources.getString(R.string.search_suggest_intent_action)

            return true

        } catch (t: Throwable) {
            Log_OC.e("TAG", "Fail creating provider", t)
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
        Log_OC.d(TAG, "query received in thread " + Thread.currentThread().name)
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

        val ocCapabilityViewModel = OCCapabilityViewModel(context, account)
        val capability = ocCapabilityViewModel.getStoredCapabilityForAccount()

        val minCharactersToSearch = capability.filesSharingSearchMinLength ?: DEFAULT_MIN_CHARACTERS_TO_SEARCH

        if (userQuery.length < minCharactersToSearch) {
            return MatrixCursor(COLUMNS)
        }

        val ocShareeViewModel = OCShareeViewModel(
            context,
            account
        )

        val shareesResource = ocShareeViewModel.getSharees(
            userQuery,
            REQUESTED_PAGE,
            RESULTS_PER_PAGE
        )

        if (!shareesResource.isSuccess()) {
            showErrorMessage(shareesResource)
        }

        val names = shareesResource.data

        // convert the responses from the OC server to the expected format
        if (names?.size!! > 0) {
            response = MatrixCursor(COLUMNS)
            val namesIt = names.iterator()
            var item: JSONObject
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

            val manager = FileDataStorageManager(
                context,
                account, context!!.contentResolver
            )
            val federatedShareAllowed = manager.getCapability(account!!.name).filesSharingFederationOutgoing
                .isTrue

            try {
                while (namesIt.hasNext()) {
                    item = namesIt.next()
                    var userName = item.getString(GetRemoteShareesOperation.PROPERTY_LABEL)
                    val value = item.getJSONObject(GetRemoteShareesOperation.NODE_VALUE)
                    val type = value.getInt(GetRemoteShareesOperation.PROPERTY_SHARE_TYPE)
                    val shareWith = value.getString(GetRemoteShareesOperation.PROPERTY_SHARE_WITH)

                    try {
                        val shareWithAdditionalInfo = value.getString(
                            GetRemoteShareesOperation.PROPERTY_SHARE_WITH_ADDITIONAL_INFO
                        )

                        userName = if (shareWithAdditionalInfo.isEmpty())
                            userName
                        else
                            "$userName ($shareWithAdditionalInfo)"

                    } catch (e: JSONException) {
                        Log_OC.e(TAG, "Exception while parsing shareWithAdditionalInfo", e)
                    }

                    if (ShareType.GROUP.value == type) {
                        displayName = context!!.getString(R.string.share_group_clarification, userName)
                        icon = R.drawable.ic_group
                        dataUri = Uri.withAppendedPath(groupBaseUri, shareWith)
                    } else if (ShareType.FEDERATED.value == type && federatedShareAllowed) {
                        icon = R.drawable.ic_user
                        if (userName == shareWith) {
                            displayName = context!!.getString(R.string.share_remote_clarification, userName)
                        } else {
                            val uriSplitted =
                                shareWith.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                            displayName = context!!.getString(
                                R.string.share_known_remote_clarification, userName,
                                uriSplitted[uriSplitted.size - 1]
                            )
                        }
                        dataUri = Uri.withAppendedPath(remoteBaseUri, shareWith)
                    } else if (ShareType.USER.value == type) {
                        displayName = userName
                        icon = R.drawable.ic_user
                        dataUri = Uri.withAppendedPath(userBaseUri, shareWith)
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
                Log_OC.e(TAG, "Exception while parsing data of users/groups", e)
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
     * Show error message
     *
     * @param resource Resource with the failure information.
     */
    private fun showErrorMessage(resource: Resource<ArrayList<JSONObject>>) {
        val handler = Handler(Looper.getMainLooper())

        // The Toast must be shown in the main thread to grant that will be hidden correctly; otherwise
        // the thread may die before, an exception will occur, and the message will be left on the screen
        // until the app dies
        handler.post {
            Toast.makeText(
                context!!.applicationContext,
                ErrorMessageAdapter.getResultMessage(
                    resource.code,
                    resource.exception,
                    OperationType.GET_SHAREES,
                    context!!.resources
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        private val TAG = UsersAndGroupsSearchProvider::class.java.simpleName

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
