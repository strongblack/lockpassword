package com.app.lockpassword.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.lockpassword.domain.LockPasswordMode
import com.app.lockpassword.presentation.LockPasswordUiState

@Composable
fun LockPasswordScreen(
    uiState: LockPasswordUiState,
) {
    Scaffold { paddingValues: PaddingValues ->
        LockPasswordContent(
            paddingValues = paddingValues,
            uiState = uiState,
        )
    }
}

@Composable
private fun LockPasswordContent(
    paddingValues: PaddingValues,
    uiState: LockPasswordUiState,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = uiState.title,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = "Mode: ${uiState.mode}",
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = uiState.description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LockPasswordScreenPreview() {
    MaterialTheme {
        LockPasswordScreen(
            uiState = LockPasswordUiState(
                title = "LockPassword",
                description = "Экран библиотеки уже работает. Следующим шагом добавим поле ввода PIN-кода.",
                mode = LockPasswordMode.CHECK,
            ),
        )
    }
}