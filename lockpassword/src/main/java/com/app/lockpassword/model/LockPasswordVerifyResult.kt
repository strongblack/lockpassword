package com.app.lockpassword.model

sealed interface LockPasswordVerifyResult {

    data object Success : LockPasswordVerifyResult

    data object NoPinConfigured : LockPasswordVerifyResult

    data class Invalid(
        val failedAttempts: Int,
        val attemptsBeforeLockout: Int,
        val remainingAttemptsBeforeLockout: Int
    ) : LockPasswordVerifyResult

    data class Locked(
        val failedAttempts: Int,
        val lockoutUntilEpochMs: Long,
        val remainingLockoutMs: Long
    ) : LockPasswordVerifyResult
}