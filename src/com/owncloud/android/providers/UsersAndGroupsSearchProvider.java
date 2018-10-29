/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Juan Carlos González Cabrero
 * @author David González Verdugo
 * Copyright (C) 2018 ownCloud GmbH.
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

package com.owncloud.android.providers;

import android.accounts.Account;
import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.GetRemoteShareesOperation;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.ui.errorhandling.ErrorMessageAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Content provider for search suggestions, to search for users and groups existing in an ownCloud server.
 */
public class UsersAndGroupsSearchProvider extends ContentProvider {

    private static final String TAG = UsersAndGroupsSearchProvider.class.getSimpleName();

    private static final String[] COLUMNS = {
            BaseColumns._ID,
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_ICON_1,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA
    };

    private static final int SEARCH = 1;

    private static final int RESULTS_PER_PAGE = 30;
    private static final int REQUESTED_PAGE = 1;

    public static final String CONTENT = "content";

    public static final String DATA_USER_SUFFIX = ".data.user";
    public static final String DATA_GROUP_SUFFIX = ".data.group";
    public static final String DATA_REMOTE_SUFFIX = ".data.remote";

    private static String sSuggestAuthority;
    private static String sSuggestIntentAction;
    private static Map<String, ShareType> sShareTypes = new HashMap<>();

    public static String getSuggestIntentAction() {
        return sSuggestIntentAction;
    }

    public static ShareType getShareType(String authority) {
        return sShareTypes.get(authority);
    }

    private UriMatcher mUriMatcher = null;

    @Nullable
    @Override
    public String getType(Uri uri) {
        // TODO implement
        return null;
    }

    @Override
    public boolean onCreate() {
        try {
            sSuggestAuthority = getContext().getResources().
                getString(R.string.search_suggest_authority);

            // init share types
            sShareTypes.put(sSuggestAuthority + DATA_USER_SUFFIX, ShareType.USER);
            sShareTypes.put(sSuggestAuthority + DATA_GROUP_SUFFIX, ShareType.GROUP);
            sShareTypes.put(sSuggestAuthority + DATA_REMOTE_SUFFIX, ShareType.FEDERATED);

            // init URI matcher
            mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
            mUriMatcher.addURI(
                sSuggestAuthority,
                SearchManager.SUGGEST_URI_PATH_QUERY + "/*",
                SEARCH
            );

            // init intent action
            sSuggestIntentAction = getContext().getResources().
                getString(R.string.search_suggest_intent_action);

            return true;

        } catch (Throwable t) {
            Log_OC.e("TAG", "Fail creating provider", t);
            return false;
        }
    }

