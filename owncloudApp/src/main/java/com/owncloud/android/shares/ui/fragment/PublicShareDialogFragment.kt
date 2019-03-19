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

package com.owncloud.android.shares.ui.fragment

import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView

import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.dialog.ExpirationDatePickerDialogFragment
import com.owncloud.android.utils.DateUtils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date

import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.owncloud.android.ViewModelFactory
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.operations.common.OperationType
import com.owncloud.android.shares.viewmodel.OCShareViewModel
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.errorhandling.ErrorMessageAdapter
import com.owncloud.android.ui.fragment.ShareFragmentListener
import com.owncloud.android.vo.Status

class PublicShareDialogFragment : DialogFragment() {

    /**
     * File to share, received as a parameter in construction time
     */
    private var file: OCFile? = null

    /**
     * Existing share to update. If NULL, the dialog will create a new share for file.
     */
    private var publicShare: RemoteShare? = null

    /*
     * OC account holding the file to share, received as a parameter in construction time
     */
    private var account: Account? = null

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

    /**
     * UI elements
     */

    private var nameSelectionLayout: LinearLayout? = null
    private var nameValueEdit: EditText? = null
    private var passwordLabel: TextView? = null
    private var passwordSwitch: SwitchCompat? = null
    private var passwordValueEdit: EditText? = null
    private var expirationDateLabel: TextView? = null
    private var expirationDateSwitch: SwitchCompat? = null
    private var expirationDateExplanationLabel: TextView? = null
    private var expirationDateValueLabel: TextView? = null
    private var errorMessageLabel: TextView? = null
    private var permissionRadioGroup: RadioGroup? = null
    private var readOnlyButton: RadioButton? = null
    private var readWriteButton: RadioButton? = null
    private var uploadOnlyButton: RadioButton? = null

    private val isSharedFolder: Boolean
        get() = file?.isFolder == true || publicShare?.isFolder == true

    private val isPasswordVisible: Boolean
        get() = view != null && passwordValueEdit!!.inputType and
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

    private// Parse expiration date and convert it to milliseconds
    // remember: format is defined by date picker
    val expirationDateValueInMillis: Long
        get() {
            var publicLinkExpirationDateInMillis: Long = -1
            val expirationDate = expirationDateValueLabel!!.text.toString()
            if (expirationDate.length > 0) {
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
        get() = if (capabilities != null && capabilities!!.filesSharingPublicExpireDateEnforced.isTrue) {

            DateUtils.addDaysToDate(
                Date(),
                capabilities!!.filesSharingPublicExpireDateDays
            )
                .time
        } else -1

    var mViewModelFactory: ViewModelProvider.Factory = ViewModelFactory.build {
        OCShareViewModel(
            account!!,
            file?.remotePath!!,
            listOf(ShareType.PUBLIC_LINK)
        )
    }

    private lateinit var ocShareViewModel: OCShareViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            file = arguments!!.getParcelable(ARG_FILE)
            account = arguments!!.getParcelable(ARG_ACCOUNT)
            publicShare = arguments!!.getParcelable(ARG_SHARE)
        }

        if (file == null && publicShare == null) {
            throw IllegalStateException("Both ARG_FILE and ARG_SHARE cannot be NULL")
        }

        setStyle(DialogFragment.STYLE_NO_TITLE, 0)
    }

    private fun updating(): Boolean {
        return publicShare != null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.share_public_dialog, container, false)

        Log_OC.d(TAG, "onCreateView")

