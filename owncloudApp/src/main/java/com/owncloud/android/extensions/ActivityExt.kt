/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Aitor Ballesteros Pavón
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

package com.owncloud.android.extensions

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NO_HISTORY
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Typeface
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.data.providers.implementation.OCSharedPreferencesProvider
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.presentation.common.ShareSheetHelper
import com.owncloud.android.presentation.security.LockEnforcedType
import com.owncloud.android.presentation.security.LockEnforcedType.Companion.parseFromInteger
import com.owncloud.android.presentation.security.LockType
import com.owncloud.android.presentation.security.SecurityEnforced
import com.owncloud.android.presentation.security.biometric.BiometricActivity
import com.owncloud.android.presentation.security.biometric.BiometricStatus
import com.owncloud.android.presentation.security.biometric.EnableBiometrics
import com.owncloud.android.presentation.security.isDeviceSecure
import com.owncloud.android.presentation.security.passcode.PassCodeActivity
import com.owncloud.android.presentation.security.pattern.PatternActivity
import com.owncloud.android.presentation.settings.privacypolicy.PrivacyPolicyActivity
import com.owncloud.android.presentation.settings.security.SettingsSecurityFragment.Companion.EXTRAS_LOCK_ENFORCED
import com.owncloud.android.providers.MdmProvider
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.ui.activity.FileDisplayActivity.Companion.ALL_FILES_SAF_REGEX
import com.owncloud.android.utils.CONFIGURATION_DEVICE_PROTECTION
import com.owncloud.android.utils.MimetypeIconUtil
import com.owncloud.android.utils.UriUtilsKt.getExposedFileUriForOCFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.File

const val FRAGMENT_TAG_CHOOSER_DIALOG = "CHOOSER_DIALOG"

fun Activity.showErrorInSnackbar(genericErrorMessageId: Int, throwable: Throwable?) =
    throwable?.let {
        showMessageInSnackbar(
            message = it.parseError(getString(genericErrorMessageId), resources)
        )
    }

fun Activity.showMessageInSnackbar(
    layoutId: Int = android.R.id.content,
    message: CharSequence,
    duration: Int = Snackbar.LENGTH_LONG
) {
    Snackbar.make(findViewById(layoutId), message, duration).show()
}

fun Activity.showErrorInToast(
    genericErrorMessageId: Int,
    throwable: Throwable?,
    duration: Int = Toast.LENGTH_SHORT
) =
    throwable?.let {
        Toast.makeText(
            this,
            it.parseError(getString(genericErrorMessageId), resources),
            duration
        ).show()
    }

fun Activity.goToUrl(
    url: String,
    flags: Int? = null
) {
    if (url.isNotEmpty()) {
        val uriUrl = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uriUrl)
        if (flags != null) intent.addFlags(flags)

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            showMessageInSnackbar(message = this.getString(R.string.file_list_no_app_for_perform_action))
            Timber.e("No Activity found to handle Intent")
        }
    }
}

fun Activity.openPrivacyPolicy() {
    val urlPrivacyPolicy = getString(R.string.url_privacy_policy)

    val cantBeOpenedWithWebView = urlPrivacyPolicy.endsWith("pdf")
    if (cantBeOpenedWithWebView) {
        goToUrl(urlPrivacyPolicy)
    } else {
        val intent = Intent(this, PrivacyPolicyActivity::class.java)
        startActivity(intent)
    }
}

fun Activity.sendEmail(
    email: String,
    subject: String? = null,
    text: String? = null
) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse(email)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        if (text != null) putExtra(Intent.EXTRA_TEXT, text)
    }

    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        showMessageInSnackbar(message = this.getString(R.string.file_list_no_app_for_perform_action))
        Timber.e("No Activity found to handle Intent")
    }
}

private fun getIntentForSavedMimeType(data: Uri, type: String): Intent {
    val intentForSavedMimeType = Intent(Intent.ACTION_VIEW)
    intentForSavedMimeType.setDataAndType(data, type)
    intentForSavedMimeType.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    return intentForSavedMimeType
}

