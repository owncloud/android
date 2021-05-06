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
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_ACCOUNT_NAME
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_BEHAVIOUR
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_ENABLED
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_PATH
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_SOURCE
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_WIFI_ONLY
import com.owncloud.android.extensions.showAlertDialog
import com.owncloud.android.presentation.viewmodels.settings.SettingsPictureUploadsViewModel
import com.owncloud.android.ui.activity.LocalFolderPickerActivity
import com.owncloud.android.ui.activity.UploadPathActivity
import com.owncloud.android.utils.DisplayUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class SettingsPictureUploadsFragment : PreferenceFragmentCompat() {

    // ViewModel
    private val picturesViewModel by viewModel<SettingsPictureUploadsViewModel>()

    private var prefEnablePictureUploads: SwitchPreferenceCompat? = null
    private var prefPictureUploadsPath: Preference? = null
    private var prefPictureUploadsOnWifi: CheckBoxPreference? = null
    private var prefPictureUploadsSourcePath: Preference? = null
    private var prefPictureUploadsBehaviour: ListPreference? = null
    private var prefPictureUploadsAccount: ListPreference? = null

    private val selectPictureUploadsPathLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            picturesViewModel.handleSelectPictureUploadsPath(result.data)
            prefPictureUploadsPath?.summary =
                DisplayUtils.getPathWithoutLastSlash(picturesViewModel.getPictureUploadsPath())
        }

    private val selectPictureUploadsSourcePathLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            picturesViewModel.handleSelectPictureUploadsSourcePath(result.data)
            prefPictureUploadsSourcePath?.summary =
                DisplayUtils.getPathWithoutLastSlash(picturesViewModel.getPictureUploadsSourcePath())
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_picture_uploads, rootKey)

        prefEnablePictureUploads = findPreference(PREF__CAMERA_PICTURE_UPLOADS_ENABLED)
        prefPictureUploadsPath = findPreference(PREF__CAMERA_PICTURE_UPLOADS_PATH)
        prefPictureUploadsOnWifi = findPreference(PREF__CAMERA_PICTURE_UPLOADS_WIFI_ONLY)
        prefPictureUploadsSourcePath = findPreference(PREF__CAMERA_PICTURE_UPLOADS_SOURCE)
        prefPictureUploadsBehaviour = findPreference(PREF__CAMERA_PICTURE_UPLOADS_BEHAVIOUR)
        prefPictureUploadsAccount = findPreference<ListPreference>(PREF__CAMERA_PICTURE_UPLOADS_ACCOUNT_NAME)?.apply {
            entries = picturesViewModel.getAccountsNames()
            entryValues = picturesViewModel.getAccountsNames()
        }

        enablePictureUploads(picturesViewModel.isPictureUploadEnabled())

        prefPictureUploadsPath?.summary =
            DisplayUtils.getPathWithoutLastSlash(picturesViewModel.getPictureUploadsPath())
        prefPictureUploadsSourcePath?.summary =
            DisplayUtils.getPathWithoutLastSlash(picturesViewModel.getPictureUploadsSourcePath())
        val comment =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) getString(
                R.string.prefs_camera_upload_source_path_title_optional
            )
            else getString(
                R.string.prefs_camera_upload_source_path_title_required
            )
        prefPictureUploadsSourcePath?.title = String.format(prefPictureUploadsSourcePath?.title.toString(), comment)

        prefEnablePictureUploads?.setOnPreferenceChangeListener { preference: Preference?, newValue: Any ->
            val value = newValue as Boolean

            if (value) {
                picturesViewModel.setEnablePictureUpload(value)
                enablePictureUploads(value)
                prefPictureUploadsAccount?.value = picturesViewModel.getPictureUploadsAccount()
                showAlertDialog(
                    title = getString(R.string.common_important),
                    message = getString(R.string.proper_pics_folder_warning_camera_upload)
                )
                true
            } else {
                showAlertDialog(
                    title = getString(R.string.confirmation_disable_camera_uploads_title),
                    message = getString(R.string.confirmation_disable_pictures_upload_message),
                    positiveButtonText = getString(R.string.common_yes),
                    positiveButtonListener = { dialog: DialogInterface?, which: Int ->
                        picturesViewModel.updatePicturesLastSync()
                        picturesViewModel.setEnablePictureUpload(value)
                        prefEnablePictureUploads?.isChecked = false
                        enablePictureUploads(false)
                        resetPreferencesAfterDisablingPicturesUploads()
                    },
                    negativeButtonText = getString(R.string.common_no)
                )
                false
            }
        }

        prefPictureUploadsPath?.setOnPreferenceClickListener {
            var uploadPath = picturesViewModel.getPictureUploadsPath()
            if (uploadPath?.endsWith(File.separator) == false) {
                uploadPath += File.separator
            }
            val intent = Intent(activity, UploadPathActivity::class.java)
            intent.putExtra(UploadPathActivity.KEY_CAMERA_UPLOAD_PATH, uploadPath)
            intent.putExtra(UploadPathActivity.KEY_CAMERA_UPLOAD_ACCOUNT, picturesViewModel.getPictureUploadsAccount())
            selectPictureUploadsPathLauncher.launch(intent)
            true
        }

        prefPictureUploadsSourcePath?.setOnPreferenceClickListener {
            var sourcePath = picturesViewModel.getPictureUploadsSourcePath()
            if (sourcePath?.endsWith(File.separator) == false) {
                sourcePath += File.separator
            }
            val intent = Intent(activity, LocalFolderPickerActivity::class.java)
            intent.putExtra(LocalFolderPickerActivity.EXTRA_PATH, sourcePath)
            selectPictureUploadsSourcePathLauncher.launch(intent)
            true
        }
    }

    override fun onStop() {
        picturesViewModel.schedulePictureUploadsSyncJob()
        super.onStop()
    }

    private fun enablePictureUploads(value: Boolean) {
        prefPictureUploadsPath?.isEnabled = value
        prefPictureUploadsOnWifi?.isEnabled = value
        prefPictureUploadsSourcePath?.isEnabled = value
        prefPictureUploadsBehaviour?.isEnabled = value
        prefPictureUploadsAccount?.isEnabled = value
    }

    private fun resetPreferencesAfterDisablingPicturesUploads() {
        prefPictureUploadsAccount?.value = null
        prefPictureUploadsPath?.summary = DisplayUtils.getPathWithoutLastSlash(picturesViewModel.getPictureUploadsPath())
    }
}