        val dialogTitleLabel = view.findViewById<TextView>(R.id.publicShareDialogTitle)
        nameSelectionLayout = view.findViewById(R.id.shareViaLinkNameSection)
        nameValueEdit = view.findViewById(R.id.shareViaLinkNameValue)
        passwordLabel = view.findViewById(R.id.shareViaLinkPasswordLabel)
        passwordSwitch = view.findViewById(R.id.shareViaLinkPasswordSwitch)
        passwordValueEdit = view.findViewById(R.id.shareViaLinkPasswordValue)
        expirationDateLabel = view.findViewById(R.id.shareViaLinkExpirationLabel)
        expirationDateSwitch = view.findViewById(R.id.shareViaLinkExpirationSwitch)
        expirationDateExplanationLabel = view.findViewById(R.id.shareViaLinkExpirationExplanationLabel)
        errorMessageLabel = view.findViewById(R.id.public_link_error_message)
        expirationDateValueLabel = view.findViewById(R.id.shareViaLinkExpirationValue)
        permissionRadioGroup = view.findViewById(R.id.shareViaLinkEditPermissionGroup)
        readOnlyButton = view.findViewById(R.id.shareViaLinkEditPermissionReadOnly)
        readWriteButton = view.findViewById(R.id.shareViaLinkEditPermissionReadAndWrite)
        uploadOnlyButton = view.findViewById(R.id.shareViaLinkEditPermissionUploadFiles)

        // Get and set the values saved previous to the screen rotation, if any
        if (savedInstanceState != null) {
            val expirationDate = savedInstanceState.getString(KEY_EXPIRATION_DATE)
            if (expirationDate!!.length > 0) {
                expirationDateValueLabel!!.visibility = View.VISIBLE
                expirationDateValueLabel!!.text = expirationDate
            }
        }

        if (updating()) {
            dialogTitleLabel.setText(R.string.share_via_link_edit_title)
            nameValueEdit!!.setText(publicShare!!.name)

            when (publicShare!!.permissions) {
                RemoteShare.CREATE_PERMISSION_FLAG
                        or RemoteShare.DELETE_PERMISSION_FLAG
                        or RemoteShare.UPDATE_PERMISSION_FLAG
                        or RemoteShare.READ_PERMISSION_FLAG -> readWriteButton!!.isChecked = true
                RemoteShare.CREATE_PERMISSION_FLAG -> uploadOnlyButton!!.isChecked = true
                else -> readOnlyButton!!.isChecked = true
            }

            if (publicShare!!.isPasswordProtected) {

                setPasswordSwitchChecked(true)
                passwordValueEdit!!.visibility = View.VISIBLE
                passwordValueEdit!!.setHint(R.string.share_via_link_default_password)
            }

            if (publicShare!!.expirationDate != 0L) {
                setExpirationDateSwitchChecked(true)
                val formattedDate = ExpirationDatePickerDialogFragment.getDateFormat().format(
                    Date(publicShare!!.expirationDate)
                )
                expirationDateValueLabel!!.visibility = View.VISIBLE
                expirationDateValueLabel!!.text = formattedDate
            }

        } else {
            nameValueEdit!!.setText(arguments!!.getString(ARG_DEFAULT_LINK_NAME, ""))
        }

        initPasswordListener()
        initExpirationListener()
        initPasswordFocusChangeListener()
        initPasswordToggleListener()

        view.findViewById<View>(R.id.saveButton)
            .setOnClickListener { v -> onSaveShareSetting() }

