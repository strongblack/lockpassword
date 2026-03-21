package com.app.lockpassword.storage

import android.content.Context
import android.content.SharedPreferences
import com.app.lockpassword.api.LockPasswordSecurityConfig
import com.app.lockpassword.model.LockPasswordSecretRecord
import com.app.lockpassword.model.LockPasswordVerifyResult
import com.app.lockpassword.util.LockPasswordKeystoreManager
import com.app.lockpassword.util.LockPasswordSecurePinHasher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.edit

class LockPasswordPrefsRepository(
    context: Context,
    private val securityConfig: LockPasswordSecurityConfig = LockPasswordSecurityConfig(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        securityConfig.preferenceFileName,
        Context.MODE_PRIVATE
    )

    private val securePinHasher = LockPasswordSecurePinHasher(
        keystoreManager = LockPasswordKeystoreManager(),
        securityConfig = securityConfig
    )

    fun hasPin(): Boolean {
        return loadRecord() != null ||
                !prefs.getString(securityConfig.legacyPlainPinKey, null).isNullOrBlank()
    }

    fun isLockedOut(nowMs: Long = System.currentTimeMillis()): Boolean {
        return getRemainingLockoutMs(nowMs) > 0L
    }

    fun getRemainingLockoutMs(nowMs: Long = System.currentTimeMillis()): Long {
        val record = loadRecord() ?: return 0L
        return (record.lockoutUntilEpochMs - nowMs).coerceAtLeast(0L)
    }

    suspend fun savePin(pin: String) {
        withContext(dispatcher) {
            validatePinOrThrow(pin)

            val record = securePinHasher.createRecord(pin)
            saveRecord(record)

            prefs.edit {
                remove(securityConfig.legacyPlainPinKey)
            }
        }
    }

    suspend fun verifyPin(pin: String): LockPasswordVerifyResult {
        return withContext(dispatcher) {
            migrateLegacyPlainPinIfNeededInternal()

            val currentRecord = loadRecord()
                ?: return@withContext LockPasswordVerifyResult.NoPinConfigured

            val nowMs = System.currentTimeMillis()

            if (currentRecord.lockoutUntilEpochMs > nowMs) {
                return@withContext LockPasswordVerifyResult.Locked(
                    failedAttempts = currentRecord.failedAttempts,
                    lockoutUntilEpochMs = currentRecord.lockoutUntilEpochMs,
                    remainingLockoutMs = currentRecord.lockoutUntilEpochMs - nowMs
                )
            }

            val isValid = securePinHasher.verify(pin, currentRecord)

            if (isValid) {
                saveRecord(
                    currentRecord.copy(
                        failedAttempts = 0,
                        lockoutUntilEpochMs = 0L
                    )
                )

                return@withContext LockPasswordVerifyResult.Success
            }

            val newFailedAttempts = currentRecord.failedAttempts + 1
            val lockoutDurationMs = securityConfig.getLockoutDurationMs(newFailedAttempts)
            val lockoutUntilEpochMs = if (lockoutDurationMs > 0L) {
                nowMs + lockoutDurationMs
            } else {
                0L
            }

            val updatedRecord = currentRecord.copy(
                failedAttempts = newFailedAttempts,
                lockoutUntilEpochMs = lockoutUntilEpochMs
            )

            saveRecord(updatedRecord)

            if (lockoutDurationMs > 0L) {
                return@withContext LockPasswordVerifyResult.Locked(
                    failedAttempts = newFailedAttempts,
                    lockoutUntilEpochMs = lockoutUntilEpochMs,
                    remainingLockoutMs = lockoutDurationMs
                )
            }

            val remainingAttempts = (
                    securityConfig.maxFailedAttemptsBeforeLockout - newFailedAttempts
                    ).coerceAtLeast(0)

            LockPasswordVerifyResult.Invalid(
                failedAttempts = newFailedAttempts,
                attemptsBeforeLockout = securityConfig.maxFailedAttemptsBeforeLockout,
                remainingAttemptsBeforeLockout = remainingAttempts
            )
        }
    }

    fun clearPin() {
        prefs.edit {
            remove(KEY_VERSION)
                .remove(KEY_ALGORITHM)
                .remove(KEY_SALT)
                .remove(KEY_ITERATIONS)
                .remove(KEY_DERIVED_KEY_SIZE_BYTES)
                .remove(KEY_VERIFIER)
                .remove(KEY_FAILED_ATTEMPTS)
                .remove(KEY_LOCKOUT_UNTIL)
                .remove(securityConfig.legacyPlainPinKey)
        }
    }

    fun resetLockState() {
        val record = loadRecord() ?: return

        saveRecord(
            record.copy(
                failedAttempts = 0,
                lockoutUntilEpochMs = 0L
            )
        )
    }

    private fun migrateLegacyPlainPinIfNeededInternal() {
        val hasSecureRecord = loadRecord() != null
        if (hasSecureRecord) {
            return
        }

        val legacyPin = prefs.getString(securityConfig.legacyPlainPinKey, null)
        if (legacyPin.isNullOrBlank()) {
            return
        }

        val secureRecord = securePinHasher.createRecord(legacyPin)
        saveRecord(secureRecord)

        prefs.edit {
            remove(securityConfig.legacyPlainPinKey)
        }
    }

    private fun validatePinOrThrow(pin: String) {
        require(pin.length >= securityConfig.minPinLength) {
            "PIN length must be >= ${securityConfig.minPinLength}"
        }

        if (securityConfig.digitsOnly) {
            require(pin.all { it.isDigit() }) {
                "PIN must contain digits only"
            }
        }
    }

    private fun loadRecord(): LockPasswordSecretRecord? {
        val version = prefs.getInt(KEY_VERSION, Int.MIN_VALUE)
        val algorithm = prefs.getString(KEY_ALGORITHM, null)
        val saltBase64 = prefs.getString(KEY_SALT, null)
        val iterations = prefs.getInt(KEY_ITERATIONS, Int.MIN_VALUE)
        val derivedKeySizeBytes = prefs.getInt(KEY_DERIVED_KEY_SIZE_BYTES, Int.MIN_VALUE)
        val verifierBase64 = prefs.getString(KEY_VERIFIER, null)

        if (
            version == Int.MIN_VALUE ||
            algorithm.isNullOrBlank() ||
            saltBase64.isNullOrBlank() ||
            iterations == Int.MIN_VALUE ||
            derivedKeySizeBytes == Int.MIN_VALUE ||
            verifierBase64.isNullOrBlank()
        ) {
            return null
        }

        return LockPasswordSecretRecord(
            version = version,
            algorithm = algorithm,
            saltBase64 = saltBase64,
            iterations = iterations,
            derivedKeySizeBytes = derivedKeySizeBytes,
            verifierBase64 = verifierBase64,
            failedAttempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0),
            lockoutUntilEpochMs = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        )
    }

    private fun saveRecord(record: LockPasswordSecretRecord) {
        prefs.edit {
            putInt(KEY_VERSION, record.version)
                .putString(KEY_ALGORITHM, record.algorithm)
                .putString(KEY_SALT, record.saltBase64)
                .putInt(KEY_ITERATIONS, record.iterations)
                .putInt(KEY_DERIVED_KEY_SIZE_BYTES, record.derivedKeySizeBytes)
                .putString(KEY_VERIFIER, record.verifierBase64)
                .putInt(KEY_FAILED_ATTEMPTS, record.failedAttempts)
                .putLong(KEY_LOCKOUT_UNTIL, record.lockoutUntilEpochMs)
        }
    }

    private companion object {
        private const val KEY_VERSION = "secure_pin_version"
        private const val KEY_ALGORITHM = "secure_pin_algorithm"
        private const val KEY_SALT = "secure_pin_salt"
        private const val KEY_ITERATIONS = "secure_pin_iterations"
        private const val KEY_DERIVED_KEY_SIZE_BYTES = "secure_pin_derived_key_size_bytes"
        private const val KEY_VERIFIER = "secure_pin_verifier"
        private const val KEY_FAILED_ATTEMPTS = "secure_pin_failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "secure_pin_lockout_until"
    }
}