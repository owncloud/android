/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2021 ownCloud GmbH.
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

package com.owncloud.android.presentation.viewmodels.settings

import androidx.lifecycle.ViewModel
import com.owncloud.android.R
import com.owncloud.android.providers.ContextProvider

class SettingsMoreViewModel(
    private val contextProvider: ContextProvider
) : ViewModel() {

    fun isHelpEnabled() = contextProvider.getBoolean(R.bool.help_enabled)

    fun getHelpUrl() = contextProvider.getString(R.string.url_help)

    fun isSyncEnabled() = contextProvider.getBoolean(R.bool.sync_calendar_contacts_enabled)

    fun getSyncUrl() = contextProvider.getString(R.string.url_sync_calendar_contacts)

    fun isRecommendEnabled() = contextProvider.getBoolean(R.bool.recommend_enabled)

    fun isFeedbackEnabled() = contextProvider.getBoolean(R.bool.feedback_enabled)

    fun isPrivacyPolicyEnabled() = contextProvider.getBoolean(R.bool.privacy_policy_enabled)

    fun isImprintEnabled() = contextProvider.getBoolean(R.bool.imprint_enabled)

    fun getImprintUrl() = contextProvider.getString(R.string.url_imprint)

    fun shouldMoreSectionBeVisible() =
        isHelpEnabled() ||
                isSyncEnabled() ||
                isRecommendEnabled() ||
                isFeedbackEnabled() ||
                isImprintEnabled() ||
                isPrivacyPolicyEnabled()
}
