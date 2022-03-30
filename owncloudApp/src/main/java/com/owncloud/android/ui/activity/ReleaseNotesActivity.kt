/**
 * ownCloud Android client application
 *
 * @author David Crespo Ríos
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

package com.owncloud.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.owncloud.android.BuildConfig
import com.owncloud.android.MainApp
import com.owncloud.android.MainApp.Companion.versionCode
import com.owncloud.android.R
import com.owncloud.android.databinding.ReleaseNotesActivityBinding
import com.owncloud.android.features.ReleaseNotesList
import com.owncloud.android.presentation.ui.authentication.LoginActivity
import com.owncloud.android.presentation.ui.security.PassCodeActivity
import com.owncloud.android.ui.adapter.ReleaseNotesAdapter
import com.owncloud.android.ui.viewmodels.ReleaseNotesViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReleaseNotesActivity : AppCompatActivity() {

    // ViewModel
    private val releaseNotesViewModel by viewModel<ReleaseNotesViewModel>()

    private var _binding: ReleaseNotesActivityBinding? = null
    val binding get() = _binding!!

    private val releaseNotesAdapter = ReleaseNotesAdapter()
    private val KEY_LAST_SEEN_VERSION_CODE = "lastSeenVersionCode"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ReleaseNotesActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setData()
        initView()
        updateVersionCode()
    }

    private fun initView() {
        binding.releaseNotes.let {
            it.layoutManager = LinearLayoutManager(this)
            it.adapter = releaseNotesAdapter
        }

        binding.btnProceed.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
    }

    fun runIfNeeded(context: Context) {
        if (context is ReleaseNotesActivity) {
            return
        }
        if (shouldShow(context)) {
            println("show $context")
            context.startActivity(Intent(context, ReleaseNotesActivity::class.java))
        }
    }

    private fun shouldShow(context: Context): Boolean {
        val showReleaseNotes = context.resources.getBoolean(R.bool.release_notes_enabled) //&& !BuildConfig.DEBUG

        return showReleaseNotes && ReleaseNotesList().getReleaseNotes().isNotEmpty() &&
                (firstRunAfterUpdate() && context is LoginActivity ||
                        (((firstRunAfterUpdate() && context is FileDisplayActivity) &&
                                context !is PassCodeActivity)))
    }

    private fun setData() {
        releaseNotesAdapter.setData(releaseNotesViewModel.getReleaseNotes())

        val header = String.format(
            getString(R.string.release_notes_header),
            getString(R.string.app_name)
        )

        val footer = String.format(
            getString(R.string.release_notes_footer),
            getString(R.string.app_name)
        )

        binding.txtHeader.text = header
        binding.txtFooter.text = footer
    }

    private fun updateVersionCode() {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = pref.edit()
        editor.putInt(KEY_LAST_SEEN_VERSION_CODE, versionCode)
        editor.apply()
    }

    private fun firstRunAfterUpdate(): Boolean {
        return getLastSeenVersionCode() != versionCode
    }

    private fun getLastSeenVersionCode(): Int {
        val pref = com.owncloud.android.db.PreferenceManager.getDefaultSharedPreferences(MainApp.appContext)
        return pref.getInt(KEY_LAST_SEEN_VERSION_CODE, 0)
    }
}