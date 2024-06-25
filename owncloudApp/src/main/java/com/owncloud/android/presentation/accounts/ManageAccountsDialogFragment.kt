/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
 * @author Juan Carlos Garrote Gascón
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

package com.owncloud.android.presentation.accounts

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import android.accounts.OperationCanceledException
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.extensions.avoidScreenshotsIfNeeded
import com.owncloud.android.extensions.collectLatestLifecycleFlow
import com.owncloud.android.extensions.showErrorInSnackbar
import com.owncloud.android.presentation.authentication.AccountUtils
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.ToolbarActivity
import com.owncloud.android.utils.PreferenceUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class ManageAccountsDialogFragment : DialogFragment(), ManageAccountsAdapter.AccountAdapterListener, AccountManagerCallback<Boolean> {

    private var accountListAdapter: ManageAccountsAdapter = ManageAccountsAdapter(this)
    private var currentAccount: Account? = null

    private lateinit var dialogView: View
    private lateinit var parentActivity: ToolbarActivity

    private val manageAccountsViewModel: ManageAccountsViewModel by viewModel()

    override fun onStart() {
        super.onStart()

        accountListAdapter.submitAccountList(accountList = getAccountListItems())

        parentActivity = requireActivity() as ToolbarActivity
        currentAccount = requireArguments().getParcelable(KEY_CURRENT_ACCOUNT)

        subscribeToViewModels()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(ContextThemeWrapper(requireContext(), R.style.Theme_AppCompat_Dialog_Alert))
        val inflater = this.layoutInflater
        dialogView = inflater.inflate(R.layout.manage_accounts_dialog, null)
        builder.setView(dialogView)

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.account_list_recycler_view)

        recyclerView.apply {
            filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(requireContext())
            adapter = accountListAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        val closeButton = dialogView.findViewById<ImageView>(R.id.cross)
        closeButton.setOnClickListener {
            dismiss()
        }

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.color.transparent)

        return dialog
    }

    override fun removeAccount(account: Account) {
        dialogView.isVisible = false
        val hasAccountAttachedCameraUploads = manageAccountsViewModel.hasCameraUploadsAttached(account.name)
        val dialog = AlertDialog.Builder(requireContext())
            .setMessage(getString(
                if (hasAccountAttachedCameraUploads) R.string.confirmation_remove_account_alert_camera_uploads
                else R.string.confirmation_remove_account_alert, account.name)
            )
            .setPositiveButton(getString(R.string.common_yes)) { _, _ ->
                val accountManager = AccountManager.get(MainApp.appContext)
                accountManager.removeAccount(account, this, null)
                if (manageAccountsViewModel.getLoggedAccounts().size > 1) {
                    dialogView.isVisible = true
                }
            }
            .setNegativeButton(getString(R.string.common_no)) { _, _ ->
                dialogView.isVisible = true
            }
            .setOnDismissListener {
                dialogView.isVisible = true
            }
            .create()
        dialog.avoidScreenshotsIfNeeded()
        dialog.show()
    }

    override fun cleanAccountLocalStorage(account: Account) {
        dialogView.isVisible = false
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.clean_data_account_title))
            .setIcon(R.drawable.ic_warning)
            .setMessage(getString(R.string.clean_data_account_message, account.name))
            .setPositiveButton(getString(R.string.clean_data_account_button_yes)) { _, _ ->
                dialogView.isVisible = true
                manageAccountsViewModel.cleanAccountLocalStorage(account.name)
            }
            .setNegativeButton(R.string.drawer_close) { _, _ ->
                dialogView.isVisible = true
            }
            .setOnDismissListener {
                dialogView.isVisible = true
            }
            .create()
        dialog.avoidScreenshotsIfNeeded()
        dialog.show()
    }

    override fun createAccount() {
        val accountManager = AccountManager.get(MainApp.appContext)
        accountManager.addAccount(
            MainApp.accountType,
            null,
            null,
            null,
            parentActivity,
            { future: AccountManagerFuture<Bundle>? ->
                if (future != null) {
                    try {
                        val result = future.result
                        val name = result.getString(AccountManager.KEY_ACCOUNT_NAME)
                        val newAccount = AccountUtils.getOwnCloudAccountByName(parentActivity.applicationContext, name)
                        changeToAccountContext(newAccount)
                    } catch (e: OperationCanceledException) {
                        Timber.e(e, "Account creation canceled")
                    } catch (e: Exception) {
                        Timber.e(e, "Account creation finished in exception")
                    }
                }
            },
            null
        )
    }

    /**
     * Switch current account to that contained in the received position of the list adapter.
     *
     * @param position A position of the account adapter containing an account.
     */
    override fun switchAccount(position: Int) {
        val clickedAccount: Account = (accountListAdapter.getItem(position) as ManageAccountsAdapter.AccountRecyclerItem.AccountItem).account
        if (currentAccount?.name == clickedAccount.name) {
            // current account selected, just go back
            dismiss()
        } else {
            // restart list of files with new account
            parentActivity.showLoadingDialog(R.string.common_loading)
            dismiss()
            changeToAccountContext(clickedAccount)
        }
    }

    /**
     * What happens when an account is removed
     */
    override fun run(future: AccountManagerFuture<Boolean>) {
        if (future.isDone) {
            if (currentAccount == manageAccountsViewModel.getCurrentAccount()) {
                // Create new adapter with the remaining accounts
                accountListAdapter.submitAccountList(accountList = getAccountListItems())
            } else if (manageAccountsViewModel.getLoggedAccounts().isEmpty()) {
                // Show create account screen if there isn't any account
                createAccount()
            } else { // At least one account left
                manageAccountsViewModel.getCurrentAccount()?.let {
                    if (currentAccount != it) {
                        parentActivity.showLoadingDialog(R.string.common_loading)
                        dismiss()
                        changeToAccountContext(it)
                    }
                }
            }
        }
    }

    private fun changeToAccountContext(account: Account) {
        AccountUtils.setCurrentOwnCloudAccount(
            parentActivity.applicationContext,
            account.name
        )
        parentActivity.account = account
        // Refresh dependencies to be used in selected account
        MainApp.initDependencyInjection()
        val i = Intent(
            parentActivity.applicationContext,
            FileDisplayActivity::class.java
        )
        i.putExtra(FileActivity.EXTRA_ACCOUNT, account)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        parentActivity.startActivity(i)
    }

    private fun subscribeToViewModels() {
        collectLatestLifecycleFlow(manageAccountsViewModel.cleanAccountLocalStorageFlow) { event ->
            event?.peekContent()?.let { uiResult ->
                when (uiResult) {
                    is UIResult.Loading -> {
                        parentActivity.showLoadingDialog(R.string.common_loading)
                        dialogView.isVisible = false
                    }
                    is UIResult.Success -> {
                        parentActivity.dismissLoadingDialog()
                        dialogView.isVisible = true
                    }
                    is UIResult.Error -> {
                        parentActivity.dismissLoadingDialog()
                        showErrorInSnackbar(R.string.common_error_unknown, uiResult.error)
                        Timber.e(uiResult.error)
                    }

                }
            }
        }
    }

    /**
     * creates the account list items list including the add-account action in case multiaccount_support is enabled.
     *
     * @return list of account list items
     */
    private fun getAccountListItems(): List<ManageAccountsAdapter.AccountRecyclerItem> {
        val accountList = manageAccountsViewModel.getLoggedAccounts()
        val provisionalAccountList = mutableListOf<ManageAccountsAdapter.AccountRecyclerItem>()
        accountList.forEach {
            provisionalAccountList.add(ManageAccountsAdapter.AccountRecyclerItem.AccountItem(it))
        }

        // Add Create Account item at the end of account list if multi-account is enabled
        if (resources.getBoolean(R.bool.multiaccount_support) || accountList.isEmpty()) {
            provisionalAccountList.add(ManageAccountsAdapter.AccountRecyclerItem.NewAccount)
        }
        return provisionalAccountList
    }

    companion object {
        const val MANAGE_ACCOUNTS_DIALOG = "MANAGE_ACCOUNTS_DIALOG"
        const val KEY_CURRENT_ACCOUNT = "KEY_CURRENT_ACCOUNT"

        fun newInstance(currentAccount: Account?): ManageAccountsDialogFragment {
            val args = Bundle().apply {
                putParcelable(KEY_CURRENT_ACCOUNT, currentAccount)
            }
            return ManageAccountsDialogFragment().apply { arguments = args }
        }
    }

}
