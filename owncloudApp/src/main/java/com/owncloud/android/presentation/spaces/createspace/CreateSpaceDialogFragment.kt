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

package com.owncloud.android.presentation.spaces.createspace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import com.owncloud.android.R
import com.owncloud.android.databinding.CreateSpaceDialogBinding

class CreateSpaceDialogFragment : DialogFragment() {
    private var _binding: CreateSpaceDialogBinding? = null
    private val binding get() = _binding!!

    private lateinit var createSpaceListener: CreateSpaceListener

    private val forbiddenRegex = Regex(FORBIDDEN_CHARACTERS)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = CreateSpaceDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            cancelCreateSpaceButton.setOnClickListener { dialog?.dismiss() }
            createSpaceDialogNameValue.doOnTextChanged { name, _, _, _ ->
                val errorMessage = validateName(name.toString())
                updateUI(errorMessage)
            }
            createSpaceButton.setOnClickListener {
                val spaceQuota = convertToBytes(binding.createSpaceDialogQuotaUnit.selectedItem.toString())
                createSpaceListener.createSpace(
                    spaceName = binding.createSpaceDialogNameValue.text.toString(),
                    spaceSubtitle = binding.createSpaceDialogSubtitleValue.text.toString(),
                    spaceQuota = spaceQuota
                )
                dialog?.dismiss()
            }
        }
    }

    private fun validateName(spaceName: String): String? =
        when {
            spaceName.trim().isEmpty() -> getString(R.string.create_space_dialog_empty_error)
            spaceName.length > 255 -> getString(R.string.create_space_dialog_length_error)
            spaceName.contains(forbiddenRegex) -> getString(R.string.create_space_dialog_characters_error)
            else -> null
        }

    private fun updateUI(errorMessage: String?) {
        val colorButton = if (errorMessage == null) {
            resources.getColor(R.color.primary_button_background_color, null)
        } else {
            resources.getColor(R.color.grey, null)
        }

        binding.createSpaceDialogNameValue.error = errorMessage
        binding.createSpaceButton.apply {
            setTextColor(colorButton)
            isEnabled = errorMessage == null
        }
    }

    private fun convertToBytes(spaceQuota: String): Long {
        val quotaNumber = spaceQuota.removeSuffix(" GB").toLongOrNull() ?: return 0L
        return quotaNumber * 1_000_000_000L
    }

    interface CreateSpaceListener {
        fun createSpace(spaceName: String, spaceSubtitle: String, spaceQuota: Long)
    }

    companion object {
        private const val ARG_ACCOUNT_NAME = "ACCOUNT_NAME"
        private const val FORBIDDEN_CHARACTERS = """[/\\.:?*"'><|]"""

        fun newInstance(accountName: String?, listener: CreateSpaceListener): CreateSpaceDialogFragment {
            val args = Bundle().apply {
                putString(ARG_ACCOUNT_NAME, accountName)
            }
            return CreateSpaceDialogFragment().apply {
                createSpaceListener = listener
                arguments = args
            }
        }
    }
}
