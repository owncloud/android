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
import androidx.fragment.app.Fragment
import com.owncloud.android.R
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.ui.controller.TransferProgressController
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.utils.PreferenceUtils
import timber.log.Timber

/**
 * This Fragment is used to monitor the progress of a file downloading.
 *
 * Creates an empty details fragment.
 *
 * It's necessary to keep a public constructor without parameters; the system uses it when tries to
 * reinstantiate a fragment automatically.
 */
class FileDownloadFragment : FileFragment(), View.OnClickListener {
    private var mAccount: Account? = null
    var mProgressController: TransferProgressController? = null
    private var mIgnoreFirstSavedState = false
    private var mError = false
    private var mProgressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        file = requireArguments().getParcelable<Parcelable>(ARG_FILE) as OCFile?
        mIgnoreFirstSavedState = requireArguments().getBoolean(ARG_IGNORE_FIRST)
        mAccount = requireArguments().getParcelable(ARG_ACCOUNT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        if (savedInstanceState != null) {
            if (!mIgnoreFirstSavedState) {
                file = savedInstanceState.getParcelable<Parcelable>(EXTRA_FILE) as OCFile?
                mAccount = savedInstanceState.getParcelable(EXTRA_ACCOUNT)
                mError = savedInstanceState.getBoolean(EXTRA_ERROR)
            } else {
                mIgnoreFirstSavedState = false
            }
        }
        val rootView = inflater.inflate(R.layout.file_download_fragment, container, false)
        mProgressBar = rootView.findViewById(R.id.progressBar)
        rootView.findViewById<View>(R.id.cancelBtn).setOnClickListener(this)
        rootView.filterTouchesWhenObscured =
            PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)
        if (mError) {
            setButtonsForRemote(rootView)
        } else {
            setButtonsForTransferring(rootView)
        }
        return rootView
    }

    override fun onActivityCreated(savedState: Bundle?) {
        super.onActivityCreated(savedState)
        mProgressController = TransferProgressController(mContainerActivity)
        mProgressController!!.setProgressBar(mProgressBar)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(EXTRA_FILE, file)
        outState.putParcelable(EXTRA_ACCOUNT, mAccount)
        outState.putBoolean(EXTRA_ERROR, mError)
    }

    override fun onStart() {
        super.onStart()
        listenForTransferProgress()
    }

    override fun onStop() {
        leaveTransferProgress()
        super.onStop()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.cancelBtn -> {
                mContainerActivity.fileOperationsHelper.cancelTransference(file)
                requireActivity().finish()
            }
            else -> Timber.e("Incorrect view clicked!")
        }
    }

    /**
     * Enables buttons for a file being downloaded
     */
    private fun setButtonsForTransferring(rootView: View?) {
        if (rootView != null) {
            rootView.findViewById<View>(R.id.cancelBtn).visibility = View.VISIBLE

            // show the progress bar for the transfer
            rootView.findViewById<View>(R.id.progressBar).visibility = View.VISIBLE
            val progressText = rootView.findViewById<TextView>(R.id.progressText)
            progressText.setText(R.string.downloader_download_in_progress_ticker)
            progressText.visibility = View.VISIBLE

            // hides the error icon
            rootView.findViewById<View>(R.id.errorText).visibility = View.GONE
            rootView.findViewById<View>(R.id.error_image).visibility = View.GONE
        }
    }

    /**
     * Enables or disables buttons for a file locally available
     */
    private fun setButtonsForDown(rootView: View?) {
        if (rootView != null) {
            rootView.findViewById<View>(R.id.cancelBtn).visibility = View.GONE

            // hides the progress bar
            rootView.findViewById<View>(R.id.progressBar).visibility = View.GONE

            // updates the text message
            val progressText = rootView.findViewById<TextView>(R.id.progressText)
            progressText.setText(R.string.common_loading)
            progressText.visibility = View.VISIBLE

            // hides the error icon
            rootView.findViewById<View>(R.id.errorText).visibility = View.GONE
            rootView.findViewById<View>(R.id.error_image).visibility = View.GONE
        }
    }

    /**
     * Enables or disables buttons for a file not locally available
     *
     *
     * Currently, this is only used when a download was failed
     */
    private fun setButtonsForRemote(rootView: View?) {
        if (rootView != null) {
            rootView.findViewById<View>(R.id.cancelBtn).visibility = View.GONE

            // hides the progress bar and message
            rootView.findViewById<View>(R.id.progressBar).visibility = View.GONE
            rootView.findViewById<View>(R.id.progressText).visibility = View.GONE

            // shows the error icon and message
            rootView.findViewById<View>(R.id.errorText).visibility = View.VISIBLE
            rootView.findViewById<View>(R.id.error_image).visibility = View.VISIBLE
        }
    }

    override fun onTransferServiceConnected() {
        listenForTransferProgress()
    }

    override fun onFileMetadataChanged(updatedFile: OCFile?) {
        if (updatedFile != null) {
            file = updatedFile
        }
        // view does not need any update
    }

    override fun onFileMetadataChanged() {
        val storageManager = mContainerActivity.storageManager
        if (storageManager != null) {
            file = storageManager.getFileByPath(file.remotePath)
        }
        // view does not need any update
    }

    override fun onFileContentChanged() {
        // view does not need any update, parent activity will replace this fragment
    }

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
        if (mProgressController != null) {
            mProgressController!!.startListeningProgressFor(file, mAccount)
            setButtonsForTransferring(view)
        }
    }

    private fun leaveTransferProgress() {
        if (mProgressController != null) {
            mProgressController!!.stopListeningProgressFor(file, mAccount)
        }
    }

    fun setError(error: Boolean) {
        mError = error
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
        fun newInstance(file: OCFile?, account: Account?, ignoreFirstSavedState: Boolean): Fragment {
            val frag = FileDownloadFragment()
            val args = Bundle()
            args.putParcelable(ARG_FILE, file)
            args.putParcelable(ARG_ACCOUNT, account)
            args.putBoolean(ARG_IGNORE_FIRST, ignoreFirstSavedState)
            frag.arguments = args
            return frag
        }
    }
}
