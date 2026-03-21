package com.app.lockpassword.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.lockpassword.R
import com.app.lockpassword.model.LockPasswordError
import com.app.lockpassword.model.LockPasswordMode
import com.app.lockpassword.model.LockPasswordUiState

private val keyButtonSize = 80.dp

@Composable
fun LockPasswordScreen(
    uiState: LockPasswordUiState,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onBiometricClick: () -> Unit,
    onBottomLeftClick: () -> Unit,
    onEmailChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val titleText = when (uiState.mode) {
        LockPasswordMode.CREATE -> stringResource(R.string.lock_title_create)
        LockPasswordMode.CONFIRM -> stringResource(R.string.lock_title_repeat)
        LockPasswordMode.ENTER -> stringResource(R.string.lock_title_enter)
    }

    val errorText = when (uiState.error) {
        LockPasswordError.PIN_MISMATCH -> stringResource(R.string.lock_error_pin_mismatch)
        LockPasswordError.WRONG_PIN -> stringResource(R.string.lock_error_wrong_pin)
        LockPasswordError.LOCKED -> stringResource(R.string.lock_error_locked)
        LockPasswordError.UNKNOWN -> stringResource(R.string.lock_error_unknown)
        null -> null
    }

    val bottomLeftText = when {
        uiState.remainingMinutes != null -> stringResource(R.string.lock_action_cancel)
        else -> ""
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                PinDotsRow(
                    enteredCount = uiState.input.length,
                    pinLength = uiState.pinLength
                )

                Spacer(modifier = Modifier.height(16.dp))

                errorText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                uiState.remainingMinutes?.let { minutes ->
                    LockStatusBadge(
                        text = pluralStringResource(
                            id = R.plurals.lock_remaining_minutes,
                            count = minutes.toInt(),
                            minutes
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                LockKeyboard(
                    bottomLeftText = bottomLeftText,
                    showBiometricButton = uiState.showBiometricButton,
                    hasInput = uiState.input.isNotEmpty(),
                    onDigitClick = onDigitClick,
                    onBackspaceClick = onBackspaceClick,
                    onBiometricClick = onBiometricClick,
                    onBottomLeftClick = onBottomLeftClick
                )
            }
        }
    }
}

@Composable
private fun PinDotsRow(
    enteredCount: Int,
    pinLength: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pinLength) { index ->
            val filled = index < enteredCount

            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        if (filled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun LockKeyboard(
    bottomLeftText: String,
    showBiometricButton: Boolean,
    hasInput: Boolean,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onBiometricClick: () -> Unit,
    onBottomLeftClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rightBottomItem = when {
        hasInput -> KeyItem.Backspace
        showBiometricButton -> KeyItem.Biometric
        else -> KeyItem.Empty
    }

    val items = listOf(
        KeyItem.Digit("1"),
        KeyItem.Digit("2"),
        KeyItem.Digit("3"),
        KeyItem.Digit("4"),
        KeyItem.Digit("5"),
        KeyItem.Digit("6"),
        KeyItem.Digit("7"),
        KeyItem.Digit("8"),
        KeyItem.Digit("9"),
        KeyItem.TextAction(bottomLeftText),
        KeyItem.Digit("0"),
        rightBottomItem
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        userScrollEnabled = false,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(items) { item ->
            when (item) {
                is KeyItem.Digit -> {
                    LockDigitButton(
                        text = item.value,
                        onClick = { onDigitClick(item.value) }
                    )
                }

                is KeyItem.TextAction -> {
                    LockTextActionButton(
                        text = item.text,
                        onClick = onBottomLeftClick
                    )
                }

                KeyItem.Backspace -> {
                    LockIconActionButton(
                        iconResId = R.drawable.backspace,
                        contentDescription = stringResource(R.string.lock_action_delete),
                        onClick = onBackspaceClick,
                        iconSize = 54.dp
                    )
                }

                KeyItem.Biometric -> {
                    LockIconActionButton(
                        iconResId = R.drawable.fingerprint,
                        contentDescription = stringResource(R.string.lock_action_biometric),
                        onClick = onBiometricClick,
                        iconSize = 64.dp
                    )
                }

                KeyItem.Empty -> {
                    LockEmptyButton()
                }
            }
        }
    }
}


@Composable
private fun LockIconActionButton(
    iconResId: Int,
    contentDescription: String,
    onClick: () -> Unit,
    iconSize: androidx.compose.ui.unit.Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(keyButtonSize),
            shape = CircleShape,
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            tonalElevation = 2.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = contentDescription,
                    modifier = Modifier.size(iconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LockDigitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(keyButtonSize),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            tonalElevation = 2.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}

@Composable
private fun LockTextActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        TextButton(
            onClick = onClick,
            enabled = text.isNotBlank(),
            modifier = Modifier.size(keyButtonSize)
        ) {
            Text(
                text = text,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun LockEmptyButton(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Spacer(modifier = Modifier.size(keyButtonSize))
    }
}

@Composable
private fun LockStatusBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Immutable
private sealed interface KeyItem {
    data class Digit(val value: String) : KeyItem
    data class TextAction(val text: String) : KeyItem
    data object Backspace : KeyItem
    data object Biometric : KeyItem
    data object Empty : KeyItem
}