    /**
     * TODO description
     * <p/>
     * Reference: http://developer.android.com/guide/topics/search/adding-custom-suggestions.html#CustomContentProvider
     *
     * @param uri           Content {@link Uri}, formattted as
     *                      "content://com.owncloud.android.providers.UsersAndGroupsSearchProvider/" +
     *                      {@link android.app.SearchManager#SUGGEST_URI_PATH_QUERY} + "/" + 'userQuery'
     * @param projection    Expected to be NULL.
     * @param selection     Expected to be NULL.
     * @param selectionArgs Expected to be NULL.
     * @param sortOrder     Expected to be NULL.
     * @return              Cursor with users and groups in the ownCloud server that match 'userQuery'.
     */
    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log_OC.d(TAG, "query received in thread " + Thread.currentThread().getName());
        int match = mUriMatcher.match(uri);
        switch (match) {
            case SEARCH:
                return searchForUsersOrGroups(uri);

            default:
                return null;
        }
    }

    private Cursor searchForUsersOrGroups(Uri uri) {
        MatrixCursor response = null;

        String userQuery = uri.getLastPathSegment().toLowerCase();


        /// need to trust on the AccountUtils to get the current account since the query in the client side is not
        /// directly started by our code, but from SearchView implementation
        Account account = AccountUtils.getCurrentOwnCloudAccount(getContext());

        /// request to the OC server about users and groups matching userQuery
        GetRemoteShareesOperation searchRequest = new GetRemoteShareesOperation(
                userQuery, REQUESTED_PAGE, RESULTS_PER_PAGE);
        RemoteOperationResult<ArrayList<JSONObject>> result = searchRequest.execute(account, getContext());

        ArrayList<JSONObject> names = result.getData();

        /// convert the responses from the OC server to the expected format
        if (names.size() > 0) {
            response = new MatrixCursor(COLUMNS);
            Iterator<JSONObject> namesIt = names.iterator();
            JSONObject item;
            String displayName = null;
            int icon = 0;
            Uri dataUri = null;
            int count = 0;

            MainApp app = (MainApp)getContext().getApplicationContext();
            Uri userBaseUri = new Uri.Builder().scheme(CONTENT).authority(
                sSuggestAuthority + DATA_USER_SUFFIX
            ).build();
            Uri groupBaseUri = new Uri.Builder().scheme(CONTENT).authority(
                sSuggestAuthority + DATA_GROUP_SUFFIX
            ).build();
            Uri remoteBaseUri = new Uri.Builder().scheme(CONTENT).authority(
                sSuggestAuthority + DATA_REMOTE_SUFFIX
            ).build();

            FileDataStorageManager manager = new FileDataStorageManager(
                    getContext(),
                    account, getContext().getContentResolver()
            );
            boolean federatedShareAllowed = manager.getCapability(account.name).getFilesSharingFederationOutgoing()
                    .isTrue();

            try {
                while (namesIt.hasNext()) {
                    item = namesIt.next();
                    String userName = item.getString(GetRemoteShareesOperation.PROPERTY_LABEL);
                    JSONObject value = item.getJSONObject(GetRemoteShareesOperation.NODE_VALUE);
                    int type = value.getInt(GetRemoteShareesOperation.PROPERTY_SHARE_TYPE);
                    String shareWith = value.getString(GetRemoteShareesOperation.PROPERTY_SHARE_WITH);

                    if (ShareType.GROUP.getValue() == type) {
                        displayName = getContext().getString(R.string.share_group_clarification, userName);
                        icon = R.drawable.ic_group;
                        dataUri = Uri.withAppendedPath(groupBaseUri, shareWith);
                    } else if (ShareType.FEDERATED.getValue() == type && federatedShareAllowed) {
                        icon = R.drawable.ic_user;
                        if (userName.equals(shareWith)) {
                            displayName = getContext().getString(R.string.share_remote_clarification, userName);
                        } else {
                            String[] uriSplitted = shareWith.split("@");
                            displayName = getContext().getString(R.string.share_known_remote_clarification, userName,
                                uriSplitted[uriSplitted.length - 1]);
                        }
                        dataUri = Uri.withAppendedPath(remoteBaseUri, shareWith);
                    } else if (ShareType.USER.getValue() == type) {
                        displayName = userName;
                        icon = R.drawable.ic_user;
                        dataUri = Uri.withAppendedPath(userBaseUri, shareWith);
                    }

                    if (displayName != null && dataUri != null) {
                        response.newRow()
                            .add(count++)             // BaseColumns._ID
                            .add(displayName)         // SearchManager.SUGGEST_COLUMN_TEXT_1
                            .add(icon)                // SearchManager.SUGGEST_COLUMN_ICON_1
                            .add(dataUri);
                    }
                }

            } catch (JSONException e) {
                Log_OC.e(TAG, "Exception while parsing data of users/groups", e);
            }
        }

        return response;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO implementation
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO implementation
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO implementation
        return 0;
    }

    /**
     * Show error message
     *
     * @param result Result with the failure information.
     */
    private void showErrorMessage(final RemoteOperationResult result) {
        Handler handler = new Handler(Looper.getMainLooper());

        // The Toast must be shown in the main thread to grant that will be hidden correctly; otherwise
        // the thread may die before, an exception will occur, and the message will be left on the screen
        // until the app dies
        handler.post(() ->
                Toast.makeText(
                        getContext().getApplicationContext(),
                        ErrorMessageAdapter.getResultMessage(
                                result,
                                null,
                                getContext().getResources()
                        ),
                        Toast.LENGTH_SHORT
                ).show());
    }

}
