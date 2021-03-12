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

package com.owncloud.android.presentation.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.owncloud.android.R
import com.owncloud.android.presentation.ui.settings.fragments.SettingsFragment
import com.owncloud.android.ui.activity.FileDisplayActivity

class SettingsActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.standard_toolbar).apply {
            setTitle(R.string.actionbar_settings)
            isVisible = true
        }
        findViewById<ConstraintLayout>(R.id.root_toolbar).apply {
            isVisible = false
        }
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.settings_container,
                SettingsFragment()
            )
            .commit()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (supportFragmentManager.findFragmentByTag("logs") != null) {
                    supportFragmentManager.popBackStack()
                    supportActionBar?.setTitle(R.string.actionbar_settings)
                } else {
                    intent = Intent(this, FileDisplayActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    startActivity(intent)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            pref.fragment)
        var tag: String? = null
        if (pref.key.equals(SettingsFragment.PREFERENCE_LOGS_SUBSECTION)) tag = "logs"
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_container, fragment, tag)
            .addToBackStack(null)
            .commit()
        supportActionBar?.setTitle(R.string.actionbar_logger)
        return true
    }
}
