/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Christian Schabesberger
 * @author David González Verdugo
 * @author Abel García de Prada
 * @author Shashvat Kedia
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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import com.owncloud.android.R
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.files.FileMenuFilter
import com.owncloud.android.media.MediaControlView
import com.owncloud.android.media.MediaService
import com.owncloud.android.media.MediaServiceBinder
import com.owncloud.android.ui.controller.TransferProgressController
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.preview.PreviewAudioFragment.MediaServiceConnection
import com.owncloud.android.utils.PreferenceUtils
import timber.log.Timber

/**
 * This fragment shows a preview of a downloaded audio.
 *
 *
 * Trying to get an instance with NULL [OCFile] or ownCloud [Account] values will
 * produce an [IllegalStateException].
 *
 *
 * If the [OCFile] passed is not downloaded, an [IllegalStateException] is
 * generated on instantiation too.
 */
class PreviewAudioFragment
/**
 * Creates an empty fragment for preview audio files.
 * MUST BE KEPT: the system uses it when tries to reinstantiate a fragment automatically
 * (for instance, when the device is turned a aside).
 * DO NOT CALL IT: an [OCFile] and [Account] must be provided for a successful
 * construction
 */
    : FileFragment() {
    private var mAccount: Account? = null
    private var mImagePreview: ImageView? = null
    private var mSavedPlaybackPosition = 0
    private var mMediaServiceBinder: MediaServiceBinder? = null
    private var mMediaController: MediaControlView? = null
    private var mMediaServiceConnection: MediaServiceConnection? = null
    private var mAutoplay = true
    private var mProgressBar: ProgressBar? = null
    var mProgressController: TransferProgressController? = null

    /**
     * {@inheritDoc}
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    /**
     * {@inheritDoc}
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        Timber.v("onCreateView")
        val view = inflater.inflate(R.layout.preview_audio_fragment, container, false)
        view.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)
        mImagePreview = view.findViewById(R.id.image_preview)
        mMediaController = view.findViewById(R.id.media_controller)
        mProgressBar = view.findViewById(R.id.syncProgressBar)
        return view
    }

    /**
     * {@inheritDoc}
     */
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Timber.v("onActivityCreated")
        val file: OCFile?
        val args = arguments
        if (savedInstanceState == null) {
            file = args!!.getParcelable(EXTRA_FILE)
            setFile(file)
            mAccount = args.getParcelable(EXTRA_ACCOUNT)
            mSavedPlaybackPosition = args.getInt(EXTRA_PLAY_POSITION)
            mAutoplay = args.getBoolean(EXTRA_PLAYING)
        } else {
            file = savedInstanceState.getParcelable(EXTRA_FILE)
            setFile(file)
            mAccount = savedInstanceState.getParcelable(EXTRA_ACCOUNT)
            mSavedPlaybackPosition = savedInstanceState.getInt(
                EXTRA_PLAY_POSITION,
                args!!.getInt(EXTRA_PLAY_POSITION)
            )
            mAutoplay = savedInstanceState.getBoolean(
                EXTRA_PLAYING,
                args.getBoolean(EXTRA_PLAYING)
            )
        }
        checkNotNull(file) { "Instanced with a NULL OCFile" }
        checkNotNull(mAccount) { "Instanced with a NULL ownCloud Account" }
        check(file.isAvailableLocally) { "There is no local file to preview" }
        check(file.isAudio) { "Not an audio file" }
        extractAndSetCoverArt(file)
        mProgressController = TransferProgressController(mContainerActivity)
        mProgressController!!.setProgressBar(mProgressBar)
    }

    /**
     * tries to read the cover art from the audio file and sets it as cover art.
     *
     * @param file audio file with potential cover art
     */
    private fun extractAndSetCoverArt(file: OCFile) {
        try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(file.storagePath)
            val data = mmr.embeddedPicture
            if (data != null) {
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                mImagePreview!!.setImageBitmap(bitmap) //associated cover art in bitmap
            } else {
                mImagePreview!!.setImageResource(R.drawable.ic_place_holder_music_cover_art)
            }
        } catch (t: Throwable) {
            mImagePreview!!.setImageResource(R.drawable.ic_place_holder_music_cover_art)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Timber.v("onSaveInstanceState")
        outState.putParcelable(EXTRA_FILE, file)
        outState.putParcelable(EXTRA_ACCOUNT, mAccount)
        if (mMediaServiceBinder != null) {
            outState.putInt(EXTRA_PLAY_POSITION, mMediaServiceBinder!!.currentPosition)
            outState.putBoolean(EXTRA_PLAYING, mMediaServiceBinder!!.isPlaying)
        }
    }

    override fun onStart() {
        super.onStart()
        Timber.v("onStart")
        val file = file
        if (file != null && file.isAvailableLocally) {
            bindMediaService()
        }
        mProgressController!!.startListeningProgressFor(getFile(), mAccount)
    }

    override fun onTransferServiceConnected() {
        if (mProgressController != null) {
            mProgressController!!.startListeningProgressFor(file, mAccount)
        }
    }

    override fun onFileMetadataChanged(updatedFile: OCFile) {
        if (updatedFile != null) {
            file = updatedFile
        }
        requireActivity().invalidateOptionsMenu()
    }

    override fun onFileMetadataChanged() {
        val storageManager = mContainerActivity.storageManager
        if (storageManager != null) {
            file = storageManager.getFileByPath(file.remotePath)
        }
        requireActivity().invalidateOptionsMenu()
    }

    override fun onFileContentChanged() {
        playAudio(true)
    }

    override fun updateViewForSyncInProgress() {
        mProgressController!!.showProgressBar()
    }

    override fun updateViewForSyncOff() {
        mProgressController!!.hideProgressBar()
    }

    /**
     * {@inheritDoc}
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.file_actions_menu, menu)
    }

    /**
     * {@inheritDoc}
     */
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val mf = FileMenuFilter(
            file,
            mAccount,
            mContainerActivity,
            activity
        )
        mf.filter(menu, false, false, false, false)

        // additional restriction for this fragment 
        // TODO allow renaming in PreviewAudioFragment
        var item = menu.findItem(R.id.action_rename_file)
        if (item != null) {
            item.isVisible = false
            item.isEnabled = false
        }

        // additional restriction for this fragment
        item = menu.findItem(R.id.action_move)
        if (item != null) {
            item.isVisible = false
            item.isEnabled = false
        }

        // additional restriction for this fragment
        item = menu.findItem(R.id.action_copy)
        if (item != null) {
            item.isVisible = false
            item.isEnabled = false
        }
        item = menu.findItem(R.id.action_search)
        if (item != null) {
            item.isVisible = false
            item.isEnabled = false
        }
        item = menu.findItem(R.id.action_sync_file)
        if (item != null) {
            item.isVisible = false
            item.isEnabled = false
        }
    }

    /**
     * {@inheritDoc}
     */
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
                val dialog = RemoveFilesDialogFragment.newInstance(file)
                dialog.show(parentFragmentManager, ConfirmationDialogFragment.FTAG_CONFIRMATION)
                true
            }
            R.id.action_see_details -> {
                seeDetails()
                true
            }
            R.id.action_send_file -> {
                mContainerActivity.fileOperationsHelper.sendDownloadedFile(file)
                true
            }
            R.id.action_sync_file -> {
                mContainerActivity.fileOperationsHelper.syncFile(file)
                true
            }
            R.id.action_set_available_offline -> {
                mContainerActivity.fileOperationsHelper.toggleAvailableOffline(file, true)
                true
            }
            R.id.action_unset_available_offline -> {
                mContainerActivity.fileOperationsHelper.toggleAvailableOffline(file, false)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun seeDetails() {
        mContainerActivity.showDetails(file)
    }

    override fun onStop() {
        Timber.v("onStop")
        mProgressController!!.stopListeningProgressFor(file, mAccount)
        if (mMediaServiceConnection != null) {
            Timber.d("Unbinding from MediaService ...")
            if (mMediaServiceBinder != null && mMediaController != null) {
                mMediaServiceBinder!!.unregisterMediaController(mMediaController)
            }
            requireActivity().unbindService(mMediaServiceConnection!!)
            mMediaServiceConnection = null
            mMediaServiceBinder = null
        }
        super.onStop()
    }

    fun playAudio(restart: Boolean) {
        val file = file
        if (restart) {
            Timber.d("restarting playback of %s", file.storagePath)
            mAutoplay = true
            mSavedPlaybackPosition = 0
            mMediaServiceBinder!!.start(mAccount, file, true, 0)
        } else if (!mMediaServiceBinder!!.isPlaying(file)) {
            Timber.d("starting playback of %s", file.storagePath)
            mMediaServiceBinder!!.start(mAccount, file, mAutoplay, mSavedPlaybackPosition)
        } else {
            if (!mMediaServiceBinder!!.isPlaying && mAutoplay) {
                mMediaServiceBinder!!.start()
                mMediaController!!.updatePausePlay()
            }
        }
    }

    private fun bindMediaService() {
        Timber.d("Binding to MediaService...")
        if (mMediaServiceConnection == null) {
            mMediaServiceConnection = MediaServiceConnection()
            requireActivity().bindService(
                Intent(
                    activity,
                    MediaService::class.java
                ),
                mMediaServiceConnection!!,
                Context.BIND_AUTO_CREATE
            )
            // follow the flow in MediaServiceConnection#onServiceConnected(...)
        }
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private inner class MediaServiceConnection : ServiceConnection {
        override fun onServiceConnected(component: ComponentName, service: IBinder) {
            if (activity != null) {
                if (component ==
                    ComponentName(requireActivity(), MediaService::class.java)
                ) {
                    Timber.d("Media service connected")
                    mMediaServiceBinder = service as MediaServiceBinder
                    if (mMediaServiceBinder != null) {
                        prepareMediaController()
                        playAudio(false)
                        Timber.d("Successfully bound to MediaService, MediaController ready")
                    } else {
                        Timber.e("Unexpected response from MediaService while binding")
                    }
                }
            }
        }

        private fun prepareMediaController() {
            mMediaServiceBinder!!.registerMediaController(mMediaController)
            if (mMediaController != null) {
                mMediaController!!.setMediaPlayer(mMediaServiceBinder)
                mMediaController!!.isEnabled = true
                mMediaController!!.updatePausePlay()
            }
        }

        override fun onServiceDisconnected(component: ComponentName) {
            if (component == ComponentName(requireActivity(), MediaService::class.java)) {
                Timber.w("Media service suddenly disconnected")
                if (mMediaController != null) {
                    mMediaController!!.setMediaPlayer(null)
                } else {
                    Timber.w("No media controller to release when disconnected from media service")
                }
                mMediaServiceBinder = null
                mMediaServiceConnection = null
            }
        }
    }

    /**
     * Opens the previewed file with an external application.
     */
    private fun openFile() {
        stopPreview()
        mContainerActivity.fileOperationsHelper.openFile(file)
        finish()
    }

    fun stopPreview() {
        mMediaServiceBinder!!.pause()
    }

    /**
     * Finishes the preview
     */
    private fun finish() {
        requireActivity().onBackPressed()
    }

    companion object {
        const val EXTRA_FILE = "FILE"
        const val EXTRA_ACCOUNT = "ACCOUNT"
        private const val EXTRA_PLAY_POSITION = "PLAY_POSITION"
        private const val EXTRA_PLAYING = "PLAYING"

        /**
         * Public factory method to create new PreviewAudioFragment instances.
         *
         * @param file                  An [OCFile] to preview in the fragment
         * @param account               ownCloud account containing file
         * @param startPlaybackPosition Time in milliseconds where the play should be started
         * @param autoplay              If 'true', the file will be played automatically when
         * the fragment is displayed.
         * @return Fragment ready to be used.
         */
        fun newInstance(
            file: OCFile?,
            account: Account?,
            startPlaybackPosition: Int,
            autoplay: Boolean
        ): PreviewAudioFragment {
            val frag = PreviewAudioFragment()
            val args = Bundle()
            args.putParcelable(EXTRA_FILE, file)
            args.putParcelable(EXTRA_ACCOUNT, account)
            args.putInt(EXTRA_PLAY_POSITION, startPlaybackPosition)
            args.putBoolean(EXTRA_PLAYING, autoplay)
            frag.arguments = args
            return frag
        }

        /**
         * Helper method to test if an [OCFile] can be passed to a [PreviewAudioFragment]
         * to be previewed.
         *
         * @param file File to test if can be previewed.
         * @return 'True' if the file can be handled by the fragment.
         */
        @JvmStatic
        fun canBePreviewed(file: OCFile?): Boolean {
            return file != null && file.isAvailableLocally && file.isAudio
        }
    }
}