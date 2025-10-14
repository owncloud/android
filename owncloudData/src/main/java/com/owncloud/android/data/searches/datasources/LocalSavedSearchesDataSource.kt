package com.owncloud.android.data.searches.datasources

import com.owncloud.android.domain.searches.model.SavedSearch

interface LocalSavedSearchesDataSource {
    fun save(savedSearch: SavedSearch): SavedSearch
    fun removeById(id: Long)
    fun getForAccount(accountName: String): List<SavedSearch>
    fun clearForAccount(accountName: String)
}
