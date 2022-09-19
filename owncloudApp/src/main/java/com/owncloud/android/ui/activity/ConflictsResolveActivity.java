/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * Copyright (C) 2012 Bartek Przybylski
 * Copyright (C) 2016 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.os.Bundle;

import androidx.work.WorkManager;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.Decision;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.OnConflictDecisionMadeListener;
import com.owncloud.android.usecases.transfers.uploads.UploadFileInConflictUseCase;
import com.owncloud.android.usecases.transfers.uploads.UploadFilesFromSystemUseCase;
import com.owncloud.android.usecases.transfers.downloads.DownloadFileUseCase;
import kotlin.Lazy;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

import java.util.ArrayList;

import static org.koin.java.KoinJavaComponent.inject;

/**
 * Wrapper activity which will be launched if keep-in-sync file will be modified by external
 * application.
 */
 public class ConflictsResolveActivity extends FileActivity implements OnConflictDecisionMadeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void conflictDecisionMade(Decision decision) {

        boolean forceOverwrite = false;

        switch (decision) {
            case CANCEL:
                finish();
                return;
            case LOCAL:
                // use local version -> overwrite on server
                forceOverwrite = true;
                break;
            case KEEP_BOTH:
                break;
            case SERVER:
                // use server version -> delete local, request download
                @NotNull Lazy<DownloadFileUseCase> downloadFileUseCase = inject(DownloadFileUseCase.class);
                DownloadFileUseCase.Params downloadFileParams = new DownloadFileUseCase.Params(getAccount().name, getFile());
                downloadFileUseCase.getValue().execute(downloadFileParams);
                finish();
                return;
            default:
                Timber.e("Unhandled conflict decision %s", decision);
                return;
        }

        WorkManager workManager = WorkManager.getInstance(getApplicationContext());
        if (forceOverwrite) {
            @NotNull Lazy<UploadFileInConflictUseCase> uploadFileInConflictUseCaseLazy = inject(UploadFileInConflictUseCase.class);
            UploadFileInConflictUseCase uploadFileInConflictUseCase = uploadFileInConflictUseCaseLazy.getValue();
            UploadFileInConflictUseCase.Params params = new UploadFileInConflictUseCase.Params(
                    getFile().getOwner(),
                    getFile().getStoragePath(),
                    getFile().getParentRemotePath()
            );
            uploadFileInConflictUseCase.execute(params);
        } else {
            @NotNull Lazy<UploadFilesFromSystemUseCase> uploadFilesFromSystemUseCaseLazy = inject(UploadFilesFromSystemUseCase.class);
            UploadFilesFromSystemUseCase uploadFilesFromSystemUseCase = uploadFilesFromSystemUseCaseLazy.getValue();
            ArrayList<String> listOfPaths = new ArrayList<>();
            listOfPaths.add(getFile().getStoragePath());
            UploadFilesFromSystemUseCase.Params params = new UploadFilesFromSystemUseCase.Params(
                    getFile().getOwner(),
                    listOfPaths,
                    getFile().getParentRemotePath()
            );
            uploadFilesFromSystemUseCase.execute(params);
        }
        finish();
    }

    @Override
    protected void onAccountSet(boolean stateWasRecovered) {
        super.onAccountSet(stateWasRecovered);
        if (getAccount() != null) {
            OCFile file = getFile();
            if (getFile() == null) {
                Timber.e("No conflictive file received");
                finish();
            } else {
                /// Check whether the 'main' OCFile handled by the Activity is contained in the current Account
                file = getStorageManager().getFileByPath(file.getRemotePath());   // file = null if not in the
                // current Account
                if (file != null) {
                    setFile(file);
                    ConflictsResolveDialog d = ConflictsResolveDialog.newInstance(file.getRemotePath(), this);
                    d.showDialog(this);

                } else {
                    // account was changed to a different one - just finish
                    finish();
                }
            }

        } else {
            finish();
        }

    }
}
