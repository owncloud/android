/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Christian Schabesberger
 * @author Abel García de Prada
 * @author Shashvat Kedia
 * @author David Crespo Rios
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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

package com.owncloud.android.files;

import android.accounts.Account;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.owncloud.android.R;
import com.owncloud.android.domain.availableoffline.model.AvailableOfflineStatus;
import com.owncloud.android.domain.capabilities.model.OCCapability;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.domain.files.model.OCFileSyncInfo;
import com.owncloud.android.domain.spaces.model.OCSpace;
import com.owncloud.android.extensions.WorkManagerExtKt;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.ui.preview.PreviewVideoFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.owncloud.android.usecases.transfers.TransferConstantsKt.TRANSFER_TAG_DOWNLOAD;

/**
 * Filters out the file actions available in a given {@link Menu} for a given {@link OCFile}
 * according to the current state of the latest.
 */
public class FileMenuFilter {

    private static final int SINGLE_SELECT_ITEMS = 1;
    private static final String TAG_SECOND_FRAGMENT = "SECOND_FRAGMENT";

    private List<OCFile> mFiles;
    private ComponentsGetter mComponentsGetter;
    private Account mAccount;
    private Context mContext;
    private List<OCFileSyncInfo> mFilesSync;

    /**
     * Constructor
     *
     * @param targetFiles List of {@link OCFile} file targets of the action to filter in the {@link Menu}.
     * @param account     ownCloud {@link Account} holding targetFile.
     * @param cg          Accessor to app components, needed to access synchronization services
     * @param context     Android {@link Context}, needed to access build setup resources.
     */
    public FileMenuFilter(List<OCFile> targetFiles, Account account, ComponentsGetter cg,
                          Context context) {
        mFiles = targetFiles;
        mAccount = account;
        mComponentsGetter = cg;
        mContext = context;
        mFilesSync = new ArrayList<>();
    }

    /**
     * Constructor
     *
     * @param targetFile {@link OCFile} target of the action to filter in the {@link Menu}.
     * @param account    ownCloud {@link Account} holding targetFile.
     * @param cg         Accessor to app components, needed to access synchronization services
     * @param context    Android {@link Context}, needed to access build setup resources.
     */
    public FileMenuFilter(OCFile targetFile, Account account, ComponentsGetter cg,
                          Context context) {
        this(Arrays.asList(targetFile), account, cg, context);
    }

    /**
     * Constructor
     *
     * @param targetFiles List of {@link OCFile} file targets of the action to filter in the {@link Menu}.
     * @param account    ownCloud {@link Account} holding targetFile.
     * @param cg         Accessor to app components, needed to access synchronization services
     * @param context    Android {@link Context}, needed to access build setup resources.
     * @param filesSync  List of {@link OCFileSyncInfo} with info about the sync status of each target file.
     */
    public FileMenuFilter(List<OCFile> targetFiles, Account account, ComponentsGetter cg,
                          Context context, List<OCFileSyncInfo> filesSync) {
        this(targetFiles, account, cg, context);
        mFilesSync = filesSync;
    }

    /**
     * Filters out the file actions available in the passed {@link Menu} taken into account
     * the state of the {@link OCFile} held by the filter.
     *
     * @param menu Options or context menu to filter.
     */
    public void filter(Menu menu, boolean displaySelectAll, boolean displaySelectInverse,
                       boolean onlyAvailableOffline, boolean sharedByLinkFiles, boolean hasWritePermission,
                       boolean hasDeletePermission) {
        if (mFiles == null || mFiles.size() <= 0) {
            hideAll(menu);

        } else {
            List<Integer> toShow = new ArrayList<>();
            List<Integer> toHide = new ArrayList<>();

            filter(toShow, toHide, displaySelectAll, displaySelectInverse, onlyAvailableOffline, sharedByLinkFiles,
                    hasDeletePermission);

            MenuItem item;
            for (int i : toShow) {
                item = menu.findItem(i);
                if (item != null) {
                    item.setVisible(true);
                    item.setEnabled(true);
                    if (i == R.id.action_open_file_with) {
                        if (!hasWritePermission) {
                            item.setTitle(R.string.actionbar_open_with_read_only);
                        } else {
                            item.setTitle(R.string.actionbar_open_with);
                        }
                    }
                }
            }

            for (int i : toHide) {
                item = menu.findItem(i);
                if (item != null) {
                    item.setVisible(false);
                    item.setEnabled(false);
                }
            }
        }
    }

