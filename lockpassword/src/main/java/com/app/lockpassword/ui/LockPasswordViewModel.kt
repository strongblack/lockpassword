package com.app.lockpassword.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.lockpassword.api.LockPasswordResult
import com.app.lockpassword.model.LockPasswordError
import com.app.lockpassword.model.LockPasswordMode
import com.app.lockpassword.model.LockPasswordUiState
import com.app.lockpassword.storage.LockPasswordPrefsRepository
import com.app.lockpassword.util.LockDurationPolicy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private val _uiEffects = MutableSharedFlow<LockPasswordUiEffect>(extraBufferCapacity = 1)
    val uiEffects: SharedFlow<LockPasswordUiEffect> = _uiEffects.asSharedFlow()

    private var firstPin: String? = null
    private var lockCountdownJob: Job? = null

    init {
        restoreLockState()
    }

    override fun onCleared() {
        lockCountdownJob?.cancel()
        super.onCleared()
    }

    fun onDigitClick(digit: Int) {
        val state = _uiState.value

        if (state.remainingLockSeconds != null) return
        if (state.input.length >= state.pinLength) return

        val newInput = state.input + digit.toString()

        _uiState.value = state.copy(
            input = newInput,
            error = null,
            attemptsLeft = null,
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
            attemptsLeft = null,
            showBiometricButton = shouldShowBiometricButton(
                mode = state.mode,
                input = newInput,
                remainingLockSeconds = state.remainingLockSeconds
            )
        )
    }

    fun onCancelClick() {
        emitResult(LockPasswordResult.Cancelled)
    }

    fun onBiometricSuccess() {
        clearSecurityState(resetLockoutLevel = true)

        _uiState.value = _uiState.value.copy(
            input = "",
            error = null,
            attemptsLeft = null,
            remainingLockSeconds = null,
            showBiometricButton = shouldShowBiometricButton(
                mode = _uiState.value.mode,
                input = "",
                remainingLockSeconds = null
            ),
            shouldAutoLaunchBiometric = false
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
                remainingLockSeconds = state.remainingLockSeconds
            )
        )
    }

    fun onEmailChange(value: String) {
        // пока не используем
    }

    fun refreshLockState() {
        restoreLockState()
    }

    fun onBiometricPromptShown() {
        _uiState.value = _uiState.value.copy(
            shouldAutoLaunchBiometric = false
        )
    }

    private fun restoreLockState() {
        val remainingSeconds = LockDurationPolicy.getRemainingLockSeconds(
            lockedUntilTimestamp = repository.getLockedUntilTimestamp()
        )

        if (remainingSeconds != null) {
            _uiState.value = _uiState.value.copy(
                input = "",
                error = LockPasswordError.LOCKED,
                attemptsLeft = 0,
                remainingLockSeconds = remainingSeconds,
                showBiometricButton = false,
                shouldAutoLaunchBiometric = false
            )

            startLockCountdown()
            return
        }

        lockCountdownJob?.cancel()
        repository.clearLockedUntilTimestamp()
        repository.setFailedAttempts(0)

        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            input = "",
            error = null,
            attemptsLeft = null,
            remainingLockSeconds = null,
            showBiometricButton = shouldShowBiometricButton(
                mode = currentState.mode,
                input = "",
                remainingLockSeconds = null
            ),
            shouldAutoLaunchBiometric = false
        )
    }

    private fun startLockCountdown() {
        lockCountdownJob?.cancel()
        lockCountdownJob = viewModelScope.launch {
            while (true) {
                val remainingSeconds = LockDurationPolicy.getRemainingLockSeconds(
                    lockedUntilTimestamp = repository.getLockedUntilTimestamp()
                )

                if (remainingSeconds == null) {
                    repository.clearLockedUntilTimestamp()
                    repository.setFailedAttempts(0)

                    val currentState = _uiState.value
                    _uiState.value = currentState.copy(
                        input = "",
                        error = null,
                        attemptsLeft = null,
                        remainingLockSeconds = null,
                        showBiometricButton = shouldShowBiometricButton(
                            mode = currentState.mode,
                            input = "",
                            remainingLockSeconds = null
                        ),
                        shouldAutoLaunchBiometric = false
                    )
                    break
                }

                _uiState.value = _uiState.value.copy(
                    input = "",
                    error = LockPasswordError.LOCKED,
                    attemptsLeft = 0,
                    remainingLockSeconds = remainingSeconds,
                    showBiometricButton = false,
                    shouldAutoLaunchBiometric = false
                )

                delay(1000L)
            }
        }
    }

    private fun handleCreatePin(input: String) {
        firstPin = input

        _uiState.value = _uiState.value.copy(
            input = "",
            mode = LockPasswordMode.CONFIRM,
            error = null,
            attemptsLeft = null,
            remainingLockSeconds = null,
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
                remainingLockSeconds = null,
                showBiometricButton = false,
                shouldAutoLaunchBiometric = false
            )

            emitUiEffect(LockPasswordUiEffect.WrongPinVibration)
            return
        }

        repository.savePin(input)
        clearSecurityState(resetLockoutLevel = true)

        firstPin = null

        _uiState.value = _uiState.value.copy(
            input = "",
            mode = LockPasswordMode.ENTER,
            error = null,
            attemptsLeft = null,
            remainingLockSeconds = null,
            showBiometricButton = shouldShowBiometricButton(
                mode = LockPasswordMode.ENTER,
                input = "",
                remainingLockSeconds = null
            ),
            shouldAutoLaunchBiometric = false
        )

        emitResult(LockPasswordResult.Success)
    }

    private fun handleEnterPin(input: String) {
        val savedPin = repository.getPin()

        if (savedPin == input) {
            clearSecurityState(resetLockoutLevel = true)

            _uiState.value = _uiState.value.copy(
                input = "",
                error = null,
                attemptsLeft = null,
                remainingLockSeconds = null,
                showBiometricButton = shouldShowBiometricButton(
                    mode = LockPasswordMode.ENTER,
                    input = "",
                    remainingLockSeconds = null
                ),
                shouldAutoLaunchBiometric = false
            )

            emitResult(LockPasswordResult.Success)
            return
        }

        val failedAttempts = repository.getFailedAttempts() + 1

        if (failedAttempts < LockDurationPolicy.MAX_ATTEMPTS_BEFORE_LOCK) {
            repository.setFailedAttempts(failedAttempts)

            val attemptsLeft = LockDurationPolicy.getAttemptsLeft(failedAttempts)

            _uiState.value = _uiState.value.copy(
                input = "",
                error = LockPasswordError.WRONG_PIN,
                attemptsLeft = attemptsLeft,
                remainingLockSeconds = null,
                showBiometricButton = false,
                shouldAutoLaunchBiometric = false
            )

            emitUiEffect(LockPasswordUiEffect.WrongPinVibration)
            emitResult(LockPasswordResult.InvalidPin(attemptsLeft))
            return
        }

        val nextLockoutLevel = repository.getLockoutLevel() + 1
        val lockedUntil = System.currentTimeMillis() +
                LockDurationPolicy.getLockDurationMillis(nextLockoutLevel)

        repository.setLockoutLevel(nextLockoutLevel)
        repository.setFailedAttempts(0)
        repository.setLockedUntilTimestamp(lockedUntil)

        val remainingSeconds = LockDurationPolicy.getRemainingLockSeconds(
            lockedUntilTimestamp = lockedUntil
        ) ?: 0L

        _uiState.value = _uiState.value.copy(
            input = "",
            error = LockPasswordError.LOCKED,
            attemptsLeft = 0,
            remainingLockSeconds = remainingSeconds,
            showBiometricButton = false,
            shouldAutoLaunchBiometric = false
        )

        startLockCountdown()
        emitUiEffect(LockPasswordUiEffect.LockedVibration)
        emitResult(LockPasswordResult.Locked(remainingSeconds))
    }

    private fun shouldShowBiometricButton(
        mode: LockPasswordMode,
        input: String,
        remainingLockSeconds: Long?
    ): Boolean {
        return repository.hasPin() &&
                isBiometricAvailable &&
                mode == LockPasswordMode.ENTER &&
                input.isEmpty() &&
                remainingLockSeconds == null
    }

    private fun clearSecurityState(resetLockoutLevel: Boolean) {
        lockCountdownJob?.cancel()
        repository.clearSecurityState(resetLockoutLevel = resetLockoutLevel)
    }

    private fun emitResult(result: LockPasswordResult) {
        viewModelScope.launch {
            _resultEvents.emit(result)
        }
    }

    private fun emitUiEffect(effect: LockPasswordUiEffect) {
        _uiEffects.tryEmit(effect)
    }
}