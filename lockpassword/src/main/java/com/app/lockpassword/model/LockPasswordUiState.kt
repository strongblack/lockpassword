package com.app.lockpassword.model

data class LockPasswordUiState(
    val mode: LockPasswordMode = LockPasswordMode.ENTER,
    val input: String = "",
    val pinLength: Int = 6,
    val error: LockPasswordError? = null,
    val attemptsLeft: Int? = null,
    val remainingMinutes: Long? = null,
    val isBiometricAvailable: Boolean = false,
    val showBiometricButton: Boolean = false
)