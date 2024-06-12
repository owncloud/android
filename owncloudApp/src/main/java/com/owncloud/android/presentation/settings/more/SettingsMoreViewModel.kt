/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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

package com.owncloud.android.presentation.settings.more

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

    fun isDocProviderAppEnabled() = contextProvider.getBoolean(R.bool.access_document_provider_app_enabled)

    fun getDocProviderAppUrl() = contextProvider.getString(R.string.url_document_provider_app)

    fun isRecommendEnabled() = contextProvider.getBoolean(R.bool.recommend_enabled)

    fun isFeedbackEnabled() = contextProvider.getBoolean(R.bool.feedback_enabled)

    fun getFeedbackMail() = contextProvider.getString(R.string.mail_feedback)

    fun isPrivacyPolicyEnabled() = contextProvider.getBoolean(R.bool.privacy_policy_enabled)

    fun isImprintEnabled() = contextProvider.getBoolean(R.bool.imprint_enabled)

    fun getImprintUrl() = contextProvider.getString(R.string.url_imprint)

    fun shouldMoreSectionBeVisible() =
        isHelpEnabled() ||
                isSyncEnabled() ||
                isDocProviderAppEnabled() ||
                isRecommendEnabled() ||
                isFeedbackEnabled() ||
                isImprintEnabled()
}
