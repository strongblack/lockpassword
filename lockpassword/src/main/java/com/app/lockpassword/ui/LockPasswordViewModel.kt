package com.app.lockpassword.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.lockpassword.api.LockPasswordResult
import com.app.lockpassword.model.LockPasswordError
import com.app.lockpassword.model.LockPasswordMode
import com.app.lockpassword.model.LockPasswordUiState
import com.app.lockpassword.storage.LockPasswordPrefsRepository
import com.app.lockpassword.util.LockDurationPolicy
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LockPasswordViewModel(
    private val repository: LockPasswordPrefsRepository,
    private val isBiometricAvailable: Boolean
) : ViewModel() {

    private val hasSavedPinAtStart: Boolean = repository.hasPin()

    private val _uiState = MutableStateFlow(
        LockPasswordUiState(
            mode = if (hasSavedPinAtStart) {
                LockPasswordMode.ENTER
            } else {
                LockPasswordMode.CREATE
            },
            isBiometricAvailable = isBiometricAvailable,
            showBiometricButton = hasSavedPinAtStart && isBiometricAvailable,
            shouldAutoLaunchBiometric = hasSavedPinAtStart && isBiometricAvailable
        )
    )
    val uiState: StateFlow<LockPasswordUiState> = _uiState.asStateFlow()

    private val _resultEvents = MutableSharedFlow<LockPasswordResult>()
    val resultEvents: SharedFlow<LockPasswordResult> = _resultEvents.asSharedFlow()

    private var firstPin: String? = null

    init {
        checkLockState()
    }

    fun onDigitClick(digit: Int) {
        val state = _uiState.value

        if (state.remainingMinutes != null) return
        if (state.input.length >= state.pinLength) return

        val newInput = state.input + digit.toString()

        _uiState.value = state.copy(
            input = newInput,
            error = null,
            showBiometricButton = false,
            shouldAutoLaunchBiometric = false
        )

        if (newInput.length == state.pinLength) {
            when (state.mode) {
                LockPasswordMode.CREATE -> handleCreatePin(newInput)
                LockPasswordMode.CONFIRM -> handleConfirmPin(newInput)
                LockPasswordMode.ENTER -> handleEnterPin(newInput)
            }
        }
    }

    fun onBackspaceClick() {
        val state = _uiState.value
        if (state.input.isEmpty()) return

        val newInput = state.input.dropLast(1)

        _uiState.value = state.copy(
            input = newInput,
            error = null,
            showBiometricButton = shouldShowBiometricButton(
                mode = state.mode,
                input = newInput,
                remainingMinutes = state.remainingMinutes
            )
        )
    }

    fun onCancelClick() {
        emitResult(LockPasswordResult.Cancelled)
    }

    fun onBiometricSuccess() {
        repository.setErrorCount(0)
        repository.clearLockTimestamp()

        _uiState.value = _uiState.value.copy(
            input = "",
            error = null,
            attemptsLeft = null,
            remainingMinutes = null,
            showBiometricButton = shouldShowBiometricButton(
                mode = _uiState.value.mode,
                input = "",
                remainingMinutes = null
            )
        )

        emitResult(LockPasswordResult.BiometricSuccess)
    }


    fun onBiometricError() {
        val state = _uiState.value
        _uiState.value = state.copy(
            shouldAutoLaunchBiometric = false,
            showBiometricButton = shouldShowBiometricButton(
                mode = state.mode,
                input = state.input,
                remainingMinutes = state.remainingMinutes
            )
        )
    }

    fun onEmailChange(value: String) {
        // пока поле email не используется
    }

    fun refreshLockState() {
        checkLockState()
    }

    private fun checkLockState() {
        val errorCount = repository.getErrorCount()
        val lockTimestamp = repository.getLockTimestamp()

        val remainingMinutes = LockDurationPolicy.getRemainingMinutes(
            lockTimestamp = lockTimestamp,
            errorCycle = if (errorCount <= 0) 1 else errorCount
        )

        if (remainingMinutes != null) {
            _uiState.value = _uiState.value.copy(
                input = "",
                error = LockPasswordError.LOCKED,
                remainingMinutes = remainingMinutes,
                attemptsLeft = null,
                showBiometricButton = false
            )

            emitResult(LockPasswordResult.Locked(remainingMinutes))
        } else {
            repository.clearLockTimestamp()

            val currentState = _uiState.value

            _uiState.value = currentState.copy(
                remainingMinutes = null,
                error = null,
                showBiometricButton = shouldShowBiometricButton(
                    mode = currentState.mode,
                    input = currentState.input,
                    remainingMinutes = null
                ),
                shouldAutoLaunchBiometric = shouldShowBiometricButton(
                    mode = currentState.mode,
                    input = currentState.input,
                    remainingMinutes = null
                )
            )
        }
    }

    fun onBiometricPromptShown() {
        _uiState.value = _uiState.value.copy(
            shouldAutoLaunchBiometric = false
        )
    }



    private fun handleCreatePin(input: String) {
        firstPin = input

        _uiState.value = _uiState.value.copy(
            input = "",
            mode = LockPasswordMode.CONFIRM,
            error = null,
            attemptsLeft = null,
            remainingMinutes = null,
            showBiometricButton = false
        )
    }

    private fun handleConfirmPin(input: String) {
        if (firstPin != input) {
            firstPin = null

            _uiState.value = _uiState.value.copy(
                input = "",
                mode = LockPasswordMode.CREATE,
                error = LockPasswordError.PIN_MISMATCH,
                attemptsLeft = null,
                remainingMinutes = null,
                showBiometricButton = false
            )
            return
        }

        repository.savePin(input)
        repository.setErrorCount(0)
        repository.clearLockTimestamp()

        firstPin = null

        _uiState.value = _uiState.value.copy(
            input = "",
            mode = LockPasswordMode.ENTER,
            error = null,
            attemptsLeft = null,
            remainingMinutes = null,
            showBiometricButton = shouldShowBiometricButton(
                mode = LockPasswordMode.ENTER,
                input = "",
                remainingMinutes = null
            )
        )

        emitResult(LockPasswordResult.Success)
    }

    private fun handleEnterPin(input: String) {
        val savedPin = repository.getPin()

        if (savedPin == input) {
            repository.setErrorCount(0)
            repository.clearLockTimestamp()

            _uiState.value = _uiState.value.copy(
                input = "",
                error = null,
                attemptsLeft = null,
                remainingMinutes = null,
                showBiometricButton = shouldShowBiometricButton(
                    mode = LockPasswordMode.ENTER,
                    input = "",
                    remainingMinutes = null
                )
            )

            emitResult(LockPasswordResult.Success)
            return
        }

        val newErrorCount = repository.getErrorCount() + 1
        repository.setErrorCount(newErrorCount)
        repository.saveLockTimestamp(System.currentTimeMillis())

        val remainingMinutes = LockDurationPolicy.getRemainingMinutes(
            lockTimestamp = repository.getLockTimestamp(),
            errorCycle = newErrorCount
        )

        _uiState.value = _uiState.value.copy(
            input = "",
            error = if (remainingMinutes != null) {
                LockPasswordError.LOCKED
            } else {
                LockPasswordError.WRONG_PIN
            },
            remainingMinutes = remainingMinutes,
            attemptsLeft = null,
            showBiometricButton = false
        )

        if (remainingMinutes != null) {
            emitResult(LockPasswordResult.Locked(remainingMinutes))
        } else {
            emitResult(LockPasswordResult.InvalidPin())
        }
    }

    private fun shouldShowBiometricButton(
        mode: LockPasswordMode,
        input: String,
        remainingMinutes: Long?
    ): Boolean {
        return repository.hasPin() &&
                isBiometricAvailable &&
                mode == LockPasswordMode.ENTER &&
                input.isEmpty() &&
                remainingMinutes == null
    }

    private fun emitResult(result: LockPasswordResult) {
        viewModelScope.launch {
            _resultEvents.emit(result)
        }
    }
}