    private void hideAll(Menu menu) {
        MenuItem item;
        for (int i = 0; i < menu.size(); i++) {
            item = menu.getItem(i);
            item.setVisible(false);
            item.setEnabled(false);
        }
    }

    /**
     * Performs the real filtering, to be applied in the {@link Menu} by the caller methods.
     * <p>
     * Decides what actions must be shown and hidden.
     *
     * @param toShow List to save the options that must be shown in the menu.
     * @param toHide List to save the options that must be shown in the menu.
     */

    private void filter(List<Integer> toShow, List<Integer> toHide, boolean displaySelectAll,
                        boolean displaySelectInverse, boolean onlyAvailableOffline, boolean sharedByLinkFiles,
                        boolean hasDeletePermission) {

        boolean synchronizing;
        if (mFilesSync.isEmpty()) {
            synchronizing = anyFileSynchronizingLookingIntoWorkers();
        } else {
            synchronizing = anyFileSynchronizingLookingIntoFilesSync();
        }

        boolean videoPreviewing = anyFileVideoPreviewing();

        boolean videoStreaming = !anyFileDown() && anyFileVideoPreviewing();

        /// decision is taken for each possible action on a file in the menu

        if (displaySelectAll) {
            toShow.add(R.id.file_action_select_all);
        } else {
            toHide.add(R.id.file_action_select_all);
        }
        if (displaySelectInverse) {
            toShow.add(R.id.action_select_inverse);
        } else {
            toHide.add(R.id.action_select_inverse);
        }

        // DOWNLOAD
        if (mFiles.isEmpty() || containsFolder() || anyFileDown() || synchronizing || videoPreviewing ||
                onlyAvailableOffline || sharedByLinkFiles) {
            toHide.add(R.id.action_download_file);

        } else {
            toShow.add(R.id.action_download_file);
        }

        // RENAME
        if (!isSingleSelection() || synchronizing || videoPreviewing || onlyAvailableOffline || sharedByLinkFiles) {
            toHide.add(R.id.action_rename_file);

        } else {
            toShow.add(R.id.action_rename_file);
        }

        // MOVE & COPY
        if (mFiles.isEmpty() || synchronizing || videoPreviewing || onlyAvailableOffline || sharedByLinkFiles) {
            toHide.add(R.id.action_move);
            toHide.add(R.id.action_copy);

        } else {
            toShow.add(R.id.action_move);
            toShow.add(R.id.action_copy);
        }

        // REMOVE
        if (mFiles.isEmpty() || synchronizing || onlyAvailableOffline || sharedByLinkFiles || !hasDeletePermission) {
            toHide.add(R.id.action_remove_file);
        } else {
            toShow.add(R.id.action_remove_file);
        }

        // OPEN WITH (different to preview!)
        if (!isSingleFile() || synchronizing) {
            toHide.add(R.id.action_open_file_with);
        } else {
            toShow.add(R.id.action_open_file_with);
        }

        // CANCEL SYNCHRONIZATION
        if (mFiles.isEmpty() || !synchronizing || anyFavorite() || onlyAvailableOffline || sharedByLinkFiles) {
            toHide.add(R.id.action_cancel_sync);

        } else {
            toShow.add(R.id.action_cancel_sync);
        }

        // SYNC CONTENTS (BOTH FILE AND FOLDER)
        if (mFiles.isEmpty() || (!anyFileDown() && !containsFolder()) || synchronizing || onlyAvailableOffline || sharedByLinkFiles) {
            toHide.add(R.id.action_sync_file);

        } else {
            toShow.add(R.id.action_sync_file);
        }

        // SHARE FILE
        boolean shareViaLinkAllowed = (mContext != null &&
                mContext.getResources().getBoolean(R.bool.share_via_link_feature));
        boolean shareWithUsersAllowed = (mContext != null &&
                mContext.getResources().getBoolean(R.bool.share_with_users_feature));

        OCCapability capability = mComponentsGetter.getStorageManager().getCapability(mAccount.name);

        boolean notAllowResharing = anyFileSharedWithMe() &&
                capability != null && capability.getFilesSharingResharing().isFalse();

        OCSpace space = mComponentsGetter.getStorageManager().getSpace(mFiles.get(0).getSpaceId(), mAccount.name);
        boolean notPersonalSpace = space != null && !space.isPersonal();

        if ((!shareViaLinkAllowed && !shareWithUsersAllowed) || !isSingleSelection() ||
                notAllowResharing || onlyAvailableOffline || notPersonalSpace) {
            toHide.add(R.id.action_share_file);
        } else {
            toShow.add(R.id.action_share_file);
        }

        // SEE DETAILS
        if (!isSingleFile()) {
            toHide.add(R.id.action_see_details);
        } else {
            toShow.add(R.id.action_see_details);
        }

        // SEND
        boolean sendAllowed = (mContext != null &&
                mContext.getString(R.string.send_files_to_other_apps).equalsIgnoreCase("on"));
        if (containsFolder() || (!areDownloaded() && !isSingleFile()) || !sendAllowed || synchronizing || videoStreaming || onlyAvailableOffline) {
            toHide.add(R.id.action_send_file);
        } else {
            toShow.add(R.id.action_send_file);
        }

        // SET AS AVAILABLE OFFLINE
        if (synchronizing || !anyUnfavorite() || videoStreaming) {
            toHide.add(R.id.action_set_available_offline);
        } else {
            toShow.add(R.id.action_set_available_offline);
        }

        // UNSET AS AVAILABLE OFFLINE
        if (!anyFavorite() || videoStreaming) {
            toHide.add(R.id.action_unset_available_offline);
        } else {
            toShow.add(R.id.action_unset_available_offline);
        }

    }

