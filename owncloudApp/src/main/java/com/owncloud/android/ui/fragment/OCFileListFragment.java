/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author masensio
 * @author David A. Velasco
 * @author Christian Schabesberger
 * @author David González Verdugo
 * @author Shashvat Kedia
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
package com.owncloud.android.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.FileListOption;
import com.owncloud.android.ui.activity.FolderPickerActivity;
import com.owncloud.android.ui.activity.OnEnforceableRefreshListener;
import com.owncloud.android.ui.adapter.FileListListAdapter;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.CreateFolderDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.dialog.RenameFileDialogFragment;
import com.owncloud.android.ui.helpers.SparseBooleanArrayParcelable;
import com.owncloud.android.ui.preview.PreviewAudioFragment;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.ui.preview.PreviewTextFragment;
import com.owncloud.android.ui.preview.PreviewVideoFragment;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.PreferenceUtils;
import timber.log.Timber;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A Fragment that lists all files and folders in a given path.
 * <p>
 * TODO refactor to get rid of direct dependency on FileDisplayActivity
 */
public class OCFileListFragment extends ExtendedListFragment implements
        SearchView.OnQueryTextListener, View.OnFocusChangeListener {

    private static final String MY_PACKAGE = OCFileListFragment.class.getPackage() != null ?
            OCFileListFragment.class.getPackage().getName() : "com.owncloud.android.ui.fragment";

    private final static String ARG_ALLOW_CONTEXTUAL_MODE = MY_PACKAGE + ".ALLOW_CONTEXTUAL";
    private final static String ARG_HIDE_FAB = MY_PACKAGE + ".HIDE_FAB";

    private static final String KEY_FILE = MY_PACKAGE + ".extra.FILE";
    private static final String KEY_FAB_EVER_CLICKED = "FAB_EVER_CLICKED";

    private static final String GRID_IS_PREFERED_PREFERENCE = "gridIsPrefered";

    private static String DIALOG_CREATE_FOLDER = "DIALOG_CREATE_FOLDER";

    private final String ALL_FILES_SAF_REGEX = "*/*";

    private FileFragment.ContainerActivity mContainerActivity;

    private OCFile mFile = null;
    private FileListListAdapter mFileListAdapter;

    private boolean mEnableSelectAll = true;

    private int mStatusBarColorActionMode;
    private int mStatusBarColor;

    private boolean mHideFab = true;
    private boolean miniFabClicked = false;
    private ActionMode mActiveActionMode;
    private OCFileListFragment.MultiChoiceModeListener mMultiChoiceModeListener;

    private SearchView mSearchView;

    /**
     * Public factory method to create new {@link OCFileListFragment} instances.
     *
     * @param justFolders          When 'true', only folders will be shown to the user, not files.
     * @param fileListOption       File list option to show. All files by default.
     * @param pickingAFolder       When 'true', only folders will be clickable when selecting a folder when copying or
     *                             moving files or configuring upload path for camera uploads
     * @param hideFAB              When 'true', floating action button is hidden.
     * @param allowContextualMode  When 'true', contextual action mode is enabled long-pressing an item.
     * @return New fragment with arguments set.
     */
    public static OCFileListFragment newInstance(
            boolean justFolders,
            FileListOption fileListOption,
            boolean pickingAFolder,
            boolean hideFAB,
            boolean allowContextualMode
    ) {
        OCFileListFragment frag = new OCFileListFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_JUST_FOLDERS, justFolders);
        if (fileListOption == null) {
            fileListOption = FileListOption.ALL_FILES;
        }
        if (fileListOption != FileListOption.ALL_FILES) {
            hideFAB = true;
        }
        args.putSerializable(ARG_LIST_FILE_OPTION, fileListOption);
        args.putBoolean(ARG_PICKING_A_FOLDER, pickingAFolder);
        args.putBoolean(ARG_HIDE_FAB, hideFAB);
        args.putBoolean(ARG_ALLOW_CONTEXTUAL_MODE, allowContextualMode);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mStatusBarColorActionMode = getResources().getColor(R.color.action_mode_status_bar_background);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Timber.v("onAttach");
        try {
            mContainerActivity = (FileFragment.ContainerActivity) context;

        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " +
                    FileFragment.ContainerActivity.class.getSimpleName());
        }
        try {
            setOnRefreshListener((OnEnforceableRefreshListener) context);

        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " +
                    SwipeRefreshLayout.OnRefreshListener.class.getSimpleName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timber.i("onCreateView() start");
        View v = super.onCreateView(inflater, container, savedInstanceState);
        Bundle args = getArguments();
        boolean allowContextualActions = (args != null) && args.getBoolean(ARG_ALLOW_CONTEXTUAL_MODE, false);
        if (allowContextualActions) {
            setChoiceModeAsMultipleModal(savedInstanceState);
        }
        Timber.i("onCreateView() end");
        return v;
    }

    @Override
    public void onDetach() {
        setOnRefreshListener(null);
        mContainerActivity = null;
        super.onDetach();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Timber.v("onActivityCreated() start");

        if (savedInstanceState != null) {
            mFile = savedInstanceState.getParcelable(KEY_FILE);
        }

        updateListOfFiles(FileListOption.ALL_FILES);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mSearchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        mSearchView.setMaxWidth(Integer.MAX_VALUE);
        mSearchView.setQueryHint(getResources().getString(R.string.actionbar_search));
        mSearchView.setOnQueryTextFocusChangeListener(this);
        mSearchView.setOnQueryTextListener(this);
    }

    public void updateFileListOption(FileListOption newFileListOption){
        updateListOfFiles(newFileListOption);
        listDirectory(true);
    }

    private void updateListOfFiles(
            FileListOption fileListOption
    ) {
        boolean justFolders = isShowingJustFolders();
        setFooterEnabled(!justFolders);

        boolean onlyAvailableOffline = fileListOption == FileListOption.AV_OFFLINE;
        boolean sharedByLinkFiles = fileListOption == FileListOption.SHARED_BY_LINK;

        boolean folderPicker = isPickingAFolder();

        mFileListAdapter = new FileListListAdapter(
                justFolders,
                onlyAvailableOffline,
                sharedByLinkFiles,
                folderPicker,
                getActivity(),
                mContainerActivity
        );
        setListAdapter(mFileListAdapter);

        Bundle args = getArguments();
        mHideFab = (args != null) && args.getBoolean(ARG_HIDE_FAB, false);
        if (mHideFab) {
            setFabEnabled(false);
        } else {
            setFabEnabled(true);
            registerFabListeners();

            // detect if a mini FAB has ever been clicked
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            if (prefs.getLong(KEY_FAB_EVER_CLICKED, 0) > 0) {
                miniFabClicked = true;
            }

            // add labels to the min FABs when none of them has ever been clicked on
            if (!miniFabClicked) {
                setFabLabels();
            } else {
                removeFabLabels();
            }
        }

        // Allow or disallow touches with other visible windows
        CoordinatorLayout coordinatorLayout = requireActivity().findViewById(R.id.coordinator_layout);
        coordinatorLayout.setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(getContext())
        );
    }

    /**
     * adds labels to all mini FABs.
     */
    private void setFabLabels() {
        getFabUpload().setTitle(getResources().getString(R.string.actionbar_upload));
        getFabMkdir().setTitle(getResources().getString(R.string.actionbar_mkdir));
    }

    /**
     * registers all listeners on all mini FABs.
     */
    private void registerFabListeners() {
        registerFabUploadListeners();
        registerFabMkDirListeners();
    }

    /**
     * registers {@link android.view.View.OnClickListener} and {@link android.view.View.OnLongClickListener}
     * on the Upload mini FAB for the linked action an {@link Snackbar} showing the underlying action.
     */
    private void registerFabUploadListeners() {
        getFabUpload().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View uploadBottomSheet = getLayoutInflater().inflate(R.layout.upload_bottom_sheet_fragment, null);
                final BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
                dialog.setContentView(uploadBottomSheet);
                final LinearLayout uploadFilesLinearLayout = uploadBottomSheet.findViewById(R.id.files_linear_layout);
                LinearLayout uploadFromCameraLinearLayout =
                        uploadBottomSheet.findViewById(R.id.upload_from_camera_linear_layout);
                TextView uploadToTextView = uploadBottomSheet.findViewById(R.id.upload_to_text_view);
                uploadFilesLinearLayout.setOnTouchListener((v13, event) -> {
                    Intent action = new Intent(Intent.ACTION_GET_CONTENT);
                    action = action.setType(ALL_FILES_SAF_REGEX).addCategory(Intent.CATEGORY_OPENABLE);
                    action.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    getActivity().startActivityForResult(
                            Intent.createChooser(action, getString(R.string.upload_chooser_title)),
                            FileDisplayActivity.REQUEST_CODE__SELECT_CONTENT_FROM_APPS
                    );
                    dialog.hide();
                    return false;
                });
                uploadFromCameraLinearLayout.setOnTouchListener((v12, event) -> {
                    ((FileDisplayActivity) getActivity()).getFilesUploadHelper().uploadFromCamera(FileDisplayActivity.REQUEST_CODE__UPLOAD_FROM_CAMERA);
                    dialog.hide();
                    return false;
                });
                uploadToTextView.setText(String.format(getResources().getString(R.string.upload_to),
                        getResources().getString(R.string.app_name)));
                final BottomSheetBehavior uploadBottomSheetBehavior =
                        BottomSheetBehavior.from((View) uploadBottomSheet.getParent());
                dialog.setOnShowListener(dialog1 ->
                        uploadBottomSheetBehavior.setPeekHeight(uploadBottomSheet.getMeasuredHeight()));
                dialog.show();
                getFabMain().collapse();
                recordMiniFabClick();
            }
        });

        getFabUpload().setOnLongClickListener(v -> {
            showSnackMessage(R.string.actionbar_upload);
            return true;
        });
    }

    /**
     * Registers {@link android.view.View.OnClickListener} and {@link android.view.View.OnLongClickListener}
     * on the 'Create Dir' mini FAB for the linked action and {@link Snackbar} showing the underlying action.
     */
    private void registerFabMkDirListeners() {
        getFabMkdir().setOnClickListener(v -> {
            CreateFolderDialogFragment dialog = CreateFolderDialogFragment.newInstance(mFile);
            dialog.show(requireActivity().getSupportFragmentManager(), DIALOG_CREATE_FOLDER);
            getFabMain().collapse();
            recordMiniFabClick();
        });

        getFabMkdir().setOnLongClickListener(v -> {
            showSnackMessage(R.string.actionbar_mkdir);
            return true;
        });
    }

    /**
     * records a click on a mini FAB and thus:
     * <ol>
     * <li>persists the click fact</li>
     * <li>removes the mini FAB labels</li>
     * </ol>
     */
    private void recordMiniFabClick() {
        // only record if it hasn't been done already at some other time
        if (!miniFabClicked) {
            final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            sp.edit().putLong(KEY_FAB_EVER_CLICKED, 1).apply();
            miniFabClicked = true;
        }
    }

    /**
     * removes the labels on all known min FABs.
     */
    private void removeFabLabels() {
        getFabUpload().setTitle(null);
        getFabMkdir().setTitle(null);
        ((TextView) getFabUpload().getTag(com.getbase.floatingactionbutton.R.id.fab_label)).setVisibility(View.GONE);
        ((TextView) getFabMkdir().getTag(com.getbase.floatingactionbutton.R.id.fab_label)).setVisibility(View.GONE);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        mFileListAdapter.filterBySearch(query);
        return true;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            setMessageForEmptyList(getString(R.string.local_file_list_search_with_no_matches));
        } else { // Set default message for empty list of files
            ((FileDisplayActivity) requireActivity()).setBackgroundText();
        }
    }

    public boolean isSingleItemChecked() {
        return mFileListAdapter.getCheckedItems(getListView()).size() == 1;
    }

    /**
     * Handler for multiple selection mode.
     * <p>
     * Manages input from the user when one or more files or folders are selected in the list.
     * <p>
     * Also listens to changes in navigation drawer to hide and recover multiple selection when it's opened
     * and closed.
     */
    private class MultiChoiceModeListener
            implements AbsListView.MultiChoiceModeListener, DrawerLayout.DrawerListener {

        private static final String KEY_ACTION_MODE_CLOSED_BY_DRAWER = "KILLED_ACTION_MODE";
        private static final String KEY_SELECTION_WHEN_CLOSED_BY_DRAWER = "CHECKED_ITEMS";

        /**
         * True when action mode is finished because the drawer was opened
         */
        private boolean mActionModeClosedByDrawer = false;

        /**
         * Selected items in list when action mode is closed by drawer
         */
        private SparseBooleanArray mSelectionWhenActionModeClosedByDrawer = null;

        @Override
        public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
            // nothing to do
        }

        @Override
        public void onDrawerOpened(@NonNull View drawerView) {
            clearLocalSearchView();
        }

        /**
         * When the navigation drawer is closed, action mode is recovered in the same state as was
         * when the drawer was (started to be) opened.
         *
         * @param drawerView Navigation drawer just closed.
         */
        @Override
        public void onDrawerClosed(@NonNull View drawerView) {
            if (mSelectionWhenActionModeClosedByDrawer != null && mActionModeClosedByDrawer) {
                for (int i = 0; i < mSelectionWhenActionModeClosedByDrawer.size(); i++) {
                    if (mSelectionWhenActionModeClosedByDrawer.valueAt(i)) {
                        getListView().setItemChecked(
                                mSelectionWhenActionModeClosedByDrawer.keyAt(i),
                                true
                        );
                    }
                }
            }
            mSelectionWhenActionModeClosedByDrawer = null;
        }

        /**
         * If the action mode is active when the navigation drawer starts to move, the action
         * mode is closed and the selection stored to be recovered when the drawer is closed.
         *
         * @param newState One of STATE_IDLE, STATE_DRAGGING or STATE_SETTLING.
         */
        @Override
        public void onDrawerStateChanged(int newState) {
            if (DrawerLayout.STATE_DRAGGING == newState && mActiveActionMode != null) {
                mSelectionWhenActionModeClosedByDrawer = getListView().getCheckedItemPositions().clone();
                mActiveActionMode.finish();
                mActionModeClosedByDrawer = true;
            }
        }

        /**
         * Update action mode bar when an item is selected / unselected in the list
         */
        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            getListView().invalidateViews();
            mode.invalidate();
            if (mFileListAdapter.getCheckedItems(getListView()).size() == mFileListAdapter.getCount()) {
                mEnableSelectAll = false;
            } else {
                if (!checked) {
                    mEnableSelectAll = true;
                }
            }
        }

        /**
         * Load menu and customize UI when action mode is started.
         */
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mActiveActionMode = mode;

            MenuInflater inflater = requireActivity().getMenuInflater();
            inflater.inflate(R.menu.file_actions_menu, menu);
            mode.invalidate();

            //set gray color
            Window w = getActivity().getWindow();
            mStatusBarColor = w.getStatusBarColor();
            w.setStatusBarColor(mStatusBarColorActionMode);

            // hide FAB in multi selection mode
            setFabEnabled(false);

            return true;
        }

        /**
         * Updates available action in menu depending on current selection.
         */
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            List<OCFile> checkedFiles = mFileListAdapter.getCheckedItems(getListView());
            final int checkedCount = checkedFiles.size();
            String title = getResources().getQuantityString(
                    R.plurals.items_selected_count,
                    checkedCount,
                    checkedCount
            );
            mode.setTitle(title);
            FileMenuFilter mf = new FileMenuFilter(
                    checkedFiles,
                    ((FileActivity) requireActivity()).getAccount(),
                    mContainerActivity,
                    getActivity()
            );
            mf.filter(menu, mEnableSelectAll, true, isShowingOnlyAvailableOffline(), isShowingSharedByLinkFiles());
            return true;
        }

        /**
         * Starts the corresponding action when a menu item is tapped by the user.
         */
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return onFileActionChosen(item.getItemId());
        }

        /**
         * Restores UI.
         */
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActiveActionMode = null;

            // reset to previous color
            requireActivity().getWindow().setStatusBarColor(mStatusBarColor);

            // show FAB on multi selection mode exit
            if (!mHideFab) {
                setFabEnabled(true);
            }
        }

        void storeStateIn(Bundle outState) {
            outState.putBoolean(KEY_ACTION_MODE_CLOSED_BY_DRAWER, mActionModeClosedByDrawer);
            if (mSelectionWhenActionModeClosedByDrawer != null) {
                SparseBooleanArrayParcelable sbap = new SparseBooleanArrayParcelable(
                        mSelectionWhenActionModeClosedByDrawer
                );
                outState.putParcelable(KEY_SELECTION_WHEN_CLOSED_BY_DRAWER, sbap);
            }
        }

        void loadStateFrom(Bundle savedInstanceState) {
            mActionModeClosedByDrawer = savedInstanceState.getBoolean(
                    KEY_ACTION_MODE_CLOSED_BY_DRAWER,
                    mActionModeClosedByDrawer
            );
            SparseBooleanArrayParcelable sbap = savedInstanceState.getParcelable(
                    KEY_SELECTION_WHEN_CLOSED_BY_DRAWER
            );
            if (sbap != null) {
                mSelectionWhenActionModeClosedByDrawer = sbap.getSparseBooleanArray();
            }
        }
    }

    private void clearLocalSearchView() {
        ((FileActivity) requireActivity()).hideSoftKeyboard();
        mFileListAdapter.clearFilterBySearch();
        if (mSearchView != null) {
            mSearchView.onActionViewCollapsed();
        }
    }

    /**
     * Init listener that will handle interactions in multiple selection mode.
     */
    private void setChoiceModeAsMultipleModal(Bundle savedInstanceState) {
        setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mMultiChoiceModeListener = new MultiChoiceModeListener();
        if (savedInstanceState != null) {
            mMultiChoiceModeListener.loadStateFrom(savedInstanceState);
        }
        setMultiChoiceModeListener(mMultiChoiceModeListener);
        ((FileActivity) requireActivity()).addDrawerListener(mMultiChoiceModeListener);
    }

    /**
     * Saves the current listed folder
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_FILE, mFile);

        // If this fragment is used to show target folders where a selected file/folder can be
        // copied/moved, multiple choice is disabled
        if (mMultiChoiceModeListener != null) {
            mMultiChoiceModeListener.storeStateIn(outState);
        }
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        if (isShowingOnlyAvailableOffline() || isShowingSharedByLinkFiles()) {
            super.onPrepareOptionsMenu(menu);

            MenuItem item = menu.findItem(R.id.action_sync_account);
            if (item != null) {
                item.setVisible(false);
                item.setEnabled(false);
            }
        }
        changeGridIcon(menu);   // this is enough if the option stays out of the action bar
    }

    /**
     * Call this, when the user presses the up button.
     * <p>
     * Tries to move up the current folder one level. If the parent folder was removed from the
     * database, it continues browsing up until finding an existing folders.
     * <p/>
     * return       Count of folder levels browsed up.
     */
    public int onBrowseUp() {
        OCFile parentDir;
        int moveCount = 0;

        if (mFile != null) {
            FileDataStorageManager storageManager = mContainerActivity.getStorageManager();

            String parentPath = null;
            if (mFile.getParentId() != FileDataStorageManager.ROOT_PARENT_ID) {
                parentPath = new File(mFile.getRemotePath()).getParent();
                parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath :
                        parentPath + OCFile.PATH_SEPARATOR;
                parentDir = storageManager.getFileByPath(parentPath);
                moveCount++;
            } else {
                parentDir = storageManager.getFileByPath(OCFile.ROOT_PATH);
            }
            while (parentDir == null) {
                parentPath = new File(parentPath).getParent();
                parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath :
                        parentPath + OCFile.PATH_SEPARATOR;
                parentDir = storageManager.getFileByPath(parentPath);
                moveCount++;
            }   // exit is granted because storageManager.getFileByPath("/") never returns null

            if (isShowingOnlyAvailableOffline() && !parentDir.isAvailableOffline()) {
                parentDir = storageManager.getFileByPath(OCFile.ROOT_PATH);
            }

            if (isShowingSharedByLinkFiles() && !parentDir.isSharedViaLink()) {
                parentDir = storageManager.getFileByPath(OCFile.ROOT_PATH);
            }

            mFile = parentDir;

            listDirectoryWidthAnimationUp(mFile);

            onRefresh(false);

            // restore index and top position
            restoreIndexAndTopPosition();

        }   // else - should never happen now

        return moveCount;
    }

    private void listDirectoryWithAnimationDown(final OCFile file) {
        if (isInPowerSaveMode()) {
            listDirectory(file);
        } else {
            Animation fadeOutFront = AnimationUtils.loadAnimation(getContext(), R.anim.dir_fadeout_front);
            Handler eventHandler = new Handler();

            // This is a ugly hack for getting rid of the "ArrayOutOfBound" exception we get when we
            // call listDirectory() from the Animation callback
            eventHandler.postDelayed(() -> {
                listDirectory(file);
                Animation fadeInBack = AnimationUtils.loadAnimation(getContext(), R.anim.dir_fadein_back);
                getListView().setAnimation(fadeInBack);
            }, getResources().getInteger(R.integer.folder_animation_duration));
            getListView().startAnimation(fadeOutFront);
        }
    }

    private boolean isInPowerSaveMode() {
        PowerManager powerManager = (PowerManager) requireActivity().getSystemService(Context.POWER_SERVICE);
        return (powerManager != null) && powerManager.isPowerSaveMode();
    }

    private void listDirectoryWidthAnimationUp(final OCFile file) {
        if (isInPowerSaveMode()) {
            listDirectory(file);
        } else {
            if (getListView().getVisibility() == View.GONE) {
                listDirectory(file);
                Animation fadeInFront = AnimationUtils.loadAnimation(getContext(), R.anim.dir_fadein_front);
                getListView().startAnimation(fadeInFront);
                return;
            }

            Handler eventHandler = new Handler();
            Animation fadeOutBack = AnimationUtils.loadAnimation(getContext(), R.anim.dir_fadeout_back);

            // This is a ugly hack for getting rid of the "ArrayOutOfBound" exception we get when we
            // call listDirectory() from the Animation callback
            eventHandler.postDelayed(() -> {
                listDirectory(file);
                Animation fadeInFront = AnimationUtils.loadAnimation(getContext(), R.anim.dir_fadein_front);
                getListView().startAnimation(fadeInFront);
            }, getResources().getInteger(R.integer.folder_animation_duration));
            getListView().startAnimation(fadeOutBack);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        OCFile file = (OCFile) mFileListAdapter.getItem(position);
        if (file != null) {
            if (file.isFolder()) {
                listDirectoryWithAnimationDown(file);
                // then, notify parent activity to let it update its state and view
                mContainerActivity.onBrowsedDownTo(file);
                // save index and top position
                saveIndexAndTopPosition(position);
            } else { /// Click on a file
                if (PreviewImageFragment.canBePreviewed(file)) {
                    // preview image - it handles the sync, if needed
                    ((FileDisplayActivity) mContainerActivity).startImagePreview(file);
                } else if (PreviewTextFragment.canBePreviewed(file)) {
                    ((FileDisplayActivity) mContainerActivity).startTextPreview(file);
                    mContainerActivity.getFileOperationsHelper().syncFile(file);

                } else if (PreviewAudioFragment.canBePreviewed(file)) {
                    // media preview
                    ((FileDisplayActivity) mContainerActivity).startAudioPreview(file, 0);
                    mContainerActivity.getFileOperationsHelper().syncFile(file);

                } else if (PreviewVideoFragment.canBePreviewed(file) &&
                        !fileIsDownloading(file)) {

                    // Available offline exception, don't initialize streaming
                    if (!file.isDown() && file.isAvailableOffline()) {
                        // sync file content, then open with external apps
                        ((FileDisplayActivity) mContainerActivity).startSyncThenOpen(file);
                    } else {
                        // media preview
                        ((FileDisplayActivity) mContainerActivity).startVideoPreview(file, 0);
                    }

                    // If the file is already downloaded sync it, just to update it if there is a
                    // new available file version
                    if (file.isDown()) {
                        mContainerActivity.getFileOperationsHelper().syncFile(file);
                    }
                } else {
                    // sync file content, then open with external apps
                    ((FileDisplayActivity) mContainerActivity).startSyncThenOpen(file);
                }

            }

        } else {
            Timber.d("Null object in ListAdapter!!");
        }

    }

    /**
     * @return 'true' if the file is being downloaded, 'false' otherwise.
     */
    private boolean fileIsDownloading(OCFile file) {
        return mContainerActivity.getFileDownloaderBinder().isDownloading(
                ((FileActivity) mContainerActivity).getAccount(), file);
    }

    public void selectAll() {
        for (int i = 0; i < mFileListAdapter.getCount(); i++) {
            getListView().setItemChecked(i, true);
        }
    }

    public int getNoOfItems() {
        return getListView().getCount();
    }

    /**
     * Start the appropriate action(s) on the currently selected files given menu selected by the user.
     *
     * @param menuId Identifier of the action menu selected by the user
     * @return 'true' if the menu selection started any action, 'false' otherwise.
     */
    private boolean onFileActionChosen(int menuId) {
        final ArrayList<OCFile> checkedFiles = mFileListAdapter.getCheckedItems(getListView());
        if (checkedFiles.size() <= 0) {
            return false;
        }

        if (checkedFiles.size() == 1) {
            /// action only possible on a single file
            OCFile singleFile = checkedFiles.get(0);
            switch (menuId) {
                case R.id.action_share_file: {
                    mContainerActivity.getFileOperationsHelper().showShareFile(singleFile);
                    mEnableSelectAll = false;
                    return true;
                }
                case R.id.action_open_file_with: {
                    mContainerActivity.getFileOperationsHelper().openFile(singleFile);
                    return true;
                }
                case R.id.action_rename_file: {
                    RenameFileDialogFragment dialog = RenameFileDialogFragment.newInstance(singleFile);
                    dialog.show(getFragmentManager(), FileDetailFragment.FTAG_RENAME_FILE);
                    return true;
                }
                case R.id.action_see_details: {
                    if (mActiveActionMode != null) {
                        mActiveActionMode.finish();
                    }
                    mContainerActivity.showDetails(singleFile);
                    return true;
                }
                case R.id.action_send_file: {
                    // Obtain the file
                    if (!singleFile.isDown()) {  // Download the file
                        Timber.d("%s : File must be downloaded", singleFile.getRemotePath());
                        ((FileDisplayActivity) mContainerActivity).startDownloadForSending(singleFile);

                    } else {
                        mContainerActivity.getFileOperationsHelper().sendDownloadedFile(singleFile);
                    }
                    return true;
                }
            }
        }

        /// actions possible on a batch of files
        switch (menuId) {
            case R.id.file_action_select_all: {
                selectAll();
                return true;
            }
            case R.id.action_select_inverse: {
                for (int i = 0; i < mFileListAdapter.getCount(); i++) {
                    if (getListView().isItemChecked(i)) {
                        getListView().setItemChecked(i, false);
                    } else {
                        getListView().setItemChecked(i, true);
                    }
                }
                return true;
            }
            case R.id.action_remove_file: {
                RemoveFilesDialogFragment dialog = RemoveFilesDialogFragment.newInstance(checkedFiles);
                dialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
                return true;
            }
            case R.id.action_download_file:
            case R.id.action_sync_file: {
                mContainerActivity.getFileOperationsHelper().syncFiles(checkedFiles);
                return true;
            }
            case R.id.action_cancel_sync: {
                ((FileDisplayActivity) mContainerActivity).cancelTransference(checkedFiles);
                return true;
            }
            case R.id.action_set_available_offline: {
                mContainerActivity.getFileOperationsHelper().toggleAvailableOffline(checkedFiles, true);
                getListView().invalidateViews();
                return true;
            }
            case R.id.action_unset_available_offline: {
                mContainerActivity.getFileOperationsHelper().toggleAvailableOffline(checkedFiles, false);
                getListView().invalidateViews();
                invalidateActionMode();
                if (isShowingOnlyAvailableOffline()) {
                    onRefresh();
                }
                return true;
            }
            case R.id.action_move: {
                Intent action = new Intent(getActivity(), FolderPickerActivity.class);
                action.putParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES, checkedFiles);
                requireActivity().startActivityForResult(action, FileDisplayActivity.REQUEST_CODE__MOVE_FILES);
                return true;
            }
            case R.id.action_copy:
                Intent action = new Intent(getActivity(), FolderPickerActivity.class);
                action.putParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES, checkedFiles);
                requireActivity().startActivityForResult(action, FileDisplayActivity.REQUEST_CODE__COPY_FILES);
                return true;
            default:
                return false;
        }
    }

    /**
     * Use this to query the {@link OCFile} that is currently
     * being displayed by this fragment
     *
     * @return The currently viewed OCFile
     */
    public OCFile getCurrentFile() {
        return mFile;
    }

    /**
     * Calls {@link OCFileListFragment#listDirectory(OCFile)} with a null parameter
     */
    public void listDirectory(boolean reloadData) {
        if (reloadData) {
            listDirectory(null);
        } else {
            getListView().invalidateViews();
        }
    }

    /**
     * Lists the given directory on the view. When the input parameter is null,
     * it will either refresh the last known directory. list the root
     * if there never was a directory.
     *
     * @param directory File to be listed
     */
    public void listDirectory(OCFile directory) {
        FileDataStorageManager storageManager = mContainerActivity.getStorageManager();
        if (storageManager != null) {

            // Check input parameters for null
            if (directory == null) {
                if (mFile != null) {
                    directory = mFile;
                } else {
                    directory = storageManager.getFileByPath(OCFile.ROOT_PATH);
                    if (directory == null) {
                        return; // no files, wait for sync
                    }
                }
            }

            // If that's not a directory -> List its parent
            if (!directory.isFolder()) {
                Timber.w("You see, that is not a directory -> %s", directory.toString());
                directory = storageManager.getFileById(directory.getParentId());
            }

            // If available offline option and folder is not available offline -> list root
            if (!directory.isAvailableOffline() && isShowingOnlyAvailableOffline()) {
                directory = storageManager.getFileByPath(OCFile.ROOT_PATH);
            }

            if (!directory.isSharedViaLink() && isShowingSharedByLinkFiles()) {
                directory = storageManager.getFileByPath(OCFile.ROOT_PATH);
            }

            mFileListAdapter.swapDirectory(directory, storageManager);
            if (mFile == null || !mFile.equals(directory)) {
                mCurrentListView.setSelection(0);
            }
            mFile = directory;

            updateLayout();
        }
    }

    private void updateLayout() {
        if (!isShowingJustFolders()) {
            int filesCount = 0, foldersCount = 0;
            int count = mFileListAdapter.getCount();
            OCFile file;
            for (int i = 0; i < count; i++) {
                file = (OCFile) mFileListAdapter.getItem(i);
                if (file.isFolder()) {
                    foldersCount++;
                } else {
                    if (!file.isHidden()) {
                        filesCount++;
                    }
                }
            }

            // decide grid vs list view
            OwnCloudVersion version = AccountUtils.getServerVersion(((FileActivity) mContainerActivity).getAccount());
            if (version != null && isGridViewPreferred(mFile)) {
                switchToGridView();
            } else {
                switchToListView();
            }

            // set footer text
            setFooterText(generateFooterText(filesCount, foldersCount));
        }
        invalidateActionMode();
        clearLocalSearchView();
    }

    private void invalidateActionMode() {
        if (mActiveActionMode != null) {
            mActiveActionMode.invalidate();
        }
    }

    private String generateFooterText(int filesCount, int foldersCount) {
        String output;
        if (filesCount <= 0) {
            if (foldersCount <= 0) {
                output = "";

            } else if (foldersCount == 1) {
                output = getResources().getString(R.string.file_list__footer__folder);

            } else { // foldersCount > 1
                output = getResources().getString(R.string.file_list__footer__folders, foldersCount);
            }

        } else if (filesCount == 1) {
            if (foldersCount <= 0) {
                output = getResources().getString(R.string.file_list__footer__file);

            } else if (foldersCount == 1) {
                output = getResources().getString(R.string.file_list__footer__file_and_folder);

            } else { // foldersCount > 1
                output = getResources().getString(R.string.file_list__footer__file_and_folders, foldersCount);
            }
        } else {    // filesCount > 1
            if (foldersCount <= 0) {
                output = getResources().getString(R.string.file_list__footer__files, filesCount);

            } else if (foldersCount == 1) {
                output = getResources().getString(R.string.file_list__footer__files_and_folder, filesCount);

            } else { // foldersCount > 1
                output = getResources().getString(
                        R.string.file_list__footer__files_and_folders, filesCount, foldersCount
                );

            }
        }
        return output;
    }

    public void sortByName(boolean descending) {
        mFileListAdapter.setSortOrder(FileStorageUtils.SORT_NAME, descending);
    }

    public void sortByDate(boolean descending) {
        mFileListAdapter.setSortOrder(FileStorageUtils.SORT_DATE, descending);
    }

    public void sortBySize(boolean descending) {
        mFileListAdapter.setSortOrder(FileStorageUtils.SORT_SIZE, descending);
    }

    /**
     * Determines if user set folder to grid or list view. If folder is not set itself,
     * it finds a parent that is set (at least root is set).
     *
     * @param file Folder to check.
     * @return 'true' is folder should be shown in grid mode, 'false' if list mode is preferred.
     */
    private boolean isGridViewPreferred(OCFile file) {
        if (file != null) {
            OCFile fileToTest = file;
            OCFile parentDir;
            String parentPath = null;
            FileDataStorageManager storageManager = mContainerActivity.getStorageManager();

            SharedPreferences setting =
                    requireActivity().getSharedPreferences(GRID_IS_PREFERED_PREFERENCE, Context.MODE_PRIVATE);

            if (setting.contains(String.valueOf(fileToTest.getFileId()))) {
                return setting.getBoolean(String.valueOf(fileToTest.getFileId()), false);
            } else {
                do {
                    if (fileToTest.getParentId() != FileDataStorageManager.ROOT_PARENT_ID) {
                        parentPath = new File(fileToTest.getRemotePath()).getParent();
                        parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath :
                                parentPath + OCFile.PATH_SEPARATOR;
                        parentDir = storageManager.getFileByPath(parentPath);
                    } else {
                        parentDir = storageManager.getFileByPath(OCFile.ROOT_PATH);
                    }

                    while (parentDir == null) {
                        parentPath = new File(parentPath).getParent();
                        parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath :
                                parentPath + OCFile.PATH_SEPARATOR;
                        parentDir = storageManager.getFileByPath(parentPath);
                    }
                    fileToTest = parentDir;
                } while (endWhile(parentDir, setting));
                return setting.getBoolean(String.valueOf(fileToTest.getFileId()), false);
            }
        } else {
            return false;
        }
    }

    private boolean endWhile(OCFile parentDir, SharedPreferences setting) {
        if (parentDir.getRemotePath().compareToIgnoreCase(OCFile.ROOT_PATH) == 0) {
            return false;
        } else {
            return !setting.contains(String.valueOf(parentDir.getFileId()));
        }
    }

    private void changeGridIcon(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.action_switch_view);
        if (isGridViewPreferred(mFile)) {
            menuItem.setTitle(getString(R.string.action_switch_list_view));
            menuItem.setIcon(R.drawable.ic_view_list);
        } else {
            menuItem.setTitle(getString(R.string.action_switch_grid_view));
            menuItem.setIcon(R.drawable.ic_view_module);
        }
    }

    public void setListAsPreferred() {
        saveGridAsPreferred(false);
        switchToListView();
    }

    public void setGridAsPreferred() {
        saveGridAsPreferred(true);
        switchToGridView();
    }

    private void saveGridAsPreferred(boolean setGrid) {
        SharedPreferences setting = requireActivity().getSharedPreferences(
                GRID_IS_PREFERED_PREFERENCE, Context.MODE_PRIVATE
        );

        SharedPreferences.Editor editor = setting.edit();
        editor.putBoolean(String.valueOf(mFile.getFileId()), setGrid);
        editor.apply();
    }

    /**
     * Show a temporary message in a Snackbar bound to the content view of the parent Activity
     *
     * @param messageResource Message to show.
     */
    private void showSnackMessage(int messageResource) {
        Snackbar snackbar = Snackbar.make(
                requireActivity().findViewById(R.id.coordinator_layout),
                messageResource,
                Snackbar.LENGTH_LONG
        );
        snackbar.show();
    }
}
