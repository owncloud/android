/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author masensio
 * @author Juan Carlos González Cabrero
 * @author David A. Velasco
 * @author Christian Schabesberger
 * @author Shashvat Kedia
 * @author Abel García de Prada
 * @author John Kalimeris
 * Copyright (C) 2012  Bartek Przybylski
 * Copyright (C) 2021 ownCloud GmbH.
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
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources.NotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.presentation.ui.files.SortBottomSheetFragment;
import com.owncloud.android.presentation.ui.files.SortOptionsView;
import com.owncloud.android.presentation.ui.files.SortOrder;
import com.owncloud.android.presentation.ui.files.SortType;
import com.owncloud.android.presentation.ui.files.ViewType;
import com.owncloud.android.syncadapter.FileSyncAdapter;
import com.owncloud.android.ui.adapter.ReceiveExternalFilesAdapter;
import com.owncloud.android.ui.asynctasks.CopyAndUploadContentUrisTask;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.CreateFolderDialogFragment;
import com.owncloud.android.ui.errorhandling.ErrorMessageAdapter;
import com.owncloud.android.ui.fragment.TaskRetainerFragment;
import com.owncloud.android.ui.helpers.UriUploader;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.Extras;
import com.owncloud.android.utils.FileStorageUtils;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import java.util.Vector;

/**
 * This can be used to upload things to an ownCloud instance.
 */
