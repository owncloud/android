package com.owncloud.android.lib.common.network

import java.security.cert.X509Certificate

interface CertificateReader {

    fun readCertificates(): List<X509Certificate>

    fun readCertificate(certificatePath: String): X509Certificate
}
