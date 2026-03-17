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

    private val _uiState = MutableStateFlow(
        LockPasswordUiState(
            mode = if (repository.hasPin()) {
                LockPasswordMode.ENTER
            } else {
                LockPasswordMode.CREATE
            },
            isBiometricAvailable = isBiometricAvailable,
            showBiometricButton = repository.hasPin() && isBiometricAvailable
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
            error = null
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

        _uiState.value = state.copy(
            input = state.input.dropLast(1),
            error = null
        )
    }

    fun onCancelClick() {
        emitResult(LockPasswordResult.Cancelled)
    }

    fun onBiometricSuccess() {
        emitResult(LockPasswordResult.BiometricSuccess)
    }
    fun onEmailChange(value: String) {
        // пока поле email в текущем uiState не используется
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
                showBiometricButton = false
            )
            emitResult(LockPasswordResult.Locked(remainingMinutes))
        } else {
            repository.clearLockTimestamp()
            _uiState.value = _uiState.value.copy(
                remainingMinutes = null,
                error = null,
                showBiometricButton = repository.hasPin() && isBiometricAvailable
            )
        }
    }

    private fun handleCreatePin(input: String) {
        firstPin = input
        _uiState.value = _uiState.value.copy(
            input = "",
            mode = LockPasswordMode.CONFIRM,
            error = null,
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
                showBiometricButton = false
            )
            return
        }

        repository.savePin(input)
        repository.setErrorCount(0)
        repository.clearLockTimestamp()

        _uiState.value = _uiState.value.copy(
            input = "",
            mode = LockPasswordMode.ENTER,
            error = null,
            attemptsLeft = null,
            remainingMinutes = null,
            showBiometricButton = isBiometricAvailable
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
                showBiometricButton = isBiometricAvailable
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

    private fun emitResult(result: LockPasswordResult) {
        viewModelScope.launch {
            _resultEvents.emit(result)
        }
    }
}