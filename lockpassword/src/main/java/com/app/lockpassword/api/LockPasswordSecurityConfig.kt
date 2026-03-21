package com.app.lockpassword.api

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class LockPasswordSecurityConfig(
    val minPinLength: Int = 6,
    val digitsOnly: Boolean = true,
    val securityPreset: LockPasswordSecurityPreset = LockPasswordSecurityPreset.BALANCED,
    val customPbkdf2Iterations: Int? = null,
    val useKeystoreWrapping: Boolean = true,
    val saltSizeBytes: Int = 16,
    val derivedKeySizeBytes: Int = 32,
    val maxFailedAttemptsBeforeLockout: Int = 5,
    val lockoutScheduleMs: List<Long> = listOf(
        30_000L,
        60_000L,
        120_000L,
        300_000L,
        900_000L
    ),
    val keystoreAlias: String = "com.app.lockpassword.pin.hmac",
    val preferenceFileName: String = "lockpassword_secure_storage",
    val legacyPlainPinKey: String = "pin"
) : Parcelable {

    init {
        require(minPinLength in 4..12) {
            "minPinLength must be in range 4..12"
        }

        require(saltSizeBytes in 16..64) {
            "saltSizeBytes must be in range 16..64"
        }

        require(derivedKeySizeBytes in 16..64) {
            "derivedKeySizeBytes must be in range 16..64"
        }

        require(maxFailedAttemptsBeforeLockout > 0) {
            "maxFailedAttemptsBeforeLockout must be > 0"
        }

        require(lockoutScheduleMs.isNotEmpty()) {
            "lockoutScheduleMs must not be empty"
        }

        require(lockoutScheduleMs.all { it >= 0L }) {
            "lockoutScheduleMs must contain only non-negative values"
        }

        if (securityPreset == LockPasswordSecurityPreset.CUSTOM) {
            require(customPbkdf2Iterations != null) {
                "customPbkdf2Iterations must be set for CUSTOM preset"
            }

            require(customPbkdf2Iterations in 10_000..5_000_000) {
                "customPbkdf2Iterations must be in range 10_000..5_000_000"
            }
        }
    }

    fun resolvedIterations(): Int {
        return when (securityPreset) {
            LockPasswordSecurityPreset.FAST -> 80_000
            LockPasswordSecurityPreset.BALANCED -> 160_000
            LockPasswordSecurityPreset.STRONG -> 280_000
            LockPasswordSecurityPreset.CUSTOM -> customPbkdf2Iterations!!
        }
    }

    fun getLockoutDurationMs(failedAttempts: Int): Long {
        val index = failedAttempts - maxFailedAttemptsBeforeLockout
        if (index < 0) {
            return 0L
        }
        return lockoutScheduleMs.getOrElse(index) { lockoutScheduleMs.last() }
    }
}