private fun getIntentForGuessedMimeType(storagePath: String, type: String, data: Uri): Intent? {
    var intentForGuessedMimeType: Intent? = null
    if (storagePath.lastIndexOf('.') >= 0) {
        val guessedMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(storagePath.substring(storagePath.lastIndexOf('.') + 1))
        if (guessedMimeType != null && guessedMimeType != type) {
            intentForGuessedMimeType = Intent(Intent.ACTION_VIEW)
            intentForGuessedMimeType.setDataAndType(data, guessedMimeType)
            intentForGuessedMimeType.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
    }
    return intentForGuessedMimeType
}

fun Activity.openFile(file: File?) {
    if (file != null) {

        val intentForSavedMimeType = getIntentForSavedMimeType(
            getExposedFileUri(this, file.path)!!,
            MimetypeIconUtil.getBestMimeTypeByFilename(file.name)
        )

        val intentForGuessedMimeType = getIntentForGuessedMimeType(
            file.path,
            MimetypeIconUtil.getBestMimeTypeByFilename(file.name), getExposedFileUri(this, file.path)!!
        )

        openFileWithIntent(intentForSavedMimeType, intentForGuessedMimeType)
    } else {
        Timber.e("Trying to open a NULL file")
    }
}

private fun getExposedFileUri(context: Context, localPath: String): Uri? {
    var exposedFileUri: Uri? = null

    if (localPath.isEmpty()) {
        return null
    }

    // Use the FileProvider to get a content URI
    try {
        exposedFileUri = FileProvider.getUriForFile(
            context,
            context.getString(R.string.file_provider_authority),
            File(localPath)
        )
    } catch (e: IllegalArgumentException) {
        Timber.e(e, "File can't be exported")
    }

    return exposedFileUri
}

fun Activity.openFileWithIntent(intentForSavedMimeType: Intent, intentForGuessedMimeType: Intent?) {
    val openFileWithIntent: Intent = intentForGuessedMimeType ?: intentForSavedMimeType
    val launchables: List<ResolveInfo> =
        this.packageManager.queryIntentActivities(openFileWithIntent, PackageManager.MATCH_DEFAULT_ONLY)
    if (launchables.isNotEmpty()) {
        try {
            this.startActivity(
                Intent.createChooser(
                    openFileWithIntent, this.getString(R.string.actionbar_open_with)
                )
            )
        } catch (anfe: ActivityNotFoundException) {
            showMessageInSnackbar(
                message = this.getString(
                    R.string.file_list_no_app_for_file_type
                )
            )
        }
    } else {
        showMessageInSnackbar(
            message = this.getString(
                R.string.file_list_no_app_for_file_type
            )
        )
    }
}

fun AppCompatActivity.sendFile(file: File?) {
    if (file != null) {
        val sendIntent: Intent = makeIntent(file, this)
        val shareSheetIntent = ShareSheetHelper().getShareSheetIntent(
            intent = sendIntent,
            context = this,
            title = R.string.activity_chooser_send_file_title,
            packagesToExclude = arrayOf()
        )
        this.startActivity(shareSheetIntent)
    } else {
        Timber.e("Trying to send a NULL file")
    }
}

private fun makeIntent(file: File?, context: Context): Intent {
    val sendIntent = Intent(Intent.ACTION_SEND)
    if (file != null) {
        // set MimeType
        sendIntent.type = MimetypeIconUtil.getBestMimeTypeByFilename(file.name)
        sendIntent.putExtra(
            Intent.EXTRA_STREAM,
            getExposedFileUri(context, file.path)
        )
    }
    sendIntent.putExtra(Intent.ACTION_SEND, true) // Send Action
    return sendIntent
}

fun Activity.hideSoftKeyboard() {
    val focusedView = currentFocus
    focusedView?.let {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            focusedView.windowToken,
            0
        )
    }
}

