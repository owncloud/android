/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
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

package com.owncloud.android.presentation.spaces.members

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R
import com.owncloud.android.databinding.MembersFragmentBinding
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.extensions.collectLatestLifecycleFlow
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.utils.DisplayUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class SpaceMembersFragment : Fragment() {
    private var _binding: MembersFragmentBinding? = null
    private val binding get() = _binding!!

    private val spaceMembersViewModel: SpaceMembersViewModel by viewModel {
        parametersOf(
            requireArguments().getString(ARG_ACCOUNT_NAME),
            requireArguments().getParcelable<OCSpace>(ARG_CURRENT_SPACE)
        )
    }

    private lateinit var spaceMembersAdapter: SpaceMembersAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = MembersFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        spaceMembersAdapter = SpaceMembersAdapter()
        recyclerView = binding.membersRecyclerView
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = spaceMembersAdapter
        }

        collectLatestLifecycleFlow(spaceMembersViewModel.spaceMembers) { event ->
            event?.let {
                when (val uiResult = event.peekContent()) {
                    is UIResult.Success -> {
                        uiResult.data?.let { spaceMembersAdapter.setSpaceMembers(it) }
                    }
                    is UIResult.Loading -> { }
                    is UIResult.Error -> { }
                }
            }
        }

        val currentSpace = requireArguments().getParcelable<OCSpace>(ARG_CURRENT_SPACE) ?: return
        binding.apply {
            itemName.text = currentSpace.name
            currentSpace.quota?.let { quota ->
                val usedQuota = quota.used
                val totalQuota = quota.total
                itemSize.text = when {
                    usedQuota == null -> getString(R.string.drawer_unavailable_used_storage)
                    totalQuota == 0L -> DisplayUtils.bytesToHumanReadable(usedQuota, requireContext(), true)
                    else -> getString(
                        R.string.drawer_quota,
                        DisplayUtils.bytesToHumanReadable(usedQuota, requireContext(), true),
                        DisplayUtils.bytesToHumanReadable(totalQuota, requireContext(), true),
                        quota.getRelative().toString())
                }
            }
        }
    }

    companion object {
        private const val ARG_CURRENT_SPACE = "CURRENT_SPACE"
        private const val ARG_ACCOUNT_NAME = "ACCOUNT_NAME"

        fun newInstance(
            accountName: String,
            currentSpace: OCSpace
        ): SpaceMembersFragment {
            val args = Bundle().apply {
                putString(ARG_ACCOUNT_NAME, accountName)
                putParcelable(ARG_CURRENT_SPACE, currentSpace)
            }
            return SpaceMembersFragment().apply {
                arguments = args
            }
        }
    }
}
