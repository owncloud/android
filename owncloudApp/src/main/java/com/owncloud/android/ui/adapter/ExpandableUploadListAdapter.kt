/**
 * ownCloud Android client application
 *
 * @author LukeOwncloud
 * @author masensio
 * @author Christian Schabesberger
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
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
package com.owncloud.android.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.database.DataSetObserver
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.owncloud.android.R
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.OCUpload
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus
import com.owncloud.android.db.UploadResult
import com.owncloud.android.files.services.TransferRequester
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimetypeIconUtil
import com.owncloud.android.utils.PreferenceUtils
import java.io.File
import java.lang.ref.WeakReference
import java.util.Comparator
import java.util.Observable
import java.util.Observer

/**
 * This Adapter populates a ListView with following types of uploads: pending,
 * active, completed. Filtering possible.
 *
 */
class ExpandableUploadListAdapter(private val parentActivity: FileActivity) :
    BaseExpandableListAdapter(), Observer {

    private val uploadsStorageManager: UploadsStorageManager

    private var progressListener: ProgressListener? = null

    private var uploadGroups = arrayListOf<UploadGroup>()

    internal interface Refresh {
        fun refresh()
    }

    internal abstract inner class UploadGroup(var groupName: String) : Refresh {
        var items = arrayListOf<OCUpload>()

        val groupCount: Int
            get() = items.size

        var comparator: Comparator<OCUpload> = object : Comparator<OCUpload> {

            override fun compare(upload1: OCUpload, upload2: OCUpload): Int {
                if (upload1.uploadStatus == UploadStatus.UPLOAD_IN_PROGRESS) {
                    if (upload2.uploadStatus != UploadStatus.UPLOAD_IN_PROGRESS) {
                        return -1
                    }
                    // both are in progress
                    val binder = parentActivity.fileUploaderBinder
                    if (binder != null) {
                        if (binder.isUploadingNow(upload1)) {
                            return -1
                        } else if (binder.isUploadingNow(upload2)) {
                            return 1
                        }
                    }
                } else if (upload2.uploadStatus == UploadStatus.UPLOAD_IN_PROGRESS) {
                    return 1
                }
                return if (upload1.uploadEndTimestamp == 0L || upload2.uploadEndTimestamp == 0L) {
                    compareUploadId(upload1, upload2)
                } else {
                    compareUpdateTime(upload1, upload2)
                }
            }

            private fun compareUploadId(upload1: OCUpload, upload2: OCUpload): Int {
                return java.lang.Long.valueOf(upload1.uploadId).compareTo(upload2.uploadId)
            }

            private fun compareUpdateTime(upload1: OCUpload, upload2: OCUpload): Int {
                return java.lang.Long.valueOf(upload2.uploadEndTimestamp)
                    .compareTo(upload1.uploadEndTimestamp)
            }
        }

        abstract val groupIcon: Int

    }

    init {
        Log_OC.d(TAG, "ExpandableUploadListAdapter")
        uploadsStorageManager = UploadsStorageManager(parentActivity.contentResolver)
        uploadGroups.add(object :
            UploadGroup(parentActivity.getString(R.string.uploads_view_group_current_uploads)) {

            override val groupIcon: Int
                get() = R.drawable.upload_in_progress

            override fun refresh() {
                items = listOf(*uploadsStorageManager.currentAndPendingUploads)
                    .toCollection(ArrayList())
                items.sortWith(comparator)
            }
        }
        )
        uploadGroups.add(object :
            UploadGroup(parentActivity.getString(R.string.uploads_view_group_failed_uploads)) {

            override val groupIcon: Int
                get() = R.drawable.upload_failed

            override fun refresh() {
                items = listOf(*uploadsStorageManager.failedButNotDelayedForWifiUploads)
                    .toCollection(ArrayList())
                items.sortWith(comparator)
            }

        }
        )
        uploadGroups.add(object :
            UploadGroup(parentActivity.getString(R.string.uploads_view_group_finished_uploads)) {

            override val groupIcon: Int
                get() = R.drawable.upload_finished

            override fun refresh() {
                items = listOf(*uploadsStorageManager.finishedUploads)
                    .toCollection(ArrayList())
                items.sortWith(comparator)
            }
        }
        )
        loadUploadItemsFromDb()
    }

    override fun registerDataSetObserver(observer: DataSetObserver) {
        super.registerDataSetObserver(observer)
        uploadsStorageManager.addObserver(this)
        Log_OC.d(TAG, "registerDataSetObserver")
    }

    override fun unregisterDataSetObserver(observer: DataSetObserver) {
        super.unregisterDataSetObserver(observer)
        uploadsStorageManager.deleteObserver(this)
        Log_OC.d(TAG, "unregisterDataSetObserver")
    }

    override fun areAllItemsEnabled(): Boolean {
        return true
    }

    @SuppressLint("SetTextI18n")
    private fun getView(
        uploadsItems: ArrayList<OCUpload>,
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        var view = convertView
        if (view == null) {
            val inflater = parentActivity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE
            ) as LayoutInflater
            view = inflater.inflate(R.layout.upload_list_item, parent, false)
            // Allow or disallow touches with other visible windows
            view!!.filterTouchesWhenObscured =
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(parentActivity)
        }

        if (uploadsItems.size > position) {
            val upload = uploadsItems[position]

            // local file name
            val fileTextView = view.findViewById<TextView>(R.id.upload_name)
            val remoteFile = File(upload.remotePath)
            var fileName = remoteFile.name
            if (fileName.isEmpty()) {
                fileName = File.separator
            }
            fileTextView.text = fileName

            // remote path to parent folder
            val pathTextView = view.findViewById<TextView>(R.id.upload_remote_path)
            var remoteParentPath = upload.remotePath
            remoteParentPath = File(remoteParentPath).parent
            pathTextView.text = parentActivity.getString(R.string.app_name) + remoteParentPath

            // file size
            val fileSizeTextView = view.findViewById<TextView>(R.id.upload_file_size)
            fileSizeTextView.text = DisplayUtils.bytesToHumanReadable(
                upload.fileSize,
                parentActivity
            ) + ", "

            //* upload date
            val uploadDateTextView = view.findViewById<TextView>(R.id.upload_date)
            val updateTime = upload.uploadEndTimestamp
            val dateString = DisplayUtils.getRelativeDateTimeString(
                parentActivity,
                updateTime,
                DateUtils.SECOND_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                0
            )
            uploadDateTextView.text = dateString

            val accountNameTextView = view.findViewById<TextView>(R.id.upload_account)
            try {
                val account =
                    AccountUtils.getOwnCloudAccountByName(parentActivity, upload.accountName)
                val oca = OwnCloudAccount(account, parentActivity)
                accountNameTextView.text = oca.displayName + " @ " +
                        DisplayUtils.convertIdn(
                            account!!.name.substring(account.name.lastIndexOf("@") + 1),
                            false
                        )
            } catch (e: Exception) {
                Log_OC.w(TAG, "Couldn't get display name for account, using old style")
                accountNameTextView.text = upload.accountName
            }

            val statusTextView = view.findViewById<TextView>(R.id.upload_status)

            val progressBar = view.findViewById<ProgressBar>(R.id.upload_progress_bar)

            /// Reset fields visibility
            uploadDateTextView.visibility = View.VISIBLE
            pathTextView.visibility = View.VISIBLE
            fileSizeTextView.visibility = View.VISIBLE
            accountNameTextView.visibility = View.VISIBLE
            statusTextView.visibility = View.VISIBLE
            progressBar.visibility = View.GONE

            /// Update information depending of upload details
            val status = getStatusText(upload)
            when (upload.uploadStatus) {
                UploadStatus.UPLOAD_IN_PROGRESS -> {
                    progressBar.progress = 0
                    progressBar.visibility = View.VISIBLE

                    val binder = parentActivity.fileUploaderBinder
                    if (binder != null) {
                        if (binder.isUploadingNow(upload)) {
                            /// really uploading, so...
                            /// ... unbind the old progress bar, if any; ...
                            if (progressListener != null) {
                                binder.removeDatatransferProgressListener(
                                    progressListener,
                                    progressListener!!.upload   // the one that was added
                                )
                            }
                            /// ... then, bind the current progress bar to listen for updates
                            progressListener = ProgressListener(upload, progressBar)
                            binder.addDatatransferProgressListener(
                                progressListener,
                                upload
                            )

                        } else {
                            /// not really uploading; stop listening progress if view is reused!
                            if (convertView != null &&
                                progressListener != null &&
                                progressListener!!.isWrapping(progressBar)
                            ) {
                                binder.removeDatatransferProgressListener(
                                    progressListener,
                                    progressListener!!.upload   // the one that was added
                                )
                                progressListener = null
                            }
                        }
                    } else {
                        Log_OC.w(
                            TAG,
                            "FileUploaderBinder not ready yet for upload " + upload.remotePath
                        )
                    }
                    uploadDateTextView.visibility = View.GONE
                    pathTextView.visibility = View.GONE
                    progressBar.invalidate()
                }
                UploadStatus.UPLOAD_FAILED -> uploadDateTextView.visibility = View.GONE
                UploadStatus.UPLOAD_SUCCEEDED -> statusTextView.visibility = View.GONE
                else -> Unit
            }
            statusTextView.text = status

            /// bind listeners to perform actions
            val rightButton = view.findViewById<ImageButton>(R.id.upload_right_button)
            when {
                upload.uploadStatus == UploadStatus.UPLOAD_IN_PROGRESS -> {
                    //Cancel
                    rightButton.setImageResource(R.drawable.ic_action_cancel_grey)
                    rightButton.visibility = View.VISIBLE
                    rightButton.setOnClickListener {
                        val uploaderBinder = parentActivity.fileUploaderBinder
                        if (uploaderBinder != null) {
                            uploaderBinder.cancel(upload)
                            refreshView()
                        }
                    }
                }
                upload.uploadStatus == UploadStatus.UPLOAD_FAILED -> {
                    //Delete
                    rightButton.setImageResource(R.drawable.ic_action_delete_grey)
                    rightButton.visibility = View.VISIBLE
                    rightButton.setOnClickListener {
                        uploadsStorageManager.removeUpload(upload)
                        refreshView()
                    }
                }
                else -> // UploadStatus.UPLOAD_SUCCESS
                    rightButton.visibility = View.INVISIBLE
            }

            // retry
            if (upload.uploadStatus == UploadStatus.UPLOAD_FAILED) {
                if (UploadResult.CREDENTIAL_ERROR == upload.lastResult) {
                    view.setOnClickListener {
                        parentActivity.fileOperationsHelper.checkCurrentCredentials(
                            upload.getAccount(parentActivity)
                        )
                    }
                } else {
                    // not a credentials error
                    view.setOnClickListener { v ->
                        val file = File(upload.localPath)
                        if (file.exists()) {
                            val requester = TransferRequester()
                            requester.retry(parentActivity, upload, false)
                            refreshView()
                        } else {
                            val message = String.format(
                                parentActivity.getString(R.string.local_file_not_found_toast)
                            )
                            val snackBar = Snackbar.make(
                                v.rootView.findViewById(android.R.id.content),
                                message,
                                Snackbar.LENGTH_LONG
                            )
                            snackBar.show()
                        }
                    }
                }
            } else {
                view.setOnClickListener(null)
            }

            /// Set icon or thumbnail
            val fileIcon = view.findViewById<ImageView>(R.id.thumbnail)
            fileIcon.setImageResource(R.drawable.file)

            /** Cancellation needs do be checked and done before changing the drawable in fileIcon, or
             * [ThumbnailsCacheManager.cancelPotentialWork] will NEVER cancel any task.
             */
            @Suppress("KDocUnresolvedReference")
            val fakeFileToCheatThumbnailsCacheManagerInterface = OCFile(upload.remotePath)
            fakeFileToCheatThumbnailsCacheManagerInterface.storagePath = upload.localPath
            fakeFileToCheatThumbnailsCacheManagerInterface.mimetype = upload.mimeType

            val allowedToCreateNewThumbnail = ThumbnailsCacheManager.cancelPotentialThumbnailWork(
                fakeFileToCheatThumbnailsCacheManagerInterface,
                fileIcon
            )

            // TODO this code is duplicated; refactor to a common place
            if (fakeFileToCheatThumbnailsCacheManagerInterface.isImage
                && fakeFileToCheatThumbnailsCacheManagerInterface.remoteId != null &&
                upload.uploadStatus == UploadStatus.UPLOAD_SUCCEEDED
            ) {
                // Thumbnail in Cache?
                var thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    fakeFileToCheatThumbnailsCacheManagerInterface.remoteId.toString()
                )
                if (thumbnail != null && !fakeFileToCheatThumbnailsCacheManagerInterface.needsUpdateThumbnail()) {
                    fileIcon.setImageBitmap(thumbnail)
                } else {
                    // generate new Thumbnail
                    if (allowedToCreateNewThumbnail) {
                        val task = ThumbnailsCacheManager.ThumbnailGenerationTask(
                            fileIcon, parentActivity.storageManager, parentActivity.account
                        )
                        if (thumbnail == null) {
                            thumbnail = ThumbnailsCacheManager.mDefaultImg
                        }
                        val asyncDrawable = ThumbnailsCacheManager.AsyncThumbnailDrawable(
                            parentActivity.resources,
                            thumbnail,
                            task
                        )
                        fileIcon.setImageDrawable(asyncDrawable)
                        task.execute(fakeFileToCheatThumbnailsCacheManagerInterface)
                    }
                }

                if ("image/png" == upload.mimeType) {
                    fileIcon.setBackgroundColor(
                        ContextCompat.getColor(parentActivity, R.color.background_color)
                    )
                }

            } else if (fakeFileToCheatThumbnailsCacheManagerInterface.isImage) {
                val file = File(upload.localPath)
                // Thumbnail in Cache?
                var thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    file.hashCode().toString()
                )
                if (thumbnail != null) {
                    fileIcon.setImageBitmap(thumbnail)
                } else {
                    // generate new Thumbnail
                    if (allowedToCreateNewThumbnail) {
                        val task = ThumbnailsCacheManager.ThumbnailGenerationTask(fileIcon)
                        thumbnail = ThumbnailsCacheManager.mDefaultImg
                        val asyncDrawable = ThumbnailsCacheManager.AsyncThumbnailDrawable(
                            parentActivity.resources,
                            thumbnail,
                            task
                        )
                        fileIcon.setImageDrawable(asyncDrawable)
                        task.execute(file)
                        Log_OC.v(TAG, "Executing task to generate a new thumbnail")
                    }
                }

                if ("image/png".equals(upload.mimeType, ignoreCase = true)) {
                    fileIcon.setBackgroundColor(
                        ContextCompat.getColor(parentActivity, R.color.background_color)
                    )
                }
            } else {
                fileIcon.setImageResource(
                    MimetypeIconUtil.getFileTypeIconId(
                        upload.mimeType,
                        fileName
                    )
                )
            }
        }

        return view
    }

    /**
     * Gets the status text to show to the user according to the status and last result of the
     * the given upload.
     *
     * @param upload        Upload to describe.
     * @return Text describing the status of the given upload.
     */
    private fun getStatusText(upload: OCUpload): String {

        var status: String
        when (upload.uploadStatus) {

            UploadStatus.UPLOAD_IN_PROGRESS -> {
                status = parentActivity.getString(R.string.uploads_view_later_waiting_to_upload)
                val binder = parentActivity.fileUploaderBinder
                if (binder != null && binder.isUploadingNow(upload)) {
                    /// really uploading, bind the progress bar to listen for progress updates
                    status = parentActivity.getString(R.string.uploader_upload_in_progress_ticker)
                }
            }

            UploadStatus.UPLOAD_SUCCEEDED -> status =
                parentActivity.getString(R.string.uploads_view_upload_status_succeeded)

            UploadStatus.UPLOAD_FAILED -> when (upload.lastResult) {
                UploadResult.CREDENTIAL_ERROR -> status = parentActivity.getString(
                    R.string.uploads_view_upload_status_failed_credentials_error
                )
                UploadResult.FOLDER_ERROR -> status = parentActivity.getString(
                    R.string.uploads_view_upload_status_failed_folder_error
                )
                UploadResult.FILE_NOT_FOUND -> status = parentActivity.getString(
                    R.string.uploads_view_upload_status_failed_localfile_error
                )
                UploadResult.FILE_ERROR -> status = parentActivity.getString(
                    R.string.uploads_view_upload_status_failed_file_error
                )
                UploadResult.PRIVILEDGES_ERROR -> status = parentActivity.getString(
                    R.string.uploads_view_upload_status_failed_permission_error
                )
                UploadResult.NETWORK_CONNECTION -> status = parentActivity.getString(
                    R.string.uploads_view_upload_status_failed_connection_error
                )
                UploadResult.DELAYED_FOR_WIFI -> status = parentActivity.getString(
                    R.string.uploads_view_upload_status_waiting_for_wifi
                )
                UploadResult.CONFLICT_ERROR -> status = parentActivity.getString(
                    R.string.uploads_view_upload_status_conflict
                )
                UploadResult.SERVICE_INTERRUPTED -> status = parentActivity.getString(
                    R.string.uploads_view_upload_status_service_interrupted
                )
                UploadResult.SERVICE_UNAVAILABLE -> status =
                    parentActivity.getString(R.string.service_unavailable)
                UploadResult.QUOTA_EXCEEDED -> status =
                    parentActivity.getString(R.string.failed_upload_quota_exceeded_text)
                UploadResult.SSL_RECOVERABLE_PEER_UNVERIFIED -> status = parentActivity.getString(
                    R.string.ssl_certificate_not_trusted
                )
                UploadResult.UNKNOWN -> status = parentActivity.getString(
                    R.string.uploads_view_upload_status_unknown_fail
                )
                UploadResult.CANCELLED ->
                    // should not get here ; cancelled uploads should be wiped out
                    status = parentActivity.getString(
                        R.string.uploads_view_upload_status_cancelled
                    )
                UploadResult.UPLOADED ->
                    // should not get here ; status should be UPLOAD_SUCCESS
                    status =
                        parentActivity.getString(R.string.uploads_view_upload_status_succeeded)
                UploadResult.SPECIFIC_FORBIDDEN ->
                    // We don't know the specific forbidden error message because it is not being
                    // saved in uploads storage
                    status =
                        String.format(parentActivity.getString(R.string.uploader_upload_forbidden))
                UploadResult.SPECIFIC_SERVICE_UNAVAILABLE ->
                    // We don't know the specific unavailable service error message because
                    // it is not being saved in uploads storage
                    status = parentActivity.getString(R.string.service_unavailable)
                UploadResult.SPECIFIC_UNSUPPORTED_MEDIA_TYPE ->
                    // We don't know the specific unsupported media type error message because
                    // it is not being saved in uploads storage
                    status = parentActivity.getString(R.string.uploads_view_unsupported_media_type)
                else -> status =
                    "Naughty devs added a new fail result but no description for the user"
            }

            else -> status = "Uncontrolled status: " + upload.uploadStatus.toString()
        }
        return status
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    /**
     * Load upload items from [UploadsStorageManager].
     */
    private fun loadUploadItemsFromDb() {
        Log_OC.d(TAG, "loadUploadItemsFromDb")

        uploadGroups.forEach {
            it.refresh()
        }

        notifyDataSetChanged()
    }

    override fun update(arg0: Observable, arg1: Any) {
        Log_OC.d(TAG, "update")
        loadUploadItemsFromDb()
    }

    fun refreshView() {
        Log_OC.d(TAG, "refreshView")
        loadUploadItemsFromDb()
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return uploadGroups[getGroupId(groupPosition).toInt()].items[childPosition]
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun getChildView(
        groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?,
        parent: ViewGroup
    ): View {
        return getView(
            uploadGroups[getGroupId(groupPosition).toInt()].items,
            childPosition,
            convertView,
            parent
        )
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return uploadGroups[getGroupId(groupPosition).toInt()].items.size
    }

    override fun getGroup(groupPosition: Int): Any {
        return uploadGroups[getGroupId(groupPosition).toInt()]
    }

    override fun getGroupCount(): Int {
        var size = 0
        for (uploadGroup in uploadGroups) {
            if (uploadGroup.items.size > 0) {
                size++
            }
        }
        return size
    }

    /**
     * Returns the groupId (that is, index in uploadGroups) for group at position groupPosition (0-based).
     * Could probably be done more intuitive but this tested methods works as intended.
     */
    override fun getGroupId(groupPosition: Int): Long {
        var id = -1
        var i = 0
        while (i <= groupPosition) {
            id++
            if (uploadGroups[id].items.size > 0) {
                i++
            }
        }
        return id.toLong()
    }

    @SuppressLint("InflateParams")
    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        var convertViewLocal = convertView
        //force group to stay unfolded
        val listView = parent as ExpandableListView
        listView.expandGroup(groupPosition)

        listView.setGroupIndicator(null)
        val group = getGroup(groupPosition) as UploadGroup
        if (convertView == null) {
            val inflater =
                parentActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertViewLocal = inflater.inflate(R.layout.upload_list_group, null)

            // Allow or disallow touches with other visible windows
            convertViewLocal!!.filterTouchesWhenObscured =
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(parentActivity)
        }
        val tvGroupName = convertViewLocal?.findViewById<TextView>(R.id.uploadListGroupName)
        val tvFileCount = convertViewLocal?.findViewById<TextView>(R.id.textViewFileCount)

        val stringResFileCount = if (group.groupCount == 1)
            R.string.uploads_view_group_file_count_single
        else
            R.string.uploads_view_group_file_count
        val fileCountText =
            String.format(parentActivity.getString(stringResFileCount), group.groupCount)

        tvGroupName?.text = group.groupName
        tvFileCount?.text = fileCountText
        return convertViewLocal!!
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    inner class ProgressListener(upload: OCUpload, progressBar: ProgressBar) :
        OnDatatransferProgressListener {
        private var lastPercent = 0
        var upload: OCUpload? = null
            internal set
        private var weakProgressBar: WeakReference<ProgressBar>? = null

        init {
            this.upload = upload
            weakProgressBar = WeakReference(progressBar)
        }

        override fun onTransferProgress(
            progressRate: Long,
            totalTransferredSoFar: Long,
            totalToTransfer: Long,
            filename: String
        ) {
            val percent =
                (100.0 * totalTransferredSoFar.toDouble() / totalToTransfer.toDouble()).toInt()
            if (percent != lastPercent) {
                val pb = weakProgressBar!!.get()
                if (pb != null) {
                    pb.progress = percent
                    pb.postInvalidate()
                }
            }
            lastPercent = percent
        }

        fun isWrapping(progressBar: ProgressBar): Boolean {
            val wrappedProgressBar = weakProgressBar!!.get()
            return wrappedProgressBar != null && wrappedProgressBar === progressBar   // on purpose; don't replace with equals
        }

    }

    fun addBinder() {
        notifyDataSetChanged()
    }

    companion object {

        private val TAG = ExpandableUploadListAdapter::class.java.simpleName
    }
}
