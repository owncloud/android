/**
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author Christian Schabesberger
 * @author David González Verdugo
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

package com.owncloud.android.presentation.sharing.sharees

import android.accounts.Account
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.owncloud.android.R
import com.owncloud.android.databinding.EditShareLayoutBinding
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.domain.utils.Event.EventObserver
import com.owncloud.android.extensions.parseError
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.lib.resources.shares.SharePermissionsBuilder
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.presentation.sharing.ShareFragmentListener
import com.owncloud.android.presentation.sharing.ShareViewModel
import com.owncloud.android.utils.PreferenceUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

/**
 * Required empty public constructor
 */
class EditPrivateShareFragment : DialogFragment() {

    /** Share to show & edit, received as a parameter in construction time  */
    private var share: OCShare? = null

    /** File bound to share, received as a parameter in construction time  */
    private var file: OCFile? = null

    /** OC account holding the shared file, received as a parameter in construction time  */
    private var account: Account? = null

    /**
     * Reference to parent listener
     */
    private var listener: ShareFragmentListener? = null

    /** Listener for changes on privilege checkboxes  */
    private var onPrivilegeChangeListener: CompoundButton.OnCheckedChangeListener? = null

    private val shareViewModel: ShareViewModel by viewModel {
        parametersOf(
            file?.remotePath,
            account?.name
        )
    }

    private var _binding: EditShareLayoutBinding? = null
    private val binding get() = _binding!!

    /**
     * {@inheritDoc}
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.v("onCreate")
        if (arguments != null) {
            file = arguments?.getParcelable(ARG_FILE)
            account = arguments?.getParcelable(ARG_ACCOUNT)
            share = savedInstanceState?.getParcelable(ARG_SHARE) ?: arguments?.getParcelable(ARG_SHARE)
            Timber.d("Share has id ${share?.id} remoteId ${share?.remoteId}")
        }

        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Timber.v("onActivityCreated")

        // To observe the changes in a just updated share
        refreshPrivateShare(share?.remoteId!!)
        observePrivateShareToEdit()

        observePrivateShareEdition()
    }

    /**
     * {@inheritDoc}
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = EditShareLayoutBinding.inflate(inflater, container, false)
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
        super.onViewCreated(view, savedInstanceState)

        binding.editShareTitle.text = resources.getString(R.string.share_with_edit_title, share?.sharedWithDisplayName)

        // Setup layout
        refreshUiFromState()

        binding.closeButton.setOnClickListener { dismiss() }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            listener = activity as ShareFragmentListener?
        } catch (e: IllegalStateException) {
            throw IllegalStateException(requireActivity().toString() + " must implement OnShareFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()

        listener = null
    }

    /**
     * Updates the UI with the current permissions in the edited [RemoteShare]
     *
     */
    private fun refreshUiFromState() {
        setPermissionsListening(false)

        val sharePermissions = share!!.permissions

        binding.canShareSwitch.isVisible = shareViewModel.isResharingAllowed()
        binding.canShareSwitch.isChecked = sharePermissions and RemoteShare.SHARE_PERMISSION_FLAG > 0

        val anyUpdatePermission = RemoteShare.CREATE_PERMISSION_FLAG or
                RemoteShare.UPDATE_PERMISSION_FLAG or
                RemoteShare.DELETE_PERMISSION_FLAG
        val canEdit = sharePermissions and anyUpdatePermission > 0
        binding.canEditSwitch.isChecked = canEdit

        if (file?.isFolder == true) {
            /// TODO change areEditOptionsAvailable in order to delete !isFederated
            // from checking when iOS is ready
            binding.canEditCreateCheckBox.apply {
                isChecked = sharePermissions and RemoteShare.CREATE_PERMISSION_FLAG > 0
                isVisible = canEdit
            }

            binding.canEditChangeCheckBox.apply {
                isChecked = sharePermissions and RemoteShare.UPDATE_PERMISSION_FLAG > 0
                isVisible = canEdit

            }
            binding.canEditDeleteCheckBox.apply {
                isChecked = sharePermissions and RemoteShare.DELETE_PERMISSION_FLAG > 0
                isVisible = canEdit
            }
        }

        setPermissionsListening(true)
    }

    /**
     * Binds or unbinds listener for user actions to enable or disable a permission on the edited share
     * to the views receiving the user events.
     *
     * @param enable            When 'true', listener is bound to view; when 'false', it is unbound.
     */
    private fun setPermissionsListening(enable: Boolean) {
        if (enable && onPrivilegeChangeListener == null) {
            onPrivilegeChangeListener = OnPrivilegeChangeListener()
        }
        val changeListener = if (enable) onPrivilegeChangeListener else null

        binding.canShareSwitch.setOnCheckedChangeListener(changeListener)
        binding.canEditSwitch.setOnCheckedChangeListener(changeListener)

        if (file?.isFolder == true) {
            binding.canEditCreateCheckBox.setOnCheckedChangeListener(changeListener)
            binding.canEditChangeCheckBox.setOnCheckedChangeListener(changeListener)
            binding.canEditDeleteCheckBox.setOnCheckedChangeListener(changeListener)
        }
    }

