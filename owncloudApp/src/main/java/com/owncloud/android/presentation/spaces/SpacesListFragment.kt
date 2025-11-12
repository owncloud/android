/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 * @author Jorge Aguado Recio
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2025 ownCloud GmbH.
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

package com.owncloud.android.presentation.spaces

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.owncloud.android.R
import com.owncloud.android.databinding.FileOptionsBottomSheetFragmentBinding
import com.owncloud.android.databinding.SpacesListFragmentBinding
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.files.model.MIME_BMP
import com.owncloud.android.domain.files.model.MIME_GIF
import com.owncloud.android.domain.files.model.MIME_JPEG
import com.owncloud.android.domain.files.model.MIME_PNG
import com.owncloud.android.domain.files.model.MIME_PREFIX_IMAGE
import com.owncloud.android.domain.files.model.MIME_X_MS_BMP
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.spaces.model.SpaceMenuOption
import com.owncloud.android.domain.transfers.model.TransferStatus
import com.owncloud.android.domain.user.model.UserPermissions
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.collectLatestLifecycleFlow
import com.owncloud.android.extensions.showAlertDialog
import com.owncloud.android.extensions.showErrorInSnackbar
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.extensions.toDrawableRes
import com.owncloud.android.extensions.toDrawableResId
import com.owncloud.android.extensions.toStringResId
import com.owncloud.android.extensions.toSubtitleStringRes
import com.owncloud.android.extensions.toTitleStringRes
import com.owncloud.android.presentation.capabilities.CapabilityViewModel
import com.owncloud.android.presentation.common.BottomSheetFragmentItemView
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.presentation.spaces.createspace.CreateSpaceDialogFragment
import com.owncloud.android.presentation.transfers.TransfersViewModel
import kotlinx.coroutines.flow.SharedFlow
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class SpacesListFragment :
    SpacesListAdapter.SpacesListAdapterListener,
    Fragment(),
    SearchView.OnQueryTextListener,
    CreateSpaceDialogFragment.CreateSpaceListener
{
    private var _binding: SpacesListFragmentBinding? = null
    private val binding get() = _binding!!

    private var isMultiPersonal = false
    private var userPermissions = mutableSetOf<UserPermissions>()
    private var editQuotaPermission = false
    private var lastUpdatedRemotePath: String? = null
    private var selectedImageName: String? = null
    private lateinit var currentSpace: OCSpace

    private val spacesListViewModel: SpacesListViewModel by viewModel {
        parametersOf(
            requireArguments().getString(BUNDLE_ACCOUNT_NAME),
            requireArguments().getBoolean(BUNDLE_SHOW_PERSONAL_SPACE),
        )
    }
    private val capabilityViewModel: CapabilityViewModel by viewModel {
        parametersOf(
            requireArguments().getString(BUNDLE_ACCOUNT_NAME),
        )
    }
    private val transfersViewModel: TransfersViewModel by viewModel()

    private val editSpaceImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult

            val selectedImageUri = result.data?.data ?: return@registerForActivityResult
            val accountName = requireArguments().getString(BUNDLE_ACCOUNT_NAME) ?: return@registerForActivityResult
            val documentFile = DocumentFile.fromSingleUri(requireContext(), selectedImageUri) ?: return@registerForActivityResult
            selectedImageName = documentFile.name

            transfersViewModel.uploadFilesFromContentUri(
                accountName = accountName,
                listOfContentUris = listOf(selectedImageUri),
                uploadFolderPath = SPACE_CONFIG_DIR,
                spaceId = currentSpace.id
            )
        }

    private lateinit var spacesListAdapter: SpacesListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SpacesListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        isMultiPersonal = capabilityViewModel.checkMultiPersonal()
        initViews()
        subscribeToViewModels()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setSpacesLayout(newConfig)
    }

    private fun initViews() {
        setHasOptionsMenu(true)
        setSpacesLayout(resources.configuration)
        spacesListAdapter = SpacesListAdapter(this, isPickerMode())
        binding.recyclerSpacesList.adapter = spacesListAdapter

        binding.swipeRefreshSpacesList.setOnRefreshListener {
            spacesListViewModel.refreshSpacesFromServer()
        }

        binding.fabCreateSpace.setOnClickListener {
            val dialog = CreateSpaceDialogFragment.newInstance(
                isEditMode = false,
                canEditQuota = false,
                currentSpace = null,
                listener = this
            )
            dialog.show(requireActivity().supportFragmentManager, DIALOG_CREATE_SPACE)
            binding.fabCreateSpace.isFocusable = false
        }
        setTextHintRootToolbar()
    }

    private fun subscribeToViewModels() {
        collectLatestLifecycleFlow(spacesListViewModel.spacesList) { uiState ->
            var spacesToListFiltered: List<OCSpace>
            if (uiState.searchFilter != "") {
                spacesToListFiltered =
                    uiState.spaces.filter { it.name.lowercase().contains(uiState.searchFilter.lowercase()) && !it.isPersonal &&
                            shouldShowDisabledSpace(it) }
                val personalSpace = uiState.spaces.find { it.isPersonal }
                personalSpace?.let {
                    spacesToListFiltered = spacesToListFiltered.toMutableList().apply {
                        add(0, personalSpace)
                    }
                }
                showOrHideEmptyView(spacesToListFiltered)
                spacesListAdapter.setData(spacesToListFiltered, isMultiPersonal)
            } else {
                spacesToListFiltered = uiState.spaces.filter { shouldShowDisabledSpace(it) }
                showOrHideEmptyView(spacesToListFiltered)
                spacesListAdapter.setData(spacesToListFiltered, isMultiPersonal)
            }
            binding.swipeRefreshSpacesList.isRefreshing = uiState.refreshing
            uiState.error?.let { showErrorInSnackbar(R.string.spaces_sync_failed, it) }

            uiState.rootFolderFromSelectedSpace?.let {
                setFragmentResult(REQUEST_KEY_CLICK_SPACE, bundleOf(BUNDLE_KEY_CLICK_SPACE to it))
            }
        }

        collectLatestLifecycleFlow(spacesListViewModel.userId) { event ->
            event?.let {
                val accountName = requireArguments().getString(BUNDLE_ACCOUNT_NAME)
                when (val uiResult = event.peekContent()) {
                    is UIResult.Success -> {
                        Timber.d("The account id for $accountName is: ${uiResult.data}")
                        uiResult.data?.let { spacesListViewModel.getUserPermissions(it) }
                    }
                    is UIResult.Loading -> { }
                    is UIResult.Error -> {
                        Timber.e(uiResult.error, "Failed to retrieve user id for account $accountName")
                    }
                }
            }
        }

        collectLatestLifecycleFlow(spacesListViewModel.userPermissions) { event ->
            event?.let {
                val accountName = requireArguments().getString(BUNDLE_ACCOUNT_NAME)
                when (val uiResult = event.peekContent()) {
                    is UIResult.Success -> {
                        Timber.d("The permissions for $accountName are: ${uiResult.data}")
                        uiResult.data?.let {
                            binding.fabCreateSpace.isVisible = it.contains(DRIVES_CREATE_ALL_PERMISSION) && !isPickerMode()
                            if(it.contains(DRIVES_READ_WRITE_ALL_PERMISSION)) userPermissions.add(UserPermissions.CAN_EDIT_SPACES)
                            editQuotaPermission = it.contains(DRIVES_READ_WRITE_PROJECT_QUOTA_ALL_PERMISSION)
                            if(it.contains(DRIVES_DELETE_PROJECT_ALL_PERMISSION)) userPermissions.add(UserPermissions.CAN_DELETE_SPACES)
                        }
                    }
                    is UIResult.Loading -> { }
                    is UIResult.Error -> {
                        Timber.e(uiResult.error, "Failed to retrieve user permissions for account $accountName")
                        binding.fabCreateSpace.isVisible = false
                    }
                }
            }
        }

        collectSpaceOperationsFlow(spacesListViewModel.createSpaceFlow, R.string.create_space_correctly, R.string.create_space_failed)
        collectSpaceOperationsFlow(spacesListViewModel.editSpaceFlow, R.string.edit_space_correctly, R.string.edit_space_failed)
        collectSpaceOperationsFlow(spacesListViewModel.editSpaceImageFlow, R.string.edit_space_image_correctly, R.string.edit_space_image_failed)
        collectSpaceOperationsFlow(spacesListViewModel.disableSpaceFlow, R.string.disable_space_correctly, R.string.disable_space_failed)
        collectSpaceOperationsFlow(spacesListViewModel.enableSpaceFlow, R.string.enable_space_correctly, R.string.enable_space_failed)
        collectSpaceOperationsFlow(spacesListViewModel.deleteSpaceFlow, R.string.delete_space_correctly, R.string.delete_space_failed)

        collectLatestLifecycleFlow(spacesListViewModel.menuOptions) { menuOptions ->
            showSpaceMenuOptionsDialog(menuOptions)
        }

        collectLatestLifecycleFlow(transfersViewModel.transfersWithSpaceStateFlow) { transfersWithSpace ->
            val remotePath = SPACE_CONFIG_DIR + selectedImageName
            val matchedTransfer = transfersWithSpace.map { it.first }.find { it.remotePath == remotePath }

            if (matchedTransfer != null && lastUpdatedRemotePath != matchedTransfer.remotePath) {
                when(matchedTransfer.status) {
                    TransferStatus.TRANSFER_SUCCEEDED -> {
                        spacesListViewModel.editSpaceImage(currentSpace.id, matchedTransfer.remotePath)
                        lastUpdatedRemotePath = matchedTransfer.remotePath
                    }
                    TransferStatus.TRANSFER_FAILED -> {
                        showMessageInSnackbar(getString(R.string.edit_space_image_failed))
                    }
                    else -> { }
                }
            }
        }

    }

    private fun collectSpaceOperationsFlow(flow: SharedFlow<Event<UIResult<Unit>>?>, successMessage: Int, errorMessage: Int) {
        collectLatestLifecycleFlow(flow) { event ->
            event?.let {
                when (val uiResult = event.peekContent()) {
                    is UIResult.Success -> { showMessageInSnackbar(getString(successMessage)) }
                    is UIResult.Loading -> { }
                    is UIResult.Error -> { showErrorInSnackbar(errorMessage, uiResult.error) }
                }
            }
        }
    }

    private fun showOrHideEmptyView(spacesList: List<OCSpace>) {
        binding.recyclerSpacesList.isVisible = spacesList.isNotEmpty()

        with(binding.emptyDataParent) {
            root.isVisible = spacesList.isEmpty()
            listEmptyDatasetIcon.setImageResource(FileListOption.SPACES_LIST.toDrawableRes())
            listEmptyDatasetTitle.setText(FileListOption.SPACES_LIST.toTitleStringRes())
            listEmptyDatasetSubTitle.setText(FileListOption.SPACES_LIST.toSubtitleStringRes())
        }
    }

    private fun setSpacesLayout(config: Configuration) {
        val layoutColumns = if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) 2 else 1
        val spacesListLayoutManager = GridLayoutManager(requireContext(), layoutColumns)
        binding.recyclerSpacesList.layoutManager = spacesListLayoutManager
    }

    override fun onItemClick(ocSpace: OCSpace) {
        spacesListViewModel.getRootFileForSpace(ocSpace)
    }

    override fun onThreeDotButtonClick(ocSpace: OCSpace) {
        currentSpace = ocSpace
        spacesListViewModel.filterMenuOptions(ocSpace, userPermissions)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        (menu.findItem(R.id.action_search).actionView as SearchView).run {
            setOnQueryTextListener(this@SpacesListFragment)
            queryHint = resources.getString(R.string.actionbar_search_space)
        }
        menu.findItem(R.id.action_share_current_folder)?.itemId?.let { menu.removeItem(it) }
    }

    override fun onQueryTextSubmit(query: String?): Boolean = false

    override fun onQueryTextChange(newText: String?): Boolean {
        newText?.let { spacesListViewModel.updateSearchFilter(it) }
        return true
    }

    override fun createSpace(spaceName: String, spaceSubtitle: String, spaceQuota: Long) {
        spacesListViewModel.createSpace(spaceName, spaceSubtitle, spaceQuota)
    }

    override fun editSpace(spaceId: String, spaceName: String, spaceSubtitle: String, spaceQuota: Long?) {
        spacesListViewModel.editSpace(spaceId, spaceName, spaceSubtitle, spaceQuota)
    }

    fun setSearchListener(searchView: SearchView) {
        searchView.setOnQueryTextListener(this)
    }

    private fun setTextHintRootToolbar() {
        val searchViewRootToolbar = requireActivity().findViewById<SearchView>(R.id.root_toolbar_search_view)
        searchViewRootToolbar.queryHint = getString(R.string.actionbar_search_space)
    }

    private fun showSpaceMenuOptionsDialog(menuOptions: List<SpaceMenuOption>) {
        val spaceOptionsBottomSheetBinding = FileOptionsBottomSheetFragmentBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(spaceOptionsBottomSheetBinding.root)

        val fileOptionsBottomSheetSingleFileBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(
            spaceOptionsBottomSheetBinding.root.parent as View)
        val closeBottomSheetButton = spaceOptionsBottomSheetBinding.closeBottomSheet
        closeBottomSheetButton.setOnClickListener {
            dialog.dismiss()
        }

        val thumbnailBottomSheet = spaceOptionsBottomSheetBinding.thumbnailBottomSheet
        thumbnailBottomSheet.setImageResource(R.drawable.ic_menu_space)

        val spaceNameBottomSheet = spaceOptionsBottomSheetBinding.fileNameBottomSheet
        spaceNameBottomSheet.text = currentSpace.name

        val spaceSizeBottomSheet = spaceOptionsBottomSheetBinding.fileSizeBottomSheet
        currentSpace.quota?.let { quota ->
            val usedQuota = quota.used
            val totalQuota = quota.total

            val quotaText = when {
                usedQuota == null -> getString(R.string.drawer_unavailable_used_storage)
                totalQuota == 0L -> DisplayUtils.bytesToHumanReadable(usedQuota, requireContext(), true)
                else -> getString(
                    R.string.drawer_quota,
                    DisplayUtils.bytesToHumanReadable(usedQuota, requireContext(), true),
                    DisplayUtils.bytesToHumanReadable(totalQuota, requireContext(), true),
                    quota.getRelative().toString())
            }
            spaceSizeBottomSheet.text = quotaText
        }

        val spaceSeparatorBottomSheet = spaceOptionsBottomSheetBinding.fileSeparatorBottomSheet
        spaceSeparatorBottomSheet.visibility = View.GONE

        menuOptions.forEach { menuOption ->
            setMenuOption(menuOption, spaceOptionsBottomSheetBinding, dialog)
        }

        fileOptionsBottomSheetSingleFileBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    fileOptionsBottomSheetSingleFileBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

        dialog.setOnShowListener { fileOptionsBottomSheetSingleFileBehavior.peekHeight = spaceOptionsBottomSheetBinding.root.measuredHeight }
        dialog.show()
    }

    private fun setMenuOption(menuOption: SpaceMenuOption, binding: FileOptionsBottomSheetFragmentBinding, dialog: BottomSheetDialog) {
        val fileOptionItemView = BottomSheetFragmentItemView(requireContext())
        fileOptionItemView.apply {
            itemIcon = ResourcesCompat.getDrawable(resources, menuOption.toDrawableResId(), null)
            title = getString(menuOption.toStringResId())
            setOnClickListener {
                dialog.dismiss()
                when(menuOption) {
                    SpaceMenuOption.EDIT -> {
                        val editDialog = CreateSpaceDialogFragment.newInstance(
                            isEditMode = true,
                            canEditQuota = editQuotaPermission,
                            currentSpace = currentSpace,
                            listener = this@SpacesListFragment
                        )
                        editDialog.show(requireActivity().supportFragmentManager, DIALOG_CREATE_SPACE)
                    }
                    SpaceMenuOption.DISABLE -> {
                        showAlertDialog(
                            title = getString(R.string.disable_space_dialog_title, currentSpace.name),
                            message = getString(R.string.disable_space_dialog_message),
                            positiveButtonText = getString(R.string.common_yes),
                            positiveButtonListener = { _: DialogInterface?, _: Int -> spacesListViewModel.disableSpace(currentSpace.id) },
                            negativeButtonText = getString(R.string.common_no)
                        )
                    }
                    SpaceMenuOption.ENABLE -> {
                        showAlertDialog(
                            title = getString(R.string.enable_space_dialog_title, currentSpace.name),
                            message = getString(R.string.enable_space_dialog_message),
                            positiveButtonText = getString(R.string.common_yes),
                            positiveButtonListener = { _: DialogInterface?, _: Int -> spacesListViewModel.enableSpace(currentSpace.id) },
                            negativeButtonText = getString(R.string.common_no)
                        )
                    }
                    SpaceMenuOption.DELETE -> {
                        showAlertDialog(
                            title = getString(R.string.delete_space_dialog_title, currentSpace.name),
                            message = getString(R.string.delete_space_dialog_message),
                            positiveButtonText = getString(R.string.common_yes),
                            positiveButtonListener = { _: DialogInterface?, _: Int ->  spacesListViewModel.deleteSpace(currentSpace.id) },
                            negativeButtonText = getString(R.string.common_no)
                        )
                    }
                    SpaceMenuOption.EDIT_IMAGE -> {
                        val action = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = MIME_PREFIX_IMAGE
                            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(MIME_JPEG, MIME_PNG, MIME_BMP, MIME_X_MS_BMP, MIME_GIF))
                        }
                        editSpaceImageLauncher.launch(action)
                    }
                }
            }
        }
        binding.fileOptionsBottomSheetLayout.addView(fileOptionItemView)
    }

    private fun shouldShowDisabledSpace(space: OCSpace): Boolean = !space.isDisabled || spacesListViewModel.showDisabledSpaces

    private fun isPickerMode(): Boolean = requireArguments().getBoolean(BUNDLE_IS_PICKER_MODE, false)

    companion object {
        const val REQUEST_KEY_CLICK_SPACE = "REQUEST_KEY_CLICK_SPACE"
        const val BUNDLE_KEY_CLICK_SPACE = "BUNDLE_KEY_CLICK_SPACE"
        const val BUNDLE_SHOW_PERSONAL_SPACE = "showPersonalSpace"
        const val BUNDLE_IS_PICKER_MODE = "isPickerMode"
        const val BUNDLE_ACCOUNT_NAME = "accountName"
        const val DRIVES_CREATE_ALL_PERMISSION = "Drives.Create.all"
        const val DRIVES_READ_WRITE_ALL_PERMISSION = "Drives.ReadWrite.all"
        const val DRIVES_READ_WRITE_PROJECT_QUOTA_ALL_PERMISSION = "Drives.ReadWriteProjectQuota.all"
        const val DRIVES_DELETE_PROJECT_ALL_PERMISSION = "Drives.DeleteProject.all"
        const val SPACE_CONFIG_DIR = "/.space/"

        private const val DIALOG_CREATE_SPACE = "DIALOG_CREATE_SPACE"

        fun newInstance(
            showPersonalSpace: Boolean,
            isPickerMode: Boolean,
            accountName: String
        ): SpacesListFragment {
            val args = Bundle().apply {
                putBoolean(BUNDLE_SHOW_PERSONAL_SPACE, showPersonalSpace)
                putBoolean(BUNDLE_IS_PICKER_MODE, isPickerMode)
                putString(BUNDLE_ACCOUNT_NAME, accountName)
            }
            return SpacesListFragment().apply { arguments = args }
        }
    }

}