        view.findViewById<View>(R.id.cancelButton)
            .setOnClickListener { v -> dismiss() }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ocShareViewModel = ViewModelProviders.of(this, mViewModelFactory).get(OCShareViewModel::class.java)
    }

    private fun onSaveShareSetting() {
        // Get data filled by user
        val publicLinkName = nameValueEdit!!.text.toString()
        var publicLinkPassword: String? = passwordValueEdit!!.text.toString()
        val publicLinkExpirationDateInMillis = expirationDateValueInMillis

        val publicLinkPermissions: Int
        var publicUploadPermission: Boolean

        when (permissionRadioGroup!!.checkedRadioButtonId) {
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
            publicUploadPermission =
                capabilities!!.versionMayor >= 10 &&
                        (capabilities!!.versionMinor > 1 || capabilities!!.versionMicro > 3)
                        && publicUploadPermission
        }

        if (!updating()) { // Creating a new public share
            ocShareViewModel.insertPublicShareForFile(
                file?.remotePath!!,
                publicLinkPermissions,
                publicLinkName,
                publicLinkPassword!!,
                publicLinkExpirationDateInMillis,
                false
            ).observe(
                this,
                Observer { resource ->
                    when (resource?.status) {
                        Status.SUCCESS -> {
                            dismiss()
                        }
                        Status.ERROR -> {
                            val errorMessage: String = resource.msg ?: ErrorMessageAdapter.getResultMessage(
                                resource.code,
                                resource.exception,
                                OperationType.CREATE_PUBLIC_SHARE,
                                resources
                            );
                            showError(errorMessage)
                            (activity as BaseActivity).dismissLoadingDialog()
                        }
                        Status.LOADING -> {
                            (activity as BaseActivity).showLoadingDialog(R.string.common_loading)
                        }
                        else -> {
                            Log.d(TAG, "Unknown status when creating share")
                        }
                    }
                }
            )
//            (activity as FileActivity).fileOperationsHelper.shareFileViaLink(
//                    file,
//                    publicLinkName,
//                    publicLinkPassword,
//                    publicLinkExpirationDateInMillis,
//                    false,
//                    publicLinkPermissions)

        } else { // Updating an existing public share
            if (!passwordSwitch!!.isChecked) {
                publicLinkPassword = ""
            } else if (passwordValueEdit!!.length() == 0) {
                // User has not added a new password, so do not update it
                publicLinkPassword = null
            }

            (activity as FileActivity).fileOperationsHelper.updateShareViaLink(
                publicShare,
                publicLinkName,
                publicLinkPassword,
                publicLinkExpirationDateInMillis,
                publicUploadPermission,
                publicLinkPermissions
            )
        }
    }

    private fun initPasswordFocusChangeListener() {
        passwordValueEdit!!.setOnFocusChangeListener { v: View, hasFocus: Boolean ->
            if (v.id == R.id.shareViaLinkPasswordValue) {
                onPasswordFocusChanged(hasFocus)
            }
        }
    }

    private fun initPasswordToggleListener() {
        passwordValueEdit!!.setOnTouchListener(object : RightDrawableOnTouchListener() {
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
            passwordValueEdit!!.setSelection(
                passwordValueEdit!!.selectionStart,
                passwordValueEdit!!.selectionEnd
            )
        }
    }

    private fun showViewPasswordButton() {
        val drawable = if (isPasswordVisible)
            R.drawable.ic_view_black
        else
            R.drawable.ic_hide_black
        if (view != null) {
            passwordValueEdit!!.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawable, 0)
        }
    }

    private fun hidePasswordButton() {
        if (view != null) {
            passwordValueEdit!!.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
    }

    private fun showPassword() {
        if (view != null) {
            passwordValueEdit!!.inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            showViewPasswordButton()
        }
    }

    private fun hidePassword() {
        if (view != null) {
            passwordValueEdit!!.inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD or
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            showViewPasswordButton()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log_OC.d(TAG, "onActivityCreated")

        // Load known capabilities of the server from DB
        refreshModelFromStorageManager()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_EXPIRATION_DATE, expirationDateValueLabel!!.text.toString())
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
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
        passwordSwitch!!.setOnCheckedChangeListener(onPasswordInteractionListener)
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
                passwordValueEdit!!.visibility = View.VISIBLE
                passwordValueEdit!!.requestFocus()

                // Show keyboard to fill in the password
                val mgr = activity!!.getSystemService(
                    Context.INPUT_METHOD_SERVICE
                ) as InputMethodManager
                mgr.showSoftInput(passwordValueEdit, InputMethodManager.SHOW_IMPLICIT)

            } else {
                passwordValueEdit!!.visibility = View.GONE
                passwordValueEdit!!.text.clear()
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
        expirationDateSwitch!!.setOnCheckedChangeListener(onExpirationDateInteractionListener)
        expirationDateLabel!!.setOnClickListener(onExpirationDateInteractionListener)
        expirationDateValueLabel!!.setOnClickListener(onExpirationDateInteractionListener)
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
                expirationDateValueLabel!!.visibility = View.INVISIBLE
                expirationDateValueLabel!!.text = ""
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
            expirationDateValueLabel!!.visibility = View.VISIBLE
            expirationDateValueLabel!!.text = date
        }

        override fun onCancelDatePicker() {

            val expirationToggle = view!!.findViewById<SwitchCompat>(R.id.shareViaLinkExpirationSwitch)

            // If the date has not been set yet, uncheck the toggle
            if (expirationToggle.isChecked && expirationDateValueLabel!!.text === "") {
                expirationToggle.isChecked = false
            }
        }
    }

    /**
     * Get known server capabilities from DB
     *
     * Depends on the parent Activity provides a [com.owncloud.android.datamodel.FileDataStorageManager]
     * instance ready to use. If not ready, does nothing.
     */
    fun refreshModelFromStorageManager() {
        if ((listener as FileActivity).storageManager != null) {
            capabilities = (listener as FileActivity).storageManager.getCapability(account!!.name)

            updateInputFormAccordingToServerCapabilities()
        }
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
        val serverVersion = OwnCloudVersion(capabilities!!.versionString)

        // Server version <= 9.x, multiple public sharing not supported
        if (!serverVersion.isMultiplePublicSharingSupported) {
            nameSelectionLayout!!.visibility = View.GONE
        } else {
            dialog.window!!.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            )
        }

        if (capabilities!!.filesSharingPublicUpload.isTrue && isSharedFolder) {
            permissionRadioGroup!!.visibility = View.VISIBLE
        }

        // Show file listing option if all the following is true:
        //  - The file to share is a folder
        //  - Upload only is supported by the server version
        //  - Upload only capability is set
        //  - Allow editing capability is set
        if (!(isSharedFolder &&
                    serverVersion.isPublicSharingWriteOnlySupported &&
                    capabilities!!.filesSharingPublicSupportsUploadOnly.isTrue &&
                    capabilities!!.filesSharingPublicUpload.isTrue)
        ) {
            permissionRadioGroup!!.visibility = View.GONE
        }

        // Show default date enforced by the server, if any
        if (!updating() && capabilities!!.filesSharingPublicExpireDateDays > 0) {

            setExpirationDateSwitchChecked(true)

            val formattedDate = SimpleDateFormat.getDateInstance().format(
                DateUtils.addDaysToDate(
                    Date(),
                    capabilities!!.filesSharingPublicExpireDateDays
                )
            )

            expirationDateValueLabel!!.visibility = View.VISIBLE

            expirationDateValueLabel!!.text = formattedDate
        }

        // Hide expiration date switch if date is enforced to prevent it is removed
        if (capabilities!!.filesSharingPublicExpireDateEnforced.isTrue) {
            expirationDateLabel!!.setText(R.string.share_via_link_expiration_date_enforced_label)
            expirationDateSwitch!!.visibility = View.GONE
            expirationDateExplanationLabel!!.visibility = View.VISIBLE
            expirationDateExplanationLabel!!.text = getString(
                R.string.share_via_link_expiration_date_explanation_label,
                capabilities!!.filesSharingPublicExpireDateDays
            )
        }

        // Set password label when opening the dialog
        if (readOnlyButton!!.isChecked && capabilities!!.filesSharingPublicPasswordEnforcedReadOnly.isTrue ||
            readWriteButton!!.isChecked && capabilities!!.filesSharingPublicPasswordEnforcedReadWrite.isTrue ||
            uploadOnlyButton!!.isChecked && capabilities!!.filesSharingPublicPasswordEnforcedUploadOnly.isTrue
        ) {
            setPasswordEnforced()
        }

        // Set password label depending on the checked permission option
        permissionRadioGroup!!.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == readOnlyButton!!.id) {
                if (capabilities!!.filesSharingPublicPasswordEnforcedReadOnly.isTrue) {
                    setPasswordEnforced()
                } else {
                    setPasswordNotEnforced()
                }
            } else if (checkedId == readWriteButton!!.id) {
                if (capabilities!!.filesSharingPublicPasswordEnforcedReadWrite.isTrue) {
                    setPasswordEnforced()
                } else {
                    setPasswordNotEnforced()
                }
            } else if (checkedId == uploadOnlyButton!!.id) {
                if (capabilities!!.filesSharingPublicPasswordEnforcedUploadOnly.isTrue) {
                    setPasswordEnforced()
                } else {
                    setPasswordNotEnforced()
                }
            }
        }

        // When there's no password enforced for capability
        val hasPasswordEnforcedFor = capabilities!!.filesSharingPublicPasswordEnforcedReadOnly.isTrue ||
                capabilities!!.filesSharingPublicPasswordEnforcedReadWrite.isTrue ||
                capabilities!!.filesSharingPublicPasswordEnforcedUploadOnly.isTrue

        // hide password switch if password is enforced to prevent it is removed
        if (!hasPasswordEnforcedFor && capabilities!!.filesSharingPublicPasswordEnforced.isTrue) {
            setPasswordEnforced()
        }
    }

    private fun setPasswordNotEnforced() {
        passwordLabel!!.setText(R.string.share_via_link_password_label)
        passwordSwitch!!.visibility = View.VISIBLE
        passwordValueEdit!!.visibility = View.GONE
    }

    private fun setPasswordEnforced() {
        passwordLabel!!.setText(R.string.share_via_link_password_enforced_label)
        passwordSwitch!!.visibility = View.GONE
        passwordValueEdit!!.visibility = View.VISIBLE
    }

    /**
     * Show error when creating or updating the public share, if any
     * @param errorMessage
     */
    fun showError(errorMessage: String) {
        errorMessageLabel!!.visibility = View.VISIBLE
        errorMessageLabel!!.text = errorMessage
    }

    private fun setPasswordSwitchChecked(checked: Boolean) {
        passwordSwitch!!.setOnCheckedChangeListener(null)
        passwordSwitch!!.isChecked = checked
        passwordSwitch!!.setOnCheckedChangeListener(onPasswordInteractionListener)
    }

    private fun setExpirationDateSwitchChecked(checked: Boolean) {
        expirationDateSwitch!!.setOnCheckedChangeListener(null)
        expirationDateSwitch!!.isChecked = checked
        expirationDateSwitch!!.setOnCheckedChangeListener(onExpirationDateInteractionListener)
    }

    companion object {

        private val TAG = PublicShareDialogFragment::class.java.simpleName

        /**
         * The fragment initialization parameters
         */
        private val ARG_FILE = "FILE"
        private val ARG_SHARE = "SHARE"
        private val ARG_ACCOUNT = "ACCOUNT"
        private val ARG_DEFAULT_LINK_NAME = "DEFAULT_LINK_NAME"
        private val KEY_EXPIRATION_DATE = "EXPIRATION_DATE"

        /**
         * Create a new instance of PublicShareDialogFragment, providing fileToShare and account
         * as an argument.
         *
         * Dialog shown this way is intended to CREATE a new public share.
         *
         * @param   fileToShare     File to share with a new public share.
         * @param   account         Account to get capabilities
         */
        fun newInstanceToCreate(
            fileToShare: OCFile,
            account: Account,
            defaultLinkName: String
        ): PublicShareDialogFragment {
            val publicShareDialogFragment = PublicShareDialogFragment()
            val args = Bundle()
            args.putParcelable(ARG_FILE, fileToShare)
            args.putParcelable(ARG_ACCOUNT, account)
            args.putString(ARG_DEFAULT_LINK_NAME, defaultLinkName)

            publicShareDialogFragment.arguments = args
            return publicShareDialogFragment
        }

        /**
         * Update an instance of PublicShareDialogFragment, providing fileToShare, publicShare and
         * account as arguments.
         *
         * Dialog shown this way is intended to UPDATE an existing public share.
         *
         * @param   publicShare           Public share to update.
         */
        fun newInstanceToUpdate(
            fileToShare: OCFile,
            publicShare: OCShare,
            account: Account
        ): PublicShareDialogFragment {
            val publicShareDialogFragment = PublicShareDialogFragment()
            val args = Bundle()
            args.putParcelable(ARG_FILE, fileToShare)
            args.putParcelable(ARG_ACCOUNT, account)
            publicShareDialogFragment.arguments = args
            return publicShareDialogFragment
        }
    }

}