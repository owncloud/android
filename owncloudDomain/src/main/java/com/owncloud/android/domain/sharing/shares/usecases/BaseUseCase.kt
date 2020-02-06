/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.domain.sharing.shares.usecases

/**
 * Parent class for use cases that do not require network operations, e.g. get data from database. That's why error
 * handling is not needed as it is in [com.owncloud.android.domain.BaseUseCaseWithResult]
 */
abstract class BaseUseCase<out Type, in Params> {

    protected abstract fun run(params: Params): Type

    fun execute(params: Params): Type = run(params)
}
