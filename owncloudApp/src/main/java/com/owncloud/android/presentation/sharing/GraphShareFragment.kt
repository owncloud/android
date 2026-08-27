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

import android.content.Context
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
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.presentation.common.UIResult
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class GraphShareFragment : Fragment() {
    private var _binding: MembersFragmentBinding? = null
    private val binding get() = _binding!!

    private val graphShareViewModel by activityViewModel<GraphShareViewModel> {
        parametersOf(
            requireArguments().getString(ARG_ACCOUNT_NAME),
            requireArguments().getParcelable<OCFile>(ARG_FILE)
        )
    }

    private lateinit var graphSharesAdapter: GraphSharesAdapter

    private var roles: List<OCRole> = emptyList()
    private var listener: GraphShareFragmentListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = MembersFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.membersTitle.text = getString(R.string.share_with_people_title)

        graphSharesAdapter = GraphSharesAdapter()
        binding.membersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = graphSharesAdapter
        }

        binding.swipeRefreshMembers.setOnRefreshListener {
            graphShareViewModel.getGraphShares()
        }

        val file = requireArguments().getParcelable<OCFile>(ARG_FILE)
        val accountName = requireArguments().getString(ARG_ACCOUNT_NAME)
        binding.addMemberButton.isVisible = file?.hasResharePermission ?: false
        binding.addMemberButton.setOnClickListener {
            if (file != null && accountName != null) {
                graphShareViewModel.resetViewModel()
                listener?.addGraphShare(file = file, accountName = accountName)
            }
        }

        subscribeToViewModels()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as GraphShareFragmentListener?
        } catch (e: ClassCastException) {
            Timber.e(e, "The activity attached does not implement GraphShareFragmentListener")
            throw ClassCastException(activity.toString() + " must implement GraphShareFragmentListener")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun subscribeToViewModels() {
        observeRoles()
        observeShares()
        observeAddShareResult()
    }

    private fun observeRoles() {
        collectLatestLifecycleFlow(graphShareViewModel.roles) { event ->
            event?.let {
                when (val uiResult = event.peekContent()) {
                    is UIResult.Success -> {
                        uiResult.data?.let {
                            roles = it
                            graphShareViewModel.getGraphShares()
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
        collectLatestLifecycleFlow(graphShareViewModel.shares) { event ->
            event?.let {
                when (val uiResult = event.peekContent()) {
                    is UIResult.Success -> {
                        uiResult.data?.let {
                            val hasMembers = it.members.isNotEmpty()
                            binding.membersRecyclerView.isVisible = hasMembers
                            binding.noSharesMessage.isVisible = !hasMembers
                            graphSharesAdapter.setShares(it.members, it.roles)
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

    private fun observeAddShareResult() {
        collectLatestLifecycleFlow(graphShareViewModel.addShareResultFlow) { event ->
            event?.peekContent()?.let { uiResult ->
                when (uiResult) {
                    is UIResult.Loading -> { }
                    is UIResult.Success -> {
                        showMessageInSnackbar(getString(R.string.share_add_correctly))
                        graphShareViewModel.resetViewModel()
                    }
                    is UIResult.Error -> { }
                }
            }
        }
    }

    interface GraphShareFragmentListener {
        fun addGraphShare(file: OCFile, accountName: String)
    }

    companion object {
        private const val ARG_FILE = "FILE"
        private const val ARG_ACCOUNT_NAME = "ACCOUNT_NAME"

        fun newInstance(file: OCFile, accountName: String): GraphShareFragment {
            val args = Bundle().apply {
                putParcelable(ARG_FILE, file)
                putString(ARG_ACCOUNT_NAME, accountName)
            }
            return GraphShareFragment().apply {
                arguments = args
            }
        }
    }
}
