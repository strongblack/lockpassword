package com.app.lockpassword.api

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.app.lockpassword.presentation.LockPasswordViewModel
import com.app.lockpassword.ui.LockPasswordScreen

class LockPasswordActivity : ComponentActivity() {

    private val lockPasswordViewModel: LockPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by lockPasswordViewModel.uiState.collectAsState()

            MaterialTheme {
                LockPasswordScreen(
                    uiState = uiState,
                )
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, LockPasswordActivity::class.java)
        }
    }
}