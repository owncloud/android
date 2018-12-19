package com.owncloud.android.datasources

import android.arch.lifecycle.LiveData
import com.owncloud.android.db.shares.Share
import com.owncloud.android.db.shares.ShareType

interface SharesLocalDataSource {
    fun shares(): LiveData<List<Share>>
    fun sharesForAFile(filePath: String, accountName: String, shareTypes: List<ShareType>): LiveData<List<Share>>
}