package com.owncloud.android.capabilities.repository

import androidx.lifecycle.LiveData
import com.owncloud.android.capabilities.db.OCCapability
import com.owncloud.android.vo.Resource

interface CapabilityRepository {
    fun getCapabilityForAccountAsLiveData(
        accountName: String,
        shouldFetchFromNetwork: Boolean = true
    ): LiveData<Resource<OCCapability>>

    fun getStoredCapabilityForAccount(
        accountName: String
    ): OCCapability
}
