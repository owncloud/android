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

package com.owncloud.android.presentation.spaces.setspaceicon

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.owncloud.android.databinding.SetSpaceIconDialogBinding
import java.io.File

class SetSpaceIconDialogFragment : DialogFragment() {
    private var _binding: SetSpaceIconDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SetSpaceIconDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            cancelSetSpaceIconButton.setOnClickListener { dialog?.dismiss() }
            emojiPicker.setOnEmojiPickedListener { emojiItem ->
                val emojiFile = convertEmojiToImageFile(emojiItem.emoji)
                dialog?.dismiss()
            }
        }
    }

    private fun convertEmojiToImageFile(emoji: String): File {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 250f
            textAlign = Paint.Align.CENTER
        }
        val bitmap = Bitmap.createBitmap(ICON_WIDTH, ICON_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bounds = Rect()
        paint.getTextBounds(emoji, 0, emoji.length, bounds)
        val baseline = (ICON_HEIGHT / 2f) - bounds.exactCenterY()

        canvas.drawText(emoji, (ICON_WIDTH / 2f), baseline, paint)

        val file = File(requireContext().cacheDir, EMOJI_FILE_NAME)
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        return file
    }

    companion object {
        private const val ICON_HEIGHT = 405
        private const val ICON_WIDTH = 720
        private const val EMOJI_FILE_NAME = "emoji.png"

        fun newInstance(): SetSpaceIconDialogFragment =
            SetSpaceIconDialogFragment().apply {
                arguments = Bundle()
            }
    }
}