fun Activity.checkPasscodeEnforced(securityEnforced: SecurityEnforced) {
    val sharedPreferencesProvider = OCSharedPreferencesProvider(this)
    val mdmProvider by inject<MdmProvider>()

    // If device protection is false, launch the previous behaviour (check the lockEnforced).
    // If device protection is true, ask for security only if device is not secure.
    val showDeviceProtectionForced: Boolean =
        mdmProvider.getBrandingBoolean(CONFIGURATION_DEVICE_PROTECTION, R.bool.device_protection) && !isDeviceSecure()
    val lockEnforced: Int = this.resources.getInteger(R.integer.lock_enforced)
    val passcodeConfigured = sharedPreferencesProvider.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
    val patternConfigured = sharedPreferencesProvider.getBoolean(PatternActivity.PREFERENCE_SET_PATTERN, false)

    when (parseFromInteger(lockEnforced)) {
        LockEnforcedType.DISABLED -> {
            if (showDeviceProtectionForced) {
                showSelectSecurityDialog(passcodeConfigured, patternConfigured, securityEnforced)
            }
        }

        LockEnforcedType.EITHER_ENFORCED -> {
            showSelectSecurityDialog(passcodeConfigured, patternConfigured, securityEnforced)
        }

        LockEnforcedType.PASSCODE_ENFORCED -> {
            if (!passcodeConfigured) {
                manageOptionLockSelected(LockType.PASSCODE)
            }
        }

        LockEnforcedType.PATTERN_ENFORCED -> {
            if (!patternConfigured) {
                manageOptionLockSelected(LockType.PATTERN)
            }
        }
    }
}

private fun Activity.showSelectSecurityDialog(
    passcodeConfigured: Boolean,
    patternConfigured: Boolean,
    securityEnforced: SecurityEnforced
) {
    if (!passcodeConfigured && !patternConfigured) {
        val options = arrayOf(getString(R.string.security_enforced_first_option), getString(R.string.security_enforced_second_option))
        var optionSelected = 0

        AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle(getString(R.string.security_enforced_title))
            .setSingleChoiceItems(options, LockType.PASSCODE.ordinal) { _, which -> optionSelected = which }
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                when (LockType.parseFromInteger(optionSelected)) {
                    LockType.PASSCODE -> securityEnforced.optionLockSelected(LockType.PASSCODE)
                    LockType.PATTERN -> securityEnforced.optionLockSelected(LockType.PATTERN)
                }
                dialog.dismiss()
            }
            .show()
    }
}

fun Activity.sendEmailOrOpenFeedbackDialogAction(feedbackMail: String) {
    if (feedbackMail.isNotEmpty()) {
        val feedback = "Android v" + BuildConfig.VERSION_NAME + " - " + getString(R.string.prefs_feedback)
        sendEmail(email = feedbackMail, subject = feedback)
    } else {
        openFeedbackDialog()
    }
}

fun Activity.openFeedbackDialog() {
    val getInContactDescription =
        getString(
            R.string.feedback_dialog_get_in_contact_description,
            DrawerActivity.CENTRAL_URL,
            DrawerActivity.TALK_MOBILE_URL,
            DrawerActivity.GITHUB_URL
        ).trimIndent()
    val spannableString = HtmlCompat.fromHtml(getInContactDescription, HtmlCompat.FROM_HTML_MODE_LEGACY)

    val descriptionSurvey = TextView(this).apply {
        text = getString(R.string.feedback_dialog_description)
        setPadding(0, 0, 0, 64)
        setTextColor(getColor(android.R.color.black))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)

    }
    val button = Button(ContextThemeWrapper(this, R.style.Button_Primary), null, 0).apply {
        text = getString(R.string.prefs_send_feedback)
        setOnClickListener {
            goToUrl(DrawerActivity.SURVEY_URL)
        }
    }

    val getInContactTitle = TextView(this).apply {
        text = getString(R.string.feedback_dialog_get_in_contact_title)
        setPadding(0, 64, 0, 0)
        setTextColor(getColor(android.R.color.black))
        setTypeface(typeface, Typeface.BOLD)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)

    }
    val getInContactDescriptionTextView = TextView(this).apply {
        text = spannableString
        setTextColor(getColor(android.R.color.black))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        movementMethod = LinkMovementMethod.getInstance()
    }

    val layout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(64, 16, 64, 16)
        addView(descriptionSurvey)
        addView(button)
        addView(getInContactTitle)
        addView(getInContactDescriptionTextView)
    }
    val builder = AlertDialog.Builder(this)
    builder.apply {
        setTitle(getString(R.string.drawer_feedback))
        setView(layout)
        setNegativeButton(R.string.drawer_close) { dialog, _ ->
            dialog.dismiss()
        }
        setCancelable(false)
    }
    val alertDialog = builder.create()
    alertDialog.show()
}

