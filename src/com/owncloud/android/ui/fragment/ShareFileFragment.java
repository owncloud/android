/**
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author Juan Carlos González Cabrero
 * @author David González Verdugo
 * Copyright (C) 2017 ownCloud GmbH.
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

package com.owncloud.android.ui.fragment;

import android.accounts.Account;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.ShareActivity;
import com.owncloud.android.ui.adapter.SharePublicLinkListAdapter;
import com.owncloud.android.ui.adapter.ShareUserListAdapter;
import com.owncloud.android.ui.dialog.RemoveShareDialogFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimetypeIconUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

/**
 * Fragment for Sharing a file with sharees (users or groups) or creating
 * a public link.
 *
 * A simple {@link Fragment} subclass.
 *
 * Activities that contain this fragment must implement the
 * {@link ShareFragmentListener} interface
 * to handle interaction events.
 *
 * Use the {@link ShareFileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ShareFileFragment extends Fragment
        implements ShareUserListAdapter.ShareUserAdapterListener, SharePublicLinkListAdapter.
        SharePublicLinkAdapterListener {

    private static final String TAG = ShareFileFragment.class.getSimpleName();
    private static final String DEFAULT_NAME_SUFFIX = " (%1$d)";

    private static final String QUOTE_START = "\\Q";
    private static final String QUOTE_END = "\\E";
    private static final String DEFAULT_NAME_REGEX_SUFFIX = " \\((\\d+)\\)\\z";
    // matches suffix (end of the string with \z) in the form "(X)", where X is an integer of any length;
    // also captures the number to reference it later during the match;
    // reference in https://developer.android.com/reference/java/util/regex/Pattern.html#sum

    /**
     * The fragment initialization parameters
     */
    private static final String ARG_FILE = "FILE";
    private static final String ARG_ACCOUNT = "ACCOUNT";

    /**
     * File to share, received as a parameter in construction time
     */
    private OCFile mFile;

    /**
     * OC account holding the file to share, received as a parameter in construction time
     */
    private Account mAccount;

    /**
     * Reference to parent listener
     */
    private ShareFragmentListener mListener;

    /**
     * List of private shares bound to the file
     */
    private ArrayList<OCShare> mPrivateShares;

    /**
     * Adapter to show private shares
     */
    private ShareUserListAdapter mUserGroupsAdapter = null;

    /**
     *  List of public links bound to the file
     */
    private ArrayList<OCShare> mPublicLinks;

    /**
     * Adapter to show public shares
     */
    private SharePublicLinkListAdapter mPublicLinksAdapter = null;

    /**
     * Capabilities of the server
     */
    private OCCapability mCapabilities;


    /**
     * Public factory method to create new ShareFileFragment instances.
     *
     * @param fileToShare An {@link OCFile} to show in the fragment
     * @param account     An ownCloud account
     * @return A new instance of fragment ShareFileFragment.
     */
    public static ShareFileFragment newInstance(OCFile fileToShare, Account account) {
        ShareFileFragment fragment = new ShareFileFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, fileToShare);
        args.putParcelable(ARG_ACCOUNT, account);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Required empty public constructor
     */
    public ShareFileFragment() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log_OC.d(TAG, "onCreate");
        if (getArguments() != null) {
            mFile = getArguments().getParcelable(ARG_FILE);
            mAccount = getArguments().getParcelable(ARG_ACCOUNT);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreateView");

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.share_file_layout, container, false);

        // Setup layout
        // Image
        ImageView icon = (ImageView) view.findViewById(R.id.shareFileIcon);
        icon.setImageResource(MimetypeIconUtil.getFileTypeIconId(mFile.getMimetype(),
                mFile.getFileName()));
        if (mFile.isImage()) {
            String remoteId = String.valueOf(mFile.getRemoteId());
            Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(remoteId);
            if (thumbnail != null) {
                icon.setImageBitmap(thumbnail);
            }
        }
        // Name
        TextView fileNameHeader = (TextView) view.findViewById(R.id.shareFileName);
        fileNameHeader.setText(mFile.getFileName());
        // Size
        TextView size = (TextView) view.findViewById(R.id.shareFileSize);
        if (mFile.isFolder()) {
            size.setVisibility(View.GONE);
        } else {
            size.setText(DisplayUtils.bytesToHumanReadable(mFile.getFileLength(), getActivity()));
        }

        // Private link button
        ImageView getPrivateLinkButton = (ImageView) view.findViewById(R.id.getPrivateLinkButton);
        if (mFile.getPrivateLink() == null || mFile.getPrivateLink().isEmpty()) {
            getPrivateLinkButton.setVisibility(View.INVISIBLE);

        } else {
            getPrivateLinkButton.setVisibility(View.VISIBLE);

            getPrivateLinkButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.copyOrSendPrivateLink(mFile);
                }
            });

            getPrivateLinkButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    // Show a toast message explaining what a private link is
                    Toast.makeText(getActivity(), R.string.private_link_info, Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        }

        OwnCloudVersion serverVersion = AccountUtils.getServerVersion(mAccount);
        final boolean shareWithUsersEnable = (serverVersion != null && serverVersion.isSearchUsersSupported());

        TextView shareNoUsers = (TextView) view.findViewById(R.id.shareNoUsers);

        //  Add User/Groups Button
        ImageButton addUserGroupButton = (ImageButton)
                view.findViewById(R.id.addUserButton);

        // Change the sharing text depending on the server version (at least version 8.2 is needed
        // for sharing with other users)
        if (!shareWithUsersEnable) {
            shareNoUsers.setText(R.string.share_incompatible_version);
            shareNoUsers.setGravity(View.TEXT_ALIGNMENT_CENTER);
            addUserGroupButton.setVisibility(View.GONE);
        }

        addUserGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (shareWithUsersEnable) {
                    // Show Search Fragment
                    mListener.showSearchUsersAndGroups();
                } else {
                    String message = getString(R.string.share_sharee_unavailable);
                    Snackbar snackbar = Snackbar.make(
                            getActivity().findViewById(android.R.id.content),
                            message,
                            Snackbar.LENGTH_LONG
                    );
                    snackbar.show();
                }
            }
        });

        //  Add Public Link Button
        ImageButton addPublicLinkButton = (ImageButton)
                view.findViewById(R.id.addPublicLinkButton);

        addPublicLinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Show Add Public Link Fragment
                mListener.showAddPublicShare(getAvailableDefaultPublicName());
            }
        });

        // Hide share features sections that are not enabled
        hideSectionsDisabledInBuildTime(view);

        return view;
    }

    private String getAvailableDefaultPublicName() {
        if (mPublicLinks == null) {
            return "";
        }

        String defaultName = getString(R.string.share_via_link_default_name_template,
                mFile.getFileName());
        String defaultNameNumberedRegex = QUOTE_START + defaultName + QUOTE_END + DEFAULT_NAME_REGEX_SUFFIX;

        // Array with numbers already set in public link names
        ArrayList<Integer> usedNumbers = new ArrayList<>();

        // Inspect public links for default names already used
        boolean isDefaultNameSet = false;
        String number;
        for (OCShare share : mPublicLinks) {
            if (defaultName.equals(share.getName())) {
                isDefaultNameSet = true;
            } else if (share.getName().matches(defaultNameNumberedRegex)) {
                number = share.getName().replaceFirst(defaultNameNumberedRegex, "$1");
                try {
                    usedNumbers.add(Integer.parseInt(number));
                } catch (Exception e) {
                    Log_OC.e(TAG, "Wrong capture of number in share named " + share.getName(), e);
                    return "";  // better not suggesting a name than crashing
                }
            }
        }

        if (!isDefaultNameSet) {
            return defaultName;
        }

        // Sort used numbers in ascending order
        Collections.sort(usedNumbers);

        // Search for lowest unused number
        int chosenNumber = -1;
        if (usedNumbers.size() == 0 || usedNumbers.get(0) != 2) {
            chosenNumber = 2;

        } else {
            for (int i = 0; i < usedNumbers.size() - 1; i++) {
                int current = usedNumbers.get(i);
                int next = usedNumbers.get(i + 1);
                if (next - current > 1) {
                    chosenNumber = current + 1;
                    break;
                }
            }
            if (chosenNumber < 0) {
                // no missing number in the list - take the next to the last one
                chosenNumber = usedNumbers.get(usedNumbers.size() - 1) + 1;
            }
        }

        return defaultName + String.format(Locale.getDefault(), DEFAULT_NAME_SUFFIX, chosenNumber);
    }

    @Override
    public void copyOrSendPublicLink(OCShare share) {
        //GetLink from the server and show ShareLinkToDialog
        mListener.copyOrSendPublicLink(share);
    }

    @Override
    public void removePublicShare(OCShare share) {
        RemoveShareDialogFragment dialog = RemoveShareDialogFragment.newInstance(share);
        dialog.show(getFragmentManager(), ShareActivity.TAG_REMOVE_SHARE_DIALOG_FRAGMENT);
    }

    @Override
    public void editPublicShare(OCShare share) {
        mListener.showEditPublicShare(share);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log_OC.d(TAG, "onActivityCreated");

        getActivity().setTitle(R.string.share_dialog_title);

        // Load known capabilities of the server from DB
        refreshCapabilitiesFromDB();

        // Load data into the list of private shares
        refreshUsersOrGroupsListFromDB();

        // Load data of public share, if exists
        refreshPublicSharesListFromDB();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ShareFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnShareFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    /**
     * Get known server capabilities from DB
     *
     * Depends on the parent Activity provides a {@link com.owncloud.android.datamodel.FileDataStorageManager}
     * instance ready to use. If not ready, does nothing.
     */
    public void refreshCapabilitiesFromDB() {
        if (((FileActivity) mListener).getStorageManager() != null) {
            mCapabilities = ((FileActivity) mListener).getStorageManager().
                    getCapability(mAccount.name);
        }
    }


    /**
     * Get users and groups from the DB to fill in the "share with" list.
     *
     * Depends on the parent Activity provides a {@link com.owncloud.android.datamodel.FileDataStorageManager}
     * instance ready to use. If not ready, does nothing.
     */
    public void refreshUsersOrGroupsListFromDB() {
        if (((FileActivity) mListener).getStorageManager() != null) {
            // Get Users and Groups
            mPrivateShares = ((FileActivity) mListener).getStorageManager().getPrivateSharesForAFile(
                    mFile.getRemotePath(),
                    mAccount.name
            );

            // Update list of users/groups
            updateListOfUserGroups();
        }
    }

    private void updateListOfUserGroups() {
        // Update list of users/groups
        // TODO Refactoring: create a new {@link ShareUserListAdapter} instance with every call should not be needed
        mUserGroupsAdapter = new ShareUserListAdapter(
                getActivity(),
                R.layout.share_user_item,
                mPrivateShares,
                this
        );

        // Show data
        TextView noShares = (TextView) getView().findViewById(R.id.shareNoUsers);
        ListView usersList = (ListView) getView().findViewById(R.id.shareUsersList);

        if (mPrivateShares.size() > 0) {
            noShares.setVisibility(View.GONE);
            usersList.setVisibility(View.VISIBLE);
            usersList.setAdapter(mUserGroupsAdapter);
            setListViewHeightBasedOnChildren(usersList);
        } else {
            noShares.setVisibility(View.VISIBLE);
            usersList.setVisibility(View.GONE);
        }

        // Set Scroll to initial position
        ScrollView scrollView = (ScrollView) getView().findViewById(R.id.shareScroll);
        scrollView.scrollTo(0, 0);
    }

    @Override
    public void unshareButtonPressed(OCShare share) {
        // Unshare
        Log_OC.d(TAG, "Removing private share with " + share.getSharedWithDisplayName());
        mListener.removeShare(share);
    }

    @Override
    public void editShare(OCShare share) {
        // move to fragment to edit share
        Log_OC.d(TAG, "Editing " + share.getSharedWithDisplayName());
        mListener.showEditPrivateShare(share);
    }

    /**
     * Get public links from the DB to fill in the "Public links" section in the UI.
     *
     * Takes into account server capabilities before reading database.
     *
     * Depends on the parent Activity provides a {@link com.owncloud.android.datamodel.FileDataStorageManager}
     * instance ready to use. If not ready, does nothing.
     */
    public void refreshPublicSharesListFromDB() {
        if (isPublicShareDisabled()) {
            hidePublicShare();

        } else if (((FileActivity) mListener).getStorageManager() != null) {

            // Get public shares
            mPublicLinks = ((FileActivity) mListener).getStorageManager().getPublicSharesForAFile(
                    mFile.getRemotePath(),
                    mAccount.name
            );

            // Update public share section
            updateListOfPublicLinks();
        }
    }

    /**
     * @return 'True' when public share is disabled in the server
     */
    private boolean isPublicShareDisabled() {
        return (mCapabilities != null &&
                mCapabilities.getFilesSharingPublicEnabled().isFalse()
        );
    }

    /**
     * Updates in the UI the section about public share with the information in the current
     * public share bound to mFile, if any
     */
    private void updateListOfPublicLinks() {

        mPublicLinksAdapter = new SharePublicLinkListAdapter(
                getActivity(),
                R.layout.share_public_link_item,
                mPublicLinks,
                this
        );

        // Show data
        TextView noPublicLinks = (TextView) getView().findViewById(R.id.shareNoPublicLinks);
        ListView publicLinksList = (ListView) getView().findViewById(R.id.sharePublicLinksList);

        // Show or hide public links and no public links message
        if (mPublicLinks.size() > 0) {
            noPublicLinks.setVisibility(View.GONE);
            publicLinksList.setVisibility(View.VISIBLE);
            publicLinksList.setAdapter(mPublicLinksAdapter);
            setListViewHeightBasedOnChildren(publicLinksList);
        } else {
            noPublicLinks.setVisibility(View.VISIBLE);
            publicLinksList.setVisibility(View.GONE);
        }

        // Show or hide button for adding a new public share depending on the capabilities and
        // the server version
        if (!enableMultiplePublicSharing()) {

            if (mPublicLinks.size() == 0) {

                getAddPublicLinkButton().setVisibility(View.VISIBLE);

            } else if (mPublicLinks.size() >= 1) {

                getAddPublicLinkButton().setVisibility(View.INVISIBLE);
            }
        }

        // Set Scroll to initial position
        ScrollView scrollView = (ScrollView) getView().findViewById(R.id.shareScroll);
        scrollView.scrollTo(0, 0);
    }


    /// BEWARE: next methods will failed with NullPointerException if called before onCreateView() finishes

    private LinearLayout getShareViaLinkSection() {
        return (LinearLayout) getView().findViewById(R.id.shareViaLinkSection);
    }

    private ImageButton getAddPublicLinkButton() {
        return (ImageButton) getView().findViewById(R.id.addPublicLinkButton);
    }

    /**
     * Hides all the UI elements related to public share
     */
    private void hidePublicShare() {

        getShareViaLinkSection().setVisibility(View.GONE);

    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0) {
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }


    /**
     * Hide share features sections that are not enabled
     *
     * @param view
     */
    private void hideSectionsDisabledInBuildTime(View view) {
        View shareWithUsersSection = view.findViewById(R.id.shareWithUsersSection);
        View shareViaLinkSection = view.findViewById(R.id.shareViaLinkSection);
        View warningAboutPerilsOfSharingPublicStuff = view.findViewById(R.id.shareWarning);

        boolean shareViaLinkAllowed = getActivity().getResources().getBoolean(R.bool.share_via_link_feature);
        boolean shareWithUsersAllowed = getActivity().getResources().getBoolean(R.bool.share_with_users_feature);
        boolean shareWarningAllowed = getActivity().getResources().getBoolean(R.bool.warning_sharing_public_link);

        // Hide share via link section if it is not enabled
        if (!shareViaLinkAllowed) {
            shareViaLinkSection.setVisibility(View.GONE);
        }

        // Hide share with users section if it is not enabled
        if (!shareWithUsersAllowed) {
            shareWithUsersSection.setVisibility(View.GONE);
        }

        // Hide warning about public links if not enabled
        if (!shareWarningAllowed) {
            warningAboutPerilsOfSharingPublicStuff.setVisibility(View.GONE);
        }
    }

    /**
     * Check if the multiple public sharing support should be enabled or not depending on the
     * capabilities and server version
     *
     * @return true if should be enabled, false otherwise
     */
    private boolean enableMultiplePublicSharing() {

        boolean enableMultiplePublicShare = true;

        OwnCloudVersion serverVersion;

        serverVersion = new OwnCloudVersion(mCapabilities.getVersionString());

        // Server version <= 9.x, multiple public sharing not supported
        if (!serverVersion.isMultiplePublicSharingSupported()) {

            enableMultiplePublicShare = false;

        } else if (mCapabilities.getFilesSharingPublicMultiple().isFalse()) {

            // Server version >= 10, multiple public sharing supported but disabled
            enableMultiplePublicShare = false;
        }

        return enableMultiplePublicShare;
    }
}
