/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * @author Abel García de Prada
 * Copyright (C) 2011  Bartek Przybylski
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

package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.snackbar.Snackbar;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.lib.common.network.CertificateCombinedException;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.operations.RenameFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.operations.SynchronizeFolderOperation;
import com.owncloud.android.presentation.ui.authentication.AuthenticatorConstants;
import com.owncloud.android.presentation.ui.authentication.LoginActivity;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.services.OperationsService.OperationsServiceBinder;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog;
import com.owncloud.android.ui.errorhandling.ErrorMessageAdapter;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import timber.log.Timber;

/**
 * Activity with common behaviour for activities handling {@link OCFile}s in ownCloud {@link Account}s .
 */
public class FileActivity extends DrawerActivity
        implements OnRemoteOperationListener, ComponentsGetter, SslUntrustedCertDialog.OnSslUntrustedCertListener {

    public static final String EXTRA_FILE = "com.owncloud.android.ui.activity.FILE";
    public static final String EXTRA_ACCOUNT = "com.owncloud.android.ui.activity.ACCOUNT";
    public static final String EXTRA_FROM_NOTIFICATION =
            "com.owncloud.android.ui.activity.FROM_NOTIFICATION";
    public static final String EXTRA_FILE_LIST_OPTION = "EXTRA_FILE_LIST_OPTION";
    // go to a high number, since the low numbers are usded by android
    public static final int REQUEST_CODE__UPDATE_CREDENTIALS = 0;
    public static final int REQUEST_CODE__LAST_SHARED = REQUEST_CODE__UPDATE_CREDENTIALS;
    private static final String KEY_WAITING_FOR_OP_ID = "WAITING_FOR_OP_ID";
    private static final String KEY_ACTION_BAR_TITLE = "ACTION_BAR_TITLE";
    protected static final long DELAY_TO_REQUEST_OPERATIONS_LATER = 200;

    /* Dialog tags */
    private static final String DIALOG_UNTRUSTED_CERT = "DIALOG_UNTRUSTED_CERT";
    private static final String DIALOG_CERT_NOT_SAVED = "DIALOG_CERT_NOT_SAVED";

    /**
     * Main {@link OCFile} handled by the activity.
     */
    private OCFile mFile;

    /**
     * Flag to signal if the activity is launched by a notification
     */
    private boolean mFromNotification;

    /**
     * Messages handler associated to the main thread and the life cycle of the activity
     */
    private Handler mHandler;

    private FileOperationsHelper mFileOperationsHelper;

    private ServiceConnection mOperationsServiceConnection = null;

    private OperationsServiceBinder mOperationsServiceBinder = null;

    private boolean mResumed = false;

    protected FileDownloaderBinder mDownloaderBinder = null;
    protected FileUploaderBinder mUploaderBinder = null;
    private ServiceConnection mDownloadServiceConnection, mUploadServiceConnection = null;

    /**
     * Loads the ownCloud {@link Account} and main {@link OCFile} to be handled by the instance of
     * the {@link FileActivity}.
     * <p>
     * Grants that a valid ownCloud {@link Account} is associated to the instance, or that the user
     * is requested to create a new one.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        mFileOperationsHelper = new FileOperationsHelper(this);
        Account account = null;
        if (savedInstanceState != null) {
            mFile = savedInstanceState.getParcelable(FileActivity.EXTRA_FILE);
            mFromNotification = savedInstanceState.getBoolean(FileActivity.EXTRA_FROM_NOTIFICATION);
            mFileOperationsHelper.setOpIdWaitingFor(
                    savedInstanceState.getLong(KEY_WAITING_FOR_OP_ID, Long.MAX_VALUE)
            );
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(savedInstanceState.getString(KEY_ACTION_BAR_TITLE));
            }
        } else {
            account = getIntent().getParcelableExtra(FileActivity.EXTRA_ACCOUNT);
            mFile = getIntent().getParcelableExtra(FileActivity.EXTRA_FILE);
            mFromNotification = getIntent().getBooleanExtra(FileActivity.EXTRA_FROM_NOTIFICATION,
                    false);
        }

        AccountUtils.updateAccountVersion(this); // best place, before any access to AccountManager
        // or database

        setAccount(account, savedInstanceState != null);

        mOperationsServiceConnection = new OperationsServiceConnection();
        bindService(new Intent(this, OperationsService.class), mOperationsServiceConnection,
                Context.BIND_AUTO_CREATE);

        mDownloadServiceConnection = newTransferenceServiceConnection();
        if (mDownloadServiceConnection != null) {
            bindService(new Intent(this, FileDownloader.class), mDownloadServiceConnection,
                    Context.BIND_AUTO_CREATE);
        }
        mUploadServiceConnection = newTransferenceServiceConnection();
        if (mUploadServiceConnection != null) {
            bindService(new Intent(this, FileUploader.class), mUploadServiceConnection,
                    Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mResumed = true;
        if (mOperationsServiceBinder != null) {
            doOnResumeAndBound();
        }
    }

    @Override
    protected void onPause() {
        if (mOperationsServiceBinder != null) {
            mOperationsServiceBinder.removeOperationListener(this);
        }
        mResumed = false;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mOperationsServiceConnection != null) {
            unbindService(mOperationsServiceConnection);
            mOperationsServiceBinder = null;
        }
        if (mDownloadServiceConnection != null) {
            unbindService(mDownloadServiceConnection);
            mDownloadServiceConnection = null;
        }
        if (mUploadServiceConnection != null) {
            unbindService(mUploadServiceConnection);
            mUploadServiceConnection = null;
        }

        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(FileActivity.EXTRA_FILE, mFile);
        outState.putBoolean(FileActivity.EXTRA_FROM_NOTIFICATION, mFromNotification);
        outState.putLong(KEY_WAITING_FOR_OP_ID, mFileOperationsHelper.getOpIdWaitingFor());
        if (getSupportActionBar() != null && getSupportActionBar().getTitle() != null) {
            // Null check in case the actionbar is used in ActionBar.NAVIGATION_MODE_LIST
            // since it doesn't have a title then
            outState.putString(KEY_ACTION_BAR_TITLE, getSupportActionBar().getTitle().toString());
        }
    }

    /**
     * Getter for the main {@link OCFile} handled by the activity.
     *
     * @return Main {@link OCFile} handled by the activity.
     */
    public OCFile getFile() {
        return mFile;
    }

    /**
     * Setter for the main {@link OCFile} handled by the activity.
     *
     * @param file Main {@link OCFile} to be handled by the activity.
     */
    public void setFile(OCFile file) {
        mFile = file;
    }

    /**
     * @return Value of mFromNotification: True if the Activity is launched by a notification
     */
    public boolean fromNotification() {
        return mFromNotification;
    }

    public OperationsServiceBinder getOperationsServiceBinder() {
        return mOperationsServiceBinder;
    }

    protected ServiceConnection newTransferenceServiceConnection() {
        return null;
    }

    public OnRemoteOperationListener getRemoteOperationListener() {
        return this;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public FileOperationsHelper getFileOperationsHelper() {
        return mFileOperationsHelper;
    }

    /**
     * @param operation Operation performed.
     * @param result    Result of the removal.
     */
    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        Timber.d("Received result of operation in FileActivity - common behaviour for all the FileActivities");

        mFileOperationsHelper.setOpIdWaitingFor(Long.MAX_VALUE);

        dismissLoadingDialog();

        if (!result.isSuccess() && (
                result.getCode() == ResultCode.UNAUTHORIZED ||
                        (result.isException() && result.getException() instanceof AuthenticatorException)
        )) {

            requestCredentialsUpdate();

            if (result.getCode() == ResultCode.UNAUTHORIZED) {
                showSnackMessage(
                        ErrorMessageAdapter.Companion.getResultMessage(result, operation, getResources())
                );
            }

        } else if (!result.isSuccess() && ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED.equals(result.getCode())) {

            showUntrustedCertDialog(result);

        } else if (operation == null ||
                operation instanceof SynchronizeFolderOperation
        ) {
            if (result.isSuccess()) {
                updateFileFromDB();

            } else if (result.getCode() != ResultCode.CANCELLED) {
                showSnackMessage(
                        ErrorMessageAdapter.Companion.getResultMessage(result, operation, getResources())
                );
            }

        } else if (operation instanceof SynchronizeFileOperation) {
            onSynchronizeFileOperationFinish((SynchronizeFileOperation) operation, result);

        } else if (operation instanceof RenameFileOperation && result.isSuccess()) {
            result.getData();
        }
    }

    protected void showRequestAccountChangeNotice(String errorMessage, boolean mustChange) {
        if (mustChange) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.auth_failure_snackbar_action)
                    .setMessage(errorMessage)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> startActivity(
                            new Intent(FileActivity.this, ManageAccountsActivity.class)))
                    .setIcon(R.drawable.common_error_grey)
                    .setCancelable(false)
                    .show();
        } else {
            Snackbar.make(findViewById(android.R.id.content), errorMessage, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.auth_failure_snackbar_action, v ->
                            startActivity(new Intent(FileActivity.this, ManageAccountsActivity.class)))
                    .show();
        }
    }

    protected void showRequestRegainAccess() {
        Snackbar.make(findViewById(android.R.id.content), R.string.auth_oauth_failure, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.auth_oauth_failure_snackbar_action, v ->
                        requestCredentialsUpdate())
                .show();
    }

    /**
     * Invalidates the credentials stored for the current OC account and requests new credentials to the user,
     * navigating to {@link LoginActivity}
     * <p>
     * Equivalent to call requestCredentialsUpdate(null);
     */
    protected void requestCredentialsUpdate() {
        requestCredentialsUpdate(null);
    }

    /**
     * Invalidates the credentials stored for the given OC account and requests new credentials to the user,
     * navigating to {@link LoginActivity}
     *
     * @param account Stored OC account to request credentials update for. If null, current account will
     *                be used.
     */
    protected void requestCredentialsUpdate(Account account) {

        if (account == null) {
            account = getAccount();
        }

        /// request credentials to user
        Intent updateAccountCredentials = new Intent(this, LoginActivity.class);
        updateAccountCredentials.putExtra(AuthenticatorConstants.EXTRA_ACCOUNT, account);
        updateAccountCredentials.putExtra(
                AuthenticatorConstants.EXTRA_ACTION,
                AuthenticatorConstants.ACTION_UPDATE_EXPIRED_TOKEN);
        updateAccountCredentials.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivityForResult(updateAccountCredentials, REQUEST_CODE__UPDATE_CREDENTIALS);
    }

    /**
     * Show untrusted cert dialog
     */
    public void showUntrustedCertDialog(RemoteOperationResult result) {
        // Show a dialog with the certificate info
        FragmentManager fm = getSupportFragmentManager();
        SslUntrustedCertDialog dialog = (SslUntrustedCertDialog) fm.findFragmentByTag(DIALOG_UNTRUSTED_CERT);
        if (dialog == null) {
            dialog = SslUntrustedCertDialog.newInstanceForFullSslError(
                    (CertificateCombinedException) result.getException());
            FragmentTransaction ft = fm.beginTransaction();
            dialog.show(ft, DIALOG_UNTRUSTED_CERT);
        }
    }

    private void onSynchronizeFileOperationFinish(SynchronizeFileOperation operation,
                                                  RemoteOperationResult result) {
        invalidateOptionsMenu();
        OCFile syncedFile = operation.getLocalFile();
        if (!result.isSuccess()) {
            if (result.getCode() == ResultCode.SYNC_CONFLICT) {
                Intent i = new Intent(this, ConflictsResolveActivity.class);
                i.putExtra(ConflictsResolveActivity.EXTRA_FILE, syncedFile);
                i.putExtra(ConflictsResolveActivity.EXTRA_ACCOUNT, getAccount());
                startActivity(i);
            }
        }
    }

    protected void updateFileFromDB() {
        OCFile file = getFile();
        if (file != null) {
            file = getStorageManager().getFileByPath(file.getRemotePath());
            setFile(file);
        }
    }

    private void doOnResumeAndBound() {
        mOperationsServiceBinder.addOperationListener(FileActivity.this, mHandler);
        long waitingForOpId = mFileOperationsHelper.getOpIdWaitingFor();
        if (waitingForOpId <= Integer.MAX_VALUE) {
            boolean wait = mOperationsServiceBinder.dispatchResultIfFinished((int) waitingForOpId,
                    this);
            if (!wait) {
                dismissLoadingDialog();
            }
        }
    }

    @Override
    public FileDownloaderBinder getFileDownloaderBinder() {
        return mDownloaderBinder;
    }

    @Override
    public FileUploaderBinder getFileUploaderBinder() {
        return mUploaderBinder;
    }

    @Override
    public void restart() {
        Intent i = new Intent(this, FileDisplayActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    public void navigateToOption(FileListOption fileListOption) {
        Intent intent;
        switch (fileListOption) {
            case ALL_FILES:
                restart();
                break;
            case SHARED_BY_LINK:
                intent = new Intent(this, FileDisplayActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(EXTRA_FILE_LIST_OPTION, (Parcelable) FileListOption.SHARED_BY_LINK);
                startActivity(intent);
                break;
            case AV_OFFLINE:
                intent = new Intent(this, FileDisplayActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(EXTRA_FILE_LIST_OPTION, (Parcelable) FileListOption.AV_OFFLINE);
                startActivity(intent);
                break;
        }
    }

    protected OCFile getCurrentDir() {
        OCFile file = getFile();
        if (file != null) {
            if (file.isFolder()) {
                return file;
            } else if (getStorageManager() != null) {
                String parentPath = file.getParentRemotePath();
                return getStorageManager().getFileByPath(parentPath);
            }
        }
        return null;
    }

    @Override
    public void onSavedCertificate() {
        // Nothing to do in this context
    }

    /* OnSslUntrustedCertListener methods */

    @Override
    public void onFailedSavingCertificate() {
        ConfirmationDialogFragment dialog = ConfirmationDialogFragment.newInstance(
                R.string.ssl_validator_not_saved, new String[]{}, 0, android.R.string.ok, -1, -1
        );
        dialog.show(getSupportFragmentManager(), DIALOG_CERT_NOT_SAVED);
    }

    @Override
    public void onCancelCertificate() {
        // nothing to do
    }

    /**
     * Implements callback methods for service binding. Passed as a parameter to {
     */
    private class OperationsServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
            if (component.equals(new ComponentName(FileActivity.this, OperationsService.class))) {
                Timber.d("Operations service connected");
                mOperationsServiceBinder = (OperationsServiceBinder) service;
                if (mResumed) {
                    doOnResumeAndBound();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(new ComponentName(FileActivity.this, OperationsService.class))) {
                Timber.d("Operations service disconnected");
                mOperationsServiceBinder = null;
                // TODO whatever could be waiting for the service is unbound
            }
        }
    }
}
