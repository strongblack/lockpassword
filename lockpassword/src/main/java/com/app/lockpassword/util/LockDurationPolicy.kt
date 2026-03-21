package com.app.lockpassword.util

object LockDurationPolicy {

    const val MAX_ATTEMPTS_BEFORE_LOCK = 3

    fun getAttemptsLeft(failedAttempts: Int): Int {
        return (MAX_ATTEMPTS_BEFORE_LOCK - failedAttempts).coerceAtLeast(0)
    }

    fun getLockDurationMillis(lockoutLevel: Int): Long {
        return when (lockoutLevel) {
            1 -> 30_000L
            2 -> 60_000L
            3 -> 5 * 60_000L
            else -> 15 * 60_000L
        }
    }

    fun getRemainingLockSeconds(
        lockedUntilTimestamp: Long,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Long? {
        if (lockedUntilTimestamp <= 0L) return null

        val remaining = lockedUntilTimestamp - currentTimeMillis
        if (remaining <= 0L) return null

        return (remaining + 999L) / 1000L
    }
}