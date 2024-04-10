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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.owncloud.android.R
import com.owncloud.android.domain.files.model.FileMenuOption
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.extensions.collectLatestLifecycleFlow
import com.owncloud.android.extensions.filterMenuOptions
import com.owncloud.android.extensions.sendDownloadedFilesByShareSheet
import com.owncloud.android.presentation.files.operations.FileOperation
import com.owncloud.android.presentation.files.operations.FileOperation.SetFilesAsAvailableOffline
import com.owncloud.android.presentation.files.operations.FileOperationsViewModel
import com.owncloud.android.presentation.previews.PreviewTextViewModel
import com.owncloud.android.ui.controller.TransferProgressController
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.utils.PreferenceUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.lang.ref.WeakReference
import kotlin.coroutines.Continuation

class PreviewTextFragment : FileFragment() {

    companion object {
        const val EXTRA_FILE = "FILE"
        const val EXTRA_ACCOUNT = "ACCOUNT"
        var isOpen = false
        var currentFilePreviewing: OCFile? = null
    }

    private var mAccount: Account? = null
    private lateinit var mProgressBar: ProgressBar
    private lateinit var mProgressController: TransferProgressController
    private lateinit var mTextPreview: TextView
    private lateinit var mTextLayout: View
    private lateinit var mTabLayout: TabLayout
    private lateinit var mViewPager2: ViewPager2
    private lateinit var rootView: RelativeLayout
    private lateinit var mTextLoadTask: TextLoadAsyncTask

    private val previewTextViewModel by viewModel<PreviewTextViewModel>()
    private val fileOperationsViewModel by viewModel<FileOperationsViewModel>()

    fun newInstance(file: OCFile, account: Account): PreviewTextFragment {
        val frag = PreviewTextFragment()
        val args = Bundle()
        args.apply {
            putParcelable(EXTRA_FILE, file)
            putParcelable(EXTRA_ACCOUNT, account)
        }
        frag.arguments = args
        return frag
    }

    // PREVIEWTEXTFRAGMENT

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    )
            : View? {
        super.onCreateView(inflater, container, savedInstanceState)
        Timber.v("onCreateView")

        val ret = inflater.inflate(R.layout.preview_text_fragment, container, false)
        ret.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)

        // We can use binding instead of findViewById
        rootView = ret.findViewById(R.id.top)
        mProgressBar = ret.findViewById(R.id.syncProgressBar)
        mTabLayout = ret.findViewById(R.id.tab_layout)
        mViewPager2 = ret.findViewById(R.id.view_pager)
        mTextPreview = ret.findViewById(R.id.text_preview)
        mTextLayout = ret.findViewById(R.id.text_layout)

        return ret
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var file: OCFile?
        if (savedInstanceState == null) {
            val args = arguments
            file = args!!.getParcelable(EXTRA_FILE)
            mAccount = args.getParcelable(EXTRA_ACCOUNT)
            if (file == null) {
                throw IllegalStateException("Instanced with a NULL OCFile")
            }

            if (mAccount == null) {
                throw IllegalStateException(("Instanced with a NULL ownCloud Account")
            }
        } else {
            file = savedInstanceState.getParcelable(EXTRA_FILE)
            mAccount = savedInstanceState.getParcelable(EXTRA_ACCOUNT)
        }
        setFile(file)
        setHasOptionsMenu(true)
        isOpen = true
        currentFilePreviewing = file!!
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mProgressController = TransferProgressController(mContainerActivity)
        mProgressController.setProgressBar(mProgressBar)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadAndShowTextPreview()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putParcelable(EXTRA_FILE, file)
            putParcelable(EXTRA_ACCOUNT, mAccount)
        }
    }

    private fun loadAndShowTextPreview() {
        mTextLoadTask = PreviewTextFragment.TextLoadAsyncTask(
            WeakReference(mTextPreview),
            WeakReference(rootView),
            WeakReference(mTextLayout),
            WeakReference(mTabLayout),
            WeakReference(mViewPager2)
        )
        mTextLoadTask.execute(file)
    }

    //TEXTLOADASYNCTASK

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.file_actions_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        if (mContainerActivity.storageManager != null) {
            val safeFile = file
            val accountName = mContainerActivity.storageManager.account.name
            previewTextViewModel.filterMenuOptions(safeFile, accountName)

            FragmentExtKt.collectLatestLifecycleFlow(
                this,
                previewTextViewModel.menuOptions,
                Lifecycle.State.CREATED
            ) { menuOptions: List<FileMenuOption?>?, continuation: Continuation<Unit>? ->
                val hasWritePermission = safeFile.hasWritePermission
                MenuExtKt.filterMenuOptions(menu, menuOptions, hasWritePermission)
                null
            }
        }

        menu.findItem(R.id.action_search)?.apply {
            isVisible = false
            isEnabled = false
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share_file -> {
                mContainerActivity.fileOperationsHelper.showShareFile(file)
                return true
            }

            R.id.action_open_file_with -> {
                openFile()
                return true
            }

            R.id.action_see_details -> {
                seeDetails()
                return true
            }

            R.id.action_send_file -> {
                requireActivity().sendDownloadedFilesByShareSheet(listOf(file))
                return true
            }

            R.id.action_set_available_offline -> {
                val fileToSetAsAvailableOffline = ArrayList<OCFile>()
                fileToSetAsAvailableOffline.add(file)
                fileOperationsViewModel.performOperation(SetFilesAsAvailableOffline(fileToSetAsAvailableOffline))
                return true
            }

            R.id.action_unset_available_offline -> {
                val fileToUnsetAsAvailableOffline = ArrayList<OCFile>()
                fileToUnsetAsAvailableOffline.add(file)
                fileOperationsViewModel.performOperation(FileOperation.UnsetFilesAsAvailableOffline(fileToUnsetAsAvailableOffline))
                return true
            }

            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    private fun seeDetails() {
        mContainerActivity.showDetails(file)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (mTextLoadTask != null) {
            mTextLoadTask.apply {
                cancel(true)
                dismmisLoadingDialog()
            }
        }
        isOpen = false
        currentFilePreviewing = null
    }

    override fun onFileMetadataChanged(updatedFile: OCFile) {
        if (updatedFile != null) {
            file = updatedFile
        }
        activity?.invalidateOptionsMenu()
    }

    override fun onFileMetadataChanged() {
        val storageManager = mContainerActivity.storageManager
        if (storageManager != null) {
            file = storageManager.getFileByPath(file.remotePath, null)
        }
        activity?.invalidateOptionsMenu()
    }

    override fun onFileContentChanged() {
        loadAndShowTextPreview()
    }

    override fun updateViewForSyncInProgress() {
        mProgressController.showProgressBar()
    }

    override fun updateViewForSyncOff() {
        mProgressController.hideProgressBar()
    }

    // Opens the previewed file with an external application.
    private fun openFile() {
        mContainerActivity.fileOperationsHelper.openFile(file)
        finish()
    }

    // Finishes the preview
    private fun finish() {
        activity?.onBackPressed()
    }

}

