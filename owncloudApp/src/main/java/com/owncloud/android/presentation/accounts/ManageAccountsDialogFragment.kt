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
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.extensions.avoidScreenshotsIfNeeded
import com.owncloud.android.presentation.authentication.AccountUtils
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.utils.PreferenceUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class ManageAccountsDialogFragment(val supportFragmentManager: FragmentManager) : DialogFragment(), ManageAccountsAdapter.AccountAdapterListener, AccountManagerCallback<Boolean> {

    private var accountListAdapter: ManageAccountsAdapter = ManageAccountsAdapter(this)

    private lateinit var originalAccounts: Set<String>
    private lateinit var originalCurrentAccount: String
    private var currentAccount: Account? = null

    private val manageAccountsViewModel: ManageAccountsViewModel by viewModel()

    override fun onStart() {
        super.onStart()

        val accountList = manageAccountsViewModel.getLoggedAccounts()
        originalAccounts = toAccountNameSet(accountList)
        originalCurrentAccount = manageAccountsViewModel.getCurrentAccount()?.name.toString()

        accountListAdapter.submitAccountList(accountList = getAccountListItems())

        currentAccount = manageAccountsViewModel.getCurrentAccount()

        subscribeToViewModels()
}

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(ContextThemeWrapper(requireContext(), R.style.Theme_AppCompat_Dialog_Alert))
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.manage_accounts_dialog, null)
        builder.setView(dialogView)

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.account_list_recycler_view)

        recyclerView.apply {
            filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)
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
        dismiss()
        val hasAccountAttachedCameraUploads = manageAccountsViewModel.hasCameraUploadsAttached(account.name)
        val dialog = AlertDialog.Builder(requireContext())
            .setMessage(getString(
                if (hasAccountAttachedCameraUploads) R.string.confirmation_remove_account_alert_camera_uploads
                else R.string.confirmation_remove_account_alert, account.name)
            )
            .setPositiveButton(getString(R.string.common_yes)) { dialog, _ ->
                val accountManager = AccountManager.get(MainApp.appContext)
                accountManager.removeAccount(account, this, null)
                dialog.dismiss()
                show(supportFragmentManager, MANAGE_ACCOUNTS_DIALOG)
            }
            .setNegativeButton(getString(R.string.common_no)) { dialog, _ ->
                dialog.dismiss()
                show(supportFragmentManager, MANAGE_ACCOUNTS_DIALOG)
            }
            .create()
        dialog.avoidScreenshotsIfNeeded()
        dialog.show()
    }

    override fun cleanAccountLocalStorage(account: Account) {
        dismiss()
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.clean_data_account_title))
            .setIcon(R.drawable.ic_warning)
            .setMessage(getString(R.string.clean_data_account_message, account.name))
            .setPositiveButton(getString(R.string.clean_data_account_button_yes)) { dialog, _ ->
                manageAccountsViewModel.cleanAccountLocalStorage(account.name)
                dialog.dismiss()
                show(supportFragmentManager, MANAGE_ACCOUNTS_DIALOG)
            }
            .setNegativeButton(R.string.drawer_close) { dialog, _ ->
                dialog.dismiss()
                show(supportFragmentManager, MANAGE_ACCOUNTS_DIALOG)
            }
            .create()
        dialog.avoidScreenshotsIfNeeded()
        dialog.show()
    }

    override fun createAccount() {
        val am = AccountManager.get(requireContext())
        am.addAccount(
            MainApp.accountType,
            null,
            null,
            null,
            requireActivity(),
            { future: AccountManagerFuture<Bundle>? ->
                if (future != null) {
                    try {
                        val result = future.result
                        val name = result.getString(AccountManager.KEY_ACCOUNT_NAME)
                        AccountUtils.setCurrentOwnCloudAccount(requireContext(), name)
                        accountListAdapter.submitAccountList(accountList = getAccountListItems())
                    } catch (e: OperationCanceledException) {
                        Timber.e(e, "Account creation canceled")
                    } catch (e: Exception) {
                        Timber.e(e, "Account creation finished in exception")
                    }
                }
            },
            null
        )
        dismiss()
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
            AccountUtils.setCurrentOwnCloudAccount(
                requireContext(),
                clickedAccount.name
            )
            // Refresh dependencies to be used in selected account
            MainApp.initDependencyInjection()
            val i = Intent(
                requireContext(),
                FileDisplayActivity::class.java
            )
            i.putExtra(FileActivity.EXTRA_ACCOUNT, clickedAccount)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(i)
        }
    }

    override fun run(future: AccountManagerFuture<Boolean>) {
        if (future.isDone) {
            // Create new adapter with the remaining accounts
            accountListAdapter.submitAccountList(accountList = getAccountListItems())
            val am = AccountManager.get(requireContext())
            if (manageAccountsViewModel.getLoggedAccounts().isEmpty()) {
                // Show create account screen if there isn't any account
                am.addAccount(
                    MainApp.accountType, null, null, null,
                    requireActivity(), null, null
                )
            } else {    // at least one account left
                if (manageAccountsViewModel.getCurrentAccount() == null) {
                    // current account was removed - set another as current
                    var accountName = ""
                    val accounts = manageAccountsViewModel.getLoggedAccounts()
                    if (accounts.isNotEmpty()) {
                        accountName = accounts[0].name
                    }
                    AccountUtils.setCurrentOwnCloudAccount(requireContext(), accountName)
                }

            }
        }
    }

    private fun subscribeToViewModels() {

    }

    /**
     * converts an array of accounts into a set of account names.
     *
     * @param accountList the account array
     * @return set of account names
     */
    private fun toAccountNameSet(accountList: Array<Account>): Set<String> {
        val actualAccounts: MutableSet<String> = HashSet(accountList.size)
        accountList.forEach { account ->
            actualAccounts.add(account.name)
        }
        return actualAccounts
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

    /**
     * checks the set of current accounts against the set of original accounts when the dialog was started.
     *
     * @return `true` if account list has changed, `false` if not
     */
    private fun hasAccountListChanged(): Boolean {
        val accountList = manageAccountsViewModel.getLoggedAccounts()
        val currentAccounts = toAccountNameSet(accountList)
        return originalAccounts != currentAccounts
    }

    /**
     * checks current account against original current account when the dialog was started.
     *
     * @return `true` if current account has changed, `false` if not
     */
    private fun hasCurrentAccountChanged(): Boolean {
        val currentAccount = manageAccountsViewModel.getCurrentAccount()
        return currentAccount != null && originalCurrentAccount != currentAccount.name
    }

    companion object {
        const val MANAGE_ACCOUNTS_DIALOG = "MANAGE_ACCOUNTS_DIALOG"

        fun newInstance(supportFragmentManager: FragmentManager): ManageAccountsDialogFragment {
            return ManageAccountsDialogFragment(supportFragmentManager)
        }
    }

}
