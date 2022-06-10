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
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.owncloud.android.R;
import com.owncloud.android.ui.activity.OnEnforceableRefreshListener;
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

    SwipeRefreshLayout mRefreshListLayout;
    private SwipeRefreshLayout mRefreshGridLayout;
    SwipeRefreshLayout mRefreshEmptyLayout;
    TextView mEmptyListMessage;

    // Save the state of the scroll in browsing
    private ArrayList<Integer> mIndexes;
    private ArrayList<Integer> mFirstPositions;
    private ArrayList<Integer> mTops;
    private int mHeightCell = 0;

    private SwipeRefreshLayout.OnRefreshListener mOnRefreshListener = null;

    AbsListView mCurrentListView;

    protected AbsListView getListView() {
        return mCurrentListView;
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // to be @overridden
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
}
