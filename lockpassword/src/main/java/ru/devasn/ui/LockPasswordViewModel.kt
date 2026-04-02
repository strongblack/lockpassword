package ru.devasn.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.devasn.api.LockPasswordResult
import ru.devasn.model.LockPasswordError
import ru.devasn.model.LockPasswordMode
import ru.devasn.model.LockPasswordUiState
import ru.devasn.model.LockPasswordVerifyResult
import ru.devasn.storage.LockPasswordPrefsRepository
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
    private val isBiometricAvailable: Boolean,
    configuredPinLength: Int
) : ViewModel() {

    private val hasSavedPinAtStart: Boolean = repository.hasPin()
    private val effectivePinLengthAtStart: Int = configuredPinLength.coerceIn(4, 6)

    private val _uiState = MutableStateFlow(
        LockPasswordUiState(
            mode = if (hasSavedPinAtStart) {
                LockPasswordMode.ENTER
            } else {
                LockPasswordMode.CREATE
            },
            pinLength = effectivePinLengthAtStart,
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
        _uiState.value = state.withTypingInput(newInput)

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
        _uiState.value = state.withTypingInput(newInput)
    }

    fun onCancelClick() {
        emitResult(LockPasswordResult.cancelled())
    }

    fun onBiometricSuccess() {
        clearSecurityState()
        clearInputAndErrorState()
        emitResult(LockPasswordResult.success())
    }

    fun onBiometricError() {
        _uiState.value = _uiState.value.withResolvedBiometricVisibility()
    }

    fun onBiometricPromptShown() {
        _uiState.value = _uiState.value.copy(
            shouldAutoLaunchBiometric = false
        )
    }

    private fun restoreLockState() {
        val remainingLockMs = repository.getRemainingLockoutMs()

        if (remainingLockMs > 0L) {
            applyLockedState(remainingLockMs)
            startLockCountdown()
            return
        }

        lockCountdownJob?.cancel()
        clearInputAndErrorState()
    }

    private fun startLockCountdown() {
        lockCountdownJob?.cancel()
        lockCountdownJob = viewModelScope.launch {
            while (true) {
                val remainingLockMs = repository.getRemainingLockoutMs()

                if (remainingLockMs <= 0L) {
                    clearInputAndErrorState()
                    break
                }

                applyLockedState(remainingLockMs)
                delay(1000L)
            }
        }
    }

    private fun handleCreatePin(input: String) {
        firstPin = input

        _uiState.value = _uiState.value.toBaseState(
            mode = LockPasswordMode.CONFIRM,
            pinLength = effectivePinLengthAtStart
        )
    }

    private fun handleConfirmPin(input: String) {
        if (firstPin != input) {
            firstPin = null

            _uiState.value = _uiState.value.toBaseState(
                mode = LockPasswordMode.CREATE,
                pinLength = effectivePinLengthAtStart,
                error = LockPasswordError.PIN_MISMATCH,
                showBiometricButton = false
            )

            emitUiEffect(LockPasswordUiEffect.WrongPinVibration)
            return
        }

        viewModelScope.launch {
            repository.savePin(input)
            clearSecurityState()
            firstPin = null

            _uiState.value = _uiState.value.toBaseState(
                mode = LockPasswordMode.ENTER,
                pinLength = input.length.coerceIn(4, 6)
            )

            emitResult(LockPasswordResult.pinCreated())
        }
    }

    private fun handleEnterPin(input: String) {
        viewModelScope.launch {
            when (val result = repository.verifyPin(input)) {
                LockPasswordVerifyResult.Success -> {
                    clearSecurityState()
                    clearInputAndErrorState()
                    emitResult(LockPasswordResult.success())
                }

                LockPasswordVerifyResult.NoPinConfigured -> {
                    firstPin = null

                    _uiState.value = _uiState.value.toBaseState(
                        mode = LockPasswordMode.CREATE,
                        pinLength = effectivePinLengthAtStart,
                        showBiometricButton = false
                    )
                }

                is LockPasswordVerifyResult.Invalid -> {
                    _uiState.value = _uiState.value.toBaseState(
                        error = LockPasswordError.WRONG_PIN,
                        attemptsLeft = result.remainingAttemptsBeforeLockout,
                        showBiometricButton = false
                    )

                    emitUiEffect(LockPasswordUiEffect.WrongPinVibration)
                }

                is LockPasswordVerifyResult.Locked -> {
                    _uiState.value = _uiState.value.toBaseState(
                        error = LockPasswordError.LOCKED,
                        attemptsLeft = 0,
                        remainingLockSeconds = toRemainingSeconds(result.remainingLockoutMs),
                        showBiometricButton = false
                    )

                    startLockCountdown()
                    emitUiEffect(LockPasswordUiEffect.LockedVibration)
                }
            }
        }
    }

    private fun applyLockedState(remainingLockMs: Long) {
        _uiState.value = _uiState.value.toBaseState(
            error = LockPasswordError.LOCKED,
            attemptsLeft = 0,
            remainingLockSeconds = toRemainingSeconds(remainingLockMs),
            showBiometricButton = false
        )
    }

    private fun clearInputAndErrorState() {
        _uiState.value = _uiState.value.toBaseState()
    }

    private fun LockPasswordUiState.withTypingInput(newInput: String): LockPasswordUiState {
        return copy(
            input = newInput,
            error = null,
            attemptsLeft = null,
            showBiometricButton = shouldShowBiometricButton(
                mode = mode,
                input = newInput,
                remainingLockSeconds = remainingLockSeconds
            ),
            shouldAutoLaunchBiometric = false
        )
    }

    private fun LockPasswordUiState.withResolvedBiometricVisibility(): LockPasswordUiState {
        return copy(
            showBiometricButton = shouldShowBiometricButton(
                mode = mode,
                input = input,
                remainingLockSeconds = remainingLockSeconds
            ),
            shouldAutoLaunchBiometric = false
        )
    }

    private fun LockPasswordUiState.toBaseState(
        mode: LockPasswordMode = this.mode,
        pinLength: Int = this.pinLength,
        input: String = "",
        error: LockPasswordError? = null,
        attemptsLeft: Int? = null,
        remainingLockSeconds: Long? = null,
        showBiometricButton: Boolean = shouldShowBiometricButton(
            mode = mode,
            input = input,
            remainingLockSeconds = remainingLockSeconds
        ),
        shouldAutoLaunchBiometric: Boolean = false
    ): LockPasswordUiState {
        return copy(
            mode = mode,
            input = input,
            pinLength = pinLength,
            error = error,
            attemptsLeft = attemptsLeft,
            remainingLockSeconds = remainingLockSeconds,
            showBiometricButton = showBiometricButton,
            shouldAutoLaunchBiometric = shouldAutoLaunchBiometric
        )
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

    private fun clearSecurityState() {
        lockCountdownJob?.cancel()
        repository.resetLockState()
    }

    private fun toRemainingSeconds(remainingLockMs: Long): Long {
        return ((remainingLockMs + 999L) / 1000L).coerceAtLeast(1L)
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