public class ReceiveExternalFilesActivity extends FileActivity
        implements OnItemClickListener, android.view.View.OnClickListener,
        CopyAndUploadContentUrisTask.OnCopyTmpFilesTaskListener, SortOptionsView.SortOptionsListener,
        SortBottomSheetFragment.SortDialogListener, SortOptionsView.CreateFolderListener, SearchView.OnQueryTextListener,
        View.OnFocusChangeListener, ReceiveExternalFilesAdapter.OnSearchQueryUpdateListener {

    private static final String FTAG_ERROR_FRAGMENT = "ERROR_FRAGMENT";

    private AccountManager mAccountManager;
    private Stack<String> mParents;
    private ArrayList<Uri> mStreamsToUpload;
    private String mUploadPath;
    private OCFile mFile;
    private SortOptionsView mSortOptionsView;
    private SearchView mSearchView;
    private TextView mEmptyListMessage;

    private LocalBroadcastManager mLocalBroadcastManager;
    private SyncBroadcastReceiver mSyncBroadcastReceiver;
    private UploadBroadcastReceiver mUploadBroadcastReceiver;
    // this is inited laizly, when an acocunt is selected. If no account is selected but an instance of this would
    // be crated it would result in an null pointer exception.
    private ReceiveExternalFilesAdapter mAdapter = null;
    private ListView mListView;
    private boolean mSyncInProgress = false;
    private boolean mAccountSelected;
    private boolean mAccountSelectionShowing;

    private final static int DIALOG_NO_ACCOUNT = 0;
    private final static int DIALOG_MULTIPLE_ACCOUNT = 1;

    private final static int REQUEST_CODE__SETUP_ACCOUNT = REQUEST_CODE__LAST_SHARED + 1;
    private final static int MAX_FILENAME_LENGTH = 223;

    private final static String KEY_PARENTS = "PARENTS";
    private final static String KEY_FILE = "FILE";
    private final static String KEY_ACCOUNT_SELECTED = "ACCOUNT_SELECTED";
    private final static String KEY_ACCOUNT_SELECTION_SHOWING = "ACCOUNT_SELECTION_SHOWING";

    private static final String DIALOG_WAIT_COPY_FILE = "DIALOG_WAIT_COPY_FILE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prepareStreamsToUpload(); //streams need to be prepared before super.onCreate() is called

        if (savedInstanceState == null) {
            mParents = new Stack<>();
            mAccountSelected = false;
            mAccountSelectionShowing = false;

        } else {
            mParents = (Stack<String>) savedInstanceState.getSerializable(KEY_PARENTS);
            mFile = savedInstanceState.getParcelable(KEY_FILE);
            mAccountSelected = savedInstanceState.getBoolean(KEY_ACCOUNT_SELECTED);
            mAccountSelectionShowing = savedInstanceState.getBoolean(KEY_ACCOUNT_SELECTION_SHOWING);
        }

        super.onCreate(savedInstanceState);

        if (mAccountSelected) {
            setAccount(savedInstanceState.getParcelable(FileActivity.EXTRA_ACCOUNT));
        }

        //init ui
        setContentView(R.layout.uploader_layout);

        mSortOptionsView = findViewById(R.id.options_layout);
        if (mSortOptionsView != null) {
            mSortOptionsView.setOnSortOptionsListener(this);
            mSortOptionsView.setOnCreateFolderListener(this);
            mSortOptionsView.selectAdditionalView(SortOptionsView.AdditionalView.CREATE_FOLDER);
        }

        mEmptyListMessage = findViewById(R.id.empty_list_view);

        mListView = findViewById(android.R.id.list);
        mListView.setOnItemClickListener(this);

        // Listen for sync messages
        IntentFilter syncIntentFilter = new IntentFilter(RefreshFolderOperation.
                EVENT_SINGLE_FOLDER_CONTENTS_SYNCED);
        syncIntentFilter.addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED);
        mSyncBroadcastReceiver = new SyncBroadcastReceiver();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mLocalBroadcastManager.registerReceiver(mSyncBroadcastReceiver, syncIntentFilter);

        // Listen for upload messages
        IntentFilter uploadIntentFilter = new IntentFilter(FileUploader.getUploadFinishMessage());
        uploadIntentFilter.addAction(FileUploader.getUploadStartMessage());
        mUploadBroadcastReceiver = new UploadBroadcastReceiver();
        mLocalBroadcastManager.registerReceiver(mUploadBroadcastReceiver, uploadIntentFilter);

        // Init Fragment without UI to retain AsyncTask across configuration changes
        FragmentManager fm = getSupportFragmentManager();
        TaskRetainerFragment taskRetainerFragment =
                (TaskRetainerFragment) fm.findFragmentByTag(TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT);
        if (taskRetainerFragment == null) {
            taskRetainerFragment = new TaskRetainerFragment();
            fm.beginTransaction()
                    .add(taskRetainerFragment, TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT).commit();
        }   // else, Fragment already created and retained across configuration change


    }

    @Override
    protected void setAccount(Account account, boolean savedAccount) {
        if (somethingToUpload()) {
            mAccountManager = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
            Account[] accounts = mAccountManager.getAccountsByType(MainApp.Companion.getAccountType());
            if (accounts.length == 0) {
                Timber.i("No ownCloud account is available");
                showDialog(DIALOG_NO_ACCOUNT);
            } else if (accounts.length > 1 && !mAccountSelected && !mAccountSelectionShowing) {
                Timber.i("More than one ownCloud is available");
                showDialog(DIALOG_MULTIPLE_ACCOUNT);
                mAccountSelectionShowing = true;
            } else {
                if (!savedAccount) {
                    setAccount(accounts[0]);
                }
            }
        } else {
            showErrorDialog(
                    R.string.uploader_error_message_no_file_to_upload,
                    R.string.uploader_error_title_no_file_to_upload
            );
        }

        super.setAccount(account, savedAccount);
    }

    @Override
    protected void onAccountSet(boolean stateWasRecovered) {
        super.onAccountSet(mAccountWasRestored);
        initTargetFolder();
        updateDirectoryList();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Timber.v("onSaveInstanceState() start");
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_PARENTS, mParents);
        //outState.putParcelable(KEY_ACCOUNT, mAccount);
        outState.putParcelable(KEY_FILE, mFile);
        outState.putBoolean(KEY_ACCOUNT_SELECTED, mAccountSelected);
        outState.putBoolean(KEY_ACCOUNT_SELECTION_SHOWING, mAccountSelectionShowing);
        outState.putParcelable(FileActivity.EXTRA_ACCOUNT, getAccount());

        Timber.v("onSaveInstanceState() end");
    }

    @Override
    protected void onDestroy() {
        if (mSyncBroadcastReceiver != null) {
            mLocalBroadcastManager.unregisterReceiver(mSyncBroadcastReceiver);
        }
        super.onDestroy();
    }

    @Override
    protected Dialog onCreateDialog(final int id) {
        final AlertDialog.Builder builder = new Builder(this);
        switch (id) {
            case DIALOG_NO_ACCOUNT:
                builder.setIcon(R.drawable.ic_warning);
                builder.setTitle(R.string.uploader_wrn_no_account_title);
                builder.setMessage(String.format(
                        getString(R.string.uploader_wrn_no_account_text),
                        getString(R.string.app_name)));
                builder.setCancelable(false);
                builder.setPositiveButton(R.string.uploader_wrn_no_account_setup_btn_text, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_ADD_ACCOUNT);
                        intent.putExtra("authorities", new String[]{MainApp.Companion.getAuthTokenType()});
                        startActivityForResult(intent, REQUEST_CODE__SETUP_ACCOUNT);
                    }
                });
                builder.setNegativeButton(R.string.uploader_wrn_no_account_quit_btn_text, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                return builder.create();
            case DIALOG_MULTIPLE_ACCOUNT:
                Account[] accounts = mAccountManager.getAccountsByType(MainApp.Companion.getAccountType());
                CharSequence[] dialogItems = new CharSequence[accounts.length];
                OwnCloudAccount oca;
                for (int i = 0; i < dialogItems.length; ++i) {
                    try {
                        oca = new OwnCloudAccount(accounts[i], this);
                        dialogItems[i] =
                                oca.getDisplayName() + " @ " +
                                        DisplayUtils.convertIdn(
                                                accounts[i].name.substring(accounts[i].name.lastIndexOf("@") + 1),
                                                false
                                        );

                    } catch (Exception e) {
                        Timber.w("Couldn't read display name of account; using account name instead");
                        dialogItems[i] = DisplayUtils.convertIdn(accounts[i].name, false);
                    }
                }
                builder.setTitle(R.string.common_choose_account);
                builder.setItems(dialogItems, (dialog, which) -> {
                    setAccount(mAccountManager.getAccountsByType(MainApp.Companion.getAccountType())[which]);
                    onAccountSet(mAccountWasRestored);
                    dialog.dismiss();
                    mAccountSelected = true;
                    mAccountSelectionShowing = false;
                });
                builder.setCancelable(true);
                builder.setOnCancelListener(dialog -> {
                    mAccountSelectionShowing = false;
                    dialog.cancel();
                    finish();
                });
                return builder.create();
            default:
                throw new IllegalArgumentException("Unknown dialog id: " + id);
        }
    }

    @Override
    public void onBackPressed() {
        if (mParents.size() <= 1) {
            super.onBackPressed();
        } else {
            mParents.pop();
            String full_path = generatePath(mParents);
            startSyncFolderOperation(getStorageManager().getFileByPath(full_path));
            updateDirectoryList();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // click on folder in the list
        Timber.d("on item click");
        // get current representation of files:
        // This does not necessarily mean this is the content of the current folder.
        // If the user searches for a folder mAdapter.getFiles() returnes only the folders/files
        // that match the currently entered search query.
        Vector<OCFile> tmpfiles = mAdapter.getFiles();
        tmpfiles = sortFileList(tmpfiles);

        if (tmpfiles.size() <= 0) {
            return;
        }
        // filter on dirtype
        Vector<OCFile> files = new Vector<>(tmpfiles);
        if (files.size() < position) {
            throw new IndexOutOfBoundsException("Incorrect item selected");
        }
        if (files.get(position).isFolder()) {
            OCFile folderToEnter = files.get(position);
            startSyncFolderOperation(folderToEnter);
            mParents.push(folderToEnter.getFileName());
            updateDirectoryList();
        }
    }

    @Override
    public void onClick(View v) {
        // click on button
        switch (v.getId()) {
            case R.id.uploader_choose_folder:
                mUploadPath = "";   // first element in mParents is root dir, represented by "";
                // init mUploadPath with "/" results in a "//" prefix
                for (String p : mParents) {
                    mUploadPath += p + File.separator;
                }
                if (!isPlainTextUpload()) {
                    Timber.d("Uploading file to dir %s", mUploadPath);
                    uploadFiles();
                } else {
                    showUploadTextDialog();
                }
                break;

            case R.id.uploader_cancel:
                finish();
                break;

            default:
                throw new IllegalArgumentException("Wrong element clicked");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Timber.i("result received. req: " + requestCode + " res: " + resultCode);
        if (requestCode == REQUEST_CODE__SETUP_ACCOUNT) {
            dismissDialog(DIALOG_NO_ACCOUNT);
            if (resultCode == RESULT_CANCELED) {
                finish();
            }
            Account[] accounts = mAccountManager.getAccountsByType(MainApp.Companion.getAuthTokenType());
            if (accounts.length == 0) {
                showDialog(DIALOG_NO_ACCOUNT);
            } else {
                // there is no need for checking for is there more then one
                // account at this point
                // since account setup can set only one account at time
                setAccount(accounts[0]);
                updateDirectoryList();
            }
        }
    }

    private void updateDirectoryList() {
        initToolbar(mParents.peek());

        String full_path = generatePath(mParents);
        Timber.d("Populating view with content of : %s", full_path);
        mFile = getStorageManager().getFileByPath(full_path);
        if (mFile != null) {
            if(mAdapter == null) {
                mAdapter = new ReceiveExternalFilesAdapter(
                        this, getStorageManager(), getAccount());
                mListView.setAdapter(mAdapter);
            }
            Vector<OCFile> files = sortFileList(getStorageManager().getFolderContent(mFile));
            mAdapter.setNewItemVector(files);

            Button btnChooseFolder = findViewById(R.id.uploader_choose_folder);
            btnChooseFolder.setOnClickListener(this);

            Button btnNewFolder = findViewById(R.id.uploader_cancel);
            btnNewFolder.setOnClickListener(this);
        }
    }

    /**
     * This activity is special, so we won't use the ToolbarActivity method to initialize the actionbar.
     */
    private void initToolbar(String current_dir) {
        Toolbar toolbar = findViewById(R.id.standard_toolbar);
        ConstraintLayout rootToolbar = findViewById(R.id.root_toolbar);
        toolbar.setVisibility(View.VISIBLE);
        rootToolbar.setVisibility(View.GONE);

        String actionBarTitle;
        if (current_dir.equals("")) {
            actionBarTitle = getString(R.string.uploader_top_message);
        } else {
            actionBarTitle = current_dir;
        }
        toolbar.setTitle(actionBarTitle);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();

        boolean notRoot = (mParents.size() > 1);

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(notRoot);
            actionBar.setHomeButtonEnabled(notRoot);
        }
    }

    @Override
    public void onSavedCertificate() {
        startSyncFolderOperation(getCurrentDir());
    }

    private void startSyncFolderOperation(OCFile folder) {

        mSyncInProgress = true;

        // perform folder synchronization
        SyncOperation synchFolderOp = new RefreshFolderOperation(
                folder,
                false,
                getAccount(),
                getApplicationContext()
        );
        synchFolderOp.execute(getStorageManager(), this, null, null);
    }

    private Vector<OCFile> sortFileList(Vector<OCFile> files) {
        // Read sorting order, default to sort by name ascending
        FileStorageUtils.mSortOrderFileDisp = PreferenceManager.getSortOrder(this, FileStorageUtils.FILE_DISPLAY_SORT);
        FileStorageUtils.mSortAscendingFileDisp = PreferenceManager.getSortAscending(this,
                FileStorageUtils.FILE_DISPLAY_SORT);

        files = FileStorageUtils.sortFolder(files, FileStorageUtils.mSortOrderFileDisp,
                FileStorageUtils.mSortAscendingFileDisp);
        return files;
    }

    private String generatePath(Stack<String> dirs) {
        StringBuilder full_path = new StringBuilder();

        for (String a : dirs) {
            full_path.append(a).append("/");
        }
        return full_path.toString();
    }

    private void prepareStreamsToUpload() {
        if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_SEND)) {
            mStreamsToUpload = new ArrayList<>();
            mStreamsToUpload.add(getIntent().getParcelableExtra(Intent.EXTRA_STREAM));
            if (mStreamsToUpload.get(0) != null) {
                String streamToUpload = mStreamsToUpload.get(0).toString();
                if (streamToUpload.contains("/data") && streamToUpload.contains(getPackageName()) &&
                        !streamToUpload.contains(getCacheDir().getPath()) &&
                        !streamToUpload.contains(Environment.getExternalStorageDirectory().toString())) {
                    finish();
                }
            }
        } else if (getIntent().getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {
            mStreamsToUpload = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        }
    }

    private boolean somethingToUpload() {
        return (mStreamsToUpload != null && mStreamsToUpload.get(0) != null ||
                isPlainTextUpload());
    }

    /**
     * Checks if the intent contains plain text and no other stream has been added yet.
     *
     * @return true/false
     */
    private boolean isPlainTextUpload() {
        return mStreamsToUpload.get(0) == null &&
                getIntent().getStringExtra(Intent.EXTRA_TEXT) != null;
    }

    public void uploadFiles() {

        UriUploader uploader = new UriUploader(
                this,
                mStreamsToUpload,
                mUploadPath,
                getAccount(),
                FileUploader.LOCAL_BEHAVIOUR_FORGET,
                true, // Show waiting dialog while file is being copied from private storage
                this  // Copy temp task listener
        );

        UriUploader.UriUploaderResultCode resultCode = uploader.uploadUris();

        // Save the path to shared preferences; even if upload is not possible, user chose the folder
        PreferenceManager.setLastUploadPath(mUploadPath, this);

        if (resultCode == UriUploader.UriUploaderResultCode.OK) {
            finish();
        } else if (resultCode != UriUploader.UriUploaderResultCode.COPY_THEN_UPLOAD) {

            int messageResTitle = R.string.uploader_error_title_file_cannot_be_uploaded;
            int messageResId = R.string.common_error_unknown;

            if (resultCode == UriUploader.UriUploaderResultCode.ERROR_NO_FILE_TO_UPLOAD) {
                messageResId = R.string.uploader_error_message_no_file_to_upload;
                messageResTitle = R.string.uploader_error_title_no_file_to_upload;
            } else if (resultCode == UriUploader.UriUploaderResultCode.ERROR_READ_PERMISSION_NOT_GRANTED) {
                messageResId = R.string.uploader_error_message_read_permission_not_granted;
            }

            showErrorDialog(
                    messageResId,
                    messageResTitle
            );
        }
    }

    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        super.onRemoteOperationFinish(operation, result);

        if (operation instanceof CreateFolderOperation) {
            onCreateFolderOperationFinish((CreateFolderOperation) operation, result);
        }

    }

    /**
     * Updates the view associated to the activity after the finish of an operation
     * trying create a new folder
     *
     * @param operation Creation operation performed.
     * @param result    Result of the creation.
     */
    private void onCreateFolderOperationFinish(CreateFolderOperation operation,
                                               RemoteOperationResult result) {
        if (result.isSuccess()) {
            updateDirectoryList();
        } else {
            try {
                showSnackMessage(
                        ErrorMessageAdapter.Companion.getResultMessage(result, operation, getResources())
                );

            } catch (NotFoundException e) {
                Timber.e(e, "Error while trying to show fail message ");
            }
        }
    }

    /**
     * Loads the target folder initialize shown to the user.
     * <p/>
     * The target account has to be chosen before this method is called.
     */
    private void initTargetFolder() {
        if (getStorageManager() == null) {
            throw new IllegalStateException("Do not call this method before " +
                    "initializing mStorageManager");
        }

        String lastPath = PreferenceManager.getLastUploadPath(this);
        // "/" equals root-directory
        if (lastPath.equals("/")) {
            mParents.add("");
        } else {
            String[] dir_names = lastPath.split("/");
            mParents.clear();
            mParents.addAll(Arrays.asList(dir_names));
        }
        //Make sure that path still exists, if it doesn't pop the stack and try the previous path
        while (!getStorageManager().fileExists(generatePath(mParents)) && mParents.size() > 1) {
            mParents.pop();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        mSearchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        mSearchView.setMaxWidth(Integer.MAX_VALUE);
        mSearchView.setQueryHint(getResources().getString(R.string.actionbar_search));
        mSearchView.setOnQueryTextFocusChangeListener(this);
        mSearchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case android.R.id.home:
                if ((mParents.size() > 1)) {
                    onBackPressed();
                }
                break;
            default:
                retval = super.onOptionsItemSelected(item);
        }
        return retval;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void sortByName(boolean isAscending) {
        mAdapter.setSortOrder(FileStorageUtils.SORT_NAME, isAscending);
    }

    private void sortBySize(boolean isAscending) {
        mAdapter.setSortOrder(FileStorageUtils.SORT_SIZE, isAscending);
    }

    private void sortByDate(boolean isAscending) {
        mAdapter.setSortOrder(FileStorageUtils.SORT_DATE, isAscending);
    }

    private OCFile getCurrentFolder() {
        OCFile file = mFile;
        if (file != null) {
            if (file.isFolder()) {
                return file;
            } else if (getStorageManager() != null) {
                return getStorageManager().getFileByPath(file.getParentRemotePath());
            }
        }
        return null;
    }

    private void browseToRoot() {
        OCFile root = getStorageManager().getFileByPath(OCFile.ROOT_PATH);
        mFile = root;
        startSyncFolderOperation(root);
    }

    @Override
    public void onSortTypeListener(@NotNull SortType sortType, @NotNull SortOrder sortOrder) {
        SortBottomSheetFragment sortBottomSheetFragment = SortBottomSheetFragment.Companion.newInstance(sortType, sortOrder);
        sortBottomSheetFragment.setSortDialogListener(this);
        sortBottomSheetFragment.show(getSupportFragmentManager(), SortBottomSheetFragment.TAG);
    }

    @Override
    public void onViewTypeListener(@NotNull ViewType viewType) {

    }

    @Override
    public void onSortSelected(@NotNull SortType sortType) {
        mSortOptionsView.setSortTypeSelected(sortType);

        boolean isAscending = mSortOptionsView.getSortOrderSelected().equals(SortOrder.SORT_ORDER_ASCENDING);
        if (sortType == SortType.SORT_TYPE_BY_NAME) {
            sortByName(isAscending);
        } else if (sortType == SortType.SORT_TYPE_BY_DATE) {
            sortByDate(isAscending);
        } else if (sortType == SortType.SORT_TYPE_BY_SIZE) {
            sortBySize(isAscending);
        }
    }

    @Override
    public void onCreateFolderListener() {
        CreateFolderDialogFragment dialog = CreateFolderDialogFragment.newInstance(mFile);
        dialog.show(getSupportFragmentManager(), CreateFolderDialogFragment.CREATE_FOLDER_FRAGMENT);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        mAdapter.filterBySearch(query);
        return true;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus && mAdapter.getFiles().isEmpty()) {
            updateEmptyListMessage(getString(R.string.local_file_list_search_with_no_matches));
        } else if (!hasFocus && mAdapter.getFiles().isEmpty()) {
            updateEmptyListMessage(getString(R.string.file_list_empty));
        }
        else {
            updateEmptyListMessage(getString(R.string.empty));
        }
    }

    @Override
    public void updateEmptyListMessage(String updateTxt) {
        mEmptyListMessage.setText(updateTxt);
    }

    private class SyncBroadcastReceiver extends BroadcastReceiver {

        /**
         * {@link BroadcastReceiver} to enable syncing feedback in UI
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getAction();
            Timber.d("Received broadcast %s", event);
            String accountName = intent.getStringExtra(FileSyncAdapter.EXTRA_ACCOUNT_NAME);
            String synchFolderRemotePath =
                    intent.getStringExtra(FileSyncAdapter.EXTRA_FOLDER_PATH);
            RemoteOperationResult synchResult =
                    (RemoteOperationResult) intent.getSerializableExtra(
                            FileSyncAdapter.EXTRA_RESULT);
            boolean sameAccount = (getAccount() != null &&
                    accountName.equals(getAccount().name) && getStorageManager() != null);

            if (sameAccount) {

                if (FileSyncAdapter.EVENT_FULL_SYNC_START.equals(event)) {
                    mSyncInProgress = true;

                } else {
                    OCFile currentFile = (mFile == null) ? null :
                            getStorageManager().getFileByPath(mFile.getRemotePath());
                    OCFile currentDir = (getCurrentFolder() == null) ? null :
                            getStorageManager().getFileByPath(getCurrentFolder().getRemotePath());

                    if (currentDir == null) {
                        // current folder was removed from the server
                        showSnackMessage(
                                String.format(
                                        getString(R.string.sync_current_folder_was_removed),
                                        getCurrentFolder().getFileName()
                                )
                        );
                        browseToRoot();

                    } else {
                        if (currentFile == null && !mFile.isFolder()) {
                            // currently selected file was removed in the server, and now we know it
                            currentFile = currentDir;
                        }

                        if (currentDir.getRemotePath().equals(synchFolderRemotePath)) {
                            updateDirectoryList();
                        }
                        mFile = currentFile;
                    }

                    mSyncInProgress = (!FileSyncAdapter.EVENT_FULL_SYNC_END.equals(event) &&
                            !RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED.equals(event));

                    if (RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED.
                            equals(event) &&
                            /// TODO refactor and make common
                            synchResult != null && !synchResult.isSuccess()) {

                        if (synchResult.getCode() == ResultCode.UNAUTHORIZED ||
                                (synchResult.isException() && synchResult.getException()
                                        instanceof AuthenticatorException)) {

                            requestCredentialsUpdate();

                        } else if (RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED.equals(synchResult.getCode())) {

                            showUntrustedCertDialog(synchResult);
                        }
                    }
                }
                Timber.d("Setting progress visibility to %s", mSyncInProgress);

            }
        }
    }

    /**
     * Process the result of CopyAndUploadContentUrisTask
     */
    @Override
    public void onTmpFilesCopied(ResultCode result) {
        try {
            dismissLoadingDialog();
        } catch (IllegalStateException illegalStateException) {
            Timber.e(illegalStateException);
        }
        finish();
    }

    /**
     * Show an error dialog, forcing the user to click a single button to exit the activity
     *
     * @param messageResId    DataResult id of the message to show in the dialog.
     * @param messageResTitle DataResult id of the title to show in the dialog. 0 to show default alert message.
     *                        -1 to show no title.
     */
    private void showErrorDialog(int messageResId, int messageResTitle) {

        ConfirmationDialogFragment errorDialog = ConfirmationDialogFragment.newInstance(
                messageResId,
                new String[]{getString(R.string.app_name)}, // see uploader_error_message_* in strings.xml
                messageResTitle,
                R.string.common_back,
                -1,
                -1
        );
        errorDialog.setCancelable(false);
        errorDialog.setOnConfirmationListener(
                new ConfirmationDialogFragment.ConfirmationDialogFragmentListener() {
                    @Override
                    public void onConfirmation(String callerTag) {
                        finish();
                    }

                    @Override
                    public void onNeutral(String callerTag) {
                    }

                    @Override
                    public void onCancel(String callerTag) {
                    }
                }
        );
        errorDialog.show(getSupportFragmentManager(), FTAG_ERROR_FRAGMENT);
    }

    /**
     * Show a dialog where the user can enter a filename for the file he wants to place the text in.
     */
    private void showUploadTextDialog() {
        final AlertDialog.Builder builder = new Builder(this);

        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_upload_text, null);
        builder.setView(dialogView);
        builder.setTitle(R.string.uploader_upload_text_dialog_title);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.uploader_btn_upload_text, null);
        builder.setNegativeButton(android.R.string.cancel, null);

        final TextInputEditText input = dialogView.findViewById(R.id.inputFileName);
        final TextInputLayout inputLayout = dialogView.findViewById(R.id.inputTextLayout);

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                inputLayout.setError(null);
                inputLayout.setErrorEnabled(false);
            }
        });

        final AlertDialog alertDialog = builder.create();
        setFileNameFromIntent(alertDialog, input);
        alertDialog.setOnShowListener(dialog -> {
            Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String fileName = input.getText().toString();
                String error = null;
                if (fileName.length() > MAX_FILENAME_LENGTH) {
                    error = String.format(getString(R.string.uploader_upload_text_dialog_filename_error_length_max),
                            MAX_FILENAME_LENGTH);
                } else if (fileName.length() == 0) {
                    error = getString(R.string.uploader_upload_text_dialog_filename_error_empty);
                } else {
                    fileName += ".txt";
                    Uri fileUri = savePlainTextToFile(fileName);
                    mStreamsToUpload.clear();
                    mStreamsToUpload.add(fileUri);
                    uploadFiles();
                }
                inputLayout.setErrorEnabled(error != null);
                inputLayout.setError(error);
            });
        });
        alertDialog.show();
    }

    /**
     * Store plain text from intent to a new file in cache dir.
     *
     * @param fileName The name of the file.
     * @return Uri from created file.
     */
    private Uri savePlainTextToFile(String fileName) {
        Uri uri = null;
        String content = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        try {
            File tmpFile = new File(getCacheDir(), fileName);
            FileOutputStream outputStream = new FileOutputStream(tmpFile);
            outputStream.write(content.getBytes());
            outputStream.close();
            uri = Uri.fromFile(tmpFile);

        } catch (IOException e) {
            Timber.w(e, "Failed to create temp file for uploading plain text: %s", e.getMessage());
        }
        return uri;
    }

    /**
     * Suggest a filename based on the extras in the intent.
     * Show soft keyboard when no filename could be suggested.
     *
     * @param alertDialog AlertDialog
     * @param input       EditText The view where to place the filename in.
     */
    private void setFileNameFromIntent(AlertDialog alertDialog, EditText input) {
        String subject = getIntent().getStringExtra(Intent.EXTRA_SUBJECT);
        String title = getIntent().getStringExtra(Intent.EXTRA_TITLE);
        String fileName = subject != null ? subject : title;

        input.setText(fileName);
        input.selectAll();

        if (fileName == null) {
            // Show soft keyboard
            Window window = alertDialog.getWindow();
            if (window != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            }
        }
    }

    private class UploadBroadcastReceiver extends BroadcastReceiver {
        /**
         * If the upload is text shared from other apps and was successfully uploaded -> delete cache
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = intent.getBooleanExtra(Extras.EXTRA_UPLOAD_RESULT, false);
            String localPath = intent.getStringExtra(Extras.EXTRA_OLD_FILE_PATH);
            if (success && localPath.contains(getCacheDir().getPath())) {
                FileStorageUtils.deleteDir(getCacheDir());
            }
        }
    }
}
