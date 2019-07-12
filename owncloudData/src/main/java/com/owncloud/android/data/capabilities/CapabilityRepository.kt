package com.owncloud.android.data.capabilities

import androidx.lifecycle.LiveData
import com.owncloud.android.data.capabilities.db.OCCapabilityEntity
import com.owncloud.android.data.DataResult

interface CapabilityRepository {
    fun getCapabilityForAccount(
        accountName: String,
        shouldFetchFromNetwork: Boolean = true
    ): LiveData<DataResult<OCCapabilityEntity>>
}
