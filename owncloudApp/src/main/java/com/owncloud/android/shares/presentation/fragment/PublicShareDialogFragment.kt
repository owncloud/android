/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * Copyright (C) 2019 ownCloud GmbH.
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
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package com.owncloud.android.shares.presentation.fragment

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.CompoundButton
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import com.owncloud.android.R
import com.owncloud.android.capabilities.db.OCCapability
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.shares.domain.OCShare
import com.owncloud.android.ui.dialog.ExpirationDatePickerDialogFragment
import com.owncloud.android.utils.DateUtils
import kotlinx.android.synthetic.main.share_public_dialog.*
import kotlinx.android.synthetic.main.share_public_dialog.view.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date

class PublicShareDialogFragment : DialogFragment() {

    /**
     * File to share, received as a parameter in construction time
     */
    private var file: OCFile? = null

    /**
     * Existing share to update. If NULL, the dialog will create a new share for file.
     */
    private var publicShare: OCShare? = null

    /**
     * Reference to parent listener
     */
    private var listener: ShareFragmentListener? = null

    /**
     * Capabilities of the server
     */
    private var capabilities: OCCapability? = null

    /**
     * Listener for changes in password switch
     */
    private var onPasswordInteractionListener: OnPasswordInteractionListener? = null

    /**
     * Listener for changes in expiration date switch
     */
    private var onExpirationDateInteractionListener: OnExpirationDateInteractionListener? = null

    private val isSharedFolder: Boolean
        get() = file?.isFolder == true || publicShare?.isFolder == true

    private val isPasswordVisible: Boolean
        get() = view != null && shareViaLinkPasswordValue.inputType and
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

    private// Parse expiration date and convert it to milliseconds
    // remember: format is defined by date picker
    val expirationDateValueInMillis: Long
        get() {
            var publicLinkExpirationDateInMillis: Long = -1
            val expirationDate = shareViaLinkExpirationValue?.text.toString()
            if (expirationDate.isNotEmpty()) {
                try {
                    publicLinkExpirationDateInMillis =
                        ExpirationDatePickerDialogFragment.getDateFormat().parse(expirationDate).time
                } catch (e: ParseException) {
                    Log_OC.e(TAG, "Error reading expiration date from input field", e)
                }

            }
            return publicLinkExpirationDateInMillis
        }

