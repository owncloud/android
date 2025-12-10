package com.owncloud.android.utils

import kotlinx.coroutines.CancellationException

suspend fun runCatchingException(
    block: suspend () -> Unit,
    exceptionHandlerBlock: suspend (Exception) -> Unit,
    completeBlock: suspend () -> Unit = {}
) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        exceptionHandlerBlock(e)
    } finally {
        completeBlock()
    }
}

suspend fun <T> runCatchingException(
    block: suspend () -> T,
    exceptionHandlerBlock: suspend (Exception) -> Unit,
    completeBlock: suspend (T) -> Unit = {}
) {
    var result: T? = null
    try {
        result = block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        exceptionHandlerBlock(e)
    } finally {
        result?.let { completeBlock(it) }
    }
}