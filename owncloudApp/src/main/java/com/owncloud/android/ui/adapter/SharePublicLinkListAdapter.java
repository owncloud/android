package com.owncloud.android.ui.adapter;

/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Christian Schabesberger
 * Copyright (C) 2019 ownCloud GmbH.
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


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.lib.resources.shares.OCShare;

import java.util.ArrayList;

/**
 * Adapter to show a list of public links
 */
public class SharePublicLinkListAdapter extends ArrayAdapter{

    private Context mContext;
    private ArrayList<OCShare> mPublicLinks;
    private SharePublicLinkListAdapter.SharePublicLinkAdapterListener mListener;

    public SharePublicLinkListAdapter(Context context, int resource, ArrayList<OCShare>shares,
                                      SharePublicLinkListAdapter.SharePublicLinkAdapterListener listener) {
        super(context, resource);
        mContext= context;
        mPublicLinks = shares;
        mListener = listener;
    }

    @Override
    public int getCount() {
        return mPublicLinks.size();
    }

    @Override
    public Object getItem(int position) {
        return mPublicLinks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        LayoutInflater inflator = (LayoutInflater) mContext.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflator.inflate(R.layout.share_public_link_item, parent, false);

        // Allow or disallow touches with other visible windows
        view.setFilterTouchesWhenObscured(
                PreferenceUtils.shouldAllowTouchesWithOtherVisibleWindows(mContext)
        );

        if (mPublicLinks != null && mPublicLinks.size() > position) {

            OCShare share = mPublicLinks.get(position);

            TextView shareName = view.findViewById(R.id.publicLinkName);

            // If there's no name, set the token as name
            shareName.setText(share.getName().equals("") ? share.getToken() : share.getName());

            // bind listener to get link
            final ImageView getPublicLinkButton = view.findViewById(R.id.getPublicLinkButton);
            getPublicLinkButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.copyOrSendPublicLink(mPublicLinks.get(position));
                }
            });

            // bind listener to delete
            final ImageView deletePublicLinkButton = view.findViewById(R.id.deletePublicLinkButton);
            deletePublicLinkButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.removePublicShare(mPublicLinks.get(position));
                }
            });

            // bind listener to edit
            final ImageView editPublicLinkButton = view.findViewById(R.id.editPublicLinkButton);
            editPublicLinkButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.editPublicShare(mPublicLinks.get(position));
                }
            });
        }

        return view;
    }

    public interface SharePublicLinkAdapterListener {
        void copyOrSendPublicLink(OCShare share);
        void removePublicShare(OCShare share);
        void editPublicShare(OCShare share);
    }
}