fun Activity.manageOptionLockSelected(type: LockType) {

    OCSharedPreferencesProvider(this).let {
        // Remove passcode
        it.removePreference(PassCodeActivity.PREFERENCE_PASSCODE)
        it.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)

        // Remove pattern
        it.removePreference(PatternActivity.PREFERENCE_PATTERN)
        it.putBoolean(PatternActivity.PREFERENCE_SET_PATTERN, false)

        // Remove biometric
        it.putBoolean(BiometricActivity.PREFERENCE_SET_BIOMETRIC, false)
    }

    when (type) {
        LockType.PASSCODE -> startActivity(Intent(this, PassCodeActivity::class.java).apply {
            action = PassCodeActivity.ACTION_CREATE
            flags = FLAG_ACTIVITY_NO_HISTORY
            putExtra(EXTRAS_LOCK_ENFORCED, true)
        })

        LockType.PATTERN -> startActivity(Intent(this, PatternActivity::class.java).apply {
            action = PatternActivity.ACTION_REQUEST_WITH_RESULT
            flags = FLAG_ACTIVITY_NO_HISTORY
            putExtra(EXTRAS_LOCK_ENFORCED, true)
        })
    }
}

fun Activity.showBiometricDialog(iEnableBiometrics: EnableBiometrics) {
    AlertDialog.Builder(this)
        .setCancelable(false)
        .setTitle(getString(R.string.biometric_dialog_title))
        .setPositiveButton(R.string.common_yes) { dialog, _ ->
            iEnableBiometrics.onOptionSelected(BiometricStatus.ENABLED_BY_USER)
            dialog.dismiss()
        }
        .setNegativeButton(R.string.common_no) { dialog, _ ->
            iEnableBiometrics.onOptionSelected(BiometricStatus.DISABLED_BY_USER)
            dialog.dismiss()
        }
        .show()
}

fun FragmentActivity.sendDownloadedFilesByShareSheet(ocFiles: List<OCFile>) {
    if (ocFiles.isEmpty()) throw IllegalArgumentException("Can't share anything")

    val sendIntent = if (ocFiles.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = ocFiles.first().mimeType
            putExtra(Intent.EXTRA_STREAM, getExposedFileUriForOCFile(this@sendDownloadedFilesByShareSheet, ocFiles.first()))
        }
    } else {
        val fileUris = ocFiles.map { getExposedFileUriForOCFile(this@sendDownloadedFilesByShareSheet, it) }
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = ALL_FILES_SAF_REGEX
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(fileUris))
        }
    }

    val packagesToExclude = arrayOf<String>(this@sendDownloadedFilesByShareSheet.packageName)
    val shareSheetIntent = ShareSheetHelper().getShareSheetIntent(
        sendIntent,
        this@sendDownloadedFilesByShareSheet,
        R.string.activity_chooser_send_file_title,
        packagesToExclude
    )
    startActivity(shareSheetIntent)
}

fun Activity.openOCFile(ocFile: OCFile) {
    val intentForSavedMimeType = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(getExposedFileUriForOCFile(this@openOCFile, ocFile), ocFile.mimeType)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        if (ocFile.hasWritePermission) {
            flags = flags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
    }

    try {
        startActivity(Intent.createChooser(intentForSavedMimeType, getString(R.string.actionbar_open_with)))
    } catch (anfe: ActivityNotFoundException) {
        showErrorInSnackbar(genericErrorMessageId = R.string.file_list_no_app_for_file_type, anfe)
    }
}

fun <T> FragmentActivity.collectLatestLifecycleFlow(
    flow: Flow<T>,
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    collect: suspend (T) -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(lifecycleState) {
            flow.collectLatest(collect)
        }
    }
}
