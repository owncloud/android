/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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

package com.owncloud.android.presentation.settings.autouploads

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.owncloud.android.R
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_ACCOUNT_NAME
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_BEHAVIOUR
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_CHARGING_ONLY
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_ENABLED
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_LAST_SYNC
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_PATH
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_SOURCE
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_WIFI_ONLY
import com.owncloud.android.domain.camerauploads.model.UploadBehavior
import com.owncloud.android.extensions.showAlertDialog
import com.owncloud.android.ui.activity.FolderPickerActivity
import com.owncloud.android.utils.DisplayUtils
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class SettingsPictureUploadsFragment : PreferenceFragmentCompat() {

    // ViewModel
    private val picturesViewModel by viewModel<SettingsPictureUploadsViewModel>()

    private var prefEnablePictureUploads: SwitchPreferenceCompat? = null
    private var prefPictureUploadsPath: Preference? = null
    private var prefPictureUploadsOnWifi: CheckBoxPreference? = null
    private var prefPictureUploadsOnCharging: CheckBoxPreference? = null
    private var prefPictureUploadsSourcePath: Preference? = null
    private var prefPictureUploadsBehaviour: ListPreference? = null
    private var prefPictureUploadsAccount: ListPreference? = null
    private var prefPictureUploadsLastSync: Preference? = null
    private var spaceId: String? = null

    private val selectPictureUploadsPathLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            picturesViewModel.handleSelectPictureUploadsPath(result.data)
        }

    private val selectPictureUploadsSourcePathLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            // here we ask the content resolver to persist the permission for us
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val contentUriForTree = result.data!!.data!!

            requireContext().contentResolver.takePersistableUriPermission(contentUriForTree, takeFlags)
            picturesViewModel.handleSelectPictureUploadsSourcePath(contentUriForTree)
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_picture_uploads, rootKey)

        prefEnablePictureUploads = findPreference(PREF__CAMERA_PICTURE_UPLOADS_ENABLED)
        prefPictureUploadsPath = findPreference(PREF__CAMERA_PICTURE_UPLOADS_PATH)
        prefPictureUploadsOnWifi = findPreference(PREF__CAMERA_PICTURE_UPLOADS_WIFI_ONLY)
        prefPictureUploadsOnCharging = findPreference(PREF__CAMERA_PICTURE_UPLOADS_CHARGING_ONLY)
        prefPictureUploadsSourcePath = findPreference(PREF__CAMERA_PICTURE_UPLOADS_SOURCE)
        prefPictureUploadsLastSync = findPreference(PREF__CAMERA_PICTURE_UPLOADS_LAST_SYNC)
        prefPictureUploadsBehaviour = findPreference<ListPreference>(PREF__CAMERA_PICTURE_UPLOADS_BEHAVIOUR)?.apply {
            entries = listOf(
                getString(R.string.pref_behaviour_entries_keep_file),
                getString(R.string.pref_behaviour_entries_remove_original_file)
            ).toTypedArray()
            entryValues = listOf(UploadBehavior.COPY.name, UploadBehavior.MOVE.name).toTypedArray()
        }
        prefPictureUploadsAccount = findPreference<ListPreference>(PREF__CAMERA_PICTURE_UPLOADS_ACCOUNT_NAME)?.apply {
            entries = picturesViewModel.getLoggedAccountNames()
            entryValues = picturesViewModel.getLoggedAccountNames()
        }

        val comment = getString(R.string.prefs_camera_upload_source_path_title_required)
        prefPictureUploadsSourcePath?.title = String.format(prefPictureUploadsSourcePath?.title.toString(), comment)

        initPreferenceListeners()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initStateObservers()
    }

    private fun initStateObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                picturesViewModel.pictureUploads.collect { pictureUploadsConfiguration ->
                    enablePictureUploads(pictureUploadsConfiguration != null)
                    pictureUploadsConfiguration?.let {
                        prefPictureUploadsAccount?.value = it.accountName
                        prefPictureUploadsPath?.summary = picturesViewModel.getUploadPathString()
                        prefPictureUploadsSourcePath?.summary = DisplayUtils.getPathWithoutLastSlash(it.sourcePath.toUri().path)
                        prefPictureUploadsOnWifi?.isChecked = it.wifiOnly
                        prefPictureUploadsOnCharging?.isChecked = it.chargingOnly
                        prefPictureUploadsBehaviour?.value = it.behavior.name
                        prefPictureUploadsLastSync?.summary = DisplayUtils.unixTimeToHumanReadable(it.lastSyncTimestamp)
                        spaceId = it.spaceId
                    } ?: resetFields()
                }
            }
        }
    }

    private fun initPreferenceListeners() {
        prefEnablePictureUploads?.setOnPreferenceChangeListener { _: Preference?, newValue: Any ->
            val value = newValue as Boolean

            if (value) {
                picturesViewModel.enablePictureUploads()
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
                    positiveButtonListener = { _: DialogInterface?, _: Int ->
                        picturesViewModel.disablePictureUploads()
                    },
                    negativeButtonText = getString(R.string.common_no)
                )
                false
            }
        }

        prefPictureUploadsPath?.setOnPreferenceClickListener {
            var uploadPath = picturesViewModel.getPictureUploadsPath()
            if (!uploadPath.endsWith(File.separator)) {
                uploadPath += File.separator
            }
            val intent = Intent(activity, FolderPickerActivity::class.java).apply {
                putExtra(FolderPickerActivity.EXTRA_PICKER_MODE, FolderPickerActivity.PickerMode.CAMERA_FOLDER)
                putExtra(FolderPickerActivity.KEY_SPACE_ID, spaceId)
                putExtra(FolderPickerActivity.KEY_ACCOUNT_NAME, picturesViewModel.getPictureUploadsAccount())
            }
            selectPictureUploadsPathLauncher.launch(intent)
            true
        }

        prefPictureUploadsSourcePath?.setOnPreferenceClickListener {
            val sourcePath = picturesViewModel.getPictureUploadsSourcePath()?.let { currentSourcePath ->
                currentSourcePath.takeUnless { it.endsWith(File.separator) } ?: currentSourcePath.plus(File.separator)
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
            selectPictureUploadsSourcePathLauncher.launch(intent)
            true
        }

        prefPictureUploadsOnWifi?.setOnPreferenceChangeListener { _, newValue ->
            newValue as Boolean
            picturesViewModel.useWifiOnly(newValue)
            newValue
        }

        prefPictureUploadsOnCharging?.setOnPreferenceChangeListener { _, newValue ->
            newValue as Boolean
            picturesViewModel.useChargingOnly(newValue)
            newValue
        }

        prefPictureUploadsAccount?.setOnPreferenceChangeListener { _, newValue ->
            newValue as String
            picturesViewModel.handleSelectAccount(newValue)
            true
        }

        prefPictureUploadsBehaviour?.setOnPreferenceChangeListener { _, newValue ->
            newValue as String
            picturesViewModel.handleSelectBehaviour(newValue)
            true
        }
    }

    override fun onDestroy() {
        picturesViewModel.schedulePictureUploads()
        super.onDestroy()
    }

    private fun enablePictureUploads(value: Boolean) {
        prefEnablePictureUploads?.isChecked = value
        prefPictureUploadsPath?.isEnabled = value
        prefPictureUploadsOnWifi?.isEnabled = value
        prefPictureUploadsOnCharging?.isEnabled = value
        prefPictureUploadsSourcePath?.isEnabled = value
        prefPictureUploadsBehaviour?.isEnabled = value
        prefPictureUploadsAccount?.isEnabled = value
        prefPictureUploadsLastSync?.isEnabled = value
    }

    private fun resetFields() {
        prefPictureUploadsAccount?.value = null
        prefPictureUploadsPath?.summary = null
        prefPictureUploadsSourcePath?.summary = null
        prefPictureUploadsOnWifi?.isChecked = false
        prefPictureUploadsOnCharging?.isChecked = false
        prefPictureUploadsBehaviour?.value = UploadBehavior.COPY.name
        prefPictureUploadsLastSync?.summary = null
    }
}
