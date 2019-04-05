package com.owncloud.android.capabilities.repository

import androidx.lifecycle.LiveData
import com.owncloud.android.capabilities.db.OCCapability
import com.owncloud.android.vo.Resource

interface CapabilityRepository {
    fun loadCapabilityForAccount(
        accountName: String
    ): LiveData<Resource<OCCapability>>
}
