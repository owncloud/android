package com.owncloud.android.domain.searches

import com.owncloud.android.domain.searches.model.SavedSearch

interface SavedSearchesRepository {
    fun save(savedSearch: SavedSearch): SavedSearch
    fun removeById(id: Long)
    fun getForAccount(accountName: String): List<SavedSearch>
    fun clearForAccount(accountName: String)
}
