package com.owncloud.android

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider

class ViewModelFactory {
    companion object {
        inline fun <VM : ViewModel> build(crossinline f: () -> VM) =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(aClass: Class<T>): T = f() as T
            }
    }
}