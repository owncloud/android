/**
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
 * @author Jose Antonio Barros Ramos
 * @author Juan Carlos Garrote Gascón
 * @author Manuel Plazas Palacio
 * @author Jorge Aguado Recio
 * @author Aitor Ballesteros Pavón
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.presentation.files.filelist

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import coil.load
import com.bumptech.glide.Glide
import com.getbase.floatingactionbutton.AddFloatingActionButton
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.owncloud.android.R
import com.owncloud.android.databinding.MainFileListFragmentBinding
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.domain.appregistry.model.AppRegistryMimeType
import com.owncloud.android.domain.exceptions.InstanceNotConfiguredException
import com.owncloud.android.domain.exceptions.TooEarlyException
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.files.model.FileMenuOption
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PATH
import com.owncloud.android.domain.files.model.OCFileSyncInfo
import com.owncloud.android.domain.files.model.OCFileWithSyncInfo
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.domain.transfers.model.TransferStatus
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.addOpenInWebMenuOptions
import com.owncloud.android.extensions.collectLatestLifecycleFlow
import com.owncloud.android.extensions.filterMenuOptions
import com.owncloud.android.extensions.parseError
import com.owncloud.android.extensions.sendDownloadedFilesByShareSheet
import com.owncloud.android.extensions.showErrorInSnackbar
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.extensions.toDrawableRes
import com.owncloud.android.extensions.toDrawableResId
import com.owncloud.android.extensions.toResId
import com.owncloud.android.extensions.toStringResId
import com.owncloud.android.extensions.toSubtitleStringRes
import com.owncloud.android.extensions.toTitleStringRes
import com.owncloud.android.presentation.authentication.AccountUtils
import com.owncloud.android.presentation.common.BottomSheetFragmentItemView
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.presentation.files.SortBottomSheetFragment
import com.owncloud.android.presentation.files.SortBottomSheetFragment.Companion.newInstance
import com.owncloud.android.presentation.files.SortBottomSheetFragment.SortDialogListener
import com.owncloud.android.presentation.files.SortOptionsView
import com.owncloud.android.presentation.files.SortOrder
import com.owncloud.android.presentation.files.SortType
import com.owncloud.android.presentation.files.ViewType
import com.owncloud.android.presentation.files.createfolder.CreateFolderDialogFragment
import com.owncloud.android.presentation.files.createshortcut.CreateShortcutDialogFragment
import com.owncloud.android.presentation.files.operations.FileOperation
import com.owncloud.android.presentation.files.operations.FileOperationsViewModel
import com.owncloud.android.presentation.files.removefile.RemoveFilesDialogFragment
import com.owncloud.android.presentation.files.removefile.RemoveFilesDialogFragment.Companion.TAG_REMOVE_FILES_DIALOG_FRAGMENT
import com.owncloud.android.presentation.files.renamefile.RenameFileDialogFragment
import com.owncloud.android.presentation.files.renamefile.RenameFileDialogFragment.Companion.FRAGMENT_TAG_RENAME_FILE
import com.owncloud.android.presentation.thumbnails.ThumbnailsRequester
import com.owncloud.android.presentation.transfers.TransfersViewModel
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.FolderPickerActivity
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimetypeIconUtil
import com.owncloud.android.utils.PreferenceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.io.File

class MainFileListFragment : Fragment(),
    CreateFolderDialogFragment.CreateFolderListener,
    FileListAdapter.FileListAdapterListener,
    SearchView.OnQueryTextListener,
    SortDialogListener,
    SortOptionsView.CreateFolderListener,
    SortOptionsView.SortOptionsListener,
    CreateShortcutDialogFragment.CreateShortcutListener {

    private val mainFileListViewModel by viewModel<MainFileListViewModel> {
        parametersOf(
            requireArguments().getParcelable(ARG_INITIAL_FOLDER_TO_DISPLAY),
            requireArguments().getParcelable(ARG_FILE_LIST_OPTION),
        )
    }
    private val fileOperationsViewModel by sharedViewModel<FileOperationsViewModel>()
    private val transfersViewModel by viewModel<TransfersViewModel>()

    private var _binding: MainFileListFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var layoutManager: StaggeredGridLayoutManager
    private lateinit var fileListAdapter: FileListAdapter
    private lateinit var viewType: ViewType

    var actionMode: ActionMode? = null

    private var statusBarColorActionMode: Int? = null
    private var statusBarColor: Int? = null

    var fileActions: FileActions? = null
    var uploadActions: UploadActions? = null

    private var currentDefaultApplication: String? = null
    private var browserOpened = false

    private var openInWebProviders: Map<String, Int> = hashMapOf()

    private var menu: Menu? = null
    private var checkedFiles: List<OCFile> = emptyList()
    private var filesToRemove: List<OCFile> = emptyList()
    private var fileSingleFile: OCFile? = null
    private var fileOptionsBottomSheetSingleFileLayout: LinearLayout? = null
    private var succeededTransfers: List<OCTransfer>? = null
    private var numberOfUploadsRefreshed: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MainFileListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        subscribeToViewModels()
    }

    override fun onResume() {
        super.onResume()
        if (browserOpened) {
            browserOpened = false
            fileOperationsViewModel.performOperation(
                FileOperation.RefreshFolderOperation(
                    folderToRefresh = mainFileListViewModel.getFile(),
                    shouldSyncContents = !isPickingAFolder(),
                )
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        (menu.findItem(R.id.action_search).actionView as SearchView).run {
            setOnQueryTextListener(this@MainFileListFragment)
            queryHint = resources.getString(R.string.actionbar_search)
        }
        (menu.findItem(R.id.action_select_all)).setOnMenuItemClickListener {
            fileListAdapter.selectAll()
            updateActionModeAfterTogglingSelected()
            true
        }
        if (isPickingAFolder() || getCurrentSpace()?.isPersonal == false) {
            menu.findItem(R.id.action_share_current_folder)?.itemId?.let { menu.removeItem(it) }
        } else {
            menu.findItem(R.id.action_share_current_folder)?.setOnMenuItemClickListener {
                fileActions?.onShareFileClicked(mainFileListViewModel.getFile())
                true
            }
        }
    }

    private fun initViews() {
        setHasOptionsMenu(true)
        statusBarColorActionMode = ContextCompat.getColor(requireContext(), R.color.action_mode_status_bar_background)

        // Set view and footer correctly
        if (mainFileListViewModel.isGridModeSetAsPreferred()) {
            layoutManager =
                StaggeredGridLayoutManager(ColumnQuantity(requireContext(), R.layout.grid_item).calculateNoOfColumns(), RecyclerView.VERTICAL)
            viewType = ViewType.VIEW_TYPE_GRID
        } else {
            layoutManager = StaggeredGridLayoutManager(1, RecyclerView.VERTICAL)
            viewType = ViewType.VIEW_TYPE_LIST
        }

        binding.optionsLayout.viewTypeSelected = viewType

        // Set RecyclerView and its adapter.
        binding.recyclerViewMainFileList.layoutManager = layoutManager

        fileListAdapter = FileListAdapter(
            context = requireContext(),
            layoutManager = layoutManager,
            isPickerMode = isPickingAFolder(),
            listener = this@MainFileListFragment,
        )

        binding.recyclerViewMainFileList.adapter = fileListAdapter

        // Set Swipe to refresh and its listener
        binding.swipeRefreshMainFileList.setOnRefreshListener {
            fileOperationsViewModel.performOperation(
                FileOperation.RefreshFolderOperation(
                    folderToRefresh = mainFileListViewModel.getFile(),
                    shouldSyncContents = !isPickingAFolder(), // For picking a folder option, we just need a refresh
                )
            )
        }

        // Set Refresh FAB and its listener
        binding.fabRefresh.setOnClickListener {
            if (fileOperationsViewModel.refreshFolderLiveData.value!!.peekContent().isLoading) {
                showMessageInSnackbar(message = getString(R.string.fab_refresh_sync_in_progress))
            } else {
                fileOperationsViewModel.performOperation(
                    FileOperation.RefreshFolderOperation(
                        folderToRefresh = mainFileListViewModel.getFile(),
                        shouldSyncContents = false,
                    )
                )
                hideRefreshFab()
            }
        }

        // Set SortOptions and its listeners
        binding.optionsLayout.onSortOptionsListener = this
        setViewTypeSelector(SortOptionsView.AdditionalView.CREATE_FOLDER)

        showOrHideFab(requireArguments().getParcelable(ARG_FILE_LIST_OPTION)!!, requireArguments().getParcelable(ARG_INITIAL_FOLDER_TO_DISPLAY)!!)

        binding.fabMain.findViewById<AddFloatingActionButton>(com.getbase.floatingactionbutton.R.id.fab_expand_menu_button).contentDescription =
            getString(R.string.content_description_add_new_content)

        setTextHintRootToolbar()
    }

    private fun setTextHintRootToolbar() {
        val searchViewRootToolbar = requireActivity().findViewById<SearchView>(R.id.root_toolbar_search_view)
        searchViewRootToolbar.queryHint = getString(R.string.actionbar_search)
    }

    private fun setViewTypeSelector(additionalView: SortOptionsView.AdditionalView) {
        if (isPickingAFolder()) {
            binding.optionsLayout.onCreateFolderListener = this
            binding.optionsLayout.selectAdditionalView(additionalView)
        }
    }

    private fun toggleSelection(position: Int) {
        fileListAdapter.toggleSelection(position)
        updateActionModeAfterTogglingSelected()
    }

    private fun subscribeToViewModels() {
        /* MainFileListViewModel observables */
        // Observe the current folder displayed
        collectLatestLifecycleFlow(mainFileListViewModel.currentFolderDisplayed) { currentFolderDisplayed: OCFile ->
            fileActions?.onCurrentFolderUpdated(currentFolderDisplayed, mainFileListViewModel.getSpace())
            val fileListOption = mainFileListViewModel.fileListOption.value
            val refreshFolderNeeded = fileListOption.isAllFiles() ||
                    (!fileListOption.isAllFiles() && currentFolderDisplayed.remotePath != ROOT_PATH)
            if (refreshFolderNeeded) {
                fileOperationsViewModel.performOperation(
                    FileOperation.RefreshFolderOperation(
                        folderToRefresh = currentFolderDisplayed,
                        shouldSyncContents = !isPickingAFolder(), // For picking a folder option, we just need a refresh
                    )
                )
            }
            showOrHideFab(fileListOption, currentFolderDisplayed)
            if (currentFolderDisplayed.hasAddSubdirectoriesPermission) {
                setViewTypeSelector(SortOptionsView.AdditionalView.CREATE_FOLDER)
            } else {
                setViewTypeSelector(SortOptionsView.AdditionalView.HIDDEN)
            }
            numberOfUploadsRefreshed = 0
            hideRefreshFab()
        }

        // Observe the current space to update the toolbar
        // We can't rely exclusively on the [currentFolderDisplayed] because sometimes retrieving the space takes more time
        collectLatestLifecycleFlow(mainFileListViewModel.space) { currentSpace: OCSpace? ->
            currentSpace?.let {
                fileActions?.onCurrentFolderUpdated(mainFileListViewModel.getFile(), currentSpace)
            }
        }

        // Observe the list of app registries that allow creating new files
        collectLatestLifecycleFlow(mainFileListViewModel.appRegistryToCreateFiles) { listAppRegistry ->
            binding.fabNewfile.isVisible = listAppRegistry.isNotEmpty()
            registerFabNewFileListener(listAppRegistry)
        }

        // Observe the open in web action to trigger browser
        collectLatestLifecycleFlow(mainFileListViewModel.openInWebFlow) {
            if (it != null) {
                val uiResult = it.peekContent()
                if (uiResult is UIResult.Success) {
                    browserOpened = true
                    val builder = CustomTabsIntent.Builder().build()
                    builder.launchUrl(
                        requireActivity(),
                        Uri.parse(uiResult.data)
                    )
                } else if (uiResult is UIResult.Error) {
                    // Mimetypes not supported via open in web, send 500
                    if (uiResult.error is InstanceNotConfiguredException) {
                        val message =
                            getString(R.string.open_in_web_error_generic) + " " + getString(R.string.error_reason) + " " + getString(R.string.open_in_web_error_not_supported)
                        this.showMessageInSnackbar(message, Snackbar.LENGTH_LONG)
                    } else if (uiResult.error is TooEarlyException) {
                        this.showMessageInSnackbar(getString(R.string.open_in_web_error_too_early), Snackbar.LENGTH_LONG)
                    } else {
                        this.showErrorInSnackbar(
                            R.string.open_in_web_error_generic,
                            uiResult.error
                        )
                    }
                }
                mainFileListViewModel.resetOpenInWebFlow()
                currentDefaultApplication = null
            }
        }

        // Observe the menu filtered options in multiselection
        collectLatestLifecycleFlow(mainFileListViewModel.menuOptions) { menuOptions ->
            val hasWritePermission = if (checkedFiles.size == 1) {
                checkedFiles.first().hasWritePermission
            } else {
                false
            }
            menu?.filterMenuOptions(menuOptions, hasWritePermission)
        }

        // Observe the app registry in multiselection
        collectLatestLifecycleFlow(mainFileListViewModel.appRegistryMimeType) { appRegistryMimeType ->
            val appProviders = appRegistryMimeType?.appProviders
            menu?.let {
                openInWebProviders = addOpenInWebMenuOptions(it, openInWebProviders, appProviders)
            }
        }

        // Observe the menu filtered options for a single file
        collectLatestLifecycleFlow(mainFileListViewModel.menuOptionsSingleFile) { menuOptions ->
            fileSingleFile?.let { file ->
                val fileOptionsBottomSheetSingleFile = layoutInflater.inflate(R.layout.file_options_bottom_sheet_fragment, null)
                val dialog = BottomSheetDialog(requireContext())
                dialog.setContentView(fileOptionsBottomSheetSingleFile)

                val fileOptionsBottomSheetSingleFileBehavior: BottomSheetBehavior<*> =
                    BottomSheetBehavior.from(fileOptionsBottomSheetSingleFile.parent as View)
                val closeBottomSheetButton = fileOptionsBottomSheetSingleFile.findViewById<ImageView>(R.id.close_bottom_sheet)
                closeBottomSheetButton.setOnClickListener {
                    dialog.hide()
                    dialog.dismiss()
                }

                val thumbnailBottomSheet = fileOptionsBottomSheetSingleFile.findViewById<ImageView>(R.id.thumbnail_bottom_sheet)
                if (file.isFolder) {
                    // Folder
                    thumbnailBottomSheet.setImageResource(R.drawable.ic_menu_archive)
                } else {
                    // Set file icon depending on its mimetype. Ask for thumbnail later.
                    thumbnailBottomSheet.setImageResource(MimetypeIconUtil.getFileTypeIconId(file.mimeType, file.fileName))
                    if (file.remoteId != null) {
                        val thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(file.remoteId)
                        if (thumbnail != null) {
                            thumbnailBottomSheet.setImageBitmap(thumbnail)
                        }
                        if (file.needsToUpdateThumbnail) {
                            // generate new Thumbnail
                            if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, thumbnailBottomSheet)) {
                                val task = ThumbnailsCacheManager.ThumbnailGenerationTask(
                                    thumbnailBottomSheet,
                                    AccountUtils.getCurrentOwnCloudAccount(requireContext())
                                )
                                val asyncDrawable = ThumbnailsCacheManager.AsyncThumbnailDrawable(resources, thumbnail, task)

                                // If drawable is not visible, do not update it.
                                if (asyncDrawable.minimumHeight > 0 && asyncDrawable.minimumWidth > 0) {
                                    thumbnailBottomSheet.setImageDrawable(asyncDrawable)
                                }
                                task.execute(file)
                            }
                        }

                        if (file.mimeType == "image/png") {
                            thumbnailBottomSheet.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_color))
                        }
                    }
                }

                val fileNameBottomSheet = fileOptionsBottomSheetSingleFile.findViewById<TextView>(R.id.file_name_bottom_sheet)
                fileNameBottomSheet.text = file.fileName

                val fileSizeBottomSheet = fileOptionsBottomSheetSingleFile.findViewById<TextView>(R.id.file_size_bottom_sheet)
                fileSizeBottomSheet.text = DisplayUtils.bytesToHumanReadable(file.length, requireContext())

                val fileLastModBottomSheet = fileOptionsBottomSheetSingleFile.findViewById<TextView>(R.id.file_last_mod_bottom_sheet)
                fileLastModBottomSheet.text = DisplayUtils.getRelativeTimestamp(requireContext(), file.modificationTimestamp)

                fileOptionsBottomSheetSingleFileLayout = fileOptionsBottomSheetSingleFile.findViewById(R.id.file_options_bottom_sheet_layout)
                menuOptions.forEach { menuOption ->
                    val fileOptionItemView = BottomSheetFragmentItemView(requireContext())
                    fileOptionItemView.apply {
                        title = if (menuOption.toResId() == R.id.action_open_file_with && !file.hasWritePermission) {
                            getString(R.string.actionbar_open_with_read_only)
                        } else {
                            getString(menuOption.toStringResId())
                        }
                        itemIcon = ResourcesCompat.getDrawable(resources, menuOption.toDrawableResId(), null)
                        setOnClickListener {
                            when (menuOption) {
                                FileMenuOption.SELECT_ALL -> {
                                    // Not applicable here
                                }

                                FileMenuOption.SELECT_INVERSE -> {
                                    // Not applicable here
                                }

                                FileMenuOption.DOWNLOAD, FileMenuOption.SYNC -> {
                                    syncFiles(listOf(file))
                                }

                                FileMenuOption.RENAME -> {
                                    val dialogRename = RenameFileDialogFragment.newInstance(file)
                                    dialogRename.show(requireActivity().supportFragmentManager, FRAGMENT_TAG_RENAME_FILE)
                                }

                                FileMenuOption.MOVE -> {
                                    val action = Intent(activity, FolderPickerActivity::class.java)
                                    action.putParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES, arrayListOf(file))
                                    action.putExtra(FolderPickerActivity.EXTRA_PICKER_MODE, FolderPickerActivity.PickerMode.MOVE)
                                    requireActivity().startActivityForResult(action, FileDisplayActivity.REQUEST_CODE__MOVE_FILES)
                                }

                                FileMenuOption.COPY -> {
                                    val action = Intent(activity, FolderPickerActivity::class.java)
                                    action.putParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES, arrayListOf(file))
                                    action.putExtra(FolderPickerActivity.EXTRA_PICKER_MODE, FolderPickerActivity.PickerMode.COPY)
                                    requireActivity().startActivityForResult(action, FileDisplayActivity.REQUEST_CODE__COPY_FILES)
                                }

                                FileMenuOption.REMOVE -> {
                                    filesToRemove = listOf(file)
                                    fileOperationsViewModel.showRemoveDialog(filesToRemove)
                                }

                                FileMenuOption.OPEN_WITH -> {
                                    fileActions?.openFile(file)
                                }

                                FileMenuOption.CANCEL_SYNC -> {
                                    fileActions?.cancelFileTransference(arrayListOf(file))
                                }

                                FileMenuOption.SHARE -> {
                                    fileActions?.onShareFileClicked(file)
                                }

                                FileMenuOption.DETAILS -> {
                                    fileActions?.showDetails(file)
                                }

                                FileMenuOption.SEND -> {
                                    if (!file.isAvailableLocally) { // Download the file
                                        Timber.d("${file.remotePath} : File must be downloaded")
                                        fileActions?.initDownloadForSending(file)
                                    } else {
                                        fileActions?.sendDownloadedFile(file)
                                    }
                                }

                                FileMenuOption.SET_AV_OFFLINE -> {
                                    fileOperationsViewModel.performOperation(FileOperation.SetFilesAsAvailableOffline(listOf(file)))
                                    if (file.isFolder) {
                                        fileOperationsViewModel.performOperation(
                                            FileOperation.SynchronizeFolderOperation(
                                                folderToSync = file,
                                                accountName = file.owner,
                                                isActionSetFolderAvailableOfflineOrSynchronize = true,
                                            )
                                        )
                                    } else {
                                        fileOperationsViewModel.performOperation(FileOperation.SynchronizeFileOperation(file, file.owner))
                                    }
                                }

                                FileMenuOption.UNSET_AV_OFFLINE -> {
                                    fileOperationsViewModel.performOperation(FileOperation.UnsetFilesAsAvailableOffline(listOf(file)))
                                }
                            }
                            dialog.hide()
                            dialog.dismiss()
                        }
                    }
                    fileOptionsBottomSheetSingleFileLayout!!.addView(fileOptionItemView)
                }
                // Disable drag gesture
                fileOptionsBottomSheetSingleFileBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                            fileOptionsBottomSheetSingleFileBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                })
                dialog.setOnShowListener { fileOptionsBottomSheetSingleFileBehavior.peekHeight = fileOptionsBottomSheetSingleFile.measuredHeight }
                dialog.show()
                mainFileListViewModel.getAppRegistryForMimeType(file.mimeType, isMultiselection = false)
            }
        }

        // Observe the app registry for a single file
        collectLatestLifecycleFlow(mainFileListViewModel.appRegistryMimeTypeSingleFile) { appRegistryMimeType ->
            fileSingleFile?.let { file ->
                val appProviders = appRegistryMimeType?.appProviders
                appProviders?.forEach { appRegistryProvider ->
                    val appProviderItemView = BottomSheetFragmentItemView(requireContext())
                    appProviderItemView.apply {
                        title = getString(R.string.ic_action_open_with_web, appRegistryProvider.name)
                        itemIcon = try {
                            removeDefaultTint()
                            getDrawableFromUrl(requireContext(), appRegistryProvider.icon)
                        } catch (e: Exception) {
                            Timber.e(e, "An exception occurred while Glide is trying to load an image")
                            addDefaultTint(R.color.bottom_sheet_fragment_item_color)
                            ResourcesCompat.getDrawable(resources, R.drawable.ic_open_in_web, null)
                        }
                        setOnClickListener {
                            mainFileListViewModel.openInWeb(file.remoteId!!, appRegistryProvider.name)
                            fileOperationsViewModel.setLastUsageFile(file)
                        }
                    }
                    fileOptionsBottomSheetSingleFileLayout!!.addView(appProviderItemView, 1)
                }
            }
            fileSingleFile = null
        }

        // Observe the file list UI state
        collectLatestLifecycleFlow(mainFileListViewModel.fileListUiState) { fileListUiState ->
            if (fileListUiState !is MainFileListViewModel.FileListUiState.Success) return@collectLatestLifecycleFlow

            fileListAdapter.updateFileList(
                filesToAdd = fileListUiState.folderContent,
                fileListOption = fileListUiState.fileListOption,
            )
            showOrHideEmptyView(fileListUiState)

            fileListUiState.space?.let {
                binding.spaceHeader.root.apply {
                    if (fileListUiState.space.isProject && fileListUiState.folderToDisplay?.remotePath == ROOT_PATH) {
                        isVisible = true
                        animate().translationY(0f).duration = 100
                    } else {
                        animate().translationY(-height.toFloat()).withEndAction { isVisible = false }
                    }
                }

                val spaceSpecialImage = it.getSpaceSpecialImage()
                if (spaceSpecialImage != null) {
                    binding.spaceHeader.spaceHeaderImage.load(
                        ThumbnailsRequester.getPreviewUriForSpaceSpecial(spaceSpecialImage),
                        ThumbnailsRequester.getCoilImageLoader()
                    ) {
                        placeholder(R.drawable.ic_spaces)
                        error(R.drawable.ic_spaces)
                    }
                }
                binding.spaceHeader.spaceHeaderName.text = it.name
                binding.spaceHeader.spaceHeaderSubtitle.text = it.description
            }

            actionMode?.invalidate()
        }

        /* FileOperationsViewModel observables */
        // Observe the refresh folder operation
        fileOperationsViewModel.refreshFolderLiveData.observe(viewLifecycleOwner) {
            binding.syncProgressBar.isIndeterminate = it.peekContent().isLoading
            binding.swipeRefreshMainFileList.isRefreshing = it.peekContent().isLoading
            hideRefreshFab()
        }

        // Observe the create file with app provider operation
        collectLatestLifecycleFlow(fileOperationsViewModel.createFileWithAppProviderFlow) {
            val uiResult = it?.peekContent()
            if (uiResult is UIResult.Error) {
                val errorMessage =
                    uiResult.error?.parseError(resources.getString(R.string.create_file_fail_msg), resources, false)
                showMessageInSnackbar(
                    message = errorMessage.toString()
                )
            } else if (uiResult is UIResult.Success) {
                val fileId = uiResult.data
                val appName = currentDefaultApplication
                if (fileId != null && appName != null) {
                    mainFileListViewModel.openInWeb(fileId, appName)
                }
            }
        }

        collectLatestLifecycleFlow(fileOperationsViewModel.checkIfFileIsLocalAndNotAvailableOfflineSharedFlow) {
            val fileActivity = (requireActivity() as FileActivity)
            when (it) {
                is UIResult.Loading -> fileActivity.showLoadingDialog(R.string.common_loading)
                is UIResult.Success -> {
                    fileActivity.dismissLoadingDialog()
                    it.data?.let { result -> onShowRemoveDialog(filesToRemove, result) }
                }

                is UIResult.Error -> {
                    fileActivity.dismissLoadingDialog()
                    showMessageInSnackbar(resources.getString(R.string.common_error_unknown))
                }
            }
        }

        /* TransfersViewModel observables */
        observeTransfers()

    }

    private suspend fun getDrawableFromUrl(context: Context, url: String): Drawable? {
        return withContext(Dispatchers.IO) {
            Glide.with(context)
                .load(url)
                .fitCenter()
                .submit()
                .get()
        }
    }

    private fun observeTransfers() {
        val maxUploadsToRefresh = resources.getInteger(R.integer.max_uploads_to_refresh)
        collectLatestLifecycleFlow(transfersViewModel.transfersWithSpaceStateFlow) { transfers ->
            if (transfers.isNotEmpty()) {
                val newlySucceededTransfers = transfers.map { it.first }.filter {
                    it.status == TransferStatus.TRANSFER_SUCCEEDED &&
                            it.accountName == AccountUtils.getCurrentOwnCloudAccount(requireContext()).name
                }
                val safeSucceededTransfers = succeededTransfers
                if (safeSucceededTransfers == null) {
                    succeededTransfers = newlySucceededTransfers
                } else if (safeSucceededTransfers != newlySucceededTransfers) {
                    val differentNewlySucceededTransfers = newlySucceededTransfers.filter { it !in safeSucceededTransfers }
                    differentNewlySucceededTransfers.forEach { transfer ->
                        numberOfUploadsRefreshed++
                        val currentFolder = mainFileListViewModel.getFile()
                        if (transfer.remotePath.toPath().parent!!.toString() == currentFolder.remotePath.toPath().toString()) {
                            if (numberOfUploadsRefreshed <= maxUploadsToRefresh) {
                                if (!fileOperationsViewModel.refreshFolderLiveData.value!!.peekContent().isLoading) {
                                    fileOperationsViewModel.performOperation(
                                        FileOperation.RefreshFolderOperation(
                                            folderToRefresh = currentFolder,
                                            shouldSyncContents = false,
                                        )
                                    )
                                }
                            } else {
                                binding.fabRefresh.apply {
                                    isVisible = true
                                    animate().translationY(0f).duration = 100
                                }
                            }
                        }
                    }

                    succeededTransfers = newlySucceededTransfers
                }
            }
        }

    }

    fun navigateToFolderId(folderId: Long) {
        mainFileListViewModel.navigateToFolderId(folderId)
    }

    fun navigateToFolder(folder: OCFile) {
        mainFileListViewModel.updateFolderToDisplay(newFolderToDisplay = folder)
    }

    private fun showOrHideEmptyView(fileListUiState: MainFileListViewModel.FileListUiState.Success) {
        binding.recyclerViewMainFileList.isVisible = fileListUiState.folderContent.isNotEmpty()

        with(binding.emptyDataParent) {
            root.isVisible = fileListUiState.folderContent.isEmpty()

            if (fileListUiState.fileListOption.isSharedByLink() && fileListUiState.space != null) {
                // Temporary solution for shares space
                listEmptyDatasetIcon.setImageResource(R.drawable.ic_ocis_shares)
                listEmptyDatasetTitle.setText(R.string.shares_list_empty_title)
                listEmptyDatasetSubTitle.setText(R.string.shares_list_empty_subtitle)
            } else {
                listEmptyDatasetIcon.setImageResource(fileListUiState.fileListOption.toDrawableRes())
                listEmptyDatasetTitle.setText(fileListUiState.fileListOption.toTitleStringRes())
                listEmptyDatasetSubTitle.setText(fileListUiState.fileListOption.toSubtitleStringRes())
            }
        }
    }

    private fun hideRefreshFab() {
        binding.fabRefresh.apply {
            animate().translationY(-this.height.toFloat() * 2).withEndAction { this.isVisible = false }
        }
    }

    override fun onSortTypeListener(sortType: SortType, sortOrder: SortOrder) {
        val sortBottomSheetFragment = newInstance(sortType, sortOrder)
        sortBottomSheetFragment.sortDialogListener = this
        sortBottomSheetFragment.show(childFragmentManager, SortBottomSheetFragment.TAG)
    }

    override fun onViewTypeListener(viewType: ViewType) {
        binding.optionsLayout.viewTypeSelected = viewType

        if (viewType == ViewType.VIEW_TYPE_LIST) {
            mainFileListViewModel.setListModeAsPreferred()
            layoutManager.spanCount = 1

        } else {
            mainFileListViewModel.setGridModeAsPreferred()
            layoutManager.spanCount = ColumnQuantity(requireContext(), R.layout.grid_item).calculateNoOfColumns()
        }

        fileListAdapter.notifyItemRangeChanged(0, fileListAdapter.itemCount)
    }

    override fun onSortSelected(sortType: SortType) {
        binding.optionsLayout.sortTypeSelected = sortType

        mainFileListViewModel.updateSortTypeAndOrder(sortType, binding.optionsLayout.sortOrderSelected)
    }

    private fun isPickingAFolder(): Boolean {
        val args = arguments
        return args != null && args.getBoolean(ARG_PICKING_A_FOLDER, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    fun updateFileListOption(newFileListOption: FileListOption, file: OCFile) {
        mainFileListViewModel.updateFolderToDisplay(file)
        mainFileListViewModel.updateFileListOption(newFileListOption)
        showOrHideFab(newFileListOption, file)
    }

    /**
     * Check whether the fab should be shown or hidden depending on the [FileListOption] and
     * the current folder displayed permissions
     *
     * Show FAB when [FileListOption.ALL_FILES] and not picking a folder
     * Hide FAB When [FileListOption.SHARED_BY_LINK], [FileListOption.AV_OFFLINE] or picking a folder
     *
     * @param newFileListOption new file list option to enable.
     */
    private fun showOrHideFab(newFileListOption: FileListOption, currentFolder: OCFile) {
        if (!newFileListOption.isAllFiles() || isPickingAFolder() || (!currentFolder.hasAddFilePermission && !currentFolder.hasAddSubdirectoriesPermission)) {
            toggleFabVisibility(false)
        } else {
            toggleFabVisibility(true)
            if (!currentFolder.hasAddFilePermission) {
                binding.fabUpload.isVisible = false
                binding.fabNewfile.isVisible = false
            } else if (!currentFolder.hasAddSubdirectoriesPermission) {
                binding.fabMkdir.isVisible = false
            }
            registerFabUploadListener()
            registerFabMkDirListener()
            registerFabNewShortcutListener()
        }
    }

    /**
     * Sets the 'visibility' state of the main FAB and its mini FABs contained in the fragment.
     *
     * When 'false' is set, FAB visibility is set to View.GONE programmatically.
     * Mini FABs are automatically hidden after hiding the main one.
     *
     * @param shouldBeShown Desired visibility for the FAB.
     */
    private fun toggleFabVisibility(shouldBeShown: Boolean) {
        binding.fabMain.isVisible = shouldBeShown
        binding.fabUpload.isVisible = shouldBeShown
        binding.fabMkdir.isVisible = shouldBeShown
    }

    /**
     * Registers [android.view.View.OnClickListener] on the 'Upload' mini FAB for the linked action.
     */
    private fun registerFabUploadListener() {
        binding.fabUpload.setOnClickListener {
            openBottomSheetToUploadFiles()
            collapseFab()
        }
    }

    /**
     * Registers [android.view.View.OnClickListener] on the 'New folder' mini FAB for the linked action.
     */
    private fun registerFabMkDirListener() {
        binding.fabMkdir.setOnClickListener {
            val dialog = CreateFolderDialogFragment.newInstance(mainFileListViewModel.getFile(), this)
            dialog.show(requireActivity().supportFragmentManager, DIALOG_CREATE_FOLDER)
            collapseFab()
        }
    }

    /**
     * Registers [android.view.View.OnClickListener] on the 'New document' mini FAB for the linked action.
     */
    private fun registerFabNewFileListener(listAppRegistry: List<AppRegistryMimeType>) {
        binding.fabNewfile.setOnClickListener {
            openBottomSheetToCreateNewFile(listAppRegistry)
            collapseFab()
        }
    }

    /**
     * Registers [android.view.View.OnClickListener] on the 'New shortcut' mini FAB for the linked action.
     */
    private fun registerFabNewShortcutListener() {
        binding.fabNewshortcut.setOnClickListener {
            val dialog = CreateShortcutDialogFragment.newInstance(mainFileListViewModel.getFile(), this)
            dialog.show(requireActivity().supportFragmentManager, DIALOG_CREATE_SHORTCUT)
            collapseFab()
        }
    }

    fun collapseFab() {
        binding.fabMain.collapse()
    }

    fun isFabExpanded() = binding.fabMain.isExpanded

    private fun openBottomSheetToUploadFiles() {
        val uploadBottomSheet = layoutInflater.inflate(R.layout.upload_bottom_sheet_fragment, null)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(uploadBottomSheet)
        val uploadFromFilesItemView: BottomSheetFragmentItemView = uploadBottomSheet.findViewById(R.id.upload_from_files_item_view)
        val uploadFromCameraItemView: BottomSheetFragmentItemView = uploadBottomSheet.findViewById(R.id.upload_from_camera_item_view)
        val uploadToTextView = uploadBottomSheet.findViewById<TextView>(R.id.upload_to_text_view)
        uploadFromFilesItemView.setOnClickListener {
            uploadActions?.uploadFromFileSystem()
            dialog.hide()
        }
        uploadFromCameraItemView.setOnClickListener {
            uploadActions?.uploadFromCamera()
            dialog.hide()
        }
        uploadToTextView.text = String.format(
            resources.getString(R.string.upload_to),
            resources.getString(R.string.app_name)
        )
        val uploadBottomSheetBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(uploadBottomSheet.parent as View)
        dialog.setOnShowListener { uploadBottomSheetBehavior.setPeekHeight(uploadBottomSheet.measuredHeight) }
        dialog.show()
    }

    private fun openBottomSheetToCreateNewFile(listAppRegistry: List<AppRegistryMimeType>) {
        val newFileBottomSheet = layoutInflater.inflate(R.layout.newfile_bottom_sheet_fragment, null)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(newFileBottomSheet)
        val docTypesBottomSheetLayout = newFileBottomSheet.findViewById<LinearLayout>(R.id.doc_types_bottom_sheet_layout)
        listAppRegistry.forEach { appRegistry ->
            val documentTypeItemView = BottomSheetFragmentItemView(requireContext())
            documentTypeItemView.apply {
                removeDefaultTint()
                title = appRegistry.name
                itemIcon = ResourcesCompat.getDrawable(resources, MimetypeIconUtil.getFileTypeIconId(appRegistry.mimeType, appRegistry.ext), null)
                if (appRegistry.ext == FILE_DOCXF_EXTENSION) {
                    itemIcon?.setTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.file_docxf)))
                }
                setOnClickListener {
                    showFilenameTextDialog(appRegistry.ext)
                    currentDefaultApplication = appRegistry.defaultApplication
                    dialog.hide()
                }
            }
            docTypesBottomSheetLayout.addView(documentTypeItemView)
        }

        val newFileBottomSheetBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(newFileBottomSheet.parent as View)
        dialog.setOnShowListener { newFileBottomSheetBehavior.setPeekHeight(newFileBottomSheet.measuredHeight) }
        dialog.show()
    }

    private fun showFilenameTextDialog(fileExtension: String?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_upload_text, null)
        dialogView.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(requireContext())

        val input = dialogView.findViewById<TextInputEditText>(R.id.inputFileName)
        val inputLayout: TextInputLayout = dialogView.findViewById(R.id.inputTextLayout)
        input.requestFocus()

        val builder = AlertDialog.Builder(requireContext()).apply {
            setView(dialogView)
            setTitle(R.string.uploader_upload_text_dialog_title)
            setCancelable(false)
            setPositiveButton(android.R.string.ok) { _, _ ->
                val currentFolder = mainFileListViewModel.getFile()
                val filename = input.text.toString()
                var error: String? = null

                if (error != null) {
                    showMessageInSnackbar(error)
                } else {
                    val filenameWithExtension = "$filename.$fileExtension"
                    fileOperationsViewModel.performOperation(
                        FileOperation.CreateFileWithAppProviderOperation(
                            currentFolder.owner,
                            currentFolder.remoteId!!,
                            filenameWithExtension
                        )
                    )
                }
            }
            setNegativeButton(android.R.string.cancel, null)
        }
        val alertDialog = builder.create()

        input.doOnTextChanged { text, _, _, _ ->
            val okButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            var error: String? = null
            if (text.isNullOrBlank()) {
                okButton.isEnabled = false
                error = getString(R.string.uploader_upload_text_dialog_filename_error_empty)
            } else if (text.length > MAX_FILENAME_LENGTH) {
                error = String.format(
                    getString(R.string.uploader_upload_text_dialog_filename_error_length_max),
                    MAX_FILENAME_LENGTH
                )
            } else if (forbiddenChars.any { text.contains(it) }) {
                error = getString(R.string.filename_forbidden_characters)
            } else {
                okButton.isEnabled = true
                error = null
                inputLayout.error = error
            }

            if (error != null) {
                okButton.isEnabled = false
                inputLayout.error = error
            }
        }


        alertDialog.apply {
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            show()
        }
    }

    private fun onShowRemoveDialog(filesToRemove: List<OCFile>, isAvailableLocallyAndNotAvailableOffline: Boolean) {
        val dialog = RemoveFilesDialogFragment.newInstance(ArrayList(filesToRemove), isAvailableLocallyAndNotAvailableOffline)
        dialog.show(requireActivity().supportFragmentManager, TAG_REMOVE_FILES_DIALOG_FRAGMENT)
        fileListAdapter.clearSelection()
        updateActionModeAfterTogglingSelected()
    }

    override fun createShortcutFileFromApp(fileName: String, url: String) {
        val fileContent = """
                [InternetShortcut]
                URL=$url
                """.trimIndent()
        val storageDir = requireActivity().externalCacheDir
        val shortcutFile = File(storageDir, "$fileName.url")
        shortcutFile.writeText(fileContent)
        val shortcutFilePath = shortcutFile.absolutePath
        uploadActions?.uploadShortcutFileFromApp(arrayOf(shortcutFilePath))
    }

    override fun onFolderNameSet(newFolderName: String, parentFolder: OCFile) {
        fileOperationsViewModel.performOperation(FileOperation.CreateFolder(newFolderName, parentFolder))
        fileOperationsViewModel.createFolder.observe(viewLifecycleOwner, Event.EventObserver { uiResult: UIResult<Unit> ->
            if (uiResult is UIResult.Error) {
                val errorMessage =
                    uiResult.error?.parseError(resources.getString(R.string.create_dir_fail_msg), resources, false)
                showMessageInSnackbar(
                    message = errorMessage.toString()
                )
            }
        })
    }

    override fun onCreateFolderListener() {
        val dialog = CreateFolderDialogFragment.newInstance(mainFileListViewModel.getFile(), this)
        dialog.show(requireActivity().supportFragmentManager, DIALOG_CREATE_FOLDER)
    }

    override fun onQueryTextSubmit(query: String?): Boolean = false

    override fun onQueryTextChange(newText: String?): Boolean {
        newText?.let { mainFileListViewModel.updateSearchFilter(it) }
        return true
    }

    fun setSearchListener(searchView: SearchView) {
        searchView.setOnQueryTextListener(this)
    }

    /**
     * Call this, when the user presses the up button.
     *
     *
     * Tries to move up the current folder one level. If the parent folder was removed from the
     * database, it continues browsing up until finding an existing folders.
     *
     *
     */
    fun onBrowseUp() {
        mainFileListViewModel.manageBrowseUp()
    }

    /**
     * Use this to query the [OCFile] that is currently
     * being displayed by this fragment
     *
     * @return The currently viewed OCFile
     */
    fun getCurrentFile(): OCFile {
        return mainFileListViewModel.getFile()
    }

    fun getCurrentSpace(): OCSpace? {
        return mainFileListViewModel.getSpace()
    }

    private fun setDrawerStatus(enabled: Boolean) {
        (activity as FileActivity).setDrawerLockMode(if (enabled) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    /**
     * Start the appropriate action(s) on the currently selected files given menu selected by the user.
     *
     * @param menuId Identifier of the action menu selected by the user
     * @return 'true' if the menu selection started any action, 'false' otherwise.
     */
    @SuppressLint("UseRequireInsteadOfGet")
    private fun onFileActionChosen(menuId: Int?): Boolean {
        val checkedFilesWithSyncInfo = fileListAdapter.getCheckedItems() as ArrayList<OCFileWithSyncInfo>

        if (checkedFilesWithSyncInfo.isEmpty()) {
            return false
        } else if (checkedFilesWithSyncInfo.size == 1) {
            /// action only possible on a single file
            val singleFile = checkedFilesWithSyncInfo.first().file

            openInWebProviders.forEach { (openInWebProviderName, menuItemId) ->
                if (menuItemId == menuId) {
                    mainFileListViewModel.openInWeb(singleFile.remoteId!!, openInWebProviderName)
                    return true
                }
            }

            when (menuId) {
                R.id.action_share_file -> {
                    fileActions?.onShareFileClicked(singleFile)
                    fileListAdapter.clearSelection()
                    updateActionModeAfterTogglingSelected()
                    return true
                }

                R.id.action_open_file_with -> {
                    fileActions?.openFile(singleFile)
                    fileListAdapter.clearSelection()
                    updateActionModeAfterTogglingSelected()
                    return true
                }

                R.id.action_rename_file -> {
                    val dialog = RenameFileDialogFragment.newInstance(singleFile)
                    dialog.show(requireActivity().supportFragmentManager, FRAGMENT_TAG_RENAME_FILE)
                    fileListAdapter.clearSelection()
                    updateActionModeAfterTogglingSelected()
                    return true
                }

                R.id.action_see_details -> {
                    fileListAdapter.clearSelection()
                    updateActionModeAfterTogglingSelected()
                    fileActions?.showDetails(singleFile)
                    return true
                }

                R.id.action_sync_file -> {
                    syncFiles(listOf(singleFile))
                    return true
                }

                R.id.action_send_file -> {
                    //Obtain the file
                    if (!singleFile.isAvailableLocally) { // Download the file
                        Timber.d("%s : File must be downloaded", singleFile.remotePath)
                        fileActions?.initDownloadForSending(singleFile)
                    } else {
                        fileActions?.sendDownloadedFile(singleFile)
                    }
                    return true
                }

                R.id.action_set_available_offline -> {
                    fileOperationsViewModel.performOperation(FileOperation.SetFilesAsAvailableOffline(listOf(singleFile)))
                    if (singleFile.isFolder) {
                        fileOperationsViewModel.performOperation(
                            FileOperation.SynchronizeFolderOperation(
                                folderToSync = singleFile,
                                accountName = singleFile.owner,
                                isActionSetFolderAvailableOfflineOrSynchronize = true,
                            )
                        )
                    } else {
                        fileOperationsViewModel.performOperation(FileOperation.SynchronizeFileOperation(singleFile, singleFile.owner))
                    }
                    return true
                }

                R.id.action_unset_available_offline -> {
                    fileOperationsViewModel.performOperation(FileOperation.UnsetFilesAsAvailableOffline(listOf(singleFile)))
                }
            }
        }

        /// Actions possible on a batch of files
        val checkedFiles = checkedFilesWithSyncInfo.map { it.file } as ArrayList<OCFile>
        when (menuId) {
            R.id.file_action_select_all -> {
                fileListAdapter.selectAll()
                updateActionModeAfterTogglingSelected()
                return true
            }

            R.id.action_select_inverse -> {
                fileListAdapter.selectInverse()
                updateActionModeAfterTogglingSelected()
                return true
            }

            R.id.action_remove_file -> {
                filesToRemove = checkedFiles
                fileOperationsViewModel.showRemoveDialog(filesToRemove)
                return true
            }

            R.id.action_download_file,
            R.id.action_sync_file -> {
                syncFiles(checkedFiles)
                return true
            }

            R.id.action_cancel_sync -> {
                fileActions?.cancelFileTransference(checkedFiles)
                return true
            }

            R.id.action_set_available_offline -> {
                fileOperationsViewModel.performOperation(FileOperation.SetFilesAsAvailableOffline(checkedFiles))
                checkedFiles.forEach { ocFile ->
                    if (ocFile.isFolder) {
                        fileOperationsViewModel.performOperation(FileOperation.SynchronizeFolderOperation(ocFile, ocFile.owner))
                    } else {
                        fileOperationsViewModel.performOperation(FileOperation.SynchronizeFileOperation(ocFile, ocFile.owner))
                    }
                }
                return true
            }

            R.id.action_unset_available_offline -> {
                fileOperationsViewModel.performOperation(FileOperation.UnsetFilesAsAvailableOffline(checkedFiles))
                return true
            }

            R.id.action_send_file -> {
                requireActivity().sendDownloadedFilesByShareSheet(checkedFiles)
            }

            R.id.action_move -> {
                val action = Intent(activity, FolderPickerActivity::class.java)
                action.putParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES, checkedFiles)
                action.putExtra(FolderPickerActivity.EXTRA_PICKER_MODE, FolderPickerActivity.PickerMode.MOVE)
                requireActivity().startActivityForResult(action, FileDisplayActivity.REQUEST_CODE__MOVE_FILES)
                fileListAdapter.clearSelection()
                updateActionModeAfterTogglingSelected()
                return true
            }

            R.id.action_copy -> {
                val action = Intent(activity, FolderPickerActivity::class.java)
                action.putParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES, checkedFiles)
                action.putExtra(FolderPickerActivity.EXTRA_PICKER_MODE, FolderPickerActivity.PickerMode.COPY)
                requireActivity().startActivityForResult(action, FileDisplayActivity.REQUEST_CODE__COPY_FILES)
                fileListAdapter.clearSelection()
                updateActionModeAfterTogglingSelected()
                return true
            }
        }

        return false
    }

    /**
     * Update or remove the actionMode after applying any change to the selected items.
     */
    private fun updateActionModeAfterTogglingSelected() {
        val selectedItems = fileListAdapter.selectedItemCount
        if (selectedItems == 0) {
            actionMode?.finish()
        } else {
            if (actionMode == null) {
                actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(actionModeCallback)
            }
            actionMode?.apply {
                title = selectedItems.toString()
                invalidate()
            }
        }
    }

    override fun onItemClick(ocFileWithSyncInfo: OCFileWithSyncInfo, position: Int) {
        if (actionMode != null) {
            toggleSelection(position)
            return
        }

        val ocFile = ocFileWithSyncInfo.file

        if (ocFile.isFolder) {
            mainFileListViewModel.updateFolderToDisplay(ocFile)
        } else { // Click on a file
            fileActions?.onFileClicked(ocFile)
        }
    }

    override fun onLongItemClick(position: Int): Boolean {
        if (isPickingAFolder()) return false

        if (actionMode == null) {
            actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(actionModeCallback)
            // Notify all when enabling multiselection for the first time to show checkboxes on every single item.
            fileListAdapter.notifyDataSetChanged()
        }
        toggleSelection(position)
        return true
    }

    override fun onThreeDotButtonClick(fileWithSyncInfo: OCFileWithSyncInfo) {
        val file = fileWithSyncInfo.file
        fileSingleFile = file
        val fileSync = OCFileSyncInfo(
            fileId = fileWithSyncInfo.file.id!!,
            uploadWorkerUuid = fileWithSyncInfo.uploadWorkerUuid,
            downloadWorkerUuid = fileWithSyncInfo.downloadWorkerUuid,
            isSynchronizing = fileWithSyncInfo.isSynchronizing
        )
        mainFileListViewModel.filterMenuOptions(
            listOf(file), listOf(fileSync),
            displaySelectAll = false, isMultiselection = false
        )
    }

    private val actionModeCallback: ActionMode.Callback = object : ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            setDrawerStatus(enabled = false)
            actionMode = mode

            val inflater = requireActivity().menuInflater
            inflater.inflate(R.menu.file_actions_menu, menu)
            this@MainFileListFragment.menu = menu

            mode?.invalidate()

            // Set gray color
            val window = activity?.window
            statusBarColor = window?.statusBarColor ?: -1

            // Hide FAB in multi selection mode
            toggleFabVisibility(false)
            fileActions?.setBottomBarVisibility(false)

            // Hide sort options view in multi-selection mode
            binding.optionsLayout.visibility = View.GONE

            return true
        }

        /**
         * Updates available action in menu depending on current selection.
         */
        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val checkedFilesWithSyncInfo = fileListAdapter.getCheckedItems()
            val checkedCount = checkedFilesWithSyncInfo.size
            val title = resources.getQuantityString(
                R.plurals.items_selected_count,
                checkedCount,
                checkedCount
            )
            mode?.title = title

            checkedFiles = checkedFilesWithSyncInfo.map { it.file }

            val checkedFilesSync = checkedFilesWithSyncInfo.map {
                OCFileSyncInfo(
                    fileId = it.file.id!!,
                    uploadWorkerUuid = it.uploadWorkerUuid,
                    downloadWorkerUuid = it.downloadWorkerUuid,
                    isSynchronizing = it.isSynchronizing
                )
            }

            val displaySelectAll = checkedCount != fileListAdapter.itemCount - 1 // -1 because one of them is the footer :S
            mainFileListViewModel.filterMenuOptions(
                checkedFiles, checkedFilesSync,
                displaySelectAll, isMultiselection = true
            )

            if (checkedFiles.size == 1) {
                mainFileListViewModel.getAppRegistryForMimeType(checkedFiles.first().mimeType, isMultiselection = true)
            } else {
                menu?.let {
                    openInWebProviders.forEach { (_, menuItemId) ->
                        it.removeItem(menuItemId)
                    }
                    openInWebProviders = emptyMap()
                }
            }

            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return onFileActionChosen(item?.itemId)
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            setDrawerStatus(enabled = true)
            actionMode = null

            // reset to previous color
            requireActivity().window.statusBarColor = statusBarColor!!

            // show or hide FAB on multi selection mode exit
            showOrHideFab(mainFileListViewModel.fileListOption.value, mainFileListViewModel.currentFolderDisplayed.value)

            fileActions?.setBottomBarVisibility(true)

            // Show sort options view when multi-selection mode finish
            binding.optionsLayout.visibility = View.VISIBLE

            fileListAdapter.clearSelection()
        }
    }

    private fun syncFiles(files: List<OCFile>) {
        for (file in files) {
            if (file.isFolder) {
                fileOperationsViewModel.performOperation(
                    FileOperation.SynchronizeFolderOperation(
                        folderToSync = file,
                        accountName = file.owner,
                        isActionSetFolderAvailableOfflineOrSynchronize = true,
                    )
                )
            } else {
                fileOperationsViewModel.performOperation(FileOperation.SynchronizeFileOperation(fileToSync = file, accountName = file.owner))
            }
        }
    }

    fun setProgressBarAsIndeterminate(indeterminate: Boolean) {
        Timber.d("Setting progress visibility to %s", indeterminate)
        binding.shadowView.visibility = View.GONE
        binding.syncProgressBar.apply {
            visibility = View.VISIBLE
            isIndeterminate = indeterminate
            postInvalidate()
        }
    }

    interface FileActions {
        fun onCurrentFolderUpdated(newCurrentFolder: OCFile, currentSpace: OCSpace? = null)
        fun onFileClicked(file: OCFile)
        fun onShareFileClicked(file: OCFile)
        fun initDownloadForSending(file: OCFile)
        fun showDetails(file: OCFile)
        fun syncFile(file: OCFile)
        fun openFile(file: OCFile)
        fun sendDownloadedFile(file: OCFile)
        fun cancelFileTransference(files: ArrayList<OCFile>)
        fun setBottomBarVisibility(isVisible: Boolean)
    }

    interface UploadActions {
        fun uploadFromCamera()
        fun uploadShortcutFileFromApp(shortcutFilePath: Array<String>)
        fun uploadFromFileSystem()
    }

    companion object {
        val ARG_PICKING_A_FOLDER = "${MainFileListFragment::class.java.canonicalName}.ARG_PICKING_A_FOLDER}"
        val ARG_INITIAL_FOLDER_TO_DISPLAY = "${MainFileListFragment::class.java.canonicalName}.ARG_INITIAL_FOLDER_TO_DISPLAY}"
        val ARG_FILE_LIST_OPTION = "${MainFileListFragment::class.java.canonicalName}.FILE_LIST_OPTION}"
        const val MAX_FILENAME_LENGTH = 223
        val forbiddenChars = listOf('/', '\\')

        private const val DIALOG_CREATE_FOLDER = "DIALOG_CREATE_FOLDER"
        private const val DIALOG_CREATE_SHORTCUT = "DIALOG_CREATE_SHORTCUT"

        private const val TAG_SECOND_FRAGMENT = "SECOND_FRAGMENT"
        private const val FILE_DOCXF_EXTENSION = "docxf"

        @JvmStatic
        fun newInstance(
            initialFolderToDisplay: OCFile,
            pickingAFolder: Boolean = false,
            fileListOption: FileListOption = FileListOption.ALL_FILES,
        ): MainFileListFragment {
            val args = Bundle()
            args.putParcelable(ARG_INITIAL_FOLDER_TO_DISPLAY, initialFolderToDisplay)
            args.putBoolean(ARG_PICKING_A_FOLDER, pickingAFolder)
            args.putParcelable(ARG_FILE_LIST_OPTION, fileListOption)
            return MainFileListFragment().apply { arguments = args }
        }
    }
}

