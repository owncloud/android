/**
 * ownCloud Android client application
 *
 * @author Christian Schabesberger
 * @author Shashvat Kedia
 * @author Juan Carlos Garrote Gascón
 * @author Parneet Singh
 * @author Aitor Ballesteros Pavón
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

package com.owncloud.android.ui.preview

import android.accounts.Account
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.owncloud.android.R
import com.owncloud.android.databinding.PreviewTextFragmentBinding
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.extensions.collectLatestLifecycleFlow
import com.owncloud.android.extensions.filterMenuOptions
import com.owncloud.android.extensions.sendDownloadedFilesByShareSheet
import com.owncloud.android.presentation.files.operations.FileOperation
import com.owncloud.android.presentation.files.operations.FileOperationsViewModel
import com.owncloud.android.presentation.files.removefile.RemoveFilesDialogFragment
import com.owncloud.android.presentation.files.removefile.RemoveFilesDialogFragment.Companion.TAG_REMOVE_FILES_DIALOG_FRAGMENT
import com.owncloud.android.presentation.previews.PreviewTextViewModel
import com.owncloud.android.ui.dialog.LoadingDialog
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.utils.PreferenceUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.getScopeName
import timber.log.Timber
import java.io.BufferedWriter
import java.io.FileInputStream
import java.io.IOException
import java.io.StringWriter
import java.util.Scanner

class PreviewTextFragment : FileFragment() {
    private var account: Account? = null
    private lateinit var textLoadTask: TextLoadAsyncTask

    private val previewTextViewModel by viewModel<PreviewTextViewModel>()
    private val fileOperationsViewModel by viewModel<FileOperationsViewModel>()

    private lateinit var binding: PreviewTextFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        Timber.v("onCreateView")

        binding = PreviewTextFragmentBinding.inflate(inflater, container, false)
        return binding.root.apply {
            filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val file: OCFile?
        if (savedInstanceState == null) {
            val args = requireArguments()
            file = args.getParcelable(EXTRA_FILE)
            account = args.getParcelable(EXTRA_ACCOUNT)
            if (file == null) {
                throw IllegalStateException("Instanced with a NULL OCFile")
            }

            if (account == null) {
                throw IllegalStateException("Instanced with a NULL ownCloud Account")
            }
        } else {
            file = savedInstanceState.getParcelable(EXTRA_FILE)
            account = savedInstanceState.getParcelable(EXTRA_ACCOUNT)
        }
        requireActivity().title = getString(R.string.text_preview_label)
        setFile(file)
        setHasOptionsMenu(true)
        isOpen = true
        currentFilePreviewing = file
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadAndShowTextPreview()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            putParcelable(EXTRA_FILE, file)
            putParcelable(EXTRA_ACCOUNT, account)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share_file -> {
                mContainerActivity.fileOperationsHelper.showShareFile(file)
                true
            }

            R.id.action_open_file_with -> {
                openFile()
                true
            }

            R.id.action_remove_file -> {
                RemoveFilesDialogFragment.newInstance(file).show(requireFragmentManager(), TAG_REMOVE_FILES_DIALOG_FRAGMENT)
                true
            }

            R.id.action_see_details -> {
                seeDetails()
                true
            }

            R.id.action_send_file -> {
                requireActivity().sendDownloadedFilesByShareSheet(listOf(file))
                true
            }

            R.id.action_sync_file -> {
                account?.let { fileOperationsViewModel.performOperation(FileOperation.SynchronizeFileOperation(file, it.name)) }
                true
            }

            R.id.action_set_available_offline -> {
                val fileToSetAsAvailableOffline = ArrayList<OCFile>()
                fileToSetAsAvailableOffline.add(file)
                fileOperationsViewModel.performOperation(FileOperation.SetFilesAsAvailableOffline(fileToSetAsAvailableOffline))
                true
            }

            R.id.action_unset_available_offline -> {
                val fileToUnsetAsAvailableOffline = ArrayList<OCFile>()
                fileToUnsetAsAvailableOffline.add(file)
                fileOperationsViewModel.performOperation(FileOperation.UnsetFilesAsAvailableOffline(fileToUnsetAsAvailableOffline))
                true
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        textLoadTask.apply {
            cancel(true)
            dismissLoadingDialog()
        }
        isOpen = false
        currentFilePreviewing = null
    }

    override fun onFileMetadataChanged(updatedFile: OCFile?) {
        updatedFile?.let {
            file = updatedFile
        }
        requireActivity().invalidateOptionsMenu()
    }

    override fun onFileMetadataChanged() {
        mContainerActivity.storageManager?.let {
            file = it.getFileByPath(file.remotePath)
        }
        requireActivity().invalidateOptionsMenu()
    }

    override fun onFileContentChanged() {
        loadAndShowTextPreview()
    }

    override fun updateViewForSyncInProgress() {
        // Nothing to do here, sync is not shown in previews
    }

    override fun updateViewForSyncOff() {
        // Nothing to do here, sync is not shown in previews
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        mContainerActivity.storageManager?.let {
            val safeFile = file
            val accountName = it.account.name
            previewTextViewModel.filterMenuOptions(safeFile, accountName)

            collectLatestLifecycleFlow(previewTextViewModel.menuOptions) { menuOptions ->
                val hasWritePermission = safeFile.hasWritePermission
                menu.filterMenuOptions(menuOptions, hasWritePermission)
            }
        }

        menu.findItem(R.id.action_search)?.apply {
            isVisible = false
            isEnabled = false
        }

        setRolesAccessibilityToMenuItems(menu)
    }

    private fun setRolesAccessibilityToMenuItems(menu: Menu) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            menu.findItem(R.id.action_see_details)?.contentDescription = "${getString(R.string.actionbar_see_details)} ${getString(R.string.button_role_accessibility)}"
        }
    }

    private fun loadAndShowTextPreview() {
        textLoadTask = TextLoadAsyncTask(
            binding.textLayout.textPreview,
            binding.top,
            binding.textLayout.root,
            binding.tabLayout,
            binding.viewPager
        )
        textLoadTask.execute(file)
    }

    private fun openFile() {
        mContainerActivity.fileOperationsHelper.openFile(file)
        finish()
    }

    private fun seeDetails() {
        mContainerActivity.showDetails(file)
    }

    private fun finish() {
        requireActivity().onBackPressed()
    }

    private inner class TextLoadAsyncTask(
        var textViewReference: TextView,
        var rootView: RelativeLayout,
        var textLayout: View,
        var tabLayout: TabLayout,
        var viewPager: ViewPager2
    ) : AsyncTask<OCFile, Unit, StringWriter>() {

        private val DIALOG_WAIT_TAG = "DIALOG_WAIT"
        private lateinit var mimeType: String

        override fun onPreExecute() {
            showLoadingDialog()
        }

        override fun doInBackground(vararg params: OCFile?): StringWriter {
            if (params.size != 1) {
                throw IllegalArgumentException("The parameter to ${this.getScopeName()} must be (1) the file location")
            }

            val file = params[0] as OCFile
            val location = file.storagePath
            mimeType = file.mimeType

            var inputStream: FileInputStream? = null
            var scanner: Scanner? = null
            val source = StringWriter()
            val bufferedWriter = BufferedWriter(source)

            try {
                inputStream = FileInputStream(location)
                scanner = Scanner(inputStream)
                while (scanner.hasNextLine()) {
                    bufferedWriter.append(scanner.nextLine())
                    if (scanner.hasNextLine()) {
                        bufferedWriter.append("\n")
                    }
                }
                bufferedWriter.close()
                val exc = scanner.ioException()
                if (exc != null) {
                    throw exc
                }
            } catch (e: IOException) {
                Timber.e(e)
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close()
                    } catch (e: IOException) {
                        Timber.e(e)
                    }
                }
                scanner?.close()
            }
            return source
        }

        override fun onPostExecute(result: StringWriter) {
            val text = String(result.buffer)
            showPreviewText(text, mimeType, rootView, textViewReference, textLayout, tabLayout, viewPager)

            try {
                dismissLoadingDialog()
            } catch (illegalStateException: java.lang.IllegalStateException) {
                Timber.w(illegalStateException, "Dismissing dialog not allowed after onSaveInstanceState")
            }
        }

        fun showLoadingDialog() {
            val waitDialogFragment = requireActivity().supportFragmentManager.findFragmentByTag(DIALOG_WAIT_TAG)
            val loading: LoadingDialog

            if (waitDialogFragment == null) {
                loading = LoadingDialog.newInstance(R.string.wait_a_moment, false)
                val fragmentManager = requireActivity().supportFragmentManager
                val fragmentTransaction = fragmentManager.beginTransaction()
                loading.show(fragmentTransaction, DIALOG_WAIT_TAG)
            } else {
                loading = waitDialogFragment as LoadingDialog
                loading.showsDialog = true
            }
        }

        fun dismissLoadingDialog() {
            val waitDialogFragment = requireActivity().supportFragmentManager.findFragmentByTag(DIALOG_WAIT_TAG)
            waitDialogFragment?.let {
                val loading = waitDialogFragment as LoadingDialog
                loading.dismiss()
            }
        }

        private fun showPreviewText(
            text: String,
            mimeType: String,
            rootView: RelativeLayout,
            textView: TextView,
            textLayout: View,
            tabLayout: TabLayout,
            viewPager: ViewPager2
        ) {
            if (mimeType == "text/markdown") {
                rootView.removeView(textLayout)
                showFormatType(text, mimeType, tabLayout, viewPager)
                tabLayout.visibility = View.VISIBLE
                viewPager.visibility = View.VISIBLE
            } else {
                rootView.removeView(tabLayout)
                rootView.removeView(viewPager)
                textView.text = text
                textLayout.visibility = View.VISIBLE
            }
        }

        private fun showFormatType(
            text: String,
            mimeType: String,
            tabLayout: TabLayout,
            viewPager: ViewPager2
        ) {
            val adapter = PreviewFormatTextFragmentStateAdapter(this@PreviewTextFragment, text, mimeType)
            viewPager.adapter = adapter

            TabLayoutMediator(tabLayout, viewPager) { tab: TabLayout.Tab, position: Int ->
                if (position == 0) {
                    tab.text = adapter.formatTypes[mimeType]
                } else {
                    tab.text = adapter.formatTypes[PreviewFormatTextFragmentStateAdapter.TYPE_PLAIN]
                }
            }.attach()
        }
    }


    companion object {
        private const val EXTRA_FILE = "FILE"
        private const val EXTRA_ACCOUNT = "ACCOUNT"
        var isOpen = false
        var currentFilePreviewing: OCFile? = null

        fun newInstance(file: OCFile, account: Account): PreviewTextFragment {
            val args = Bundle().apply {
                putParcelable(EXTRA_FILE, file)
                putParcelable(EXTRA_ACCOUNT, account)
            }

            return PreviewTextFragment().apply {
                arguments = args
            }
        }

        fun canBePreviewed(file: OCFile?): Boolean {
            val unsupportedTypes = listOf(
                "test/richtest",
                "text/rtf",
                "text/vnd.abc",
                "text/vnd.fmi.flexstor",
                "text/vnd.rn-realtext",
                "text/vnd.wap.wml",
                "text/vnd.wap.wmlscript",
                "text/uri-list",
            )

            return (file != null && file.isAvailableLocally && file.isText &&
                    !unsupportedTypes.contains(file.mimeType) &&
                    !unsupportedTypes.contains(file.getMimeTypeFromName()))
        }
    }
}

