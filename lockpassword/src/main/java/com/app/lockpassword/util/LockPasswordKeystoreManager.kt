package com.app.lockpassword.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

class LockPasswordKeystoreManager {

    private val keyCache = mutableMapOf<String, SecretKey>()

    fun signHmac(alias: String, data: ByteArray): ByteArray {
        val secretKey = getOrCreateHmacKey(alias)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKey)
        return mac.doFinal(data)
    }

    @Synchronized
    private fun getOrCreateHmacKey(alias: String): SecretKey {
        keyCache[alias]?.let { return it }

        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        val existingKey = keyStore.getKey(alias, null) as? SecretKey
        if (existingKey != null) {
            keyCache[alias] = existingKey
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
            "AndroidKeyStore"
        )

        val parameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN
        )
            .setKeySize(256)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(parameterSpec)
        val newKey = keyGenerator.generateKey()
        keyCache[alias] = newKey
        return newKey
    }
}