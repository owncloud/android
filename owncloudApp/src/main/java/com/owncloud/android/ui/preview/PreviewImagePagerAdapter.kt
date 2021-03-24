/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
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
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.files.services.FileDownloader
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.utils.FileStorageUtils
import timber.log.Timber
import java.util.HashMap
import java.util.HashSet

/**
 * Adapter class that provides Fragment instances
 */
//public class PreviewImagePagerAdapter extends PagerAdapter {
class PreviewImagePagerAdapter(
    fragmentManager: FragmentManager?, parentFolder: OCFile?,
    account: Account, storageManager: FileDataStorageManager?
) : FragmentStatePagerAdapter(
    fragmentManager!!
) {
    private var mImageFiles: MutableList<OCFile>
    private val mAccount: Account
    private val mObsoleteFragments: MutableSet<Any>
    private val mObsoletePositions: MutableSet<Int>
    private val mDownloadErrors: MutableSet<Int>
    private val mStorageManager: FileDataStorageManager
    private val mCachedFragments: MutableMap<Int, FileFragment>

    /**
     * Constructor.
     *
     * @param fragmentManager [FragmentManager] instance that will handle
     * the [Fragment]s provided by the adapter.
     * @param parentFolder    Folder where images will be searched for.
     * @param storageManager  Bridge to database.
     */
    init {
        requireNotNull(fragmentManager) { "NULL FragmentManager instance" }
        requireNotNull(parentFolder) { "NULL parent folder" }
        requireNotNull(storageManager) { "NULL storage manager" }
        mAccount = account
        mStorageManager = storageManager
        mImageFiles = mStorageManager.getFolderImages(parentFolder).toMutableList()
        mImageFiles = FileStorageUtils.sortFolder(
            mImageFiles, FileStorageUtils.mSortOrderFileDisp,
            FileStorageUtils.mSortAscendingFileDisp
        )
        mObsoleteFragments = HashSet()
        mObsoletePositions = HashSet()
        mDownloadErrors = HashSet()
        //mFragmentManager = fragmentManager;
        mCachedFragments = HashMap()
    }

    /**
     * Returns the image files handled by the adapter.
     *
     * @return A vector with the image files handled by the adapter.
     */
    fun getFileAt(position: Int): OCFile = mImageFiles[position]

    override fun getItem(i: Int): Fragment {
        val file = mImageFiles[i]
        val fragment: Fragment
        when {
            file.isAvailableLocally -> {
                fragment = PreviewImageFragment.newInstance(file, mAccount, mObsoletePositions.contains(i))
            }
            mDownloadErrors.contains(i) -> {
                fragment = FileDownloadFragment.newInstance(file, mAccount, true)
                (fragment as FileDownloadFragment).setError(true)
                mDownloadErrors.remove(i)
            }
            else -> {
                fragment = FileDownloadFragment.newInstance(file, mAccount, mObsoletePositions.contains(i))
            }
        }
        mObsoletePositions.remove(i)
        return fragment
    }

    fun getFilePosition(file: OCFile) = mImageFiles.indexOf(file)

    override fun getCount() = mImageFiles.size

    override fun getPageTitle(position: Int): CharSequence = mImageFiles[position].fileName

    private fun updateFile(position: Int, file: OCFile) {
        val fragmentToUpdate = mCachedFragments[position]
        if (fragmentToUpdate != null) {
            mObsoleteFragments.add(fragmentToUpdate)
        }
        mObsoletePositions.add(position)
        mImageFiles[position] = file
    }

    private fun updateWithDownloadError(position: Int) {
        val fragmentToUpdate = mCachedFragments[position]
        if (fragmentToUpdate != null) {
            mObsoleteFragments.add(fragmentToUpdate)
        }
        mDownloadErrors.add(position)
    }

    fun onTransferServiceConnected() {
        for (fragmentToUpdate in mCachedFragments.values) {
            fragmentToUpdate.onTransferServiceConnected()
        }
    }

    fun clearErrorAt(position: Int) {
        val fragmentToUpdate = mCachedFragments[position]
        if (fragmentToUpdate != null) {
            mObsoleteFragments.add(fragmentToUpdate)
        }
        mDownloadErrors.remove(position)
    }

    override fun getItemPosition(`object`: Any): Int {
        if (mObsoleteFragments.contains(`object`)) {
            mObsoleteFragments.remove(`object`)
            return POSITION_NONE
        }
        return super.getItemPosition(`object`)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = super.instantiateItem(container, position)
        mCachedFragments[position] = fragment as FileFragment
        return fragment
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        mCachedFragments.remove(position)
        super.destroyItem(container, position, `object`)
    }

    fun pendingErrorAt(position: Int): Boolean {
        return mDownloadErrors.contains(position)
    }

    /**
     * Reset the image zoom to default value for each CachedFragments
     */
    fun resetZoom() {
        val entries: Iterator<FileFragment> = mCachedFragments.values.iterator()
        while (entries.hasNext()) {
            val fileFragment = entries.next()
            if (fileFragment is PreviewImageFragment) {
                fileFragment.getImageView().setScale(1f, true)
            }
        }
    }

    fun onDownloadEvent(file: OCFile, action: String, success: Boolean) {
        val position = getFilePosition(file)
        if (position >= 0) {
            if (action == FileDownloader.getDownloadFinishMessage()) {
                if (success) {
                    updateFile(position, file)
                } else {
                    updateWithDownloadError(position)
                }
            }
            val fragment = mCachedFragments[position]
            if (fragment is FileDownloadFragment && success) {
                // trigger the creation of new PreviewImageFragment to replace current FileDownloadFragment
                // only if the download succeded. If not trigger an error
                notifyDataSetChanged()
            } else fragment?.onSyncEvent(action, success, null)
        } else {
            Timber.d("Download finished, but the fragment is offscreen")
        }
    }
}
