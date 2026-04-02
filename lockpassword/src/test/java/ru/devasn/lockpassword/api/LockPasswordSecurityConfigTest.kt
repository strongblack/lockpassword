package ru.devasn.lockpassword.api

import ru.devasn.config.LockPasswordSecurityConfig
import ru.devasn.config.LockPasswordSecurityPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LockPasswordSecurityConfigTest {

    @Test
    fun defaultConfig_hasExpectedValues() {
        val config = LockPasswordSecurityConfig()

        assertEquals(6, config.pinLength)
        assertEquals(LockPasswordSecurityPreset.BALANCED, config.securityPreset)
        assertEquals(null, config.customPbkdf2Iterations)
        assertTrue(config.useKeystoreWrapping)
        assertEquals(16, config.saltSizeBytes)
        assertEquals(32, config.derivedKeySizeBytes)
        assertEquals(5, config.maxFailedAttemptsBeforeLockout)
        assertEquals(
            listOf(30_000L, 60_000L, 120_000L, 300_000L, 900_000L),
            config.lockoutScheduleMs
        )
        assertEquals("ru.devasn.pin.hmac", config.keystoreAlias)
        assertEquals("lockpassword_secure_storage", config.preferenceFileName)
        assertEquals("pin", config.legacyPlainPinKey)
    }

    @Test
    fun isPinLengthValid_returnsTrue_forLengthFrom4To6() {
        assertTrue(LockPasswordSecurityConfig(pinLength = 4).isPinLengthValid())
        assertTrue(LockPasswordSecurityConfig(pinLength = 5).isPinLengthValid())
        assertTrue(LockPasswordSecurityConfig(pinLength = 6).isPinLengthValid())
    }

    @Test
    fun resolvedPinLength_coercesValueToRange4To6() {
        assertEquals(4, LockPasswordSecurityConfig(pinLength = 3).resolvedPinLength())
        assertEquals(4, LockPasswordSecurityConfig(pinLength = 4).resolvedPinLength())
        assertEquals(5, LockPasswordSecurityConfig(pinLength = 5).resolvedPinLength())
        assertEquals(6, LockPasswordSecurityConfig(pinLength = 6).resolvedPinLength())
        assertEquals(6, LockPasswordSecurityConfig(pinLength = 7).resolvedPinLength())
    }

    @Test
    fun resolvedIterations_returnsExpectedValueForFast() {
        val config = LockPasswordSecurityConfig(
            securityPreset = LockPasswordSecurityPreset.FAST
        )

        assertEquals(80_000, config.resolvedIterations())
    }

    @Test
    fun resolvedIterations_returnsExpectedValueForBalanced() {
        val config = LockPasswordSecurityConfig(
            securityPreset = LockPasswordSecurityPreset.BALANCED
        )

        assertEquals(160_000, config.resolvedIterations())
    }

    @Test
    fun resolvedIterations_returnsExpectedValueForStrong() {
        val config = LockPasswordSecurityConfig(
            securityPreset = LockPasswordSecurityPreset.STRONG
        )

        assertEquals(280_000, config.resolvedIterations())
    }

    @Test
    fun resolvedIterations_returnsCustomValueForCustomPreset() {
        val config = LockPasswordSecurityConfig(
            securityPreset = LockPasswordSecurityPreset.CUSTOM,
            customPbkdf2Iterations = 500_000
        )

        assertEquals(500_000, config.resolvedIterations())
    }

    @Test(expected = IllegalArgumentException::class)
    fun customPreset_withoutIterations_throwsException() {
        LockPasswordSecurityConfig(
            securityPreset = LockPasswordSecurityPreset.CUSTOM,
            customPbkdf2Iterations = null
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun customPreset_withTooSmallIterations_throwsException() {
        LockPasswordSecurityConfig(
            securityPreset = LockPasswordSecurityPreset.CUSTOM,
            customPbkdf2Iterations = 9_999
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun customPreset_withTooLargeIterations_throwsException() {
        LockPasswordSecurityConfig(
            securityPreset = LockPasswordSecurityPreset.CUSTOM,
            customPbkdf2Iterations = 5_000_001
        )
    }

    @Test
    fun getLockoutDurationMs_returnsZeroBeforeThreshold() {
        val config = LockPasswordSecurityConfig(
            maxFailedAttemptsBeforeLockout = 5
        )

        assertEquals(0L, config.getLockoutDurationMs(1))
        assertEquals(0L, config.getLockoutDurationMs(2))
        assertEquals(0L, config.getLockoutDurationMs(3))
        assertEquals(0L, config.getLockoutDurationMs(4))
    }

    @Test
    fun getLockoutDurationMs_returnsFirstValueAtThreshold() {
        val config = LockPasswordSecurityConfig(
            maxFailedAttemptsBeforeLockout = 5,
            lockoutScheduleMs = listOf(30_000L, 60_000L, 120_000L)
        )

        assertEquals(30_000L, config.getLockoutDurationMs(5))
    }

    @Test
    fun getLockoutDurationMs_returnsNextValuesAfterThreshold() {
        val config = LockPasswordSecurityConfig(
            maxFailedAttemptsBeforeLockout = 5,
            lockoutScheduleMs = listOf(30_000L, 60_000L, 120_000L)
        )

        assertEquals(60_000L, config.getLockoutDurationMs(6))
        assertEquals(120_000L, config.getLockoutDurationMs(7))
    }

    @Test
    fun getLockoutDurationMs_returnsLastValueWhenScheduleExceeded() {
        val config = LockPasswordSecurityConfig(
            maxFailedAttemptsBeforeLockout = 5,
            lockoutScheduleMs = listOf(30_000L, 60_000L, 120_000L)
        )

        assertEquals(120_000L, config.getLockoutDurationMs(8))
        assertEquals(120_000L, config.getLockoutDurationMs(20))
    }
}