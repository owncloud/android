/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2022 ownCloud GmbH.
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

package com.owncloud.android.presentation.ui.transfers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.owncloud.android.R
import com.owncloud.android.databinding.FragmentTransferListBinding
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.presentation.adapters.transfers.TransfersAdapter

class TransferListFragment : Fragment(R.layout.fragment_transfer_list) {

    private var _binding: FragmentTransferListBinding? = null
    val binding get() = _binding!!

    private val transfersAdapter = TransfersAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransferListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.transfersRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            adapter = transfersAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        setData(mutableListOf())
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun setData(items: List<OCTransfer>) {
        binding.transfersRecyclerView.isVisible = items.isNotEmpty()
        binding.emptyListText.isVisible = items.isEmpty()

        transfersAdapter.setData(items)
    }
}
