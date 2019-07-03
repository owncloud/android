package com.owncloud.android.data.capabilities

import androidx.lifecycle.LiveData
import com.owncloud.android.data.capabilities.db.OCCapabilityEntity
import com.owncloud.android.data.Resource

interface CapabilityRepository {
    fun getCapabilityForAccount(
        accountName: String,
        shouldFetchFromNetwork: Boolean = true
    ): LiveData<Resource<OCCapabilityEntity>>

    fun getStoredCapabilityForAccount(
        accountName: String
    ): OCCapabilityEntity
}
