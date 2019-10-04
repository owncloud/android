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

package com.owncloud.android.shares.presentation.fragment

import android.accounts.Account
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.owncloud.android.R
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.capabilities.db.OCCapability
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.shares.domain.OCShare
import com.owncloud.android.shares.presentation.SharePublicLinkListAdapter
import com.owncloud.android.shares.presentation.ShareUserListAdapter
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimetypeIconUtil
import kotlinx.android.synthetic.main.share_file_layout.*
import kotlinx.android.synthetic.main.share_file_layout.view.*
import java.util.Collections
import java.util.Locale

/**
 * Fragment for Sharing a file with sharees (users or groups) or creating
 * a public link.
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
    private var file: OCFile? = null

    /**
     * OC account holding the file to share, received as a parameter in construction time
     */
    private var account: Account? = null

    /**
     * Reference to parent listener
     */
    private var listener: ShareFragmentListener? = null

    /**
     * List of private shares bound to the file
     */
    private var privateShares: ArrayList<OCShare>? = null

    /**
     * Adapter to show private shares
     */
    private var userGroupsAdapter: ShareUserListAdapter? = null

    /**
     * List of public links bound to the file
     */
    private var publicLinks: ArrayList<OCShare>? = null

    /**
     * Adapter to show public shares
     */
    private var publicLinksAdapter: SharePublicLinkListAdapter? = null

    /**
     * Capabilities of the server
     */
    private var capabilities: OCCapability? = null

    private var serverVersion: OwnCloudVersion? = null

    private// Array with numbers already set in public link names
    // Inspect public links for default names already used
    // better not suggesting a name than crashing
    // Sort used numbers in ascending order
    // Search for lowest unused number
    // no missing number in the list - take the next to the last one
    val availableDefaultPublicName: String
        get() {
            if (publicLinks == null) {
                return ""
            }

            val defaultName = getString(
                R.string.share_via_link_default_name_template,
                file?.fileName
            )
            val defaultNameNumberedRegex = QUOTE_START + defaultName + QUOTE_END + DEFAULT_NAME_REGEX_SUFFIX
            val usedNumbers = ArrayList<Int>()
            var isDefaultNameSet = false
            var number: String
            for (share in publicLinks as ArrayList<OCShare>) {
                if (defaultName == share.name) {
                    isDefaultNameSet = true
                } else if (share.name?.matches(defaultNameNumberedRegex.toRegex())!!) {
                    number = share.name!!.replaceFirst(defaultNameNumberedRegex.toRegex(), "$1")
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

            return defaultName + String.format(
                Locale.getDefault(),
                DEFAULT_NAME_SUFFIX, chosenNumber
            )
        }

    private val isShareApiEnabled: Boolean
        get() = capabilities?.filesSharingApiEnabled == CapabilityBooleanType.TRUE.value ||
                capabilities?.filesSharingApiEnabled == CapabilityBooleanType.UNKNOWN.value

    /**
     * @return 'True' when public share is disabled in the server
     */
    private val isPublicShareEnabled: Boolean
        get() = capabilities?.filesSharingPublicEnabled == CapabilityBooleanType.TRUE.value ||
                capabilities?.filesSharingPublicEnabled == CapabilityBooleanType.UNKNOWN.value

    /**
     * {@inheritDoc}
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log_OC.d(TAG, "onCreate")
        if (arguments != null) {
            file = arguments!!.getParcelable(ARG_FILE)
            account = arguments!!.getParcelable(ARG_ACCOUNT)
            serverVersion = arguments!!.getParcelable(ARG_SERVER_VERSION)
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
        view.shareFileIcon?.setImageResource(
            MimetypeIconUtil.getFileTypeIconId(
                file?.mimetype,
                file?.fileName
            )
        )
        if (file!!.isImage) {
            val remoteId = file?.remoteId.toString()
            val thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(remoteId)
            if (thumbnail != null) {
                view.shareFileIcon?.setImageBitmap(thumbnail)
            }
        }
        // Name
        view.shareFileName?.text = file?.fileName

        // Size
        if (file!!.isFolder) {
            view.shareFileSize?.visibility = View.GONE
        } else {
            view.shareFileSize?.text = DisplayUtils.bytesToHumanReadable(file!!.fileLength, activity)
        }

        // Private link button
        if (file?.privateLink.isNullOrEmpty()) {
            view.getPrivateLinkButton?.visibility = View.INVISIBLE
        } else {
            view.getPrivateLinkButton?.visibility = View.VISIBLE
        }

        val shareWithUsersEnable = serverVersion != null && serverVersion!!.isSearchUsersSupported

        // Change the sharing text depending on the server version (at least version 8.2 is needed
        // for sharing with other users)
        if (!shareWithUsersEnable) {
            view.shareNoUsers?.setText(R.string.share_incompatible_version)
            view.shareNoUsers?.gravity = View.TEXT_ALIGNMENT_CENTER
            view.addUserButton?.visibility = View.GONE
        }

        // Hide share features sections that are not enabled
        hideSectionsDisabledInBuildTime(view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        getPrivateLinkButton?.setOnClickListener { listener?.copyOrSendPrivateLink(file!!) }

        getPrivateLinkButton?.setOnLongClickListener {
            // Show a toast message explaining what a private link is
            Toast.makeText(activity, R.string.private_link_info, Toast.LENGTH_LONG).show()
            true
        }

        val shareWithUsersEnable = serverVersion != null && serverVersion!!.isSearchUsersSupported

        addUserButton?.setOnClickListener {
            if (shareWithUsersEnable) {
                // Show Search Fragment
                listener?.showSearchUsersAndGroups()
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
        addPublicLinkButton.setOnClickListener {
            // Show Add Public Link Fragment
            listener?.showAddPublicShare(availableDefaultPublicName)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log_OC.d(TAG, "onActivityCreated")

        activity!!.setTitle(R.string.share_dialog_title)

        // Load all shares in the list
        listener?.refreshAllShares()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as ShareFragmentListener?
        } catch (e: ClassCastException) {
            throw ClassCastException(activity!!.toString() + " must implement OnShareFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**************************************************************************************************************
     ************************************************ CAPABILITIES ************************************************
     **************************************************************************************************************/

    fun updateCapabilities(capabilities: OCCapability?) {
        this.capabilities = capabilities

        updatePublicLinkButton()

        // Update view depending on updated capabilities
        shareHeaderDivider?.isVisible = isShareApiEnabled
        shareWithUsersSection?.isVisible = isShareApiEnabled
        shareViaLinkSection?.isVisible = isShareApiEnabled && isPublicShareEnabled
    }

    /**************************************************************************************************************
     *********************************************** PRIVATE SHARES ***********************************************
     **************************************************************************************************************/

    fun updatePrivateShares(privateShares: ArrayList<OCShare>) {
        // Get Users and Groups
        this.privateShares = ArrayList(privateShares.filter {
            it.shareType == ShareType.USER.value ||
                    it.shareType == ShareType.GROUP.value ||
                    it.shareType == ShareType.FEDERATED.value
        })

        // Update list of users/groups
        updateListOfUserGroups()
    }

    private fun updateListOfUserGroups() {
        // Update list of users/groups
        // TODO Refactoring: create a new {@link ShareUserListAdapter} instance with every call should not be needed
        userGroupsAdapter = ShareUserListAdapter(
            context!!,
            R.layout.share_user_item,
            privateShares,
            this
        )

        // Show data
        if (privateShares!!.size > 0) {
            shareNoUsers?.visibility = View.GONE
            shareUsersList?.visibility = View.VISIBLE
            shareUsersList?.adapter = userGroupsAdapter
            setListViewHeightBasedOnChildren(shareUsersList)
        } else {
            shareNoUsers?.visibility = View.VISIBLE
            shareUsersList?.visibility = View.GONE
        }

        // Set Scroll to initial position
        shareScroll?.scrollTo(0, 0)
    }

    override fun editShare(share: OCShare) {
        // move to fragment to edit share
        Log_OC.d(TAG, "Editing " + share.sharedWithDisplayName)
        listener?.showEditPrivateShare(share)
    }

    /**************************************************************************************************************
     *********************************************** PUBLIC SHARES ************************************************
     **************************************************************************************************************/

    fun updatePublicShares(publicShares: ArrayList<OCShare>) {
        publicLinks = publicShares
        updatePublicLinkButton()
        updateListOfPublicLinks()
    }

    /**
     * Show or hide button for adding a new public share depending on the capabilities and the server version
     */
    private fun updatePublicLinkButton() {
        // Since capabilities and publicLinks are loaded asynchronously, let's check whether they both exist
        if (capabilities == null || publicLinks == null) {
            return
        }

        if (!enableMultiplePublicSharing()) {
            if (publicLinks?.isNullOrEmpty() == true) {
                addPublicLinkButton.visibility = View.VISIBLE
                return
            }
            addPublicLinkButton.visibility = View.INVISIBLE
        }
    }

    /**
     * Updates in the UI the section about public share with the information in the current
     * public share bound to file, if any
     */
    private fun updateListOfPublicLinks() {
        publicLinksAdapter = SharePublicLinkListAdapter(
            context!!,
            R.layout.share_public_link_item,
            publicLinks,
            this
        )

        // Show or hide public links and no public links message
        if (!publicLinks.isNullOrEmpty()) {
            shareNoPublicLinks?.visibility = View.GONE
            sharePublicLinksList?.visibility = View.VISIBLE
            sharePublicLinksList?.adapter = publicLinksAdapter
            setListViewHeightBasedOnChildren(sharePublicLinksList)
        } else {
            shareNoPublicLinks?.visibility = View.VISIBLE
            sharePublicLinksList?.visibility = View.GONE
        }

        // Set Scroll to initial position
        shareScroll?.scrollTo(0, 0)
    }

    override fun copyOrSendPublicLink(share: OCShare) {
        //GetLink from the server and show ShareLinkToDialog
        listener?.copyOrSendPublicLink(share)
    }

    override fun editPublicShare(share: OCShare) {
        listener?.showEditPublicShare(share)
    }

    override fun removePublicShare(share: OCShare) {
        // Remove public link from server
        listener?.showRemovePublicShare(share)
    }

    /**
     * Check if the multiple public sharing support should be enabled or not depending on the
     * capabilities and server version
     *
     * @return true if should be enabled, false otherwise
     */
    private fun enableMultiplePublicSharing(): Boolean {
        if (capabilities == null) return true

        val serverVersion = OwnCloudVersion(capabilities?.versionString!!)

        return when {
            // Server version <= 9.x, multiple public sharing not supported
            !serverVersion.isMultiplePublicSharingSupported -> false
            // Server version >= 10, multiple public sharing supported but disabled
            capabilities?.filesSharingPublicMultiple == CapabilityBooleanType.FALSE.value -> false
            else -> true
        }
    }

    /**************************************************************************************************************
     *************************************************** COMMON ***************************************************
     **************************************************************************************************************/

    /**
     * Hide share features sections that are not enabled
     *
     */
    private fun hideSectionsDisabledInBuildTime(view: View) {
        val shareViaLinkAllowed = activity!!.resources.getBoolean(R.bool.share_via_link_feature)
        val shareWithUsersAllowed = activity!!.resources.getBoolean(R.bool.share_with_users_feature)
        val shareWarningAllowed = activity!!.resources.getBoolean(R.bool.warning_sharing_public_link)

        // Hide share via link section if it is not enabled
        if (!shareViaLinkAllowed) {
            view.shareViaLinkSection.visibility = View.GONE
        }

        // Hide share with users section if it is not enabled
        if (!shareWithUsersAllowed) {
            view.shareWithUsersSection?.visibility = View.GONE
        }

        // Hide warning about public links if not enabled
        if (!shareWarningAllowed) {
            view.shareWarning?.visibility = View.GONE
        }
    }

    override fun unshareButtonPressed(share: OCShare) {
        Log_OC.d(TAG, "Removing private share with " + share.sharedWithDisplayName)
        listener?.removeShare(share.remoteId)
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
        private val ARG_SERVER_VERSION = "SERVER_VERSION"

        /**
         * Public factory method to create new ShareFileFragment instances.
         *
         * @param fileToShare An [OCFile] to show in the fragment
         * @param account     An ownCloud account
         * @return A new instance of fragment ShareFileFragment.
         */
        fun newInstance(
            fileToShare: OCFile,
            account: Account,
            serverVersion: OwnCloudVersion? = AccountUtils.getServerVersion(account)
        ): ShareFileFragment {
            val fragment = ShareFileFragment()
            val args = Bundle()
            args.putParcelable(ARG_FILE, fileToShare)
            args.putParcelable(ARG_ACCOUNT, account)
            args.putParcelable(ARG_SERVER_VERSION, serverVersion)
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
