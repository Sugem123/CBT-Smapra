package com.smapra.exambro.security

import android.net.Uri
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SEBCompatibility @Inject constructor() {

    companion object {
        // Token rahasia: dicocokkan di nginx seb-guard.conf
        // Format: SEB UA + SMAPRA token (tidak ada di app Exambro lain)
        private const val SMAPRA_TOKEN = "SMAPRA-CBT/1.0"
        const val SEB_USER_AGENT =
            "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 (KHTML, like Gecko) SEB/3.3.2 $SMAPRA_TOKEN"

        private const val SEB_SCHEME = "seb"
        private const val SEBS_SCHEME = "sebs"
    }

    fun generateUserAgent(): String = SEB_USER_AGENT

    fun computeConfigKeyHash(configData: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(configData.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun computeBrowserExamKey(url: String, salt: String = ""): String {
        val input = url + salt
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun parseSebUrl(sebUrl: String): String? {
        if (sebUrl.isBlank()) return null

        val uri = try {
            Uri.parse(sebUrl)
        } catch (_: Exception) {
            return null
        }

        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != SEB_SCHEME && scheme != SEBS_SCHEME) return null

        val httpScheme = if (scheme == SEBS_SCHEME) "https" else "http"
        val host = uri.host ?: return null
        val port = if (uri.port != -1) ":${uri.port}" else ""
        val path = uri.path.orEmpty()
        val query = uri.query?.let { "?$it" }.orEmpty()
        val fragment = uri.fragment?.let { "#$it" }.orEmpty()

        return "$httpScheme://$host$port$path$query$fragment"
    }

    fun extractDomainFromUrl(url: String): String? {
        return try {
            Uri.parse(url).host
        } catch (_: Exception) {
            null
        }
    }

    fun isUrlAllowed(url: String, allowedDomain: String): Boolean {
        val urlHost = extractDomainFromUrl(url) ?: return false
        return urlHost == allowedDomain || urlHost.endsWith(".$allowedDomain")
    }
}