    private boolean anyFileSynchronizingLookingIntoWorkers() {
        boolean synchronizing = false;
        if (!mFiles.isEmpty() && mAccount != null) {
            WorkManager workManager = WorkManager.getInstance(mContext);
            List<WorkInfo> workInfos = WorkManagerExtKt.getRunningWorkInfosByTags(workManager, Arrays.asList(TRANSFER_TAG_DOWNLOAD, mAccount.name));
            for (int i = 0; !synchronizing && i < workInfos.size(); i++) {
                if (!workInfos.get(i).getState().isFinished()) {
                    for (int j = 0; !synchronizing && j < mFiles.size(); j++) {
                        synchronizing = workInfos.get(i).getTags().contains(mFiles.get(j).getId().toString());
                    }
                }
            }
        }
        return synchronizing;
    }

    private boolean anyFileSynchronizingLookingIntoFilesSync() {
        boolean synchronizing = false;
        if (!mFilesSync.isEmpty()) {
            for (int i = 0; !synchronizing && i < mFilesSync.size(); i++) {
                synchronizing = mFilesSync.get(i).isSynchronizing();
            }
        }
        return synchronizing;
    }

    private boolean anyFileVideoPreviewing() {
        final FragmentActivity activity = (FragmentActivity) mContext;
        Fragment secondFragment = activity.getSupportFragmentManager().findFragmentByTag(
                TAG_SECOND_FRAGMENT);
        boolean videoPreviewing = false;
        if (secondFragment instanceof PreviewVideoFragment) {
            for (int i = 0; !videoPreviewing && i < mFiles.size(); i++) {
                videoPreviewing = ((PreviewVideoFragment) secondFragment).
                        getFile().equals(mFiles.get(i));
            }
        }
        return videoPreviewing;
    }

    private boolean isSingleSelection() {
        return mFiles.size() == SINGLE_SELECT_ITEMS;
    }

    private boolean isSingleFile() {
        return isSingleSelection() && !mFiles.get(0).isFolder();
    }

    private boolean areDownloaded() {
        for (OCFile file : mFiles) {
            if (!file.isAvailableLocally()) {
                return false;
            }
        }
        return true;
    }

    private boolean containsFolder() {
        for (OCFile file : mFiles) {
            if (file.isFolder()) {
                return true;
            }
        }
        return false;
    }

    private boolean anyFileDown() {
        for (OCFile file : mFiles) {
            if (file.isAvailableLocally()) {
                return true;
            }
        }
        return false;
    }

    private boolean anyFavorite() {
        for (OCFile file : mFiles) {
            if (file.getAvailableOfflineStatus() == AvailableOfflineStatus.AVAILABLE_OFFLINE) {
                return true;
            }
        }
        return false;
    }

    private boolean anyUnfavorite() {
        for (OCFile file : mFiles) {
            if (file.getAvailableOfflineStatus() == AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE) {
                return true;
            }
        }
        return false;
    }

    private boolean anyFileSharedWithMe() {
        for (OCFile file : mFiles) {
            if (file.isSharedWithMe()) {
                return true;
            }
        }
        return false;
    }
}
