package com.owncloud.android.shares.ui

import android.view.View
import androidx.fragment.app.Fragment
import androidx.test.espresso.IdlingResource

class FragmentVisibilityIdlingResource(private val fragment: Fragment?, private val expectedVisibility: Int) :
    IdlingResource {
    private var idle: Boolean = false
    private var resourceCallback: IdlingResource.ResourceCallback? = null

    init {
        this.idle = false
        this.resourceCallback = null
    }

    override fun getName(): String {
        return FragmentVisibilityIdlingResource::class.java.simpleName
    }

    override fun isIdleNow(): Boolean {
        if (fragment == null) return false

        idle = idle || (fragment.isVisible && expectedVisibility == View.VISIBLE) ||
                (!fragment.isVisible && expectedVisibility == View.INVISIBLE)

        if (idle) {
            if (resourceCallback != null) {
                resourceCallback!!.onTransitionToIdle()
            }
        }

        return idle
    }

    override fun registerIdleTransitionCallback(resourceCallback: IdlingResource.ResourceCallback) {
        this.resourceCallback = resourceCallback
    }
}
