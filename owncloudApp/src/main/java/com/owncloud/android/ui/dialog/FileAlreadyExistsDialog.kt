package com.owncloud.android.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.owncloud.android.databinding.DialogFileAlreadyExistsBinding

class FileAlreadyExistsDialog : DialogFragment() {

    private lateinit var binding: DialogFileAlreadyExistsBinding
    internal var isCheckBoxChecked: Boolean = false

    interface DialogButtonClickListener {
        fun onKeepBothButtonClick()
        fun onSkipButtonClick()
        fun onReplaceButtonClick()
    }

    companion object {
        var mListener: DialogButtonClickListener? = null

        const val TITLE_TEXT = "titleText"
        const val DESCRIPTION_TEXT = "descriptionText"
        const val CHECKBOX_TEXT = "checkboxText"
        private const val CHECKBOX_VISIBLE = "checkboxVisible"

        fun newInstance(
            titleText: String?,
            descriptionText: String?,
            checkboxText: String?,
            checkboxVisible: Boolean,
            dialogClickListener: DialogButtonClickListener? = null,
            ): FileAlreadyExistsDialog {
            val fragment = FileAlreadyExistsDialog()
            val args = Bundle()
            args.putString(TITLE_TEXT, titleText)
            args.putString(DESCRIPTION_TEXT, descriptionText)
            args.putString(CHECKBOX_TEXT, checkboxText)
            args.putBoolean(CHECKBOX_VISIBLE, checkboxVisible)

            mListener = dialogClickListener
            fragment.arguments = args
            return fragment
        }
    }

    fun setDialogButtonClickListener(listener: DialogButtonClickListener) = apply { mListener = listener }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = DialogFileAlreadyExistsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val titleText = arguments?.getString(TITLE_TEXT)
        val descriptionText = arguments?.getString(DESCRIPTION_TEXT)
        val checkboxText = arguments?.getString(CHECKBOX_TEXT)
        val checkboxVisible = arguments?.getBoolean(CHECKBOX_VISIBLE)

        binding.dialogFileAlreadyExistsTitle.text = titleText
        binding.dialogFileAlreadyExistsInformation.text = descriptionText
        binding.dialogFileAlreadyExistsCheckbox.text = checkboxText

        binding.dialogFileAlreadyExistsKeepBoth.setOnClickListener { mListener?.onKeepBothButtonClick() }
        binding.dialogFileAlreadyExistsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            isCheckBoxChecked = isChecked
        }
        binding.dialogFileAlreadyExistsReplace.setOnClickListener { mListener?.onReplaceButtonClick() }
        binding.dialogFileAlreadyExistsSkip.setOnClickListener { mListener?.onSkipButtonClick() }

        binding.dialogFileAlreadyExistsCheckbox.visibility = if (checkboxVisible == true) { View.VISIBLE } else { View.GONE }
    }

}