package com.app.lockpassword.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LockPasswordViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        LockPasswordUiState(),
    )

    val uiState: StateFlow<LockPasswordUiState> = _uiState.asStateFlow()
}