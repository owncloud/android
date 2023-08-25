package com.owncloud.android.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.owncloud.android.databinding.DialogFileAlreadyExistsBinding

class FileAlreadyExistsDialog private constructor(
    private val titleText: String?,
    private val descriptionText: String?,
    private val checkboxText: String?,
    private var dialogClickListener: DialogButtonClickListener? = null,
) : DialogFragment() {

    private lateinit var binding: DialogFileAlreadyExistsBinding
    internal var isCheckBoxChecked: Boolean = false
    internal var checkboxVisible: Boolean = false

    interface DialogButtonClickListener {
        fun onKeepBothButtonClick()
        fun onSkipButtonClick()
        fun onReplaceButtonClick()
    }

    data class Builder(
        private var titleText: String? = null,
        private var descriptionText: String? = null,
        private var checkboxText: String? = null,
        private var checkboxVisible: Boolean? = false,
    ) {

        fun setTitle(titleText: String) = apply { this.titleText = titleText }
        fun setDescription(descriptionText: String) = apply { this.descriptionText = descriptionText }
        fun setCheckboxText(checkboxText: String) = apply { this.checkboxText = checkboxText }
        fun build() = FileAlreadyExistsDialog(
            titleText = titleText,
            descriptionText = descriptionText,
            checkboxText = checkboxText,
        )
    }

    fun setDialogButtonClickListener(listener: DialogButtonClickListener) = apply { dialogClickListener = listener }

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

        binding.dialogFileAlreadyExistsTitle.text = titleText
        binding.dialogFileAlreadyExistsInformation.text = descriptionText
        binding.dialogFileAlreadyExistsCheckbox.text = checkboxText
        binding.dialogFileAlreadyExistsKeepBoth.setOnClickListener { dialogClickListener?.onKeepBothButtonClick() }
        binding.dialogFileAlreadyExistsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            isCheckBoxChecked = isChecked
        }
        binding.dialogFileAlreadyExistsReplace.setOnClickListener { dialogClickListener?.onReplaceButtonClick() }
        binding.dialogFileAlreadyExistsSkip.setOnClickListener { dialogClickListener?.onSkipButtonClick() }

        if (!checkboxVisible) {
            binding.dialogFileAlreadyExistsCheckbox.visibility = View.GONE
        } else {
            binding.dialogFileAlreadyExistsCheckbox.visibility = View.VISIBLE
        }
    }

}