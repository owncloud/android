package com.owncloud.android.data.searches.repository

import com.owncloud.android.data.searches.datasources.LocalSavedSearchesDataSource
import com.owncloud.android.domain.searches.SavedSearchesRepository
import com.owncloud.android.domain.searches.model.SavedSearch

class OCSavedSearchesRepository(
    private val localDataSource: LocalSavedSearchesDataSource
) : SavedSearchesRepository {

    override fun save(savedSearch: SavedSearch): SavedSearch =
        localDataSource.save(savedSearch)

    override fun removeById(id: Long) =
        localDataSource.removeById(id)

    override fun getForAccount(accountName: String): List<SavedSearch> =
        localDataSource.getForAccount(accountName)

    override fun clearForAccount(accountName: String) =
        localDataSource.clearForAccount(accountName)
}
