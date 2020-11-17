/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 *
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
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package com.owncloud.android.providers.impl

import android.content.Context
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.utils.ConnectivityUtils

class OCContextProvider(private val context: Context) : ContextProvider {

    override fun getBoolean(id: Int): Boolean = context.resources.getBoolean(id)

    override fun getString(id: Int): String = context.resources.getString(id)

    override fun getContext(): Context = context

    override fun isConnected(): Boolean {
        return ConnectivityUtils.isAppConnected(context)
    }
}