    /**
     * Get expiration date imposed by the server, if any
     */
    private val imposedExpirationDate: Long
        get() = if (capabilities?.filesSharingPublicExpireDateEnforced == CapabilityBooleanType.TRUE.value) {
            DateUtils.addDaysToDate(
                Date(),
                capabilities?.filesSharingPublicExpireDateDays!!
            )
                .time
        } else -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            file = arguments!!.getParcelable(ARG_FILE)
            publicShare = arguments!!.getParcelable(ARG_SHARE)
        }

        if (file == null && publicShare == null) {
            throw IllegalStateException("Both ARG_FILE and ARG_SHARE cannot be NULL")
        }

        setStyle(STYLE_NO_TITLE, 0)
    }

    private fun updating(): Boolean = publicShare != null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.share_public_dialog, container, false)

        Log_OC.d(TAG, "onCreateView")

        // Get and set the values saved previous to the screen rotation, if any
        if (savedInstanceState != null) {
            val expirationDate = savedInstanceState.getString(KEY_EXPIRATION_DATE)
            if (!expirationDate.isNullOrEmpty()) {
                view.shareViaLinkExpirationValue?.visibility = View.VISIBLE
                view.shareViaLinkExpirationValue?.text = expirationDate
            }
        }

        if (updating()) {
            view.publicShareDialogTitle?.setText(R.string.share_via_link_edit_title)
            view.shareViaLinkNameValue?.setText(publicShare?.name)

            when (publicShare?.permissions) {
                RemoteShare.CREATE_PERMISSION_FLAG
                        or RemoteShare.DELETE_PERMISSION_FLAG
                        or RemoteShare.UPDATE_PERMISSION_FLAG
                        or RemoteShare.READ_PERMISSION_FLAG ->
                    view.shareViaLinkEditPermissionReadAndWrite.isChecked = true
                RemoteShare.CREATE_PERMISSION_FLAG -> view.shareViaLinkEditPermissionUploadFiles.isChecked = true
                else -> view.shareViaLinkEditPermissionReadOnly.isChecked = true
            }

            if (publicShare?.isPasswordProtected!!) {
                setPasswordSwitchChecked(view, true)
                view.shareViaLinkPasswordValue?.visibility = View.VISIBLE
                view.shareViaLinkPasswordValue?.hint = getString(R.string.share_via_link_default_password)
            }

            if (publicShare?.expirationDate != 0L) {
                setExpirationDateSwitchChecked(view, true)
                val formattedDate = ExpirationDatePickerDialogFragment.getDateFormat().format(
                    Date(publicShare?.expirationDate!!)
                )
                view.shareViaLinkExpirationValue?.visibility = View.VISIBLE
                view.shareViaLinkExpirationValue?.text = formattedDate
            }

        } else {
            view.shareViaLinkNameValue?.setText(arguments!!.getString(ARG_DEFAULT_LINK_NAME, ""))
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initPasswordListener()
        initExpirationListener()
        initPasswordFocusChangeListener()
        initPasswordToggleListener()

        saveButton.setOnClickListener { onSaveShareSetting() }
        cancelButton.setOnClickListener { dismiss() }
    }

    private fun onSaveShareSetting() {
        // Get data filled by user
        val publicLinkName = shareViaLinkNameValue?.text.toString()
        var publicLinkPassword: String? = shareViaLinkPasswordValue?.text.toString()
        val publicLinkExpirationDateInMillis = expirationDateValueInMillis

        val publicLinkPermissions: Int
        var publicUploadPermission: Boolean

        when (shareViaLinkEditPermissionGroup?.checkedRadioButtonId) {
            R.id.shareViaLinkEditPermissionUploadFiles -> {
                publicLinkPermissions = RemoteShare.CREATE_PERMISSION_FLAG
                publicUploadPermission = true
            }
            R.id.shareViaLinkEditPermissionReadAndWrite -> {
                publicLinkPermissions = (RemoteShare.CREATE_PERMISSION_FLAG
                        or RemoteShare.DELETE_PERMISSION_FLAG
                        or RemoteShare.UPDATE_PERMISSION_FLAG
                        or RemoteShare.READ_PERMISSION_FLAG)
                publicUploadPermission = true
            }
            R.id.shareViaLinkEditPermissionReadOnly -> {
                publicLinkPermissions = RemoteShare.READ_PERMISSION_FLAG
                publicUploadPermission = false
            }
            else -> {
                publicLinkPermissions = RemoteShare.READ_PERMISSION_FLAG
                publicUploadPermission = false
            }
        }

        // since the public link permission foo got a bit despagetified in the server somewhere
        // at 10.0.4 we don't need publicUploadPermission there anymore. By setting it to false
        // it will not be sent to the server.
        if (capabilities != null) {
            val serverVersion = OwnCloudVersion(capabilities?.versionString!!)
            publicUploadPermission = serverVersion.isPublicUploadPermissionNeeded && publicUploadPermission
        }

        if (!updating()) { // Creating a new public share
            listener?.createPublicShare(
                publicLinkPermissions,
                publicLinkName,
                publicLinkPassword!!,
                publicLinkExpirationDateInMillis,
                false
            )
        } else { // Updating an existing public share
            if (!shareViaLinkPasswordSwitch.isChecked) {
                publicLinkPassword = ""
            } else if (shareViaLinkPasswordValue.text.isEmpty()) {
                // User has not added a new password, so do not update it
                publicLinkPassword = null
            }

            listener?.updatePublicShare(
                publicShare?.remoteId!!,
                publicLinkName,
                publicLinkPassword,
                publicLinkExpirationDateInMillis,
                publicLinkPermissions,
                publicUploadPermission
            )
        }
    }

    private fun initPasswordFocusChangeListener() {
        shareViaLinkPasswordValue?.setOnFocusChangeListener { v: View, hasFocus: Boolean ->
            if (v.id == R.id.shareViaLinkPasswordValue) {
                onPasswordFocusChanged(hasFocus)
            }
        }
    }

    private fun initPasswordToggleListener() {
        shareViaLinkPasswordValue?.setOnTouchListener(object : RightDrawableOnTouchListener() {
            override fun onDrawableTouch(event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_UP) {
                    onViewPasswordClick()
                }
                return true
            }
        })
    }

    private abstract class RightDrawableOnTouchListener : View.OnTouchListener {

        private val fuzz = 75

        /**
         * {@inheritDoc}
         */
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            var rightDrawable: Drawable? = null
            if (view is TextView) {
                val drawables = view.compoundDrawables
                if (drawables.size > 2) {
                    rightDrawable = drawables[2]
                }
            }
            if (rightDrawable != null) {
                val x = event.x.toInt()
                val y = event.y.toInt()
                val bounds = rightDrawable.bounds
                if (x >= view.right - bounds.width() - fuzz &&
                    x <= view.right - view.paddingRight + fuzz &&
                    y >= view.paddingTop - fuzz &&
                    y <= view.height - view.paddingBottom + fuzz
                ) {

                    return onDrawableTouch(event)
                }
            }
            return false
        }

        abstract fun onDrawableTouch(event: MotionEvent): Boolean
    }

    /**
     * Handles changes in focus on the text input for the password (basic authorization).
     * When (hasFocus), the button to toggle password visibility is shown.
     * When (!hasFocus), the button is made invisible and the password is hidden.
     *
     * @param hasFocus          'True' if focus is received, 'false' if is lost
     */
    private fun onPasswordFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            showViewPasswordButton()
        } else {
            hidePassword()
            hidePasswordButton()
        }
    }

    /**
     * Called when the eye icon in the password field is clicked.
     *
     * Toggles the visibility of the password in the field.
     */
    fun onViewPasswordClick() {
        if (view != null) {
            if (isPasswordVisible) {
                hidePassword()
            } else {
                showPassword()
            }
            shareViaLinkPasswordValue?.setSelection(
                shareViaLinkPasswordValue.selectionStart,
                shareViaLinkPasswordValue.selectionEnd
            )
        }
    }

    private fun showViewPasswordButton() {
        val drawable = if (isPasswordVisible)
            R.drawable.ic_view_black
        else
            R.drawable.ic_hide_black
        if (view != null) {
            shareViaLinkPasswordValue?.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawable, 0)
        }
    }

    private fun hidePasswordButton() {
        if (view != null) {
            shareViaLinkPasswordValue?.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
    }

    private fun showPassword() {
        if (view != null) {
            shareViaLinkPasswordValue?.inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            showViewPasswordButton()
        }
    }

    private fun hidePassword() {
        if (view != null) {
            shareViaLinkPasswordValue?.inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD or
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            showViewPasswordButton()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log_OC.d(TAG, "onActivityCreated")

        listener?.refreshCapabilities(false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_EXPIRATION_DATE, shareViaLinkExpirationValue?.text.toString())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = activity as ShareFragmentListener?
        } catch (e: IllegalStateException) {
            throw IllegalStateException(activity!!.toString() + " must implement OnShareFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * Binds listener for user actions that start any update on a password for the public link
     * to the views receiving the user events.
     *
     */
    private fun initPasswordListener() {
        onPasswordInteractionListener = OnPasswordInteractionListener()
        shareViaLinkPasswordSwitch?.setOnCheckedChangeListener(onPasswordInteractionListener)
    }

    /**
     * Listener for user actions that start any update on a password for the public link.
     */
    private inner class OnPasswordInteractionListener : CompoundButton.OnCheckedChangeListener {
        /**
         * Called by R.id.shareViaLinkPasswordSwitch to set or clear the password.
         *
         * @param switchView [SwitchCompat] toggled by the user, R.id.shareViaLinkPasswordSwitch
         * @param isChecked  New switch state.
         */
        override fun onCheckedChanged(switchView: CompoundButton, isChecked: Boolean) {
            if (isChecked) {
                shareViaLinkPasswordValue?.visibility = View.VISIBLE
                shareViaLinkPasswordValue?.requestFocus()

                // Show keyboard to fill in the password
                val mgr = activity!!.getSystemService(
                    Context.INPUT_METHOD_SERVICE
                ) as InputMethodManager
                mgr.showSoftInput(shareViaLinkPasswordValue, InputMethodManager.SHOW_IMPLICIT)

            } else {
                shareViaLinkPasswordValue?.visibility = View.GONE
                shareViaLinkPasswordValue?.text?.clear()
            }
        }
    }

    /**
     * Binds listener for user actions that start any update on a expiration date
     * for the public link to the views receiving the user events.
     *
     */
    private fun initExpirationListener() {
        onExpirationDateInteractionListener = OnExpirationDateInteractionListener()
        shareViaLinkExpirationSwitch?.setOnCheckedChangeListener(onExpirationDateInteractionListener)
        shareViaLinkExpirationLabel?.setOnClickListener(onExpirationDateInteractionListener)
        shareViaLinkExpirationValue?.setOnClickListener(onExpirationDateInteractionListener)
    }

    /**
     * Listener for user actions that start any update on the expiration date for the public link.
     */
    private inner class OnExpirationDateInteractionListener : CompoundButton.OnCheckedChangeListener,
        View.OnClickListener, ExpirationDatePickerDialogFragment.DatePickerFragmentListener {

        /**
         * Called by R.id.shareViaLinkExpirationSwitch to set or clear the expiration date.
         *
         * @param switchView [SwitchCompat] toggled by the user, R.id.shareViaLinkExpirationSwitch
         * @param isChecked  New switch state.
         */
        override fun onCheckedChanged(switchView: CompoundButton, isChecked: Boolean) {
            if (!isResumed) {
                // very important, setCheched(...) is called automatically during
                // Fragment recreation on device rotations
                return
            }

            if (isChecked) {
                // Show calendar to set the expiration date
                val dialog = ExpirationDatePickerDialogFragment.newInstance(
                    expirationDateValueInMillis,
                    imposedExpirationDate
                )
                dialog.setDatePickerListener(this)
                dialog.show(
                    activity!!.supportFragmentManager,
                    ExpirationDatePickerDialogFragment.DATE_PICKER_DIALOG
                )
            } else {
                shareViaLinkExpirationValue?.visibility = View.INVISIBLE
                shareViaLinkExpirationValue?.text = ""
            }
        }

        /**
         * Called by R.id.shareViaLinkExpirationLabel or R.id.shareViaLinkExpirationValue
         * to change the current expiration date.
         *
         * @param expirationView Label or value view touched by the user.
         */
        override fun onClick(expirationView: View) {

            // Show calendar to set the expiration date
            val dialog = ExpirationDatePickerDialogFragment.newInstance(
                expirationDateValueInMillis,
                imposedExpirationDate
            )
            dialog.setDatePickerListener(this)
            dialog.show(
                activity!!.supportFragmentManager,
                ExpirationDatePickerDialogFragment.DATE_PICKER_DIALOG
            )
        }

        /**
         * Update the selected date for the public link
         *
         * @param date date selected by the user
         */
        override fun onDateSet(date: String) {
            shareViaLinkExpirationValue?.visibility = View.VISIBLE
            shareViaLinkExpirationValue?.text = date
        }

        override fun onCancelDatePicker() {

            val expirationToggle = view!!.findViewById<SwitchCompat>(R.id.shareViaLinkExpirationSwitch)

            // If the date has not been set yet, uncheck the toggle
            if (expirationToggle.isChecked && shareViaLinkExpirationValue?.text === "") {
                expirationToggle.isChecked = false
            }
        }
    }

    fun updateCapabilities(capabilities: OCCapability?) {
        this.capabilities = capabilities
        updateInputFormAccordingToServerCapabilities()
    }

    /**
     * Updates the UI according to enforcements and allowances set by the server administrator.
     *
     * Includes:
     * - hide the link name section if multiple public share is not supported, showing the keyboard
     * to fill in the public share name otherwise
     * - hide show file listing option
     * - hide or show the switch to disable the password if it is enforced or not;
     * - hide or show the switch to disable the expiration date it it is enforced or not;
     * - show or hide the switch to allow public uploads if it is allowed or not;
     * - set the default value for expiration date if defined (only if creating a new share).
     */
    private fun updateInputFormAccordingToServerCapabilities() {
        val serverVersion = OwnCloudVersion(capabilities?.versionString!!)

        // Server version <= 9.x, multiple public sharing not supported
        if (!serverVersion.isMultiplePublicSharingSupported) {
            publicShareDialogTitle?.visibility = View.GONE
        } else { // Show keyboard to fill the public share name
            dialog?.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            )
        }

        if (capabilities?.filesSharingPublicUpload == CapabilityBooleanType.TRUE.value && isSharedFolder) {
            shareViaLinkEditPermissionGroup?.visibility = View.VISIBLE
        }

        // Show file listing option if all the following is true:
        //  - The file to share is a folder
        //  - Upload only is supported by the server version
        //  - Upload only capability is set
        //  - Allow editing capability is set
        if (!(isSharedFolder &&
                    serverVersion.isPublicSharingWriteOnlySupported &&
                    capabilities?.filesSharingPublicSupportsUploadOnly == CapabilityBooleanType.TRUE.value &&
                    capabilities?.filesSharingPublicUpload == CapabilityBooleanType.TRUE.value)
        ) {
            shareViaLinkEditPermissionGroup?.visibility = View.GONE
        }

        // Show default date enforced by the server, if any
        if (!updating() && capabilities?.filesSharingPublicExpireDateDays!! > 0) {
            setExpirationDateSwitchChecked(checked = true)

            val formattedDate = SimpleDateFormat.getDateInstance().format(
                DateUtils.addDaysToDate(
                    Date(),
                    capabilities?.filesSharingPublicExpireDateDays!!
                )
            )

            shareViaLinkExpirationValue?.visibility = View.VISIBLE
            shareViaLinkExpirationValue?.text = formattedDate
        }

        // Hide expiration date switch if date is enforced to prevent it is removed
        if (capabilities?.filesSharingPublicExpireDateEnforced == CapabilityBooleanType.TRUE.value) {
            shareViaLinkExpirationLabel?.text = getString(R.string.share_via_link_expiration_date_enforced_label)
            shareViaLinkExpirationSwitch?.visibility = View.GONE
            shareViaLinkExpirationExplanationLabel?.visibility = View.VISIBLE
            shareViaLinkExpirationExplanationLabel?.text = getString(
                R.string.share_via_link_expiration_date_explanation_label,
                capabilities?.filesSharingPublicExpireDateDays
            )
        }

        // Set password label when opening the dialog
        if (shareViaLinkEditPermissionReadOnly.isChecked &&
            capabilities?.filesSharingPublicPasswordEnforcedReadOnly == CapabilityBooleanType.TRUE.value ||
            shareViaLinkEditPermissionReadAndWrite.isChecked &&
            capabilities?.filesSharingPublicPasswordEnforcedReadWrite == CapabilityBooleanType.TRUE.value ||
            shareViaLinkEditPermissionUploadFiles.isChecked &&
            capabilities?.filesSharingPublicPasswordEnforcedUploadOnly == CapabilityBooleanType.TRUE.value
        ) {
            setPasswordEnforced()
        }

        // Set password label depending on the checked permission option
        shareViaLinkEditPermissionGroup?.setOnCheckedChangeListener { group, checkedId ->
            public_link_error_message?.isGone = true

            if (checkedId == shareViaLinkEditPermissionReadOnly.id) {
                if (capabilities?.filesSharingPublicPasswordEnforcedReadOnly == CapabilityBooleanType.TRUE.value) {
                    setPasswordEnforced()
                } else {
                    setPasswordNotEnforced()
                }
            } else if (checkedId == shareViaLinkEditPermissionReadAndWrite.id) {
                if (capabilities?.filesSharingPublicPasswordEnforcedReadWrite == CapabilityBooleanType.TRUE.value) {
                    setPasswordEnforced()
                } else {
                    setPasswordNotEnforced()
                }
            } else if (checkedId == shareViaLinkEditPermissionUploadFiles.id) {
                if (capabilities?.filesSharingPublicPasswordEnforcedUploadOnly == CapabilityBooleanType.TRUE.value) {
                    setPasswordEnforced()
                } else {
                    setPasswordNotEnforced()
                }
            }
        }

        // When there's no password enforced for capability
        val hasPasswordEnforcedFor = capabilities?.filesSharingPublicPasswordEnforcedReadOnly ==
                CapabilityBooleanType.TRUE.value ||
                capabilities?.filesSharingPublicPasswordEnforcedReadWrite == CapabilityBooleanType.TRUE.value ||
                capabilities?.filesSharingPublicPasswordEnforcedUploadOnly == CapabilityBooleanType.TRUE.value

        // hide password switch if password is enforced to prevent it is removed
        if (!hasPasswordEnforcedFor && capabilities?.filesSharingPublicPasswordEnforced ==
            CapabilityBooleanType.TRUE.value
        ) {
            setPasswordEnforced()
        }
    }

    private fun setPasswordNotEnforced() {
        shareViaLinkPasswordLabel?.text = getString(R.string.share_via_link_password_label)
        shareViaLinkPasswordSwitch?.visibility = View.VISIBLE
        if (!shareViaLinkPasswordSwitch.isChecked) {
            shareViaLinkPasswordValue?.visibility = View.GONE
        }
    }

    private fun setPasswordEnforced() {
        shareViaLinkPasswordLabel?.text = getString(R.string.share_via_link_password_enforced_label)
        shareViaLinkPasswordSwitch?.isChecked = true
        shareViaLinkPasswordSwitch?.visibility = View.GONE
        shareViaLinkPasswordValue?.visibility = View.VISIBLE
    }

    /**
     * Show error when creating or updating the public share, if any
     * @param errorMessage
     */
    fun showError(errorMessage: String) {
        public_link_error_message?.visibility = View.VISIBLE
        public_link_error_message?.text = errorMessage
    }

    private fun setPasswordSwitchChecked(view: View, checked: Boolean) {
        view.shareViaLinkPasswordSwitch?.setOnCheckedChangeListener(null)
        view.shareViaLinkPasswordSwitch?.isChecked = checked
        view.shareViaLinkPasswordSwitch?.setOnCheckedChangeListener(onPasswordInteractionListener)
    }

    private fun setExpirationDateSwitchChecked(view: View? = null, checked: Boolean) {
        if (view != null) {
            view.shareViaLinkExpirationSwitch?.setOnCheckedChangeListener(null)
            view.shareViaLinkExpirationSwitch?.isChecked = checked
            view.shareViaLinkExpirationSwitch?.setOnCheckedChangeListener(onExpirationDateInteractionListener)
        } else {
            shareViaLinkExpirationSwitch?.setOnCheckedChangeListener(null)
            shareViaLinkExpirationSwitch?.isChecked = checked
            shareViaLinkExpirationSwitch?.setOnCheckedChangeListener(onExpirationDateInteractionListener)
        }
    }

    companion object {
        private val TAG = PublicShareDialogFragment::class.java.simpleName

        /**
         * The fragment initialization parameters
         */
        private const val ARG_FILE = "FILE"
        private const val ARG_SHARE = "SHARE"
        private const val ARG_DEFAULT_LINK_NAME = "DEFAULT_LINK_NAME"
        private const val KEY_EXPIRATION_DATE = "EXPIRATION_DATE"

        /**
         * Create a new instance of PublicShareDialogFragment, providing fileToShare as an argument.
         *
         * Dialog shown this way is intended to CREATE a new public share.
         *
         * @param   fileToShare     File to share with a new public share.
         */
        fun newInstanceToCreate(
            fileToShare: OCFile,
            defaultLinkName: String
        ): PublicShareDialogFragment {
            val publicShareDialogFragment = PublicShareDialogFragment()
            val args = Bundle()
            args.putParcelable(ARG_FILE, fileToShare)
            args.putString(ARG_DEFAULT_LINK_NAME, defaultLinkName)
            publicShareDialogFragment.arguments = args
            return publicShareDialogFragment
        }

        /**
         * Update an instance of PublicShareDialogFragment, providing fileToShare, publicShare as arguments.
         *
         * Dialog shown this way is intended to UPDATE an existing public share.
         *
         * @param   publicShare           Public share to update.
         */
        fun newInstanceToUpdate(
            fileToShare: OCFile,
            publicShare: OCShare
        ): PublicShareDialogFragment {
            val publicShareDialogFragment = PublicShareDialogFragment()
            val args = Bundle()
            args.putParcelable(ARG_FILE, fileToShare)
            args.putParcelable(ARG_SHARE, publicShare)
            publicShareDialogFragment.arguments = args
            return publicShareDialogFragment
        }
    }
}
