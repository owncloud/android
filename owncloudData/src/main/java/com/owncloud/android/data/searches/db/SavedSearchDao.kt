package com.owncloud.android.data.searches.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.SAVED_SEARCHES_TABLE_NAME

@Dao
interface SavedSearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(savedSearchEntity: SavedSearchEntity): Long

    @Update
    fun update(savedSearchEntity: SavedSearchEntity)

    @Query(SELECT_SAVED_SEARCH_WITH_ID)
    fun getById(id: Long): SavedSearchEntity?

    @Query(SELECT_SAVED_SEARCHES_FOR_ACCOUNT)
    fun getForAccount(accountName: String): List<SavedSearchEntity>

    @Query(DELETE_SAVED_SEARCH_WITH_ID)
    fun deleteById(id: Long)

    @Query(DELETE_SAVED_SEARCHES_FOR_ACCOUNT)
    fun deleteForAccount(accountName: String)

    companion object {
        private const val SELECT_SAVED_SEARCH_WITH_ID = """
            SELECT *
            FROM $SAVED_SEARCHES_TABLE_NAME
            WHERE id = :id
        """

        private const val SELECT_SAVED_SEARCHES_FOR_ACCOUNT = """
            SELECT *
            FROM $SAVED_SEARCHES_TABLE_NAME
            WHERE accountName = :accountName
            ORDER BY createdAt DESC
        """

        private const val DELETE_SAVED_SEARCH_WITH_ID = """
            DELETE
            FROM $SAVED_SEARCHES_TABLE_NAME
            WHERE id = :id
        """

        private const val DELETE_SAVED_SEARCHES_FOR_ACCOUNT = """
            DELETE
            FROM $SAVED_SEARCHES_TABLE_NAME
            WHERE accountName = :accountName
        """
    }
}


