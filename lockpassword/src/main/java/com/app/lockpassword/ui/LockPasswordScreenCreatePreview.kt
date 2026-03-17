package com.app.lockpassword.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.app.lockpassword.model.LockPasswordError
import com.app.lockpassword.model.LockPasswordMode
import com.app.lockpassword.model.LockPasswordUiState

@Preview(showBackground = true)
@Composable
private fun LockPasswordScreenCreatePreview() {
    MaterialTheme {
        LockPasswordScreen(
            uiState = LockPasswordUiState(
                mode = LockPasswordMode.CREATE,
                input = "12",
                pinLength = 6,
                error = null,
                remainingMinutes = null,
                showBiometricButton = false
            ),
            onDigitClick = {},
            onBackspaceClick = {},
            onBiometricClick = {},
            onBottomLeftClick = {},
            onEmailChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LockPasswordScreenUnlockPreview() {
    MaterialTheme {
        LockPasswordScreen(
            uiState = LockPasswordUiState(
                mode = LockPasswordMode.ENTER,
                input = "123",
                pinLength = 6,
                error = LockPasswordError.WRONG_PIN,
                remainingMinutes = null,
                showBiometricButton = true
            ),
            onDigitClick = {},
            onBackspaceClick = {},
            onBiometricClick = {},
            onBottomLeftClick = {},
            onEmailChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LockPasswordScreenLockedPreview() {
    MaterialTheme {
        LockPasswordScreen(
            uiState = LockPasswordUiState(
                mode = LockPasswordMode.ENTER,
                input = "",
                pinLength = 6,
                error = LockPasswordError.LOCKED,
                remainingMinutes = 5,
                showBiometricButton = false
            ),
            onDigitClick = {},
            onBackspaceClick = {},
            onBiometricClick = {},
            onBottomLeftClick = {},
            onEmailChange = {}
        )
    }
}