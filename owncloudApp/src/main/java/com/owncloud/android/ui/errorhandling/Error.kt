package com.owncloud.android.ui.errorhandling

import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode

data class Error(var code: ResultCode, var exception: Exception)