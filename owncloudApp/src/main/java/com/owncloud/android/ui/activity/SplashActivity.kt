/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 *
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

package com.owncloud.android.ui.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.owncloud.android.BuildConfig
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.providers.MdmProvider
import com.owncloud.android.utils.CONFIGURATION_SERVER_URL
import com.owncloud.android.utils.CONFIGURATION_SERVER_URL_INPUT_VISIBILITY

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.FLAVOR == MainApp.MDM_FLAVOR) {
            val mdmProvider = MdmProvider(this)
            mdmProvider.cacheStringRestriction(CONFIGURATION_SERVER_URL, R.string.server_url_configuration_feedback_ok)
            mdmProvider.cacheBooleanRestriction(CONFIGURATION_SERVER_URL_INPUT_VISIBILITY, R.string.server_url_input_visibility_configuration_feedback_ok)
        }

        startActivity(Intent(this, FileDisplayActivity::class.java))
        finish()
    }

}
