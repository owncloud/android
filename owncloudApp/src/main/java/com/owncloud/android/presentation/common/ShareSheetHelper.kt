/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.presentation.common

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Parcelable
import androidx.annotation.StringRes

class ShareSheetHelper {

    fun getShareSheetIntent(
        intent: Intent,
        context: Context,
        @StringRes title: Int,
        packagesToExclude: Array<String>
    ): Intent {

        // Get excluding specific targets by component. We want to hide oC targets.
        val resInfo: List<ResolveInfo> =
            context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val excludeLists = ArrayList<ComponentName>()
        if (resInfo.isNotEmpty()) {
            for (info in resInfo) {
                val activityInfo = info.activityInfo
                for (packageToExclude in packagesToExclude) {
                    if (activityInfo != null && activityInfo.packageName == packageToExclude) {
                        excludeLists.add(ComponentName(activityInfo.packageName, activityInfo.name))
                    }
                }
            }
        }

        // Return a new ShareSheet intent
        return Intent.createChooser(intent, "").apply {
            putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, excludeLists.toArray(arrayOf<Parcelable>()))
            putExtra(Intent.EXTRA_TITLE, context.getString(title))
        }
    }
}
