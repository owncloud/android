/**
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author Juan Carlos González Cabrero
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
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.fragment

import android.accounts.Account
import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.owncloud.android.R
import com.owncloud.android.ViewModelFactory
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.shares.viewmodel.ShareViewModel
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.ShareActivity
import com.owncloud.android.ui.adapter.SharePublicLinkListAdapter
import com.owncloud.android.ui.adapter.ShareUserListAdapter
import com.owncloud.android.ui.dialog.RemoveShareDialogFragment
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimetypeIconUtil
import java.util.*
import kotlin.collections.ArrayList

/**
 * Fragment for Sharing a file with sharees (users or groups) or creating
 * a public link.
 *
 * A simple [Fragment] subclass.
 *
 * Activities that contain this fragment must implement the
 * [ShareFragmentListener] interface
 * to handle interaction events.
 *
 * Use the [ShareFileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
/**
 * Required empty public constructor
 */
class ShareFileFragment : Fragment(), ShareUserListAdapter.ShareUserAdapterListener,
    SharePublicLinkListAdapter.SharePublicLinkAdapterListener {

    /**
     * File to share, received as a parameter in construction time
     */
    private var mFile: OCFile? = null

    /**
     * OC account holding the file to share, received as a parameter in construction time
     */
    private var mAccount: Account? = null

    /**
     * Reference to parent listener
     */
    private var mListener: ShareFragmentListener? = null

    /**
     * List of private shares bound to the file
     */
    private var mPrivateShares: ArrayList<OCShare>? = null

    /**
     * Adapter to show private shares
     */
    private var mUserGroupsAdapter: ShareUserListAdapter? = null

    /**
     * List of public links bound to the file
     */
    private var mPublicLinks: ArrayList<OCShare>? = null

    /**
     * Adapter to show public shares
     */
    private var mPublicLinksAdapter: SharePublicLinkListAdapter? = null

    /**
     * Capabilities of the server
     */
    private var mCapabilities: OCCapability? = null

    private// Array with numbers already set in public link names
    // Inspect public links for default names already used
    // better not suggesting a name than crashing
    // Sort used numbers in ascending order
    // Search for lowest unused number
    // no missing number in the list - take the next to the last one
    val availableDefaultPublicName: String
        get() {
            if (mPublicLinks == null) {
                return ""
            }

            val defaultName = getString(
                R.string.share_via_link_default_name_template,
                mFile!!.fileName
            )
            val defaultNameNumberedRegex = QUOTE_START + defaultName + QUOTE_END + DEFAULT_NAME_REGEX_SUFFIX
            val usedNumbers = ArrayList<Int>()
            var isDefaultNameSet = false
            var number: String
            for (share in mPublicLinks!!) {
                if (defaultName == share.name) {
                    isDefaultNameSet = true
                } else if (share.name.matches(defaultNameNumberedRegex.toRegex())) {
                    number = share.name.replaceFirst(defaultNameNumberedRegex.toRegex(), "$1")
                    try {
                        usedNumbers.add(Integer.parseInt(number))
                    } catch (e: Exception) {
                        Log_OC.e(TAG, "Wrong capture of number in share named " + share.name, e)
                        return ""
                    }

                }
            }

            if (!isDefaultNameSet) {
                return defaultName
            }
            Collections.sort(usedNumbers)
            var chosenNumber = -1
            if (usedNumbers.size == 0 || usedNumbers[0] != 2) {
                chosenNumber = 2

            } else {
                for (i in 0 until usedNumbers.size - 1) {
                    val current = usedNumbers[i]
                    val next = usedNumbers[i + 1]
                    if (next - current > 1) {
                        chosenNumber = current + 1
                        break
                    }
                }
                if (chosenNumber < 0) {
                    chosenNumber = usedNumbers[usedNumbers.size - 1] + 1
                }
            }

            return defaultName + String.format(Locale.getDefault(), DEFAULT_NAME_SUFFIX, chosenNumber)
        }

    /**
     * @return 'True' when public share is disabled in the server
     */
    private val isPublicShareDisabled: Boolean
        get() = mCapabilities != null && mCapabilities!!.filesSharingPublicEnabled.isFalse


    /// BEWARE: next methods will failed with NullPointerException if called before onCreateView() finishes

    private val shareViaLinkSection: LinearLayout
        get() = view!!.findViewById<View>(R.id.shareViaLinkSection) as LinearLayout

    private val addPublicLinkButton: ImageButton
        get() = view!!.findViewById<View>(R.id.addPublicLinkButton) as ImageButton

    private lateinit var shareViewModel: ShareViewModel

    /**
     * {@inheritDoc}
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log_OC.d(TAG, "onCreate")
        if (arguments != null) {
            mFile = arguments!!.getParcelable(ARG_FILE)
            mAccount = arguments!!.getParcelable(ARG_ACCOUNT)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log_OC.d(TAG, "onCreateView")

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.share_file_layout, container, false)

        // Setup layout
        // Image
        val icon = view.findViewById<ImageView>(R.id.shareFileIcon)
        icon.setImageResource(
            MimetypeIconUtil.getFileTypeIconId(
                mFile!!.mimetype,
                mFile!!.fileName
            )
        )
        if (mFile!!.isImage) {
            val remoteId = mFile!!.remoteId.toString()
            val thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(remoteId)
            if (thumbnail != null) {
                icon.setImageBitmap(thumbnail)
            }
        }
        // Name
        val fileNameHeader = view.findViewById<TextView>(R.id.shareFileName)
        fileNameHeader.text = mFile!!.fileName
        // Size
        val size = view.findViewById<TextView>(R.id.shareFileSize)
        if (mFile!!.isFolder) {
            size.visibility = View.GONE
        } else {
            size.text = DisplayUtils.bytesToHumanReadable(mFile!!.fileLength, activity)
        }

        // Private link button
        val getPrivateLinkButton = view.findViewById<ImageView>(R.id.getPrivateLinkButton)
        if (mFile!!.privateLink == null || mFile!!.privateLink.isEmpty()) {
            getPrivateLinkButton.visibility = View.INVISIBLE

        } else {
            getPrivateLinkButton.visibility = View.VISIBLE

            getPrivateLinkButton.setOnClickListener { mListener!!.copyOrSendPrivateLink(mFile) }

            getPrivateLinkButton.setOnLongClickListener {
                // Show a toast message explaining what a private link is
                Toast.makeText(activity, R.string.private_link_info, Toast.LENGTH_LONG).show()
                true
            }
        }

        val serverVersion = AccountUtils.getServerVersion(mAccount)
        val shareWithUsersEnable = serverVersion != null && serverVersion.isSearchUsersSupported

        val shareNoUsers = view.findViewById<TextView>(R.id.shareNoUsers)

        //  Add User/Groups Button
        val addUserGroupButton = view.findViewById<ImageButton>(R.id.addUserButton)

        // Change the sharing text depending on the server version (at least version 8.2 is needed
        // for sharing with other users)
        if (!shareWithUsersEnable) {
            shareNoUsers.setText(R.string.share_incompatible_version)
            shareNoUsers.gravity = View.TEXT_ALIGNMENT_CENTER
            addUserGroupButton.visibility = View.GONE
        }

        addUserGroupButton.setOnClickListener {
            if (shareWithUsersEnable) {
                // Show Search Fragment
                mListener!!.showSearchUsersAndGroups()
            } else {
                val message = getString(R.string.share_sharee_unavailable)
                val snackbar = Snackbar.make(
                    activity!!.findViewById(android.R.id.content),
                    message,
                    Snackbar.LENGTH_LONG
                )
                snackbar.show()
            }
        }

        //  Add Public Link Button
        val addPublicLinkButton = view.findViewById<ImageButton>(R.id.addPublicLinkButton)

        addPublicLinkButton.setOnClickListener {
            // Show Add Public Link Fragment
            mListener!!.showAddPublicShare(availableDefaultPublicName)
        }

        // Hide share features sections that are not enabled
        hideSectionsDisabledInBuildTime(view)

        return view
    }

    override fun copyOrSendPublicLink(share: OCShare) {
        //GetLink from the server and show ShareLinkToDialog
        mListener!!.copyOrSendPublicLink(share)
    }

    override fun removePublicShare(share: OCShare) {
        val dialog = RemoveShareDialogFragment.newInstance(share)
        dialog.show(fragmentManager!!, ShareActivity.TAG_REMOVE_SHARE_DIALOG_FRAGMENT)
    }

    override fun editPublicShare(share: OCShare) {
        mListener!!.showEditPublicShare(share)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log_OC.d(TAG, "onActivityCreated")

        activity!!.setTitle(R.string.share_dialog_title)

        // Load known capabilities of the server from DB
        refreshCapabilitiesFromDB()

        // Load data into the list of private shares
        refreshUsersOrGroupsListFromDB()

        // Load data of public share, if exists
        observePublicShares()
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        try {
            mListener = activity as ShareFragmentListener?
        } catch (e: ClassCastException) {
            throw ClassCastException(activity!!.toString() + " must implement OnShareFragmentInteractionListener")
        }

    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }


    /**
     * Get known server capabilities from DB
     *
     * Depends on the parent Activity provides a [com.owncloud.android.datamodel.FileDataStorageManager]
     * instance ready to use. If not ready, does nothing.
     */
    fun refreshCapabilitiesFromDB() {
        if ((mListener as FileActivity).storageManager != null) {
            mCapabilities = (mListener as FileActivity).storageManager.getCapability(mAccount!!.name)
        }
    }


    /**
     * Get users and groups from the DB to fill in the "share with" list.
     *
     * Depends on the parent Activity provides a [com.owncloud.android.datamodel.FileDataStorageManager]
     * instance ready to use. If not ready, does nothing.
     */
    fun refreshUsersOrGroupsListFromDB() {
        if ((mListener as FileActivity).storageManager != null) {
            // Get Users and Groups
            mPrivateShares = (mListener as FileActivity).storageManager.getPrivateSharesForAFile(
                mFile!!.remotePath,
                mAccount!!.name
            )

            // Update list of users/groups
            updateListOfUserGroups()
        }
    }

    private fun updateListOfUserGroups() {
        // Update list of users/groups
        // TODO Refactoring: create a new {@link ShareUserListAdapter} instance with every call should not be needed
        mUserGroupsAdapter = ShareUserListAdapter(
            activity,
            R.layout.share_user_item,
            mPrivateShares,
            this
        )

        // Show data
        val noShares = view!!.findViewById<TextView>(R.id.shareNoUsers)
        val usersList = view!!.findViewById<ListView>(R.id.shareUsersList)

        if (mPrivateShares!!.size > 0) {
            noShares.visibility = View.GONE
            usersList.visibility = View.VISIBLE
            usersList.adapter = mUserGroupsAdapter
            setListViewHeightBasedOnChildren(usersList)
        } else {
            noShares.visibility = View.VISIBLE
            usersList.visibility = View.GONE
        }

        // Set Scroll to initial position
        val scrollView = view!!.findViewById<ScrollView>(R.id.shareScroll)
        scrollView.scrollTo(0, 0)
    }

    override fun unshareButtonPressed(share: OCShare) {
        // Unshare
        Log_OC.d(TAG, "Removing private share with " + share.sharedWithDisplayName)
        mListener!!.removeShare(share)
    }

    override fun editShare(share: OCShare) {
        // move to fragment to edit share
        Log_OC.d(TAG, "Editing " + share.sharedWithDisplayName)
        mListener!!.showEditPrivateShare(share)
    }

    /**
     * Listen public shares for changes in database
     *
     * Takes into account server capabilities before reading database.
     *
     * Depends on the parent Activity provides a [com.owncloud.android.datamodel.FileDataStorageManager]
     * instance ready to use. If not ready, does nothing.
     */
    fun observePublicShares() {
        if (isPublicShareDisabled) {
            hidePublicShare()

        } else{
            shareViewModel = ViewModelProviders.of(this, ViewModelFactory.build {
              ShareViewModel(
                  activity?.application!!,
                  this.context!!,
                  OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(
                      OwnCloudAccount(mAccount, this.context),
                      this.context
                  ),
                  mFile?.remotePath!!,
                  listOf(ShareType.PUBLIC_LINK)
              )
            }).get(ShareViewModel::class.java)
        }

        shareViewModel.sharesForFile.observe(
            this,
            android.arch.lifecycle.Observer {
                    sharesForFile ->
                run {
                    mPublicLinks = sharesForFile as ArrayList<OCShare>
                    updateListOfPublicLinks()
                }
            }
        )
    }

    /**
     * Updates in the UI the section about public share with the information in the current
     * public share bound to mFile, if any
     */
    private fun updateListOfPublicLinks() {
        mPublicLinksAdapter = SharePublicLinkListAdapter(
            activity,
            R.layout.share_public_link_item,
            mPublicLinks,
            this
        )

        // Show data
        val noPublicLinks = view!!.findViewById<TextView>(R.id.shareNoPublicLinks)
        val publicLinksList = view!!.findViewById<ListView>(R.id.sharePublicLinksList)

        // Show or hide public links and no public links message
        if (mPublicLinks!!.size > 0) {
            noPublicLinks.visibility = View.GONE
            publicLinksList.visibility = View.VISIBLE
            publicLinksList.adapter = mPublicLinksAdapter
            setListViewHeightBasedOnChildren(publicLinksList)
        } else {
            noPublicLinks.visibility = View.VISIBLE
            publicLinksList.visibility = View.GONE
        }

        // TODO New Android Components
        // Show or hide button for adding a new public share depending on the capabilities and
        // the server version
//        if (!enableMultiplePublicSharing()) {
//            if (mPublicLinks!!.size == 0) {
//
//                addPublicLinkButton.visibility = View.VISIBLE
//
//            } else if (mPublicLinks!!.size >= 1) {
//
//                addPublicLinkButton.visibility = View.INVISIBLE
//            }
//        }

        // Set Scroll to initial position
        val scrollView = view!!.findViewById<ScrollView>(R.id.shareScroll)
        scrollView.scrollTo(0, 0)
    }

    /**
     * Hides all the UI elements related to public share
     */
    private fun hidePublicShare() {
        shareViaLinkSection.visibility = View.GONE
    }

    /**
     * Hide share features sections that are not enabled
     *
     * @param view
     */
    private fun hideSectionsDisabledInBuildTime(view: View) {
        val shareWithUsersSection = view.findViewById<View>(R.id.shareWithUsersSection)
        val shareViaLinkSection = view.findViewById<View>(R.id.shareViaLinkSection)
        val warningAboutPerilsOfSharingPublicStuff = view.findViewById<View>(R.id.shareWarning)

        val shareViaLinkAllowed = activity!!.resources.getBoolean(R.bool.share_via_link_feature)
        val shareWithUsersAllowed = activity!!.resources.getBoolean(R.bool.share_with_users_feature)
        val shareWarningAllowed = activity!!.resources.getBoolean(R.bool.warning_sharing_public_link)

        // Hide share via link section if it is not enabled
        if (!shareViaLinkAllowed) {
            shareViaLinkSection.visibility = View.GONE
        }

        // Hide share with users section if it is not enabled
        if (!shareWithUsersAllowed) {
            shareWithUsersSection.visibility = View.GONE
        }

        // Hide warning about public links if not enabled
        if (!shareWarningAllowed) {
            warningAboutPerilsOfSharingPublicStuff.visibility = View.GONE
        }
    }

    /**
     * Check if the multiple public sharing support should be enabled or not depending on the
     * capabilities and server version
     *
     * @return true if should be enabled, false otherwise
     */
    private fun enableMultiplePublicSharing(): Boolean {

        var enableMultiplePublicShare = true

        val serverVersion: OwnCloudVersion

        serverVersion = OwnCloudVersion(mCapabilities!!.versionString)

        // Server version <= 9.x, multiple public sharing not supported
        if (!serverVersion.isMultiplePublicSharingSupported) {

            enableMultiplePublicShare = false

        } else if (mCapabilities!!.filesSharingPublicMultiple.isFalse) {

            // Server version >= 10, multiple public sharing supported but disabled
            enableMultiplePublicShare = false
        }

        return enableMultiplePublicShare
    }

    companion object {

        private val TAG = ShareFileFragment::class.java.simpleName
        private val DEFAULT_NAME_SUFFIX = " (%1\$d)"

        private val QUOTE_START = "\\Q"
        private val QUOTE_END = "\\E"
        private val DEFAULT_NAME_REGEX_SUFFIX = " \\((\\d+)\\)\\z"
        // matches suffix (end of the string with \z) in the form "(X)", where X is an integer of any length;
        // also captures the number to reference it later during the match;
        // reference in https://developer.android.com/reference/java/util/regex/Pattern.html#sum

        /**
         * The fragment initialization parameters
         */
        private val ARG_FILE = "FILE"
        private val ARG_ACCOUNT = "ACCOUNT"


        /**
         * Public factory method to create new ShareFileFragment instances.
         *
         * @param fileToShare An [OCFile] to show in the fragment
         * @param account     An ownCloud account
         * @return A new instance of fragment ShareFileFragment.
         */
        fun newInstance(fileToShare: OCFile, account: Account): ShareFileFragment {
            val fragment = ShareFileFragment()
            val args = Bundle()
            args.putParcelable(ARG_FILE, fileToShare)
            args.putParcelable(ARG_ACCOUNT, account)
            fragment.arguments = args
            return fragment
        }

        fun setListViewHeightBasedOnChildren(listView: ListView) {
            val listAdapter = listView.adapter ?: return
            val desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.width, View.MeasureSpec.AT_MOST)
            var totalHeight = 0
            var view: View? = null
            for (i in 0 until listAdapter.count) {
                view = listAdapter.getView(i, view, listView)
                if (i == 0) {
                    view!!.layoutParams = ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                view!!.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED)
                totalHeight += view.measuredHeight
            }
            val params = listView.layoutParams
            params.height = totalHeight + listView.dividerHeight * (listAdapter.count - 1)
            listView.layoutParams = params
            listView.requestLayout()
        }
    }
}