    /**
     * Listener for user actions that enable or disable a privilege
     */
    private inner class OnPrivilegeChangeListener : CompoundButton.OnCheckedChangeListener {

        /**
         * Called by every [SwitchCompat] and [CheckBox] in the fragment to update
         * the state of its associated permission.
         *
         * @param compound  [CompoundButton] toggled by the user
         * @param isChecked     New switch state.
         */
        override fun onCheckedChanged(compound: CompoundButton, isChecked: Boolean) {
            if (!isResumed) {
                // very important, setCheched(...) is called automatically during
                // Fragment recreation on device rotations
                return
            }
            /// else, getView() cannot be NULL

            var subordinate: CompoundButton
            when (compound.id) {
                R.id.canShareSwitch -> {
                    Timber.v("canShareCheckBox toggled to $isChecked")
                    updatePermissionsToShare()
                }

                R.id.canEditSwitch -> {
                    Timber.v("canEditCheckBox toggled to $isChecked")
                    /// sync subordinate CheckBoxes
                    val isFederated = share?.shareType == ShareType.FEDERATED
                    if (file?.isFolder == true) {
                        if (isChecked) {
                            if (!isFederated) {
                                /// not federated shares -> enable all the subpermisions
                                for (i in sSubordinateCheckBoxIds.indices) {
                                    //noinspection ConstantConditions, prevented in the method beginning
                                    subordinate = view!!.findViewById(sSubordinateCheckBoxIds[i])
                                    if (!isFederated) { // TODO delete when iOS is ready
                                        subordinate.visibility = View.VISIBLE
                                    }
                                    if (!subordinate.isChecked && !file!!.isSharedWithMe) {          // see (1)
                                        toggleDisablingListener(subordinate)
                                    }
                                }
                            } else {
                                /// federated share -> enable delete subpermission, as server side; TODO why?
                                //noinspection ConstantConditions, prevented in the method beginning
                                subordinate = binding.canEditDeleteCheckBox
                                if (!subordinate.isChecked) {
                                    toggleDisablingListener(subordinate)
                                }
                            }
                        } else {
                            for (i in sSubordinateCheckBoxIds.indices) {
                                //noinspection ConstantConditions, prevented in the method beginning
                                subordinate = view!!.findViewById(sSubordinateCheckBoxIds[i])
                                subordinate.visibility = View.GONE
                                if (subordinate.isChecked) {
                                    toggleDisablingListener(subordinate)
                                }
                            }
                        }
                    }

                    if (!(file?.isFolder == true && isChecked && file?.isSharedWithMe == true) ||       // see (1)
                        isFederated
                    ) {
                        updatePermissionsToShare()
                    }
                }

                R.id.canEditCreateCheckBox -> {
                    Timber.v("canEditCreateCheckBox toggled to $isChecked")
                    syncCanEditSwitch(compound, isChecked)
                    updatePermissionsToShare()
                }

                R.id.canEditChangeCheckBox -> {
                    Timber.v("canEditChangeCheckBox toggled to $isChecked")
                    syncCanEditSwitch(compound, isChecked)
                    updatePermissionsToShare()
                }

                R.id.canEditDeleteCheckBox -> {
                    Timber.v("canEditDeleteCheckBox toggled to $isChecked")
                    syncCanEditSwitch(compound, isChecked)
                    updatePermissionsToShare()
                }
            } // updatePermissionsToShare()   // see (1)
            // (1) These modifications result in an exceptional UI behaviour for the case
            // where the switch 'can edit' is enabled for a *reshared folder*; if the same
            // behaviour was applied than for owned folder, and the user did not have full
            // permissions to update the folder, an error would be reported by the server
            // and the children checkboxes would be automatically hidden again
        }

        /**
         * Sync value of "can edit" [SwitchCompat] according to a change in one of its subordinate checkboxes.
         *
         * If all the subordinates are disabled, "can edit" has to be disabled.
         *
         * If any subordinate is enabled, "can edit" has to be enabled.
         *
         * @param subordinateCheckBoxView   Subordinate [CheckBox] that was changed.
         * @param isChecked                 'true' iif subordinateCheckBoxView was checked.
         */
        private fun syncCanEditSwitch(subordinateCheckBoxView: View, isChecked: Boolean) {
            val canEditCompound = binding.canEditSwitch
            if (isChecked) {
                if (!canEditCompound.isChecked) {
                    toggleDisablingListener(canEditCompound)
                }
            } else {
                var allDisabled = true
                run {
                    var i = 0
                    while (allDisabled && i < sSubordinateCheckBoxIds.size) {
                        allDisabled =
                            allDisabled and (sSubordinateCheckBoxIds[i] == subordinateCheckBoxView.id || !(view?.findViewById<View>(
                                sSubordinateCheckBoxIds[i]
                            ) as CheckBox).isChecked)
                        i++
                    }
                }
                if (canEditCompound.isChecked && allDisabled) {
                    toggleDisablingListener(canEditCompound)
                    for (i in sSubordinateCheckBoxIds.indices) {
                        view?.findViewById<View>(sSubordinateCheckBoxIds[i])?.visibility = View.GONE
                    }
                }
            }
        }

        /**
         * Toggle value of received [CompoundButton] granting that its change listener is not called.
         *
         * @param compound      [CompoundButton] (switch or checkBox) to toggle without reporting to
         * the change listener
         */
        private fun toggleDisablingListener(compound: CompoundButton) {
            compound.setOnCheckedChangeListener(null)
            compound.toggle()
            compound.setOnCheckedChangeListener(this)
        }
    }

