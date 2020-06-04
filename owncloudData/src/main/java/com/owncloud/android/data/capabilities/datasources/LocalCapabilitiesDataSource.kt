/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.data.capabilities.datasources

import androidx.lifecycle.LiveData
import com.owncloud.android.domain.capabilities.model.OCCapability

interface LocalCapabilitiesDataSource {
    fun getCapabilitiesForAccountAsLiveData(
        accountName: String
    ): LiveData<OCCapability?>

    fun getCapabilityForAccount(
        accountName: String
    ): OCCapability?

    fun insert(ocCapabilities: List<OCCapability>)
}
