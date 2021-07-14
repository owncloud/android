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
import android.provider.DocumentsContract
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.owncloud.android.R
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_VIDEO_UPLOADS_ACCOUNT_NAME
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_VIDEO_UPLOADS_BEHAVIOUR
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_VIDEO_UPLOADS_ENABLED
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_VIDEO_UPLOADS_PATH
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_VIDEO_UPLOADS_SOURCE
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_VIDEO_UPLOADS_WIFI_ONLY
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration
import com.owncloud.android.extensions.showAlertDialog
import com.owncloud.android.presentation.viewmodels.settings.SettingsVideoUploadsViewModel
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
    private var prefVideoUploadsAccount: ListPreference? = null
    private var prefVideoUploadsLastSync: Preference? = null

    private val selectVideoUploadsPathLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            videosViewModel.handleSelectVideoUploadsPath(result.data)
        }

    private val selectVideoUploadsSourcePathLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            // here we ask the content resolver to persist the permission for us
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val contentUriForTree = result.data!!.data!!

            requireContext().contentResolver.takePersistableUriPermission(contentUriForTree, takeFlags)
            videosViewModel.handleSelectVideoUploadsSourcePath(contentUriForTree)
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_video_uploads, rootKey)

        prefEnableVideoUploads = findPreference(PREF__CAMERA_VIDEO_UPLOADS_ENABLED)
        prefVideoUploadsPath = findPreference(PREF__CAMERA_VIDEO_UPLOADS_PATH)
        prefVideoUploadsOnWifi = findPreference(PREF__CAMERA_VIDEO_UPLOADS_WIFI_ONLY)
        prefVideoUploadsSourcePath = findPreference(PREF__CAMERA_VIDEO_UPLOADS_SOURCE)
        prefVideoUploadsLastSync = findPreference(PreferenceManager.PREF__CAMERA_VIDEO_UPLOADS_LAST_SYNC)
        prefVideoUploadsBehaviour = findPreference<ListPreference>(PREF__CAMERA_VIDEO_UPLOADS_BEHAVIOUR)?.apply {
            entries = listOf(getString(R.string.pref_behaviour_entries_keep_file), getString(R.string.pref_behaviour_entries_move)).toTypedArray()
            entryValues = listOf(FolderBackUpConfiguration.Behavior.COPY.name, FolderBackUpConfiguration.Behavior.MOVE.name).toTypedArray()
        }
        prefVideoUploadsAccount = findPreference<ListPreference>(PREF__CAMERA_VIDEO_UPLOADS_ACCOUNT_NAME)?.apply {
            entries = videosViewModel.getLoggedAccountNames()
            entryValues = videosViewModel.getLoggedAccountNames()
        }

        val comment = getString(R.string.prefs_camera_upload_source_path_title_required)
        prefVideoUploadsSourcePath?.title = String.format(prefVideoUploadsSourcePath?.title.toString(), comment)

        initPreferenceListeners()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initLiveDataObservers()
    }

    private fun initLiveDataObservers() {
        videosViewModel.videoUploads.observe(viewLifecycleOwner, { videoUploadsConfiguration ->
            enableVideoUploads(videoUploadsConfiguration != null)
            videoUploadsConfiguration?.let {
                prefVideoUploadsAccount?.value = it.accountName
                prefVideoUploadsPath?.summary = DisplayUtils.getPathWithoutLastSlash(it.uploadPath)
                prefVideoUploadsSourcePath?.summary = DisplayUtils.getPathWithoutLastSlash(it.sourcePath.toUri().path)
                prefVideoUploadsOnWifi?.isChecked = it.wifiOnly
                prefVideoUploadsBehaviour?.value = it.behavior.name
                prefVideoUploadsLastSync?.summary = DisplayUtils.unixTimeToHumanReadable(it.lastSyncTimestamp)
            } ?: resetFields()
        })
    }

    private fun initPreferenceListeners() {
        prefEnableVideoUploads?.setOnPreferenceChangeListener { _: Preference?, newValue: Any ->
            val value = newValue as Boolean

            if (value) {
                videosViewModel.enableVideoUploads()
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
                    positiveButtonListener = { _: DialogInterface?, _: Int ->
                        videosViewModel.disableVideoUploads()
                    },
                    negativeButtonText = getString(R.string.common_no)
                )
                false
            }
        }

        prefVideoUploadsPath?.setOnPreferenceClickListener {
            var uploadPath = videosViewModel.getVideoUploadsPath()
            if (!uploadPath.endsWith(File.separator)) {
                uploadPath += File.separator
            }
            val intent = Intent(activity, UploadPathActivity::class.java).apply {
                putExtra(UploadPathActivity.KEY_CAMERA_UPLOAD_PATH, uploadPath)
                putExtra(UploadPathActivity.KEY_CAMERA_UPLOAD_ACCOUNT, videosViewModel.getVideoUploadsAccount())
            }
            selectVideoUploadsPathLauncher.launch(intent)
            true
        }

        prefVideoUploadsSourcePath?.setOnPreferenceClickListener {
            var sourcePath = videosViewModel.getVideoUploadsSourcePath()
                .takeUnless { it.isNullOrBlank() } ?: videosViewModel.getDefaultCameraSourcePath()
            if (!sourcePath.endsWith(File.separator)) {
                sourcePath += File.separator
            }
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, sourcePath)
                }
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                            or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                )
            }
            selectVideoUploadsSourcePathLauncher.launch(intent)
            true
        }

        prefVideoUploadsOnWifi?.setOnPreferenceChangeListener { _, newValue ->
            newValue as Boolean
            videosViewModel.useWifiOnly(newValue)
            newValue
        }

        prefVideoUploadsAccount?.setOnPreferenceChangeListener { _, newValue ->
            newValue as String
            videosViewModel.handleSelectAccount(newValue)
            true
        }

        prefVideoUploadsBehaviour?.setOnPreferenceChangeListener { _, newValue ->
            newValue as String
            videosViewModel.handleSelectBehaviour(newValue)
            true
        }
    }

    override fun onDestroy() {
        videosViewModel.scheduleVideoUploads()
        super.onDestroy()
    }

    private fun enableVideoUploads(value: Boolean) {
        prefEnableVideoUploads?.isChecked = value
        prefVideoUploadsPath?.isEnabled = value
        prefVideoUploadsOnWifi?.isEnabled = value
        prefVideoUploadsSourcePath?.isEnabled = value
        prefVideoUploadsBehaviour?.isEnabled = value
        prefVideoUploadsAccount?.isEnabled = value
        prefVideoUploadsLastSync?.isEnabled = value
    }

    private fun resetFields() {
        prefVideoUploadsAccount?.value = null
        prefVideoUploadsPath?.summary = null
        prefVideoUploadsSourcePath?.summary = null
        prefVideoUploadsOnWifi?.isChecked = false
        prefVideoUploadsBehaviour?.value = FolderBackUpConfiguration.Behavior.COPY.name
        prefVideoUploadsLastSync?.summary = null
    }
}
