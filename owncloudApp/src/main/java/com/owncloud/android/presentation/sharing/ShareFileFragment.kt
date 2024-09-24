/**
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author Juan Carlos González Cabrero
 * @author David González Verdugo
 * @author Christian Schabesberger
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

package com.owncloud.android.presentation.sharing

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
import com.owncloud.android.R
import com.owncloud.android.databinding.ShareFileLayoutBinding
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.extensions.showErrorInSnackbar
import com.owncloud.android.presentation.capabilities.CapabilityViewModel
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.presentation.sharing.sharees.ShareUserListAdapter
import com.owncloud.android.presentation.sharing.shares.SharePublicLinkListAdapter
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimetypeIconUtil
import com.owncloud.android.utils.PreferenceUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber
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
    private var privateShares: List<OCShare> = listOf()

    /**
     * Adapter to show private shares
     */
    private var userGroupsAdapter: ShareUserListAdapter? = null

    /**
     * List of public links bound to the file
     */
    private var publicLinks: List<OCShare> = listOf()

    /**
     * Adapter to show public shares
     */
    private var publicLinksAdapter: SharePublicLinkListAdapter? = null

    /**
     * Capabilities of the server
     */
    private var capabilities: OCCapability? = null

    private// Array with numbers already set in public link names
    // Inspect public links for default names already used
    // better not suggesting a name than crashing
    // Sort used numbers in ascending order
    // Search for lowest unused number
    // no missing number in the list - take the next to the last one
    val availableDefaultPublicName: String
        get() {
            val defaultName = getString(
                R.string.share_via_link_default_name_template,
                file?.fileName
            )
            val defaultNameNumberedRegex = QUOTE_START + defaultName + QUOTE_END + DEFAULT_NAME_REGEX_SUFFIX
            val usedNumbers = ArrayList<Int>()
            var isDefaultNameSet = false
            var number: String
            for (share in publicLinks) {
                if (defaultName == share.name) {
                    isDefaultNameSet = true
                } else if (share.name?.matches(defaultNameNumberedRegex.toRegex())!!) {
                    number = share.name!!.replaceFirst(defaultNameNumberedRegex.toRegex(), "$1")
                    try {
                        usedNumbers.add(Integer.parseInt(number))
                    } catch (e: Exception) {
                        Timber.e(e, "Wrong capture of number in share named ${share.name}")
                        return ""
                    }
                }
            }

            if (!isDefaultNameSet) {
                return defaultName
            }
            usedNumbers.sort()
            var chosenNumber = UNUSED_NUMBER
            if (usedNumbers.firstOrNull() != USED_NUMBER_SECOND) {
                chosenNumber = USED_NUMBER_SECOND
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

    private val isShareApiEnabled
        get() = capabilities?.filesSharingApiEnabled == CapabilityBooleanType.TRUE

    private val isPublicShareEnabled
        get() = capabilities?.filesSharingPublicEnabled == CapabilityBooleanType.TRUE

    private val isPrivateLinkDisabled
        get() = capabilities?.filesPrivateLinks == CapabilityBooleanType.FALSE

    private val capabilityViewModel: CapabilityViewModel by viewModel {
        parametersOf(
            account?.name
        )
    }

    private val shareViewModel: ShareViewModel by viewModel {
        parametersOf(
            file?.remotePath,
            account?.name
        )
    }

    private var _binding: ShareFileLayoutBinding? = null
    private val binding get() = _binding!!

    /**
     * {@inheritDoc}
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            file = it.getParcelable(ARG_FILE)
            account = it.getParcelable(ARG_ACCOUNT)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = ShareFileLayoutBinding.inflate(inflater, container, false)
        return binding.root.apply {
            // Allow or disallow touches with other visible windows
            filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // Setup layout
        // Image
        binding.shareFileIcon.setImageResource(
            MimetypeIconUtil.getFileTypeIconId(
                file?.mimeType,
                file?.fileName
            )
        )
        if (file!!.isImage) {
            val remoteId = file?.remoteId.toString()
            val thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(remoteId)
            if (thumbnail != null) {
                binding.shareFileIcon.setImageBitmap(thumbnail)
            }
        }
        // Name
        binding.shareFileName.text = file?.fileName

        // Size
        if (file!!.isFolder) {
            binding.shareFileSize.isVisible = false
        } else {
            binding.shareFileSize.text = DisplayUtils.bytesToHumanReadable(file!!.length, activity)
        }

        // Private link button
        showOrHidePrivateLink()

        // Hide share features sections that are not enabled
        hideSectionsDisabledInBuildTime(view)

        binding.addUserButton.setOnClickListener {
            // Show Search Fragment
            listener?.showSearchUsersAndGroups()
        }

        //  Add Public Link Button
        binding.addPublicLinkButton.setOnClickListener {
            // Show Add Public Link Fragment
            listener?.showAddPublicShare(availableDefaultPublicName)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Timber.d("onActivityCreated")
        requireActivity().setTitle(R.string.share_dialog_title)
        observeCapabilities() // Get capabilities to update some UI elements depending on them
        observeShares()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as ShareFragmentListener?
        } catch (e: ClassCastException) {
            throw ClassCastException(activity.toString() + " must implement OnShareFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private fun observeCapabilities() {
        capabilityViewModel.capabilities.observe(viewLifecycleOwner) { event ->
            val uiResult = event.peekContent()
            val capabilities = uiResult.getStoredData()
            when (uiResult) {
                is UIResult.Success -> {
                    capabilities?.let {
                        updateCapabilities(it)
                    }
                    listener?.dismissLoading()
                }
                is UIResult.Error -> {
                    capabilities?.let {
                        updateCapabilities(it)
                    }
                    event.getContentIfNotHandled()?.let {
                        showErrorInSnackbar(R.string.get_capabilities_error, uiResult.error)
                    }
                    listener?.dismissLoading()
                }
                is UIResult.Loading -> {
                    listener?.showLoading()
                    capabilities?.let {
                        updateCapabilities(it)
                    }
                }
            }
        }
    }

    private fun observeShares() {
        shareViewModel.shares.observe(viewLifecycleOwner) { event ->
            val uiResult = event.peekContent()
            val shares = uiResult.getStoredData()
            when (uiResult) {
                is UIResult.Success -> {
                    shares?.let {
                        updateShares(it)
                    }
                    listener?.dismissLoading()
                }
                is UIResult.Error -> {
                    shares?.let {
                        updateShares(it)
                    }
                    event.getContentIfNotHandled()?.let {
                        showErrorInSnackbar(R.string.get_shares_error, uiResult.error)
                    }
                    listener?.dismissLoading()
                }
                is UIResult.Loading -> {
                    listener?.showLoading()
                    shares?.let {
                        updateShares(it)
                    }
                }
            }
        }
    }

    private fun updateShares(shares: List<OCShare>) {
        shares.filter { share ->
            share.shareType == ShareType.USER ||
                    share.shareType == ShareType.GROUP ||
                    share.shareType == ShareType.FEDERATED
        }.let { privateShares ->
            updatePrivateShares(privateShares)
        }

        shares.filter { share ->
            share.shareType == ShareType.PUBLIC_LINK
        }.let { publicShares ->
            updatePublicShares(publicShares)
        }
    }

    private fun showOrHidePrivateLink() {
        if (file?.privateLink.isNullOrEmpty() || isPrivateLinkDisabled) {
            binding.getPrivateLinkButton.visibility = View.INVISIBLE
        } else {
            with(binding.getPrivateLinkButton) {
                visibility = View.VISIBLE
                setOnClickListener { listener?.copyOrSendPrivateLink(file!!) }

                setOnLongClickListener {
                    // Show a toast message explaining what a private link is
                    Toast.makeText(activity, R.string.private_link_info, Toast.LENGTH_LONG).show()
                    true
                }
            }
        }
    }

    /**************************************************************************************************************
     ************************************************ CAPABILITIES ************************************************
     **************************************************************************************************************/

    private fun updateCapabilities(capabilities: OCCapability?) {
        this.capabilities = capabilities

        updatePublicLinkButton()

        showOrHidePrivateLink()

        // Update view depending on updated capabilities
        binding.shareHeaderDivider.isVisible = isShareApiEnabled
        binding.shareWithUsersSection.isVisible = isShareApiEnabled
        binding.shareViaLinkSection.isVisible = isShareApiEnabled && isPublicShareEnabled
    }

    /**************************************************************************************************************
     *********************************************** PRIVATE SHARES ***********************************************
     **************************************************************************************************************/

    private fun updatePrivateShares(privateShares: List<OCShare>) {
        // Get Users and Groups
        this.privateShares = privateShares.filter {
            it.shareType == ShareType.USER ||
                    it.shareType == ShareType.GROUP ||
                    it.shareType == ShareType.FEDERATED
        }

        // Update list of users/groups
        updateListOfUserGroups()
    }

    private fun updateListOfUserGroups() {
        // Update list of users/groups
        // TODO Refactoring: create a new {@link ShareUserListAdapter} instance with every call should not be needed
        userGroupsAdapter = ShareUserListAdapter(
            requireContext(),
            R.layout.share_user_item,
            privateShares,
            this
        )

        // Show data
        if (privateShares.isNotEmpty()) {
            binding.shareNoUsers.isVisible = false
            binding.shareUsersList.isVisible = true
            binding.shareUsersList.adapter = userGroupsAdapter
            setListViewHeightBasedOnChildren(binding.shareUsersList)
        } else {
            binding.shareNoUsers.isVisible = true
            binding.shareUsersList.isVisible = false
        }

        // Set Scroll to initial position
        binding.shareScroll.scrollTo(0, 0)
    }

    override fun unshareButtonPressed(share: OCShare) {
        // Unshare
        Timber.d("Removing private share with ${share.sharedWithDisplayName}")
        removeShare(share)
    }

    override fun editShare(share: OCShare) {
        // move to fragment to edit share
        Timber.d("Editing ${share.sharedWithDisplayName}")
        listener?.showEditPrivateShare(share)
    }

    /**************************************************************************************************************
     *********************************************** PUBLIC SHARES ************************************************
     **************************************************************************************************************/

    private fun updatePublicShares(publicShares: List<OCShare>) {
        publicLinks = publicShares
        updatePublicLinkButton()
        updateListOfPublicLinks()
    }

    /**
     * Show or hide button for adding a new public share depending on the capabilities and the server version
     */
    private fun updatePublicLinkButton() {
        // Since capabilities and publicLinks are loaded asynchronously, let's check whether they both exist
        if (capabilities == null) {
            return
        }

        if (!enableMultiplePublicSharing()) {
            if (publicLinks.isNullOrEmpty()) {
                binding.addPublicLinkButton.visibility = View.VISIBLE
                return
            }
            binding.addPublicLinkButton.visibility = View.INVISIBLE
        }
    }

    /**
     * Updates in the UI the section about public share with the information in the current
     * public share bound to file, if any
     */
    private fun updateListOfPublicLinks() {
        publicLinksAdapter = SharePublicLinkListAdapter(
            requireContext(),
            R.layout.share_public_link_item,
            publicLinks,
            this
        )

        // Show or hide public links and no public links message
        if (!publicLinks.isNullOrEmpty()) {
            binding.shareNoPublicLinks.isVisible = false
            binding.sharePublicLinksList.isVisible = true
            binding.sharePublicLinksList.adapter = publicLinksAdapter
            setListViewHeightBasedOnChildren(binding.sharePublicLinksList)
        } else {
            binding.shareNoPublicLinks.isVisible = true
            binding.sharePublicLinksList.isVisible = false
        }

        // Set Scroll to initial position
        binding.shareScroll.scrollTo(0, 0)
    }

    override fun copyOrSendPublicLink(share: OCShare) {
        //GetLink from the server and show ShareLinkToDialog
        listener?.copyOrSendPublicLink(share)
    }

    /**
     * Check if the multiple public sharing support should be enabled or not depending on the
     * capabilities and server version
     *
     * @return true if should be enabled, false otherwise
     */
    private fun enableMultiplePublicSharing() = capabilities?.filesSharingPublicMultiple?.isTrue ?: false

    override fun editPublicShare(share: OCShare) {
        listener?.showEditPublicShare(share)
    }

    override fun removeShare(share: OCShare) {
        // Remove public link from server
        listener?.showRemoveShare(share)
    }

    /**
     * Hide share features sections that are not enabled
     *
     */
    private fun hideSectionsDisabledInBuildTime(view: View) {
        val shareViaLinkAllowed = requireActivity().resources.getBoolean(R.bool.share_via_link_feature)
        val shareWithUsersAllowed = requireActivity().resources.getBoolean(R.bool.share_with_users_feature)
        val shareWarningAllowed = requireActivity().resources.getBoolean(R.bool.warning_sharing_public_link)

        // Hide share via link section if it is not enabled
        if (!shareViaLinkAllowed) {
            binding.shareViaLinkSection.isVisible = false
        }

        // Hide share with users section if it is not enabled
        if (!shareWithUsersAllowed) {
            binding.shareWithUsersSection.isVisible = false
        }

        // Hide warning about public links if not enabled
        if (!shareWarningAllowed) {
            binding.shareWarning.isVisible = false
        }
    }

    companion object {
        private const val DEFAULT_NAME_SUFFIX = " (%1\$d)"

        private const val QUOTE_START = "\\Q"
        private const val QUOTE_END = "\\E"
        private const val DEFAULT_NAME_REGEX_SUFFIX = " \\((\\d+)\\)\\z"
        // matches suffix (end of the string with \z) in the form "(X)", where X is an integer of any length;
        // also captures the number to reference it later during the match;
        // reference in https://developer.android.com/reference/java/util/regex/Pattern.html#sum

        /**
         * The fragment initialization parameters
         */
        private const val ARG_FILE = "FILE"
        private const val ARG_ACCOUNT = "ACCOUNT"

        private const val UNUSED_NUMBER = -1
        private const val USED_NUMBER_SECOND = 2

        /**
         * Public factory method to create new ShareFileFragment instances.
         *
         * @param fileToShare An [OCFile] to show in the fragment
         * @param account     An ownCloud account
         * @return A new instance of fragment ShareFileFragment.
         */
        fun newInstance(
            fileToShare: OCFile,
            account: Account
        ): ShareFileFragment {
            val args = Bundle().apply {
                putParcelable(ARG_FILE, fileToShare)
                putParcelable(ARG_ACCOUNT, account)
            }
            return ShareFileFragment().apply { arguments = args }
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
