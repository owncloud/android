package com.owncloud.android

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import androidx.test.espresso.IdlingResource
import com.owncloud.android.lib.common.accounts.AccountUtils

class AccountIdlingResource(
    context: Context,
    private val currentAccount: Account
) : IdlingResource {
    private var idle: Boolean = false
    private var resourceCallback: IdlingResource.ResourceCallback? = null
    private var accountManager: AccountManager

    init {
        this.idle = false
        this.resourceCallback = null
        this.accountManager = AccountManager.get(context)
    }

    override fun getName(): String {
        return AccountIdlingResource::class.java.simpleName
    }

    override fun isIdleNow(): Boolean {
        idle = idle || accountManager.getUserData(currentAccount, AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION) != null

        if (idle) {
            if (resourceCallback != null) {
                resourceCallback!!.onTransitionToIdle()
            }
        }

        return idle
    }

    override fun registerIdleTransitionCallback(resourceCallback: IdlingResource.ResourceCallback?) {
        this.resourceCallback = resourceCallback
    }
}
