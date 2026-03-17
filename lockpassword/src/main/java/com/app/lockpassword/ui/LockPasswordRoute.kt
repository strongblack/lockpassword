package com.app.lockpassword.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.app.lockpassword.api.LockPasswordResult

@Composable
fun LockPasswordRoute(
    viewModel: LockPasswordViewModel,
    onResult: (LockPasswordResult) -> Unit,
    onBiometricRequest: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.resultEvents.collect { result ->
            onResult(result)
        }
    }

    LockPasswordScreen(
        uiState = uiState,
        onDigitClick = { digit -> viewModel.onDigitClick(digit.toInt()) },
        onBackspaceClick = viewModel::onBackspaceClick,
        onBiometricClick = onBiometricRequest,
        onBottomLeftClick = viewModel::onCancelClick,
        onEmailChange = viewModel::onEmailChange
    )
}