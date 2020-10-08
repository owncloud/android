/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * Copyright (C) 2020 ownCloud GmbH.
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
package com.owncloud.android.ui.preview;

import android.accounts.Account;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.FileStorageUtils;
import timber.log.Timber;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapter class that provides Fragment instances
 */
//public class PreviewImagePagerAdapter extends PagerAdapter {
public class PreviewImagePagerAdapter extends FragmentStatePagerAdapter {

    private List<OCFile> mImageFiles;
    private Account mAccount;
    private Set<Object> mObsoleteFragments;
    private Set<Integer> mObsoletePositions;
    private Set<Integer> mDownloadErrors;
    private FileDataStorageManager mStorageManager;

    private Map<Integer, FileFragment> mCachedFragments;

    /**
     * Constructor.
     *
     * @param fragmentManager {@link FragmentManager} instance that will handle
     *                        the {@link Fragment}s provided by the adapter.
     * @param parentFolder    Folder where images will be searched for.
     * @param storageManager  Bridge to database.
     */
    public PreviewImagePagerAdapter(FragmentManager fragmentManager, OCFile parentFolder,
                                    Account account, FileDataStorageManager storageManager) {
        super(fragmentManager);

        if (fragmentManager == null) {
            throw new IllegalArgumentException("NULL FragmentManager instance");
        }
        if (parentFolder == null) {
            throw new IllegalArgumentException("NULL parent folder");
        }
        if (storageManager == null) {
            throw new IllegalArgumentException("NULL storage manager");
        }

        mAccount = account;
        mStorageManager = storageManager;
        mImageFiles = mStorageManager.getFolderImages(parentFolder);

        mImageFiles = FileStorageUtils.sortFolder(mImageFiles, FileStorageUtils.mSortOrderFileDisp,
                FileStorageUtils.mSortAscendingFileDisp);

        mObsoleteFragments = new HashSet<>();
        mObsoletePositions = new HashSet<>();
        mDownloadErrors = new HashSet<>();
        //mFragmentManager = fragmentManager;
        mCachedFragments = new HashMap<>();
    }

    /**
     * Returns the image files handled by the adapter.
     *
     * @return A vector with the image files handled by the adapter.
     */
    protected OCFile getFileAt(int position) {
        return mImageFiles.get(position);
    }

    public Fragment getItem(int i) {
        OCFile file = mImageFiles.get(i);
        Fragment fragment;
        if (file.isDown()) {
            fragment = PreviewImageFragment.newInstance(
                    file,
                    mAccount,
                    mObsoletePositions.contains(i)
            );

        } else if (mDownloadErrors.contains(i)) {
            fragment = FileDownloadFragment.newInstance(file, mAccount, true);
            ((FileDownloadFragment) fragment).setError(true);
            mDownloadErrors.remove(i);

        } else {
            fragment = FileDownloadFragment.newInstance(
                    file, mAccount, mObsoletePositions.contains(i)
            );
        }
        mObsoletePositions.remove(i);
        return fragment;
    }

    public int getFilePosition(OCFile file) {
        return mImageFiles.indexOf(file);
    }

    @Override
    public int getCount() {
        return mImageFiles.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mImageFiles.get(position).getName();
    }

    private void updateFile(int position, OCFile file) {
        FileFragment fragmentToUpdate = mCachedFragments.get(position);
        if (fragmentToUpdate != null) {
            mObsoleteFragments.add(fragmentToUpdate);
        }
        mObsoletePositions.add(position);
        mImageFiles.set(position, file);
    }

    private void updateWithDownloadError(int position) {
        FileFragment fragmentToUpdate = mCachedFragments.get(position);
        if (fragmentToUpdate != null) {
            mObsoleteFragments.add(fragmentToUpdate);
        }
        mDownloadErrors.add(position);
    }

    public void onTransferServiceConnected() {
        for (FileFragment fragmentToUpdate : mCachedFragments.values()) {
            if (fragmentToUpdate != null) {
                fragmentToUpdate.onTransferServiceConnected();
            }
        }
    }

    public void clearErrorAt(int position) {
        FileFragment fragmentToUpdate = mCachedFragments.get(position);
        if (fragmentToUpdate != null) {
            mObsoleteFragments.add(fragmentToUpdate);
        }
        mDownloadErrors.remove(position);
    }

    @Override
    public int getItemPosition(Object object) {
        if (mObsoleteFragments.contains(object)) {
            mObsoleteFragments.remove(object);
            return POSITION_NONE;
        }
        return super.getItemPosition(object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Object fragment = super.instantiateItem(container, position);
        mCachedFragments.put(position, (FileFragment) fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        mCachedFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    public boolean pendingErrorAt(int position) {
        return mDownloadErrors.contains(position);
    }

    /**
     * Reset the image zoom to default value for each CachedFragments
     */
    public void resetZoom() {
        Iterator<FileFragment> entries = mCachedFragments.values().iterator();
        while (entries.hasNext()) {
            FileFragment fileFragment = entries.next();
            if (fileFragment instanceof PreviewImageFragment) {
                ((PreviewImageFragment) fileFragment).getImageView().setScale(1, true);
            }
        }
    }

    public void onDownloadEvent(OCFile file, String action, boolean success) {
        int position = getFilePosition(file);
        if (position >= 0) {
            if (action.equals(FileDownloader.getDownloadFinishMessage())) {
                if (success) {
                    updateFile(position, file);

                } else {
                    updateWithDownloadError(position);
                }
            }
            FileFragment fragment = mCachedFragments.get(position);
            if (fragment instanceof FileDownloadFragment && success) {
                // trigger the creation of new PreviewImageFragment to replace current FileDownloadFragment
                // only if the download succeded. If not trigger an error
                notifyDataSetChanged();
            } else if (fragment != null) {
                fragment.onSyncEvent(action, success, null);
            }
        } else {
            Timber.d("Download finished, but the fragment is offscreen");
        }
    }
}
