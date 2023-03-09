/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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

package com.owncloud.android.presentation.transfers

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.owncloud.android.R
import com.owncloud.android.databinding.FragmentTransferListBinding
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.domain.transfers.model.TransferResult
import com.owncloud.android.extensions.collectLatestLifecycleFlow
import com.owncloud.android.presentation.authentication.AccountUtils
import com.owncloud.android.ui.activity.FileActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class TransferListFragment : Fragment() {

    private val transfersViewModel by viewModel<TransfersViewModel>()

    private var _binding: FragmentTransferListBinding? = null
    val binding get() = _binding!!

    private lateinit var transfersAdapter: TransfersAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransferListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        transfersAdapter = TransfersAdapter(
            cancel = { transfer ->
                transfersViewModel.cancelUpload(transfer)
            },
            retry = { transfer: OCTransfer ->
                if (transfer.lastResult == TransferResult.CREDENTIAL_ERROR) {
                    val parentActivity = requireActivity() as FileActivity
                    val account = AccountUtils.getOwnCloudAccountByName(requireContext(), transfer.accountName)
                    parentActivity.fileOperationsHelper.checkCurrentCredentials(account)
                } else {
                    val file = File(transfer.localPath)
                    if (file.exists()) {
                        transfersViewModel.retryUploadFromSystem(transfer.id!!)
                    } else if (DocumentFile.isDocumentUri(requireContext(), Uri.parse(transfer.localPath))) {
                        transfersViewModel.retryUploadFromContentUri(transfer.id!!)
                    } else {
                        Snackbar.make(
                            view,
                            getString(R.string.local_file_not_found_toast),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            },
            clearFailed = {
                transfersViewModel.clearFailedTransfers()
            },
            retryFailed = {
                transfersViewModel.retryFailedTransfers()
            },
            clearSuccessful = {
                transfersViewModel.clearSuccessfulTransfers()
            },
            personalName = getString(R.string.bottom_nav_personal),
        )
        binding.transfersRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            adapter = transfersAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        transfersViewModel.transfersListLiveData.observe(viewLifecycleOwner) { transfers ->
            val recyclerViewState = binding.transfersRecyclerView.layoutManager?.onSaveInstanceState()
            setData(transfers)
            binding.transfersRecyclerView.layoutManager?.onRestoreInstanceState(recyclerViewState)
        }

        transfersViewModel.workInfosListLiveData.observe(viewLifecycleOwner) { workInfos ->
            workInfos.forEach { workInfo ->
                transfersAdapter.updateTransferProgress(workInfo)
            }
        }

        collectLatestLifecycleFlow(transfersViewModel.spaces) {
            transfersViewModel.transfersListLiveData.value?.let { transfers -> setData(transfers) }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun setData(items: List<OCTransfer>) {
        binding.transfersRecyclerView.isVisible = items.isNotEmpty()
        binding.transfersListEmpty.apply {
            root.isVisible = items.isEmpty()
            listEmptyDatasetIcon.setImageResource(R.drawable.ic_uploads)
            listEmptyDatasetTitle.setText(R.string.upload_list_empty)
            listEmptyDatasetSubTitle.setText(R.string.upload_list_empty_subtitle)
        }
        transfersAdapter.setData(items, transfersViewModel.spaces.value)
    }
}
