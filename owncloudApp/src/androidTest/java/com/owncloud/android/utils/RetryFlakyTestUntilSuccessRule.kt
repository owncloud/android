package com.owncloud.android.utils

import android.util.Log
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class RetryFlakyTestUntilSuccessRule(val count: Int = 10) : TestRule {

    companion object {
        private const val TAG = "RetryFlakyTestUntilSuccessRule"
    }

    override fun apply(base: Statement, description: Description): Statement = statement(base, description)

    private fun statement(base: Statement, description: Description): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                var throwable: Throwable? = null
                val displayName = description.displayName
                for (i in 1 until count + 1) {
                    try {
                        Log.i(TAG, "$displayName: Run $i")
                        base.evaluate()
                        return
                    } catch (t: Throwable) {
                        throwable = t
                        Log.e(TAG, "$displayName: Run $i failed.")
                    }
                }
                Log.e(TAG, "$displayName: Giving up after run $count failures.")
                throw throwable!!
            }
        }
    }
}