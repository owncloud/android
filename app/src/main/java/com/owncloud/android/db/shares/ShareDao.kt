package com.owncloud.android.db.shares

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import com.owncloud.android.datasources.SharesLocalDataSource

@Dao
interface ShareDao: SharesLocalDataSource {
    @Query("SELECT * from shares_table ORDER BY id")
    override fun shares(): LiveData<List<Share>>

    @Query("SELECT * from shares_table " +
            "WHERE path = :filePath " +
            "AND accountOwner = :accountName AND type IN(:shareTypes)"
    )
    override fun sharesForAFile(
        filePath: String, accountName: String, shareTypes: List<ShareType>
    ): LiveData<List<Share>>
}