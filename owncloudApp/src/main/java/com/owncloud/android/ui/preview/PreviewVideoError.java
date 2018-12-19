/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   @author David González Verdugo
 *   Copyright (C) 2016 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.ui.preview;

/**
 * Models the preview video error
 */

public class PreviewVideoError {

    private String errorMessage;

    // Error should trigger the preview file synchronization
    private boolean fileSyncNeeded;

    // Error should trigger the preview file parent folder synchronization
    private boolean parentFolderSyncNeeded;


    public PreviewVideoError(String errorMessage, boolean fileSyncNeeded,
                                boolean parentFolderSyncNeeded) {

        this.errorMessage = errorMessage;
        this.fileSyncNeeded = fileSyncNeeded;
        this.parentFolderSyncNeeded = parentFolderSyncNeeded;
    }


    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isFileSyncNeeded() {
        return fileSyncNeeded;
    }

    public void setFileSyncNeeded(boolean fileSyncNeeded) {
        this.fileSyncNeeded = fileSyncNeeded;
    }

    public boolean isParentFolderSyncNeeded() {
        return parentFolderSyncNeeded;
    }

    public void setParentFolderSyncNeeded(boolean parentFolderSyncNeeded) {
        this.parentFolderSyncNeeded = parentFolderSyncNeeded;
    }
}
