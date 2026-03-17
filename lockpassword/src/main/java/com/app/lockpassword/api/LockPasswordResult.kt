package com.app.lockpassword.api

sealed interface LockPasswordResult {

    data object Success : LockPasswordResult

    data object BiometricSuccess : LockPasswordResult

    data object Cancelled : LockPasswordResult

    data class InvalidPin(
        val attemptsLeft: Int? = null
    ) : LockPasswordResult

    data class Locked(
        val remainingMinutes: Long
    ) : LockPasswordResult

    data class Error(
        val message: String? = null,
        val throwable: Throwable? = null
    ) : LockPasswordResult
}