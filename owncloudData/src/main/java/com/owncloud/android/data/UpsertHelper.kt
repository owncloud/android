/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2022 ownCloud GmbH.
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

package com.owncloud.android.data

/**
 * Adapted from https://github.com/android/nowinandroid/pull/311/files#diff-e57442ef4a5bca050805509aa8cebe7e2b1c2a34792b0f355a67492f5b91b1d3
 *
 * Performs an upsert by first attempting to insert [item] using [insert] with the the result
 * of the insert returned.
 *
 * If it was not inserted due to conflicts, it is updated using [update]
 */
fun <T> upsert(
    item: T,
    insert: (T) -> Long,
    update: (T) -> Unit,
) {
    val insertResult = insert(item)
    if (insertResult == -1L) update(item)
}
