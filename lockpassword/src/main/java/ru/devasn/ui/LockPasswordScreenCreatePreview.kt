package ru.devasn.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ru.devasn.model.LockPasswordError
import ru.devasn.model.LockPasswordMode
import ru.devasn.model.LockPasswordUiState
import ru.devasn.ui.theme.LockPasswordTheme

@Preview(showBackground = true)
@Composable
private fun LockPasswordScreenCreatePreview() {
    LockPasswordTheme {
        LockPasswordScreen(
            uiState = LockPasswordUiState(
                mode = LockPasswordMode.CREATE,
                input = "12",
                pinLength = 6,
                error = null,
                remainingLockSeconds = null,
                showBiometricButton = false
            ),
            onDigitClick = {},
            onBackspaceClick = {},
            onBiometricClick = {},
            onBottomLeftClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LockPasswordScreenUnlockPreview() {
    LockPasswordTheme {
        LockPasswordScreen(
            uiState = LockPasswordUiState(
                mode = LockPasswordMode.ENTER,
                input = "123",
                pinLength = 6,
                error = LockPasswordError.WRONG_PIN,
                remainingLockSeconds = null,
                showBiometricButton = true
            ),
            onDigitClick = {},
            onBackspaceClick = {},
            onBiometricClick = {},
            onBottomLeftClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LockPasswordScreenLockedPreview() {
    LockPasswordTheme {
        LockPasswordScreen(
            uiState = LockPasswordUiState(
                mode = LockPasswordMode.ENTER,
                input = "",
                pinLength = 6,
                error = LockPasswordError.LOCKED,
                remainingLockSeconds = 5,
                showBiometricButton = false
            ),
            onDigitClick = {},
            onBackspaceClick = {},
            onBiometricClick = {},
            onBottomLeftClick = {}

        )
    }
}