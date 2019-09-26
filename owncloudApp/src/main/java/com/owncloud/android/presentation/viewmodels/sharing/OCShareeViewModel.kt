/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.presentation.viewmodels.sharing

import android.accounts.Account
import android.content.Context
import androidx.lifecycle.ViewModel
import com.owncloud.android.domain.sharing.sharees.GetShareesAsyncUseCase
import com.owncloud.android.presentation.UIResult
import org.json.JSONObject

class OCShareeViewModel(
    val context: Context,
    account: Account,
    private val getShareesAsyncUseCase: GetShareesAsyncUseCase = GetShareesAsyncUseCase(
        context,
        account
    )
) : ViewModel() {

    suspend fun getSharees(searchString: String, page: Int, perPage: Int): UIResult<ArrayList<JSONObject>> {
        val useCaseResult = getShareesAsyncUseCase.execute(
            GetShareesAsyncUseCase.Params(
                searchString,
                page,
                perPage
            )
        )

        return if (useCaseResult.isSuccess)
            UIResult.Success(useCaseResult.getDataOrNull()) else
            UIResult.Error(
                useCaseResult.getThrowableOrNull()
            )
    }
}
