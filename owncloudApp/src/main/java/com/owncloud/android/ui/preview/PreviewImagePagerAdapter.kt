/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
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
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.files.services.FileDownloader
import com.owncloud.android.ui.fragment.FileFragment
import timber.log.Timber
import java.util.HashMap
import java.util.HashSet

/**
 * Adapter class that provides Fragment instances
 */
class PreviewImagePagerAdapter(
    fragmentManager: FragmentManager,
    private val account: Account,
    private val mImageFiles: MutableList<OCFile>
) : FragmentStatePagerAdapter(
    fragmentManager
) {
    private val mObsoleteFragments: MutableSet<Any>
    private val mObsoletePositions: MutableSet<Int>
    private val mDownloadErrors: MutableSet<Int>
    private val mCachedFragments: MutableMap<Int, FileFragment>

    init {
        mObsoleteFragments = HashSet()
        mObsoletePositions = HashSet()
        mDownloadErrors = HashSet()
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
                fragment = PreviewImageFragment.newInstance(file, account, mObsoletePositions.contains(i))
            }
            mDownloadErrors.contains(i) -> {
                fragment = FileDownloadFragment.newInstance(file, account, true)
                (fragment as FileDownloadFragment).setError(true)
                mDownloadErrors.remove(i)
            }
            else -> {
                fragment = FileDownloadFragment.newInstance(file, account, mObsoletePositions.contains(i))
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

    fun pendingErrorAt(position: Int) = mDownloadErrors.contains(position)

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
