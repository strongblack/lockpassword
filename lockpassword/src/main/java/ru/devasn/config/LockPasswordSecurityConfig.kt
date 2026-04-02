package ru.devasn.config

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import ru.devasn.config.LockPasswordSecurityPreset

@Parcelize
data class LockPasswordSecurityConfig(
    val pinLength: Int = 6,
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
    val keystoreAlias: String = "ru.devasn.pin.hmac",
    val preferenceFileName: String = "lockpassword_secure_storage",
    val legacyPlainPinKey: String = "pin"
) : Parcelable {

    init {
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

    fun isPinLengthValid(): Boolean {
        return pinLength in 4..6
    }

    fun resolvedPinLength(): Int {
        return pinLength.coerceIn(4, 6)
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

    class Builder {
        private var pinLength: Int = 6
        private var securityPreset: LockPasswordSecurityPreset =
            LockPasswordSecurityPreset.BALANCED
        private var customPbkdf2Iterations: Int? = null
        private var useKeystoreWrapping: Boolean = true
        private var saltSizeBytes: Int = 16
        private var derivedKeySizeBytes: Int = 32
        private var maxFailedAttemptsBeforeLockout: Int = 5
        private var lockoutScheduleMs: List<Long> = listOf(
            30_000L,
            60_000L,
            120_000L,
            300_000L,
            900_000L
        )

        fun setPinLength(pinLength: Int) = apply {
            this.pinLength = pinLength
        }

        fun setSecurityPreset(securityPreset: LockPasswordSecurityPreset) = apply {
            this.securityPreset = securityPreset
        }

        fun setCustomPbkdf2Iterations(customPbkdf2Iterations: Int?) = apply {
            this.customPbkdf2Iterations = customPbkdf2Iterations
        }

        fun setUseKeystoreWrapping(useKeystoreWrapping: Boolean) = apply {
            this.useKeystoreWrapping = useKeystoreWrapping
        }

        fun setSaltSizeBytes(saltSizeBytes: Int) = apply {
            this.saltSizeBytes = saltSizeBytes
        }

        fun setDerivedKeySizeBytes(derivedKeySizeBytes: Int) = apply {
            this.derivedKeySizeBytes = derivedKeySizeBytes
        }

        fun setMaxFailedAttemptsBeforeLockout(maxFailedAttemptsBeforeLockout: Int) = apply {
            this.maxFailedAttemptsBeforeLockout = maxFailedAttemptsBeforeLockout
        }

        fun setLockoutScheduleMs(lockoutScheduleMs: List<Long>) = apply {
            this.lockoutScheduleMs = lockoutScheduleMs
        }


        fun build(): LockPasswordSecurityConfig {
            return LockPasswordSecurityConfig(
                pinLength = pinLength,
                securityPreset = securityPreset,
                customPbkdf2Iterations = customPbkdf2Iterations,
                useKeystoreWrapping = useKeystoreWrapping,
                saltSizeBytes = saltSizeBytes,
                derivedKeySizeBytes = derivedKeySizeBytes,
                maxFailedAttemptsBeforeLockout = maxFailedAttemptsBeforeLockout,
                lockoutScheduleMs = lockoutScheduleMs,
            )
        }
    }

    companion object {
        @JvmStatic
        fun balanced(pinLength: Int): LockPasswordSecurityConfig {
            return Builder()
                .setPinLength(pinLength)
                .setSecurityPreset(LockPasswordSecurityPreset.BALANCED)
                .build()
        }

        @JvmStatic
        fun fast(pinLength: Int): LockPasswordSecurityConfig {
            return Builder()
                .setPinLength(pinLength)
                .setSecurityPreset(LockPasswordSecurityPreset.FAST)
                .build()
        }

        @JvmStatic
        fun strong(pinLength: Int): LockPasswordSecurityConfig {
            return Builder()
                .setPinLength(pinLength)
                .setSecurityPreset(LockPasswordSecurityPreset.STRONG)
                .build()
        }

        @JvmStatic
        fun custom(
            pinLength: Int,
            customPbkdf2Iterations: Int
        ): LockPasswordSecurityConfig {
            return Builder()
                .setPinLength(pinLength)
                .setSecurityPreset(LockPasswordSecurityPreset.CUSTOM)
                .setCustomPbkdf2Iterations(customPbkdf2Iterations)
                .build()
        }
    }
}