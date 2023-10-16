package com.owncloud.android.domain.exceptions

class DeepLinkException() : Exception(INVALID_FORMAT)

const val INVALID_FORMAT = "Invalid deep link format"