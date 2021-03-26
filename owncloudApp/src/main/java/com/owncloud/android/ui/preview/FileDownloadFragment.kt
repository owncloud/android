/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Christian Schabesberger
 * Copyright (C) 2020 ownCloud GmbH.
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.preview

import android.accounts.Account
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.extensions.observeWorkerTillItFinishes
import com.owncloud.android.extensions.transformToObserveJustLastWork
import com.owncloud.android.presentation.manager.TransferManager
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.utils.PreferenceUtils

/**
 * This Fragment is used to monitor the progress of a file downloading.
 *
 * Creates an empty details fragment.
 *
 * It's necessary to keep a public constructor without parameters; the system uses it when tries to
 * reinstantiate a fragment automatically.
 */
class FileDownloadFragment : FileFragment() {
    private var account: Account? = null
    private var ignoreFirstSavedState = false
    private var error = false
    private var progressBar: ProgressBar? = null
    private var liveData: LiveData<MutableList<WorkInfo>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with(requireArguments()) {
            file = getParcelable<Parcelable>(ARG_FILE) as OCFile?
            ignoreFirstSavedState = getBoolean(ARG_IGNORE_FIRST)
            account = getParcelable(ARG_ACCOUNT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.file_download_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.let {
            if (!ignoreFirstSavedState) {
                file = it.getParcelable<Parcelable>(EXTRA_FILE) as OCFile?
                account = it.getParcelable(EXTRA_ACCOUNT)
                error = it.getBoolean(EXTRA_ERROR)
            } else {
                ignoreFirstSavedState = false
            }
        }

        view.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)

        progressBar = view.findViewById(R.id.progressBar)

        view.findViewById<View>(R.id.cancelBtn).setOnClickListener {
            mContainerActivity.fileOperationsHelper.cancelTransference(file)
            requireActivity().finish()
        }

        if (error) {
            setButtonsForRemote(view)
        } else {
            setButtonsForTransferring(view)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putParcelable(EXTRA_FILE, file)
            putParcelable(EXTRA_ACCOUNT, account)
            putBoolean(EXTRA_ERROR, error)
        }
    }

    override fun onStart() {
        super.onStart()
        listenForTransferProgress()
    }

    override fun onStop() {
        leaveTransferProgress()
        super.onStop()
    }

    /**
     * Enables buttons for a file being downloaded
     */
    private fun setButtonsForTransferring(rootView: View?) {
        rootView?.run {
            findViewById<View>(R.id.cancelBtn).isVisible = true
            findViewById<View>(R.id.progressBar).isVisible = true
            findViewById<TextView>(R.id.progressText).apply {
                setText(R.string.downloader_download_in_progress_ticker)
                isVisible = true
            }
            // hides the error icon
            findViewById<View>(R.id.errorText).isVisible = false
            findViewById<View>(R.id.error_image).isVisible = false
        }
    }

    /**
     * Enables or disables buttons for a file locally available
     */
    private fun setButtonsForDown(rootView: View?) {
        rootView?.run {
            findViewById<View>(R.id.cancelBtn).isVisible = false
            findViewById<View>(R.id.progressBar).isVisible = false

            // updates the text message
            findViewById<TextView>(R.id.progressText).apply {
                setText(R.string.common_loading)
                isVisible = true
            }

            // hides the error icon
            findViewById<View>(R.id.errorText).isVisible = false
            findViewById<View>(R.id.error_image).isVisible = false
        }
    }

    /**
     * Enables or disables buttons for a file not locally available
     *
     *
     * Currently, this is only used when a download was failed
     */
    private fun setButtonsForRemote(rootView: View?) {
        rootView?.run {
            findViewById<View>(R.id.cancelBtn).isVisible = false

            // hides the progress bar and message
            findViewById<View>(R.id.progressBar).isVisible = false
            findViewById<View>(R.id.progressText).isVisible = false

            // shows the error icon and message
            findViewById<View>(R.id.errorText).isVisible = true
            findViewById<View>(R.id.error_image).isVisible = true
        }
    }

    override fun onTransferServiceConnected() {
        listenForTransferProgress()
    }

    // view does not need any update
    override fun onFileMetadataChanged(updatedFile: OCFile?) {
        updatedFile?.let { file = it }
    }

    // view does not need any update
    override fun onFileMetadataChanged() {
        mContainerActivity.storageManager?.let {
            file = it.getFileByPath(file.remotePath)
        }
    }

    // view does not need any update, parent activity will replace this fragment
    override fun onFileContentChanged() {}

    override fun updateViewForSyncInProgress() {
        setButtonsForTransferring(view)
    }

    override fun updateViewForSyncOff() {
        if (file.isAvailableLocally) {
            setButtonsForDown(view)
        } else {
            setButtonsForRemote(view)
        }
    }

    private fun listenForTransferProgress() {
        val transferManager = TransferManager(MainApp.appContext)
        account?.let {
            liveData = transferManager.getLiveDataForDownloadingFile(it, file)
            liveData?.transformToObserveJustLastWork()?.observeWorkerTillItFinishes(
                owner = this,
                onWorkEnqueued = { progressBar?.isIndeterminate = true },
                onWorkRunning = { progress ->
                    progressBar?.apply {
                        isIndeterminate = false
                        setProgress(progress)
                        invalidate()
                    }
                },
                onWorkSucceeded = { },
                onWorkFailed = { }
            )
        }
        setButtonsForTransferring(view)
    }

    private fun leaveTransferProgress() {
        liveData?.removeObservers(this)
    }

    fun setError(error: Boolean) {
        this.error = error
    }

    companion object {
        const val EXTRA_FILE = "FILE"
        const val EXTRA_ACCOUNT = "ACCOUNT"
        private const val EXTRA_ERROR = "ERROR"
        private const val ARG_FILE = "FILE"
        private const val ARG_IGNORE_FIRST = "IGNORE_FIRST"
        private const val ARG_ACCOUNT = "ACCOUNT"

        /**
         * Public factory method to create a new fragment that shows the progress of a file download.
         *
         * Android strongly recommends keep the empty constructor of fragments as the only public constructor, and
         * use [.setArguments] to set the needed arguments.
         *
         * This method hides to client objects the need of doing the construction in two steps.
         *
         * When 'file' is null creates a dummy layout (useful when a file wasn't tapped before).
         *
         * @param file                      An [OCFile] to show in the fragment
         * @param account                   An OC account; needed to start downloads
         * @param ignoreFirstSavedState     Flag to work around an unexpected behaviour of [FragmentStatePagerAdapter]
         * TODO better solution
         */
        fun newInstance(file: OCFile?, account: Account, ignoreFirstSavedState: Boolean): Fragment {
            val args = Bundle().apply {
                putParcelable(ARG_FILE, file)
                putParcelable(ARG_ACCOUNT, account)
                putBoolean(ARG_IGNORE_FIRST, ignoreFirstSavedState)
            }

            return FileDownloadFragment().apply { arguments = args }
        }
    }
}
