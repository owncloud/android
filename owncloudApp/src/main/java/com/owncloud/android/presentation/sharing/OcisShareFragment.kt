/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2026 ownCloud GmbH.
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

package com.owncloud.android.presentation.sharing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.owncloud.android.R
import com.owncloud.android.databinding.MembersFragmentBinding
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.roles.model.OCRole
import com.owncloud.android.extensions.collectLatestLifecycleFlow
import com.owncloud.android.extensions.showErrorInSnackbar
import com.owncloud.android.presentation.common.UIResult
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class OcisShareFragment : Fragment() {
    private var _binding: MembersFragmentBinding? = null
    private val binding get() = _binding!!

    private val ocisShareViewModel by viewModel<OcisShareViewModel> {
        parametersOf(
            requireArguments().getString(ARG_ACCOUNT_NAME),
            requireArguments().getParcelable<OCFile>(ARG_FILE)
        )
    }

    private lateinit var ocisSharesAdapter: OcisSharesAdapter

    private var roles: List<OCRole> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = MembersFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.membersTitle.text = getString(R.string.share_with_people_title)

        ocisSharesAdapter = OcisSharesAdapter()
        binding.membersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ocisSharesAdapter
        }

        subscribeToViewModels()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun subscribeToViewModels() {
        observeRoles()
        observeShares()
    }

    private fun observeRoles() {
        collectLatestLifecycleFlow(ocisShareViewModel.roles) { event ->
            event?.let {
                when (val uiResult = event.peekContent()) {
                    is UIResult.Success -> {
                        uiResult.data?.let {
                            roles = it
                            ocisShareViewModel.getOcisShares()
                        }
                    }
                    is UIResult.Loading -> { }
                    is UIResult.Error -> {
                        showErrorInSnackbar(R.string.share_sync_failed, uiResult.error)
                        Timber.e(uiResult.error, "Failed to retrieve platform roles")
                    }
                }
            }
        }
    }

    private fun observeShares() {
        collectLatestLifecycleFlow(ocisShareViewModel.shares) { event ->
            event?.let {
                when (val uiResult = event.peekContent()) {
                    is UIResult.Success -> {
                        uiResult.data?.let {
                            val hasMembers = it.members.isNotEmpty()
                            binding.membersRecyclerView.isVisible = hasMembers
                            binding.noSharesMessage.isVisible = !hasMembers
                            ocisSharesAdapter.setShares(it.members, it.roles)
                            binding.swipeRefreshMembers.isRefreshing = false
                        }
                    }
                    is UIResult.Loading -> { binding.swipeRefreshMembers.isRefreshing = true }
                    is UIResult.Error -> {
                        binding.swipeRefreshMembers.isRefreshing = false
                        showErrorInSnackbar(R.string.share_sync_failed, uiResult.error)
                        Timber.e(uiResult.error, "Failed to retrieve shares")
                    }
                }
            }
        }
    }

    companion object {
        private const val ARG_FILE = "FILE"
        private const val ARG_ACCOUNT_NAME = "ACCOUNT_NAME"

        fun newInstance(file: OCFile, accountName: String): OcisShareFragment {
            val args = Bundle().apply {
                putParcelable(ARG_FILE, file)
                putString(ARG_ACCOUNT_NAME, accountName)
            }
            return OcisShareFragment().apply {
                arguments = args
            }
        }
    }
}
