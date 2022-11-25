/**
 * ownCloud Android client application
 *
 * @author Javier Rodríguez Pérez
 *
 * Copyright (C) 2022 ownCloud GmbH.
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

package com.owncloud.android.presentation.ui.accounts

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import android.accounts.OperationCanceledException
import android.content.ContentResolver
import android.content.Intent
import android.content.SyncRequest
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MenuItem
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.MainApp.Companion.accountType
import com.owncloud.android.MainApp.Companion.authority
import com.owncloud.android.MainApp.Companion.initDependencyInjection
import com.owncloud.android.R
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.presentation.adapters.accounts.AccountManagementAdapter
import com.owncloud.android.presentation.ui.authentication.ACTION_UPDATE_TOKEN
import com.owncloud.android.presentation.ui.authentication.EXTRA_ACTION
import com.owncloud.android.presentation.ui.authentication.LoginActivity
import com.owncloud.android.presentation.viewmodels.accounts.AccountsManagementViewModel
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.RemoveAccountDialogFragment.Companion.newInstance
import com.owncloud.android.ui.dialog.RemoveAccountDialogViewModel
import com.owncloud.android.utils.PreferenceUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import com.owncloud.android.presentation.ui.authentication.EXTRA_ACCOUNT as EXTRA_ACCOUNT_LOGIN_ACTIVITY

const val KEY_CURRENT_ACCOUNT_CHANGED = "CURRENT_ACCOUNT_CHANGED"
const val KEY_ACCOUNT_LIST_CHANGED = "ACCOUNT_LIST_CHANGED"

class AccountManagementActivity : FileActivity(), AccountManagementAdapter.AccountAdapterListener, AccountManagerCallback<Boolean> {

    private val accountListAdapter: AccountManagementAdapter = AccountManagementAdapter(this)
    private lateinit var originalAccounts: Set<String>
    private lateinit var originalCurrentAccount: String
    private lateinit var accountBeingRemoved: String
    private lateinit var tintedCheck: Drawable

    private val removeAccountDialogViewModel: RemoveAccountDialogViewModel by viewModel()
    private val accountsManagementViewModel: AccountsManagementViewModel by viewModel()

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.accounts_layout)
        tintedCheck = ContextCompat.getDrawable(this, R.drawable.ic_current_white)!!
        tintedCheck = DrawableCompat.wrap(tintedCheck)
        val tint = ContextCompat.getColor(this, R.color.actionbar_start_color)
        DrawableCompat.setTint(tintedCheck, tint)

        val recyclerView: RecyclerView = findViewById(R.id.account_list_recycler_view)
        recyclerView.run {
            filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(applicationContext)
            adapter = accountListAdapter
            layoutManager = LinearLayoutManager(this@AccountManagementActivity)
        }

        setupStandardToolbar(
            getString(R.string.prefs_manage_accounts),
            displayHomeAsUpEnabled = true,
            homeButtonEnabled = true,
            displayShowTitleEnabled = true
        )

        val accountList = accountsManagementViewModel.getLoggedAccounts()
        originalAccounts = toAccountNameSet(accountList)
        originalCurrentAccount = accountsManagementViewModel.getCurrentAccount()!!.name


        accountListAdapter.submitAccountList(accountList = getAccountListItems())

        account = accountsManagementViewModel.getCurrentAccount()
        onAccountSet(false)

    }

    /**
     * converts an array of accounts into a set of account names.
     *
     * @param accountList the account array
     * @return set of account names
     */
    private fun toAccountNameSet(accountList: Array<Account>): Set<String> {
        val actualAccounts: MutableSet<String> = HashSet(accountList.size)
        for (account in accountList) {
            actualAccounts.add(account.name)
        }
        return actualAccounts
    }

    override fun onBackPressed() {
        val resultIntent = Intent()
        resultIntent.putExtra(KEY_ACCOUNT_LIST_CHANGED, hasAccountListChanged())
        resultIntent.putExtra(KEY_CURRENT_ACCOUNT_CHANGED, hasCurrentAccountChanged())
        setResult(RESULT_OK, resultIntent)
        finish()
        super.onBackPressed()
    }

    /**
     * checks the set of actual accounts against the set of original accounts when the activity has been started.
     *
     * @return `true` if account list has changed, `false` if not
     */
    private fun hasAccountListChanged(): Boolean {
        val accountList = accountsManagementViewModel.getLoggedAccounts()
        val actualAccounts = toAccountNameSet(accountList)
        return originalAccounts != actualAccounts
    }

    /**
     * checks actual current account against current accounts when the activity has been started.
     *
     * @return `true` if account list has changed, `false` if not
     */
    private fun hasCurrentAccountChanged(): Boolean {
        val currentAccount = accountsManagementViewModel.getCurrentAccount()
        return currentAccount != null && originalCurrentAccount != currentAccount.name
    }

    /**
     * Switch current account to that contained in the received position of the list adapter.
     *
     * @param position A position of the account adapter containing an account.
     */
    override fun switchAccount(position: Int) {
        val clickedAccount: Account = (accountListAdapter.getItem(position) as AccountManagementAdapter.AccountRecyclerItem.AccountItem).account
        if (account.name == clickedAccount.name) {
            // current account selected, just go back
            finish()
        } else {
            // restart list of files with new account
            AccountUtils.setCurrentOwnCloudAccount(
                this,
                clickedAccount.name
            )
            // Refresh dependencies to be used in selected account
            initDependencyInjection()
            val i = Intent(
                this,
                FileDisplayActivity::class.java
            )
            i.putExtra(EXTRA_ACCOUNT, clickedAccount)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(i)
        }
    }

    override fun removeAccount(account: Account) {
        accountBeingRemoved = account.name
        val dialog = newInstance(
            account,
            removeAccountDialogViewModel.hasCameraUploadsAttached(account.name)
        )
        dialog.show(supportFragmentManager, ConfirmationDialogFragment.FTAG_CONFIRMATION)
    }

    override fun changePasswordOfAccount(account: Account) {
        val updateAccountCredentials = Intent(this, LoginActivity::class.java)
        updateAccountCredentials.putExtra(EXTRA_ACCOUNT_LOGIN_ACTIVITY, account)
        updateAccountCredentials.putExtra(
            EXTRA_ACTION,
            ACTION_UPDATE_TOKEN
        )
        startActivity(updateAccountCredentials)
    }

    override fun refreshAccount(account: Account) {
        Timber.d("Got to start sync")
        Timber.d("Requesting sync for " + account.name + " at " + authority + " with new API")
        val builder = SyncRequest.Builder()
        builder.setSyncAdapter(account, authority)
        builder.setExpedited(true)
        builder.setManual(true)
        builder.syncOnce()

        // Fix bug in Android Lollipop when you click on refresh the whole account
        val extras = Bundle()
        builder.setExtras(extras)

        val request = builder.build()
        ContentResolver.requestSync(request)

        showSnackMessage(getString(R.string.synchronizing_account))
    }

    override fun createAccount() {
        val am = AccountManager.get(applicationContext)
        am.addAccount(
            accountType,
            null,
            null,
            null,
            this,
            { future: AccountManagerFuture<Bundle>? ->
                if (future != null) {
                    try {
                        val result = future.result
                        val name = result.getString(AccountManager.KEY_ACCOUNT_NAME)
                        AccountUtils.setCurrentOwnCloudAccount(applicationContext, name)
                        accountListAdapter.submitAccountList(accountList = getAccountListItems())
                    } catch (e: OperationCanceledException) {
                        Timber.e(e, "Account creation canceled")
                    } catch (e: Exception) {
                        Timber.e(e, "Account creation finished in exception")
                    }
                }
            }, handler
        )

    }

    override fun run(future: AccountManagerFuture<Boolean>) {
        if (future.isDone) {
            // Create new adapter with the remaining accounts
            accountListAdapter.submitAccountList(accountList = getAccountListItems())
            val am = AccountManager.get(this)
            if (accountsManagementViewModel.getLoggedAccounts().isEmpty()) {
                // Show create account screen if there isn't any account
                am.addAccount(
                    accountType,
                    null, null, null,
                    this,
                    null, null
                )
            } else {    // at least one account left
                if (accountsManagementViewModel.getCurrentAccount() == null) {
                    // current account was removed - set another as current
                    var accountName = ""
                    val accounts = accountsManagementViewModel.getLoggedAccounts()
                    if (accounts.isNotEmpty()) {
                        accountName = accounts[0].name
                    }
                    AccountUtils.setCurrentOwnCloudAccount(this, accountName)
                }
            }
        }
    }

    /**
     * creates the account list items list including the add-account action in case multiaccount_support is enabled.
     *
     * @return list of account list items
     */
    private fun getAccountListItems(): List<AccountManagementAdapter.AccountRecyclerItem> {
        val accountList = accountsManagementViewModel.getLoggedAccounts()
        val provisionalAccountList = mutableListOf<AccountManagementAdapter.AccountRecyclerItem>()
        accountList.forEach {
            provisionalAccountList.add(AccountManagementAdapter.AccountRecyclerItem.AccountItem(it))
        }

        // Add Create Account item at the end of account list if multi-account is enabled
        if (resources.getBoolean(R.bool.multiaccount_support) || accountList.isEmpty()) {
            provisionalAccountList.add(AccountManagementAdapter.AccountRecyclerItem.NewAccount)
        }
        return provisionalAccountList
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var retval = true
        when (item.itemId) {
            R.id.home -> onBackPressed()
            else -> retval = super.onOptionsItemSelected(item)
        }
        return retval
    }
}
