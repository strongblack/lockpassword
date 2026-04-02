package ru.devasn.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import ru.devasn.api.LockPasswordResult
import ru.devasn.model.LockPasswordMode

@Composable
fun LockPasswordRoute(
    viewModel: LockPasswordViewModel,
    onResult: (LockPasswordResult) -> Unit,
    onBiometricRequest: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.resultEvents.collect { result ->
            onResult(result)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                LockPasswordUiEffect.WrongPinVibration -> {
                    context.vibrateWrongPin()
                }

                LockPasswordUiEffect.LockedVibration -> {
                    context.vibrateLockedPin()
                }
            }
        }
    }

    LaunchedEffect(
        uiState.shouldAutoLaunchBiometric,
        uiState.mode,
        uiState.input,
        uiState.showBiometricButton,
        uiState.remainingLockSeconds
    ) {
        if (
            uiState.shouldAutoLaunchBiometric &&
            uiState.mode == LockPasswordMode.ENTER &&
            uiState.input.isEmpty() &&
            uiState.showBiometricButton &&
            uiState.remainingLockSeconds == null
        ) {
            viewModel.onBiometricPromptShown()
            onBiometricRequest()
        }
    }

    LockPasswordScreen(
        uiState = uiState,
        onDigitClick = { digit -> viewModel.onDigitClick(digit.toInt()) },
        onBackspaceClick = viewModel::onBackspaceClick,
        onBiometricClick = onBiometricRequest,
        onBottomLeftClick = viewModel::onCancelClick,
    )
}

private fun Context.vibrateWrongPin() {
    val vibrator = getAppVibrator() ?: return

    vibrator.vibrate(
        VibrationEffect.createOneShot(120L, VibrationEffect.DEFAULT_AMPLITUDE)
    )
}

private fun Context.vibrateLockedPin() {
    val vibrator = getAppVibrator() ?: return

    vibrator.vibrate(
        VibrationEffect.createWaveform(
            longArrayOf(0L, 80L, 60L, 140L),
            -1
        )
    )
}

private fun Context.getAppVibrator(): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
}