/**
 * ownCloud Android client application
 *
 * @author Christian Schabesberger
 * @author Abel García de Prada
 * @author Shashvat Kedia
 * Copyright (C) 2012 Bartek Przybylski
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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.owncloud.android.R;
import com.owncloud.android.ui.ExtendedListView;
import com.owncloud.android.ui.activity.FileListOption;
import com.owncloud.android.ui.activity.OnEnforceableRefreshListener;
import com.owncloud.android.utils.PreferenceUtils;
import third_parties.in.srain.cube.GridViewWithHeaderAndFooter;
import timber.log.Timber;

import java.util.ArrayList;

public class ExtendedListFragment extends Fragment
        implements OnItemClickListener, OnEnforceableRefreshListener {

    private static final String KEY_SAVED_LIST_POSITION = "SAVED_LIST_POSITION";

    private static final String KEY_INDEXES = "INDEXES";
    private static final String KEY_FIRST_POSITIONS = "FIRST_POSITIONS";
    private static final String KEY_TOPS = "TOPS";
    private static final String KEY_HEIGHT_CELL = "HEIGHT_CELL";
    private static final String KEY_EMPTY_LIST_MESSAGE = "EMPTY_LIST_MESSAGE";
    private static final String KEY_IS_GRID_VISIBLE = "IS_GRID_VISIBLE";

    static final String ARG_JUST_FOLDERS = ExtendedListFragment.class.getCanonicalName() + ".JUST_FOLDERS";
    static final String ARG_LIST_FILE_OPTION = ExtendedListFragment.class.getCanonicalName() +
            ".LIST_FILE_OPTION";
    protected static final String ARG_PICKING_A_FOLDER = ExtendedListFragment.class.getCanonicalName() +
            ".ARG_PICKING_A_FOLDER";

    private ProgressBar mProgressBar;
    private View mShadowView;

    SwipeRefreshLayout mRefreshListLayout;
    private SwipeRefreshLayout mRefreshGridLayout;
    SwipeRefreshLayout mRefreshEmptyLayout;
    TextView mEmptyListMessage;

    private FloatingActionsMenu mFabMain;
    private FloatingActionButton mFabUpload;
    private FloatingActionButton mFabMkdir;

    // Save the state of the scroll in browsing
    private ArrayList<Integer> mIndexes;
    private ArrayList<Integer> mFirstPositions;
    private ArrayList<Integer> mTops;
    private int mHeightCell = 0;

    private SwipeRefreshLayout.OnRefreshListener mOnRefreshListener = null;

    AbsListView mCurrentListView;
    private ExtendedListView mListView;
    private View mListFooterView;
    private GridViewWithHeaderAndFooter mGridView;
    private View mGridFooterView;

    private ListAdapter mAdapter;

    void setListAdapter(ListAdapter listAdapter) {
        mAdapter = listAdapter;
        mCurrentListView.setAdapter(listAdapter);
        mCurrentListView.invalidateViews();
    }

    protected AbsListView getListView() {
        return mCurrentListView;
    }

    FloatingActionButton getFabUpload() {
        return mFabUpload;
    }

    FloatingActionButton getFabMkdir() {
        return mFabMkdir;
    }

    public FloatingActionsMenu getFabMain() {
        return mFabMain;
    }

    void switchToGridView() {
        if (!isGridEnabled()) {
            mListView.setAdapter(null);
            mRefreshListLayout.setVisibility(View.GONE);
            mRefreshGridLayout.setVisibility(View.VISIBLE);
            mCurrentListView = mGridView;
            setListAdapter(mAdapter);
        }
    }

    void switchToListView() {
        if (isGridEnabled()) {
            mGridView.setAdapter(null);
            mRefreshGridLayout.setVisibility(View.GONE);
            mRefreshListLayout.setVisibility(View.VISIBLE);
            mCurrentListView = mListView;
            setListAdapter(mAdapter);
        }
    }

    public boolean isGridEnabled() {
        return (mCurrentListView == mGridView);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timber.v("onCreateView");
        View v = inflater.inflate(R.layout.list_fragment, null);

        mProgressBar = v.findViewById(R.id.syncProgressBar);
        mShadowView = v.findViewById(R.id.shadow_view);

        mListView = v.findViewById(R.id.list_root);
        mListView.setOnItemClickListener(this);
        mListFooterView = inflater.inflate(R.layout.list_footer, null, false);

        mListFooterView.setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(getContext())
        );

        mGridView = v.findViewById(R.id.grid_root);
        mGridView.setNumColumns(GridView.AUTO_FIT);
        mGridView.setOnItemClickListener(this);

        mGridFooterView = inflater.inflate(R.layout.list_footer, null, false);

        mGridFooterView.setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(getContext())
        );

        // Pull-down to refresh layout
        mRefreshListLayout = v.findViewById(R.id.swipe_containing_list);
        mRefreshGridLayout = v.findViewById(R.id.swipe_containing_grid);
        mRefreshEmptyLayout = v.findViewById(R.id.swipe_containing_empty);
        mEmptyListMessage = v.findViewById(R.id.empty_list_view);

        onCreateSwipeToRefresh(mRefreshListLayout);
        onCreateSwipeToRefresh(mRefreshGridLayout);
        onCreateSwipeToRefresh(mRefreshEmptyLayout);

        mListView.setEmptyView(mRefreshEmptyLayout);
        mGridView.setEmptyView(mRefreshEmptyLayout);

        mFabMain = v.findViewById(R.id.fab_main);
        mFabUpload = v.findViewById(R.id.fab_upload);
        mFabMkdir = v.findViewById(R.id.fab_mkdir);

        mCurrentListView = mListView;   // list by default
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(KEY_IS_GRID_VISIBLE, false)) {
                switchToGridView();
            }
            int referencePosition = savedInstanceState.getInt(KEY_SAVED_LIST_POSITION);
            if (isGridEnabled()) {
                Timber.v("Setting grid position %s", referencePosition);
                mGridView.setSelection(referencePosition);
            } else {
                Timber.v("Setting and centering around list position %s", referencePosition);
                mListView.setAndCenterSelection(referencePosition);
            }
        }

        return v;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mIndexes = savedInstanceState.getIntegerArrayList(KEY_INDEXES);
            mFirstPositions = savedInstanceState.getIntegerArrayList(KEY_FIRST_POSITIONS);
            mTops = savedInstanceState.getIntegerArrayList(KEY_TOPS);
            mHeightCell = savedInstanceState.getInt(KEY_HEIGHT_CELL);
            setMessageForEmptyList(savedInstanceState.getString(KEY_EMPTY_LIST_MESSAGE));

        } else {
            mIndexes = new ArrayList<>();
            mFirstPositions = new ArrayList<>();
            mTops = new ArrayList<>();
            mHeightCell = 0;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Timber.v("onSaveInstanceState()");
        savedInstanceState.putBoolean(KEY_IS_GRID_VISIBLE, isGridEnabled());
        savedInstanceState.putInt(KEY_SAVED_LIST_POSITION, getReferencePosition());
        savedInstanceState.putIntegerArrayList(KEY_INDEXES, mIndexes);
        savedInstanceState.putIntegerArrayList(KEY_FIRST_POSITIONS, mFirstPositions);
        savedInstanceState.putIntegerArrayList(KEY_TOPS, mTops);
        savedInstanceState.putInt(KEY_HEIGHT_CELL, mHeightCell);
        savedInstanceState.putString(KEY_EMPTY_LIST_MESSAGE, getEmptyViewText());
    }

    /**
     * Calculates the position of the item that will be used as a reference to
     * reposition the visible items in the list when the device is turned to
     * other position.
     * <p>
     * The current policy is take as a reference the visible item in the center
     * of the screen.
     *
     * @return The position in the list of the visible item in the center of the
     * screen.
     */
    protected int getReferencePosition() {
        if (mCurrentListView != null) {
            return (mCurrentListView.getFirstVisiblePosition() +
                    mCurrentListView.getLastVisiblePosition()) / 2;
        } else {
            return 0;
        }
    }

    /*
     * Restore index and position
     */
    protected void restoreIndexAndTopPosition() {
        if (mIndexes.size() > 0) {
            // needs to be checked; not every browse-up had a browse-down before 

            int index = mIndexes.remove(mIndexes.size() - 1);
            final int firstPosition = mFirstPositions.remove(mFirstPositions.size() - 1);
            int top = mTops.remove(mTops.size() - 1);

            Timber.v("Setting selection to position: " + firstPosition + "; top: " + top + "; index: " + index);

            if (mCurrentListView == mListView) {
                if (mHeightCell * index <= mListView.getHeight()) {
                    mListView.setSelectionFromTop(firstPosition, top);
                } else {
                    mListView.setSelectionFromTop(index, 0);
                }

            } else {
                if (mHeightCell * index <= mGridView.getHeight()) {
                    mGridView.setSelection(firstPosition);
                    //mGridView.smoothScrollToPosition(firstPosition);
                } else {
                    mGridView.setSelection(index);
                    //mGridView.smoothScrollToPosition(index);
                }
            }

        }
    }

    /*
     * Save index and top position
     */
    protected void saveIndexAndTopPosition(int index) {

        mIndexes.add(index);

        int firstPosition = mCurrentListView.getFirstVisiblePosition();
        mFirstPositions.add(firstPosition);

        View view = mCurrentListView.getChildAt(0);
        int top = (view == null) ? 0 : view.getTop();

        mTops.add(top);

        // Save the height of a cell
        mHeightCell = (view == null || mHeightCell != 0) ? mHeightCell : view.getHeight();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // to be @overriden
    }

    @Override
    public void onRefresh() {
        mRefreshListLayout.setRefreshing(false);
        mRefreshGridLayout.setRefreshing(false);
        mRefreshEmptyLayout.setRefreshing(false);

        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh();
        }
    }

    public void setOnRefreshListener(OnEnforceableRefreshListener listener) {
        mOnRefreshListener = listener;
    }

    /**
     * Disables swipe gesture.
     * <p>
     * Sets the 'enabled' state of the refresh layouts contained in the fragment.
     * <p>
     * When 'false' is set, prevents user gestures but keeps the option to refresh programatically,
     *
     * @param enabled Desired state for capturing swipe gesture.
     */
    public void setSwipeEnabled(boolean enabled) {
        mRefreshListLayout.setEnabled(enabled);
        mRefreshGridLayout.setEnabled(enabled);
        mRefreshEmptyLayout.setEnabled(enabled);
    }

    /**
     * Sets the 'visibility' state of the FAB contained in the fragment.
     * <p>
     * When 'false' is set, FAB visibility is set to View.GONE programatically,
     *
     * @param enabled Desired visibility for the FAB.
     */
    public void setFabEnabled(boolean enabled) {
        if (enabled) {
            mFabMain.setVisibility(View.VISIBLE);
        } else {
            mFabMain.setVisibility(View.GONE);
        }
    }

    /**
     * Set message for empty list view
     */
    public void setMessageForEmptyList(String message) {
        if (mEmptyListMessage != null) {
            mEmptyListMessage.setText(message);
        }
    }

    /**
     * Get the text of EmptyListMessage TextView
     *
     * @return String
     */
    public String getEmptyViewText() {
        return (mEmptyListMessage != null) ? mEmptyListMessage.getText().toString() : "";
    }

    protected void onCreateSwipeToRefresh(SwipeRefreshLayout refreshLayout) {
        // Colors in animations
        refreshLayout.setColorSchemeResources(R.color.color_accent, R.color.primary,
                R.color.primary_dark);

        refreshLayout.setOnRefreshListener(this);
    }

    @Override
    public void onRefresh(boolean ignoreETag) {
        mRefreshListLayout.setRefreshing(false);
        mRefreshGridLayout.setRefreshing(false);
        mRefreshEmptyLayout.setRefreshing(false);

        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh();
        }
    }

    protected void setChoiceMode(int choiceMode) {
        mListView.setChoiceMode(choiceMode);
        mGridView.setChoiceMode(choiceMode);
    }

    protected void setMultiChoiceModeListener(AbsListView.MultiChoiceModeListener listener) {
        mListView.setMultiChoiceModeListener(listener);
        mGridView.setMultiChoiceModeListener(listener);
    }

    /**
     * TODO doc
     * To be called before setAdapter, or GridViewWithHeaderAndFooter will throw an exception
     *
     * @param enabled
     */
    protected void setFooterEnabled(boolean enabled) {
        if (enabled) {
            if (mGridView.getFooterViewCount() == 0) {
                if (mGridFooterView.getParent() != null) {
                    ((ViewGroup) mGridFooterView.getParent()).removeView(mGridFooterView);
                }
                try {
                    mGridView.addFooterView(mGridFooterView, null, false);
                } catch (IllegalStateException ie) {
                    Timber.w("Could not add footer to grid view, because it exists");
                }
            }
            mGridFooterView.invalidate();

            if (mListView.getFooterViewsCount() == 0) {
                if (mListFooterView.getParent() != null) {
                    ((ViewGroup) mListFooterView.getParent()).removeView(mListFooterView);
                }
                mListView.addFooterView(mListFooterView, null, false);
            }
            mListFooterView.invalidate();

        } else {
            mGridView.removeFooterView(mGridFooterView);
            mListView.removeFooterView(mListFooterView);
        }
    }

    public ProgressBar getProgressBar() {
        return mProgressBar;
    }

    public View getShadowView() {
        return mShadowView;
    }

    /**
     * TODO doc
     *
     * @param text
     */
    protected void setFooterText(String text) {
        if (text != null && text.length() > 0) {
            ((TextView) mListFooterView.findViewById(R.id.footerText)).setText(text);
            ((TextView) mGridFooterView.findViewById(R.id.footerText)).setText(text);
            setFooterEnabled(true);

        } else {
            setFooterEnabled(false);
        }
    }

    public void setProgressBarAsIndeterminate(boolean indeterminate) {
        Timber.d("Setting progress visibility to %s", indeterminate);
        mShadowView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.setIndeterminate(indeterminate);
        mProgressBar.postInvalidate();
    }

    boolean isShowingJustFolders() {
        Bundle args = getArguments();
        return ((args != null) && args.getBoolean(ARG_JUST_FOLDERS, false));
    }

    boolean isShowingOnlyAvailableOffline() {
        Bundle args = getArguments();
        return ((args != null) && args.getSerializable(ARG_LIST_FILE_OPTION) == FileListOption.AV_OFFLINE);
    }

    boolean isShowingSharedByLinkFiles() {
        Bundle args = getArguments();
        return ((args != null) && args.getSerializable(ARG_LIST_FILE_OPTION) == FileListOption.SHARED_BY_LINK);
    }

    boolean isPickingAFolder() {
        Bundle args = getArguments();
        return ((args != null) && args.getBoolean(ARG_PICKING_A_FOLDER, false));
    }
}
