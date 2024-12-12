/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
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

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.owncloud.android.R
import com.owncloud.android.databinding.MainEmptyListFragmentBinding

class MainEmptyListFragment : Fragment() {

    private var _binding: MainEmptyListFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MainEmptyListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.emptyDataParent.apply {
            listEmptyDatasetIcon.setImageResource(R.drawable.ic_folder)
            listEmptyDatasetTitle.setText(R.string.file_list_empty_title_all_files)
            listEmptyDatasetSubTitle.setText(R.string.light_users_subtitle)
        }
        val titleToolbar = requireActivity().findViewById<TextView>(R.id.root_toolbar_title)
        titleToolbar.apply {
            setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
            isClickable = false
        }
    }


}
