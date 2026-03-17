package com.app.lockpassword.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.app.lockpassword.R

@Composable
fun PinKeyboard(
    showBiometricButton: Boolean,
    onDigitClick: (Int) -> Unit,
    onBackspaceClick: () -> Unit,
    onBiometricClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NumberRow(1, 2, 3, onDigitClick)
        NumberRow(4, 5, 6, onDigitClick)
        NumberRow(7, 8, 9, onDigitClick)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showBiometricButton) {
                Button(
                    onClick = onBiometricClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Text(text = stringResource(R.string.lock_action_biometric))
                }
            } else {
                TextButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Text(text = "")
                }
            }

            Button(
                onClick = { onDigitClick(0) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(text = stringResource(R.string.lock_digit_zero))
            }

            Button(
                onClick = onBackspaceClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(text = stringResource(R.string.lock_action_delete))
            }
        }
    }
}

@Composable
private fun NumberRow(
    a: Int,
    b: Int,
    c: Int,
    onDigitClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DigitButton(
            number = a,
            onDigitClick = onDigitClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
        )

        DigitButton(
            number = b,
            onDigitClick = onDigitClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
        )

        DigitButton(
            number = c,
            onDigitClick = onDigitClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
        )
    }
}

@Composable
private fun DigitButton(
    number: Int,
    onDigitClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { onDigitClick(number) },
        modifier = modifier
    ) {
        Text(text = number.toString())
    }
}