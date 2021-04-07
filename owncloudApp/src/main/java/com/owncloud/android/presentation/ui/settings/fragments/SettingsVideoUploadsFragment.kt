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

package com.owncloud.android.presentation.ui.settings.fragments

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.owncloud.android.R
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_VIDEO_UPLOADS_BEHAVIOUR
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_VIDEO_UPLOADS_ENABLED
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_VIDEO_UPLOADS_PATH
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_VIDEO_UPLOADS_SOURCE
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_VIDEO_UPLOADS_WIFI_ONLY
import com.owncloud.android.extensions.showAlertDialog
import com.owncloud.android.presentation.viewmodels.settings.SettingsVideoUploadsViewModel
import com.owncloud.android.ui.activity.LocalFolderPickerActivity
import com.owncloud.android.ui.activity.UploadPathActivity
import com.owncloud.android.utils.DisplayUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class SettingsVideoUploadsFragment : PreferenceFragmentCompat() {

    // ViewModel
    private val videosViewModel by viewModel<SettingsVideoUploadsViewModel>()

    private var prefEnableVideoUploads: SwitchPreferenceCompat? = null
    private var prefVideoUploadsPath: Preference? = null
    private var prefVideoUploadsOnWifi: CheckBoxPreference? = null
    private var prefVideoUploadsSourcePath: Preference? = null
    private var prefVideoUploadsBehaviour: ListPreference? = null

    private val selectVideoUploadsPathLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            videosViewModel.handleSelectVideoUploadsPath(result.data)
            prefVideoUploadsPath?.summary =
                DisplayUtils.getPathWithoutLastSlash(videosViewModel.getVideoUploadsPath())
        }

    private val selectVideoUploadsSourcePathLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            videosViewModel.handleSelectVideoUploadsSourcePath(result.data)
            prefVideoUploadsSourcePath?.summary =
                DisplayUtils.getPathWithoutLastSlash(videosViewModel.getVideoUploadsSourcePath())
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_video_uploads, rootKey)

        prefEnableVideoUploads = findPreference(PREF__CAMERA_VIDEO_UPLOADS_ENABLED)
        prefVideoUploadsPath = findPreference(PREF__CAMERA_VIDEO_UPLOADS_PATH)
        prefVideoUploadsOnWifi = findPreference(PREF__CAMERA_VIDEO_UPLOADS_WIFI_ONLY)
        prefVideoUploadsSourcePath = findPreference(PREF__CAMERA_VIDEO_UPLOADS_SOURCE)
        prefVideoUploadsBehaviour = findPreference(PREF__CAMERA_VIDEO_UPLOADS_BEHAVIOUR)

        with(videosViewModel.isVideoUploadEnabled()) {
            enableSettings(this)
        }

        videosViewModel.loadVideoUploadsPath()
        prefVideoUploadsPath?.summary =
            DisplayUtils.getPathWithoutLastSlash(videosViewModel.getVideoUploadsPath())
        videosViewModel.loadVideoUploadsSourcePath()
        prefVideoUploadsSourcePath?.summary =
            DisplayUtils.getPathWithoutLastSlash(videosViewModel.getVideoUploadsSourcePath())
        val comment =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) getString(
                R.string.prefs_camera_upload_source_path_title_optional
            )
            else getString(
                R.string.prefs_camera_upload_source_path_title_required
            )
        prefVideoUploadsSourcePath?.title = String.format(prefVideoUploadsSourcePath?.title.toString(), comment)

        prefEnableVideoUploads?.setOnPreferenceChangeListener { preference: Preference?, newValue: Any ->
            val value = newValue as Boolean

            if (value) {
                videosViewModel.setEnableVideoUpload(value)
                enableSettings(value)
                showAlertDialog(
                    title = getString(R.string.common_important),
                    message = getString(R.string.proper_videos_folder_warning_camera_upload)
                )
                true
            } else {
                showAlertDialog(
                    title = getString(R.string.confirmation_disable_camera_uploads_title),
                    message = getString(R.string.confirmation_disable_videos_upload_message),
                    positiveButtonText = getString(R.string.common_yes),
                    positiveButtonListener = { dialog: DialogInterface?, which: Int ->
                        videosViewModel.updateVideosLastSync()
                        videosViewModel.setEnableVideoUpload(value)
                        prefEnableVideoUploads?.isChecked = false
                        enableSettings(false)
                    },
                    negativeButtonText = getString(R.string.common_no)
                )
                false
            }
        }

        prefVideoUploadsPath?.setOnPreferenceClickListener {
            var uploadPath = videosViewModel.getVideoUploadsPath()
            if (uploadPath?.endsWith(File.separator) == false) {
                uploadPath += File.separator
            }
            val intent = Intent(activity, UploadPathActivity::class.java)
            intent.putExtra(UploadPathActivity.KEY_CAMERA_UPLOAD_PATH, uploadPath)
            selectVideoUploadsPathLauncher.launch(intent)
            true
        }

        prefVideoUploadsSourcePath?.setOnPreferenceClickListener {
            var sourcePath = videosViewModel.getVideoUploadsSourcePath()
            if (sourcePath?.endsWith(File.separator) == false) {
                sourcePath += File.separator
            }
            val intent = Intent(activity, LocalFolderPickerActivity::class.java)
            intent.putExtra(LocalFolderPickerActivity.EXTRA_PATH, sourcePath)
            selectVideoUploadsSourcePathLauncher.launch(intent)
            true
        }
    }

    override fun onStop() {
        videosViewModel.scheduleVideoUploadsSyncJob()
        super.onStop()
    }

    private fun enableSettings(value: Boolean) {
        prefVideoUploadsPath?.isEnabled = value
        prefVideoUploadsOnWifi?.isEnabled = value
        prefVideoUploadsSourcePath?.isEnabled = value
        prefVideoUploadsBehaviour?.isEnabled = value
    }
}
