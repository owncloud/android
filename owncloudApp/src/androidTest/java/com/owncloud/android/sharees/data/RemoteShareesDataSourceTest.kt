package com.owncloud.android.sharees.data

import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.GetRemoteShareesOperation
import com.owncloud.android.sharees.data.datasources.RemoteShareesDataSource
import org.json.JSONObject

class RemoteShareesDataSourceTest(private val remoteOperationResult: RemoteOperationResult<ArrayList<JSONObject>>) :
    RemoteShareesDataSource {
    override fun getSharees(
        searchString: String,
        page: Int,
        perPage: Int,
        getRemoteShareesOperation: GetRemoteShareesOperation
    ): RemoteOperationResult<ArrayList<JSONObject>> {
        return remoteOperationResult
    }
}
