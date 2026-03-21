package com.app.lockpassword.util

import android.util.Base64
import com.app.lockpassword.api.LockPasswordSecurityConfig
import com.app.lockpassword.model.LockPasswordSecretRecord
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class LockPasswordSecurePinHasher(
    private val keystoreManager: LockPasswordKeystoreManager,
    private val securityConfig: LockPasswordSecurityConfig
) {

    fun createRecord(pin: String): LockPasswordSecretRecord {
        val pinChars = pin.toCharArray()

        return try {
            createRecord(pinChars)
        } finally {
            pinChars.fill('\u0000')
        }
    }

    fun createRecord(pin: CharArray): LockPasswordSecretRecord {
        require(pin.isNotEmpty()) { "PIN must not be empty" }

        val saltSizeBytes = securityConfig.saltSizeBytes
        val derivedKeySizeBytes = securityConfig.derivedKeySizeBytes
        val iterations = securityConfig.resolvedIterations()

        validateCreateParams(
            saltSizeBytes = saltSizeBytes,
            derivedKeySizeBytes = derivedKeySizeBytes,
            iterations = iterations
        )

        val salt = ByteArray(saltSizeBytes)
        secureRandom.nextBytes(salt)

        val derivedKey = pbkdf2(
            pin = pin,
            salt = salt,
            iterations = iterations,
            keyLengthBytes = derivedKeySizeBytes
        )

        val verifier = if (securityConfig.useKeystoreWrapping) {
            keystoreManager.signHmac(
                alias = securityConfig.keystoreAlias,
                data = derivedKey
            )
        } else {
            derivedKey.copyOf()
        }

        return try {
            LockPasswordSecretRecord(
                version = CURRENT_VERSION,
                algorithm = if (securityConfig.useKeystoreWrapping) {
                    ALGORITHM_PBKDF2_SHA256_HMAC_WRAPPED
                } else {
                    ALGORITHM_PBKDF2_SHA256
                },
                saltBase64 = encodeBase64(salt),
                iterations = iterations,
                derivedKeySizeBytes = derivedKeySizeBytes,
                verifierBase64 = encodeBase64(verifier),
                failedAttempts = 0,
                lockoutUntilEpochMs = 0L
            )
        } finally {
            salt.fill(0)
            derivedKey.fill(0)
            verifier.fill(0)
        }
    }

    fun verify(
        pin: String,
        record: LockPasswordSecretRecord
    ): Boolean {
        val pinChars = pin.toCharArray()

        return try {
            verify(pinChars, record)
        } finally {
            pinChars.fill('\u0000')
        }
    }

    fun verify(
        pin: CharArray,
        record: LockPasswordSecretRecord
    ): Boolean {
        if (pin.isEmpty()) {
            return false
        }

        if (!isRecordStructurallyValid(record)) {
            return false
        }

        val normalizedAlgorithm = normalizeAlgorithm(record.algorithm) ?: return false

        val salt = decodeBase64OrNull(record.saltBase64) ?: return false
        val expectedVerifier = decodeBase64OrNull(record.verifierBase64) ?: run {
            salt.fill(0)
            return false
        }

        if (!isDecodedRecordDataValid(record, salt, expectedVerifier)) {
            salt.fill(0)
            expectedVerifier.fill(0)
            return false
        }

        val derivedKey = try {
            pbkdf2(
                pin = pin,
                salt = salt,
                iterations = record.iterations,
                keyLengthBytes = record.derivedKeySizeBytes
            )
        } catch (_: Exception) {
            salt.fill(0)
            expectedVerifier.fill(0)
            return false
        }

        val actualVerifier = try {
            when (normalizedAlgorithm) {
                ALGORITHM_PBKDF2_SHA256_HMAC_WRAPPED -> {
                    keystoreManager.signHmac(
                        alias = securityConfig.keystoreAlias,
                        data = derivedKey
                    )
                }

                ALGORITHM_PBKDF2_SHA256 -> {
                    derivedKey.copyOf()
                }

                else -> {
                    derivedKey.fill(0)
                    salt.fill(0)
                    expectedVerifier.fill(0)
                    return false
                }
            }
        } catch (_: Exception) {
            derivedKey.fill(0)
            salt.fill(0)
            expectedVerifier.fill(0)
            return false
        }

        return try {
            MessageDigest.isEqual(expectedVerifier, actualVerifier)
        } finally {
            salt.fill(0)
            expectedVerifier.fill(0)
            derivedKey.fill(0)
            actualVerifier.fill(0)
        }
    }

    private fun pbkdf2(
        pin: CharArray,
        salt: ByteArray,
        iterations: Int,
        keyLengthBytes: Int
    ): ByteArray {
        val spec = PBEKeySpec(
            pin,
            salt,
            iterations,
            keyLengthBytes * 8
        )

        return try {
            val secretKeyFactory = SecretKeyFactory.getInstance(PBKDF2_HMAC_SHA256)
            secretKeyFactory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun validateCreateParams(
        saltSizeBytes: Int,
        derivedKeySizeBytes: Int,
        iterations: Int
    ) {
        require(saltSizeBytes in MIN_SALT_SIZE_BYTES..MAX_SALT_SIZE_BYTES) {
            "saltSizeBytes must be in range $MIN_SALT_SIZE_BYTES..$MAX_SALT_SIZE_BYTES"
        }

        require(derivedKeySizeBytes in MIN_DERIVED_KEY_SIZE_BYTES..MAX_DERIVED_KEY_SIZE_BYTES) {
            "derivedKeySizeBytes must be in range $MIN_DERIVED_KEY_SIZE_BYTES..$MAX_DERIVED_KEY_SIZE_BYTES"
        }

        require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) {
            "iterations must be in range $MIN_ITERATIONS..$MAX_ITERATIONS"
        }
    }

    private fun isRecordStructurallyValid(record: LockPasswordSecretRecord): Boolean {
        if (record.version !in MIN_SUPPORTED_VERSION..CURRENT_VERSION) {
            return false
        }

        if (record.iterations !in MIN_ITERATIONS..MAX_ITERATIONS) {
            return false
        }

        if (record.derivedKeySizeBytes !in MIN_DERIVED_KEY_SIZE_BYTES..MAX_DERIVED_KEY_SIZE_BYTES) {
            return false
        }

        if (record.saltBase64.isBlank()) {
            return false
        }

        if (record.verifierBase64.isBlank()) {
            return false
        }

        if (normalizeAlgorithm(record.algorithm) == null) {
            return false
        }

        return true
    }

    private fun isDecodedRecordDataValid(
        record: LockPasswordSecretRecord,
        salt: ByteArray,
        expectedVerifier: ByteArray
    ): Boolean {
        if (salt.size !in MIN_SALT_SIZE_BYTES..MAX_SALT_SIZE_BYTES) {
            return false
        }

        if (record.derivedKeySizeBytes !in MIN_DERIVED_KEY_SIZE_BYTES..MAX_DERIVED_KEY_SIZE_BYTES) {
            return false
        }

        if (expectedVerifier.isEmpty()) {
            return false
        }

        return true
    }

    private fun normalizeAlgorithm(algorithm: String): String? {
        return when (algorithm) {
            ALGORITHM_PBKDF2_SHA256,
            LEGACY_ALGORITHM_PBKDF2_SHA256_V2 -> ALGORITHM_PBKDF2_SHA256

            ALGORITHM_PBKDF2_SHA256_HMAC_WRAPPED,
            LEGACY_ALGORITHM_PBKDF2_SHA256_HMAC_WRAPPED_V1,
            LEGACY_ALGORITHM_PBKDF2_SHA256_HMAC_WRAPPED_V2 -> ALGORITHM_PBKDF2_SHA256_HMAC_WRAPPED

            else -> null
        }
    }

    private fun encodeBase64(value: ByteArray): String {
        return Base64.encodeToString(value, Base64.NO_WRAP)
    }

    private fun decodeBase64OrNull(value: String): ByteArray? {
        return try {
            Base64.decode(value, Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private companion object {
        private const val PBKDF2_HMAC_SHA256 = "PBKDF2WithHmacSHA256"

        private const val ALGORITHM_PBKDF2_SHA256 = "PBKDF2_HMAC_SHA256"
        private const val ALGORITHM_PBKDF2_SHA256_HMAC_WRAPPED = "PBKDF2_HMAC_SHA256_HMAC_WRAPPED"

        private const val LEGACY_ALGORITHM_PBKDF2_SHA256_V2 = "PBKDF2_HMAC_SHA256_V2"
        private const val LEGACY_ALGORITHM_PBKDF2_SHA256_HMAC_WRAPPED_V1 =
            "PBKDF2_HMAC_SHA256_HMAC_WRAPPED_V1"
        private const val LEGACY_ALGORITHM_PBKDF2_SHA256_HMAC_WRAPPED_V2 =
            "PBKDF2_HMAC_SHA256_HMAC_WRAPPED_V2"

        private const val MIN_SUPPORTED_VERSION = 1
        private const val CURRENT_VERSION = 2

        private const val MIN_SALT_SIZE_BYTES = 16
        private const val MAX_SALT_SIZE_BYTES = 64

        private const val MIN_DERIVED_KEY_SIZE_BYTES = 16
        private const val MAX_DERIVED_KEY_SIZE_BYTES = 64

        private const val MIN_ITERATIONS = 10_000
        private const val MAX_ITERATIONS = 5_000_000

        private val secureRandom = SecureRandom()
    }
}