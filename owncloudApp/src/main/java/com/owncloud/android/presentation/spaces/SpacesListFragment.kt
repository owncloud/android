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

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.owncloud.android.R
import com.owncloud.android.databinding.FileOptionsBottomSheetFragmentBinding
import com.owncloud.android.databinding.SpacesListFragmentBinding
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.spaces.model.SpaceMenuOption
import com.owncloud.android.extensions.collectLatestLifecycleFlow
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
    private var editSpacesPermission = false
    private var editQuotaPermission = false
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
        spacesListAdapter = SpacesListAdapter(this)
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
            if (uiState.searchFilter != "") {
                var spacesToListFiltered =
                    uiState.spaces.filter { it.name.lowercase().contains(uiState.searchFilter.lowercase()) && !it.isPersonal && !it.isDisabled }
                val personalSpace = uiState.spaces.find { it.isPersonal }
                personalSpace?.let {
                    spacesToListFiltered = spacesToListFiltered.toMutableList().apply {
                        add(0, personalSpace)
                    }
                }
                showOrHideEmptyView(spacesToListFiltered)
                spacesListAdapter.setData(spacesToListFiltered, isMultiPersonal)
            } else {
                showOrHideEmptyView(uiState.spaces)
                spacesListAdapter.setData(uiState.spaces.filter { !it.isDisabled }, isMultiPersonal)
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
                            binding.fabCreateSpace.isVisible = it.contains(DRIVES_CREATE_ALL_PERMISSION)
                            editSpacesPermission = it.contains(DRIVES_READ_WRITE_ALL_PERMISSION)
                            editQuotaPermission = it.contains(DRIVES_READ_WRITE_PROJECT_QUOTA_ALL_PERMISSION)
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

        collectLatestLifecycleFlow(spacesListViewModel.createSpaceFlow) { event ->
            event?.let {
                when (val uiResult = event.peekContent()) {
                    is UIResult.Success -> { showMessageInSnackbar(getString(R.string.create_space_correctly)) }
                    is UIResult.Loading -> { }
                    is UIResult.Error -> { showErrorInSnackbar(R.string.create_space_failed, uiResult.error) }
                }
            }
        }

        collectLatestLifecycleFlow(spacesListViewModel.editSpaceFlow) { event ->
            event?.let {
                when (val uiResult = event.peekContent()) {
                    is UIResult.Success -> { showMessageInSnackbar(getString(R.string.edit_space_correctly)) }
                    is UIResult.Loading -> { }
                    is UIResult.Error -> { showErrorInSnackbar(R.string.edit_space_failed, uiResult.error) }
                }
            }
        }

        collectLatestLifecycleFlow(spacesListViewModel.menuOptions) { menuOptions ->
            showSpaceMenuOptionsDialog(menuOptions)
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
        spacesListViewModel.filterMenuOptions(ocSpace, editSpacesPermission)
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
        spaceSizeBottomSheet.text = DisplayUtils.bytesToHumanReadable(currentSpace.quota?.used ?: 0L, requireContext(), true)

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
                }
            }
        }
        binding.fileOptionsBottomSheetLayout.addView(fileOptionItemView)
    }

    companion object {
        const val REQUEST_KEY_CLICK_SPACE = "REQUEST_KEY_CLICK_SPACE"
        const val BUNDLE_KEY_CLICK_SPACE = "BUNDLE_KEY_CLICK_SPACE"
        const val BUNDLE_SHOW_PERSONAL_SPACE = "showPersonalSpace"
        const val BUNDLE_ACCOUNT_NAME = "accountName"
        const val DRIVES_CREATE_ALL_PERMISSION = "Drives.Create.all"
        const val DRIVES_READ_WRITE_ALL_PERMISSION = "Drives.ReadWrite.all"
        const val DRIVES_READ_WRITE_PROJECT_QUOTA_ALL_PERMISSION = "Drives.ReadWriteProjectQuota.all"

        private const val DIALOG_CREATE_SPACE = "DIALOG_CREATE_SPACE"

        fun newInstance(
            showPersonalSpace: Boolean,
            accountName: String
        ): SpacesListFragment {
            val args = Bundle().apply {
                putBoolean(BUNDLE_SHOW_PERSONAL_SPACE, showPersonalSpace)
                putString(BUNDLE_ACCOUNT_NAME, accountName)
            }
            return SpacesListFragment().apply { arguments = args }
        }
    }

}
