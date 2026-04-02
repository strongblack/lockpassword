package ru.devasn.util

import android.util.Base64
import ru.devasn.config.LockPasswordSecurityConfig
import ru.devasn.model.LockPasswordSecretRecord
import java.security.GeneralSecurityException
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
            wipe(pinChars)
        }
    }

    fun createRecord(pin: CharArray): LockPasswordSecretRecord {
        validatePinForCreate(pin)

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

        var verifier: ByteArray? = null

        return try {
            verifier = buildVerifier(
                derivedKey = derivedKey,
                useKeystoreWrapping = securityConfig.useKeystoreWrapping
            )

            LockPasswordSecretRecord(
                version = CURRENT_VERSION,
                algorithm = resolveAlgorithmName(securityConfig.useKeystoreWrapping),
                saltBase64 = encodeBase64(salt),
                iterations = iterations,
                derivedKeySizeBytes = derivedKeySizeBytes,
                verifierBase64 = encodeBase64(verifier),
                failedAttempts = 0,
                lockoutUntilEpochMs = 0L
            )
        } finally {
            wipe(salt, derivedKey, verifier)
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
            wipe(pinChars)
        }
    }

    fun verify(
        pin: CharArray,
        record: LockPasswordSecretRecord
    ): Boolean {
        if (!isPinValidForVerify(pin)) {
            return false
        }

        if (!isRecordStructurallyValid(record)) {
            return false
        }

        val normalizedAlgorithm = normalizeAlgorithm(record.algorithm) ?: return false

        val salt = decodeBase64OrNull(record.saltBase64) ?: return false
        val expectedVerifier = decodeBase64OrNull(record.verifierBase64)
        if (expectedVerifier == null) {
            wipe(salt)
            return false
        }

        if (!isDecodedRecordDataValid(record, normalizedAlgorithm, salt, expectedVerifier)) {
            wipe(salt, expectedVerifier)
            return false
        }

        val derivedKey = try {
            pbkdf2(
                pin = pin,
                salt = salt,
                iterations = record.iterations,
                keyLengthBytes = record.derivedKeySizeBytes
            )
        } catch (_: GeneralSecurityException) {
            wipe(salt, expectedVerifier)
            return false
        }

        val actualVerifier = try {
            buildVerifierByAlgorithm(
                derivedKey = derivedKey,
                normalizedAlgorithm = normalizedAlgorithm
            )
        } catch (_: GeneralSecurityException) {
            wipe(salt, expectedVerifier, derivedKey)
            return false
        } catch (_: IllegalArgumentException) {
            wipe(salt, expectedVerifier, derivedKey)
            return false
        }

        return try {
            actualVerifier.size == expectedVerifier.size &&
                    MessageDigest.isEqual(expectedVerifier, actualVerifier)
        } finally {
            wipe(salt, expectedVerifier, derivedKey, actualVerifier)
        }
    }

    @Throws(GeneralSecurityException::class)
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

    @Throws(GeneralSecurityException::class)
    private fun buildVerifier(
        derivedKey: ByteArray,
        useKeystoreWrapping: Boolean
    ): ByteArray {
        return if (useKeystoreWrapping) {
            keystoreManager.signHmac(
                alias = securityConfig.keystoreAlias,
                data = derivedKey
            )
        } else {
            derivedKey.copyOf()
        }
    }

    @Throws(GeneralSecurityException::class)
    private fun buildVerifierByAlgorithm(
        derivedKey: ByteArray,
        normalizedAlgorithm: String
    ): ByteArray {
        return when (normalizedAlgorithm) {
            ALGORITHM_PBKDF2_SHA256 -> derivedKey.copyOf()

            ALGORITHM_PBKDF2_SHA256_HMAC_WRAPPED -> {
                keystoreManager.signHmac(
                    alias = securityConfig.keystoreAlias,
                    data = derivedKey
                )
            }

            else -> {
                throw IllegalArgumentException("Unsupported algorithm: $normalizedAlgorithm")
            }
        }
    }

    private fun validatePinForCreate(pin: CharArray) {
        require(pin.isNotEmpty()) {
            "PIN must not be empty"
        }

        require(pin.size == securityConfig.resolvedPinLength()) {
            "PIN length must be exactly ${securityConfig.resolvedPinLength()}"
        }
    }

    private fun isPinValidForVerify(pin: CharArray): Boolean {
        if (pin.isEmpty()) {
            return false
        }

        if (pin.size != securityConfig.resolvedPinLength()) {
            return false
        }

        return true
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

        if (record.failedAttempts < 0) {
            return false
        }

        if (record.lockoutUntilEpochMs < 0L) {
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
        normalizedAlgorithm: String,
        salt: ByteArray,
        expectedVerifier: ByteArray
    ): Boolean {
        if (salt.size !in MIN_SALT_SIZE_BYTES..MAX_SALT_SIZE_BYTES) {
            return false
        }

        if (record.derivedKeySizeBytes !in MIN_DERIVED_KEY_SIZE_BYTES..MAX_DERIVED_KEY_SIZE_BYTES) {
            return false
        }

        val expectedVerifierSize = expectedVerifierSizeBytes(
            normalizedAlgorithm = normalizedAlgorithm,
            derivedKeySizeBytes = record.derivedKeySizeBytes
        )

        if (expectedVerifier.size != expectedVerifierSize) {
            return false
        }

        return true
    }

    private fun expectedVerifierSizeBytes(
        normalizedAlgorithm: String,
        derivedKeySizeBytes: Int
    ): Int {
        return when (normalizedAlgorithm) {
            ALGORITHM_PBKDF2_SHA256 -> derivedKeySizeBytes
            ALGORITHM_PBKDF2_SHA256_HMAC_WRAPPED -> HMAC_SHA256_OUTPUT_SIZE_BYTES
            else -> -1
        }
    }

    private fun resolveAlgorithmName(useKeystoreWrapping: Boolean): String {
        return if (useKeystoreWrapping) {
            ALGORITHM_PBKDF2_SHA256_HMAC_WRAPPED
        } else {
            ALGORITHM_PBKDF2_SHA256
        }
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

    private fun wipe(vararg arrays: ByteArray?) {
        arrays.forEach { array ->
            array?.fill(0)
        }
    }

    private fun wipe(vararg arrays: CharArray?) {
        arrays.forEach { array ->
            array?.fill('\u0000')
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

        private const val HMAC_SHA256_OUTPUT_SIZE_BYTES = 32

        private val secureRandom = SecureRandom()
    }
}