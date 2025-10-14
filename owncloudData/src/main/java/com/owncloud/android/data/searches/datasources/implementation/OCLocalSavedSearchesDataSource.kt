package com.owncloud.android.data.searches.datasources.implementation

import com.owncloud.android.data.searches.datasources.LocalSavedSearchesDataSource
import com.owncloud.android.data.searches.db.SavedSearchDao
import com.owncloud.android.data.searches.db.SavedSearchEntity
import com.owncloud.android.domain.searches.model.SavedSearch

class OCLocalSavedSearchesDataSource(
    private val savedSearchDao: SavedSearchDao
) : LocalSavedSearchesDataSource {

    override fun save(savedSearch: SavedSearch): SavedSearch {
        val entity = savedSearch.toEntity()
        val id = savedSearchDao.upsert(entity)
        return savedSearch.copy(id = if (savedSearch.id != null && savedSearch.id != 0L) savedSearch.id else id)
    }

    override fun removeById(id: Long) {
        savedSearchDao.deleteById(id)
    }

    override fun getForAccount(accountName: String): List<SavedSearch> =
        savedSearchDao.getForAccount(accountName).map { it.toModel() }

    override fun clearForAccount(accountName: String) {
        savedSearchDao.deleteForAccount(accountName)
    }

    private fun SavedSearch.toEntity(): SavedSearchEntity =
        SavedSearchEntity(
            accountName = accountName,
            name = name,
            searchPattern = searchPattern,
            ignoreCase = ignoreCase,
            minSize = minSize,
            maxSize = maxSize,
            mimePrefix = mimePrefix,
            minDate = minDate,
            maxDate = maxDate,
            createdAt = createdAt,
        ).also { if (id != null) it.id = id!! }

    private fun SavedSearchEntity.toModel(): SavedSearch =
        SavedSearch(
            id = id,
            accountName = accountName,
            name = name,
            searchPattern = searchPattern,
            ignoreCase = ignoreCase,
            minSize = minSize,
            maxSize = maxSize,
            mimePrefix = mimePrefix,
            minDate = minDate,
            maxDate = maxDate,
            createdAt = createdAt,
        )
}
