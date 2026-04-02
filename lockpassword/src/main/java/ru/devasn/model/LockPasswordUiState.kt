package ru.devasn.model

data class LockPasswordUiState(
    val mode: ru.devasn.model.LockPasswordMode = _root_ide_package_.ru.devasn.model.LockPasswordMode.CREATE,
    val input: String = "",
    val pinLength: Int = 6,
    val error: ru.devasn.model.LockPasswordError? = null,
    val attemptsLeft: Int? = null,
    val remainingLockSeconds: Long? = null,
    val isBiometricAvailable: Boolean = false,
    val showBiometricButton: Boolean = false,
    val shouldAutoLaunchBiometric: Boolean = false
)