    private fun observePrivateShareToEdit() {
        shareViewModel.privateShare.observe(
            this,
            EventObserver { uiResult ->
                when (uiResult) {
                    is UIResult.Success -> {
                        updateShare(uiResult.data)
                    }
                    is UIResult.Error -> {}
                    is UIResult.Loading -> {}
                }
            }
        )
    }

    private fun observePrivateShareEdition() {
        shareViewModel.privateShareEditionStatus.observe(
            this,
            EventObserver { uiResult ->
                when (uiResult) {
                    is UIResult.Error -> {
                        showError(getString(R.string.update_link_file_error), uiResult.error)
                        listener?.dismissLoading()
                    }
                    is UIResult.Loading -> {
                        listener?.showLoading()
                    }
                    is UIResult.Success -> {}
                }
            }
        )
    }

    /**
     * Updates the permissions of the [RemoteShare] according to the values set in the UI
     */
    private fun updatePermissionsToShare() {
        binding.privateShareErrorMessage.isVisible = false

        val sharePermissionsBuilder = SharePermissionsBuilder().apply {
            setSharePermission(binding.canShareSwitch.isChecked)
            if (file?.isFolder == true) {
                setUpdatePermission(binding.canEditChangeCheckBox.isChecked)
                setCreatePermission(binding.canEditCreateCheckBox.isChecked)
                setDeletePermission(binding.canEditDeleteCheckBox.isChecked)
            } else {
                setUpdatePermission(binding.canEditSwitch.isChecked)
            }
        }
        val permissions = sharePermissionsBuilder.build()

        shareViewModel.updatePrivateShare(share?.remoteId!!, permissions, account?.name!!)
    }

    private fun refreshPrivateShare(remoteId: String) {
        shareViewModel.refreshPrivateShare(remoteId)
    }

    /**
     * Updates the UI after the result of an update operation on the edited [RemoteShare] permissions.
     *
     */
    private fun updateShare(updatedShare: OCShare?) {
        share = updatedShare
        refreshUiFromState()
    }

    /**
     * Show error when updating the private share, if any
     */
    private fun showError(genericErrorMessage: String, throwable: Throwable?) {
        binding.privateShareErrorMessage.apply {
            text = throwable?.parseError(genericErrorMessage, resources)
            isVisible = true
        }
    }

    companion object {
        /** The fragment initialization parameters  */
        private const val ARG_SHARE = "SHARE"
        private const val ARG_FILE = "FILE"
        private const val ARG_ACCOUNT = "ACCOUNT"

        /** Ids of CheckBoxes depending on R.id.canEdit CheckBox  */
        private val sSubordinateCheckBoxIds =
            intArrayOf(R.id.canEditCreateCheckBox, R.id.canEditChangeCheckBox, R.id.canEditDeleteCheckBox)

        /**
         * Public factory method to create new EditPrivateShareFragment instances.
         *
         * @param shareToEdit   An [OCShare] to show and edit in the fragment
         * @param sharedFile    The [OCFile] bound to 'shareToEdit'
         * @param account       The ownCloud account holding 'sharedFile'
         * @return A new instance of fragment EditPrivateShareFragment.
         */
        fun newInstance(shareToEdit: OCShare, sharedFile: OCFile, account: Account): EditPrivateShareFragment {
            val args = Bundle().apply {
                putParcelable(ARG_SHARE, shareToEdit)
                putParcelable(ARG_FILE, sharedFile)
                putParcelable(ARG_ACCOUNT, account)
            }

            return EditPrivateShareFragment().apply { arguments = args }
        }
    }
}
