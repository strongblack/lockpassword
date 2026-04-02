package ru.devasn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.devasn.lockpassword.R
import ru.devasn.model.LockPasswordError
import ru.devasn.model.LockPasswordMode
import ru.devasn.model.LockPasswordUiState
import ru.devasn.ui.theme.LockPasswordThemeTokens
import kotlin.math.min

@Composable
fun LockPasswordScreen(
    uiState: LockPasswordUiState,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onBiometricClick: () -> Unit,
    onBottomLeftClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLocked = uiState.remainingLockSeconds != null
    val colors = LockPasswordThemeTokens.colors
    val sizes = LockPasswordThemeTokens.sizes

    val titleText = when (uiState.mode) {
        LockPasswordMode.CREATE -> stringResource(R.string.lock_title_create)
        LockPasswordMode.CONFIRM -> stringResource(R.string.lock_title_repeat)
        LockPasswordMode.ENTER -> stringResource(R.string.lock_title_enter)
    }

    val messageTitle: String?
    val messageDescription: String?
    val messageIsError: Boolean

    when {
        uiState.error == LockPasswordError.LOCKED && uiState.remainingLockSeconds != null -> {
            messageTitle = stringResource(R.string.lock_error_locked)
            messageDescription = stringResource(
                R.string.lock_message_try_later,
                formatLockTime(uiState.remainingLockSeconds)
            )
            messageIsError = true
        }

        uiState.error == LockPasswordError.WRONG_PIN && uiState.attemptsLeft != null -> {
            messageTitle = stringResource(R.string.lock_error_wrong_pin)
            messageDescription = formatAttemptsLeftText(uiState.attemptsLeft)
            messageIsError = true
        }

        uiState.error == LockPasswordError.PIN_MISMATCH -> {
            messageTitle = stringResource(R.string.lock_error_pin_mismatch)
            messageDescription = stringResource(R.string.lock_message_pin_mismatch_repeat)
            messageIsError = true
        }

        uiState.error == LockPasswordError.UNKNOWN -> {
            messageTitle = stringResource(R.string.lock_error_unknown)
            messageDescription = stringResource(R.string.lock_message_try_again)
            messageIsError = true
        }

        else -> {
            messageTitle = null
            messageDescription = null
            messageIsError = false
        }
    }

    val showErrorBlock = messageTitle != null

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.screenBackground
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            val metrics = lockScreenMetrics(
                maxWidth = maxWidth,
                maxHeight = maxHeight,
                buttonScale = sizes.buttonScale
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = metrics.contentMaxWidth)
                        .padding(horizontal = metrics.horizontalPadding)
                        .imePadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LockHeroHeader(
                        isLocked = isLocked,
                        outerSize = metrics.heroOuterSize,
                        innerSize = metrics.heroInnerSize,
                        iconSize = metrics.heroIconSize
                    )

                    Spacer(modifier = Modifier.height(metrics.spaceAfterHeader))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = metrics.messageBlockMinHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        if (showErrorBlock) {
                            LockMessageCard(
                                title = messageTitle,
                                description = messageDescription.orEmpty(),
                                isError = messageIsError,
                                cornerRadius = metrics.messageCornerRadius,
                                horizontalPadding = metrics.messageHorizontalPadding,
                                verticalPadding = metrics.messageVerticalPadding,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = titleText,
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center,
                                    color = colors.title
                                )

                                Spacer(modifier = Modifier.height(metrics.spaceBetweenTitleAndDots))

                                PinDotsRow(
                                    enteredCount = uiState.input.length,
                                    pinLength = uiState.pinLength,
                                    dotSize = metrics.dotSize,
                                    dotSpacing = metrics.dotSpacing
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(metrics.spaceBeforeKeyboard))

                    LockKeyboard(
                        bottomLeftText = if (isLocked) {
                            stringResource(R.string.lock_action_cancel)
                        } else {
                            ""
                        },
                        showBiometricButton = uiState.showBiometricButton,
                        hasInput = uiState.input.isNotEmpty(),
                        keypadEnabled = !isLocked,
                        keyButtonSize = metrics.keyButtonSize,
                        keyGridSpacing = metrics.keyGridSpacing,
                        keyGridPadding = metrics.keyGridPadding,
                        backspaceIconSize = metrics.backspaceIconSize,
                        biometricIconSize = metrics.biometricIconSize,
                        onDigitClick = onDigitClick,
                        onBackspaceClick = onBackspaceClick,
                        onBiometricClick = onBiometricClick,
                        onBottomLeftClick = onBottomLeftClick
                    )
                }
            }
        }
    }
}

private data class LockScreenMetrics(
    val contentMaxWidth: Dp,
    val horizontalPadding: Dp,
    val heroOuterSize: Dp,
    val heroInnerSize: Dp,
    val heroIconSize: Dp,
    val spaceAfterHeader: Dp,
    val messageBlockMinHeight: Dp,
    val messageCornerRadius: Dp,
    val messageHorizontalPadding: Dp,
    val messageVerticalPadding: Dp,
    val spaceBetweenTitleAndDots: Dp,
    val spaceBeforeKeyboard: Dp,
    val dotSize: Dp,
    val dotSpacing: Dp,
    val keyButtonSize: Dp,
    val keyGridSpacing: Dp,
    val keyGridPadding: Dp,
    val backspaceIconSize: Dp,
    val biometricIconSize: Dp
)

private fun lockScreenMetrics(
    maxWidth: Dp,
    maxHeight: Dp,
    buttonScale: Float
): LockScreenMetrics {
    val widthScale = (maxWidth.value / 360f).coerceIn(0.92f, 1.12f)
    val heightScale = (maxHeight.value / 780f).coerceIn(0.88f, 1.06f)
    val scale = min(widthScale, heightScale)

    val compactWidth = maxWidth < 360.dp
    val compactHeight = maxHeight < 700.dp
    val normalizedButtonScale = buttonScale.coerceIn(0.85f, 1.15f)

    fun scaled(value: Float): Dp = (value * scale).dp
    fun scaledBy(base: Dp, factor: Float): Dp = (base.value * factor).dp

    return LockScreenMetrics(
        contentMaxWidth = 420.dp,
        horizontalPadding = if (compactWidth) 16.dp else 24.dp,
        heroOuterSize = scaled(108f).coerceIn(96.dp, 116.dp),
        heroInnerSize = scaled(84f).coerceIn(74.dp, 92.dp),
        heroIconSize = scaled(42f).coerceIn(36.dp, 46.dp),
        spaceAfterHeader = if (compactHeight) 12.dp else scaled(16f),
        messageBlockMinHeight = if (compactHeight) 86.dp else scaled(100f),
        messageCornerRadius = scaled(20f).coerceIn(16.dp, 22.dp),
        messageHorizontalPadding = scaled(18f).coerceIn(14.dp, 20.dp),
        messageVerticalPadding = scaled(14f).coerceIn(12.dp, 16.dp),
        spaceBetweenTitleAndDots = if (compactHeight) 14.dp else scaled(20f),
        spaceBeforeKeyboard = if (compactHeight) 10.dp else scaled(18f),
        dotSize = scaled(14f).coerceIn(12.dp, 16.dp),
        dotSpacing = scaled(8f).coerceIn(6.dp, 10.dp),
        keyButtonSize = scaledBy(
            scaled(80f).coerceIn(68.dp, 88.dp),
            normalizedButtonScale
        ).coerceIn(64.dp, 96.dp),

        keyGridSpacing = scaled(12f).coerceIn(8.dp, 14.dp),
        keyGridPadding = scaled(8f).coerceIn(4.dp, 10.dp),

        backspaceIconSize = scaledBy(
            scaled(50f).coerceIn(42.dp, 58.dp),
            normalizedButtonScale
        ).coerceIn(40.dp, 64.dp),

        biometricIconSize = scaledBy(
            scaled(60f).coerceIn(50.dp, 68.dp),
            normalizedButtonScale
        ).coerceIn(48.dp, 74.dp)
    )
}

@Composable
private fun LockHeroHeader(
    isLocked: Boolean,
    outerSize: Dp,
    innerSize: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier
) {
    val colors = LockPasswordThemeTokens.colors

    Box(
        modifier = modifier.size(outerSize),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(outerSize)
                .clip(CircleShape)
                .background(colors.lockOuterCircle)
        )

        Surface(
            modifier = Modifier.size(innerSize),
            shape = CircleShape,
            color = if (isLocked) {
                colors.errorBackground
            } else {
                colors.lockInnerCircle
            },
            tonalElevation = 4.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.lock),
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = if (isLocked) {
                        colors.errorText
                    } else {
                        colors.lockIcon
                    }
                )
            }
        }
    }
}

@Composable
private fun LockMessageCard(
    title: String,
    description: String,
    isError: Boolean,
    cornerRadius: Dp,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    modifier: Modifier = Modifier
) {
    val colors = LockPasswordThemeTokens.colors

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        color = if (isError) {
            colors.errorBackground
        } else {
            colors.lockOuterCircle
        },
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = horizontalPadding,
                    vertical = verticalPadding
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                color = if (isError) {
                    colors.errorText
                } else {
                    colors.title
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = colors.messageText
            )
        }
    }
}

@Composable
private fun PinDotsRow(
    enteredCount: Int,
    pinLength: Int,
    dotSize: Dp,
    dotSpacing: Dp,
    modifier: Modifier = Modifier
) {
    val colors = LockPasswordThemeTokens.colors

    Row(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pinLength) { index ->
            val filled = index < enteredCount

            Box(
                modifier = Modifier
                    .padding(horizontal = dotSpacing)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(
                        if (filled) {
                            colors.dotFilled
                        } else {
                            colors.dotEmpty
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = colors.dotBorder,
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
    keypadEnabled: Boolean,
    keyButtonSize: Dp,
    keyGridSpacing: Dp,
    keyGridPadding: Dp,
    backspaceIconSize: Dp,
    biometricIconSize: Dp,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onBiometricClick: () -> Unit,
    onBottomLeftClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rightBottomItem = when {
        !keypadEnabled -> KeyItem.Empty
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
        horizontalArrangement = Arrangement.spacedBy(keyGridSpacing),
        verticalArrangement = Arrangement.spacedBy(keyGridSpacing),
        contentPadding = PaddingValues(keyGridPadding)
    ) {
        items(items) { item ->
            when (item) {
                is KeyItem.Digit -> {
                    LockDigitButton(
                        text = item.value,
                        enabled = keypadEnabled,
                        buttonSize = keyButtonSize,
                        onClick = { onDigitClick(item.value) }
                    )
                }

                is KeyItem.TextAction -> {
                    LockTextActionButton(
                        text = item.text,
                        buttonSize = keyButtonSize,
                        onClick = onBottomLeftClick
                    )
                }

                KeyItem.Backspace -> {
                    LockIconActionButton(
                        iconResId = R.drawable.backspace,
                        contentDescription = "",
                        enabled = keypadEnabled,
                        buttonSize = keyButtonSize,
                        iconSize = backspaceIconSize,
                        onClick = onBackspaceClick
                    )
                }

                KeyItem.Biometric -> {
                    LockIconActionButton(
                        iconResId = R.drawable.fingerprint,
                        contentDescription = "",
                        enabled = keypadEnabled,
                        buttonSize = keyButtonSize,
                        iconSize = biometricIconSize,
                        onClick = onBiometricClick
                    )
                }

                KeyItem.Empty -> {
                    LockEmptyButton(buttonSize = keyButtonSize)
                }
            }
        }
    }
}

@Composable
private fun LockIconActionButton(
    iconResId: Int,
    contentDescription: String,
    enabled: Boolean,
    buttonSize: Dp,
    onClick: () -> Unit,
    iconSize: Dp,
    modifier: Modifier = Modifier
) {
    val colors = LockPasswordThemeTokens.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .alpha(if (enabled) 1f else 0.35f),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(buttonSize),
            shape = CircleShape,
            color = Color.Transparent,
            contentColor = colors.actionIcon,
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
                    tint = colors.actionIcon
                )
            }
        }
    }
}

@Composable
private fun LockDigitButton(
    text: String,
    enabled: Boolean,
    buttonSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LockPasswordThemeTokens.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .alpha(if (enabled) 1f else 0.45f),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(buttonSize),
            shape = CircleShape,
            color = colors.keypadButtonBackground,
            contentColor = colors.keypadDigit,
            tonalElevation = 2.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.keypadDigit
                )
            }
        }
    }
}

@Composable
private fun LockTextActionButton(
    text: String,
    buttonSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LockPasswordThemeTokens.colors
    val enabled = text.isNotBlank()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        TextButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(buttonSize)
        ) {
            Text(
                text = text,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.actionText.copy(alpha = if (enabled) 1f else 0.35f)
            )
        }
    }
}

@Composable
private fun LockEmptyButton(
    buttonSize: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Spacer(modifier = Modifier.size(buttonSize))
    }
}

@Composable
private fun formatAttemptsLeftText(attemptsLeft: Int): String {
    return pluralStringResource(
        id = R.plurals.lock_attempts_left,
        count = attemptsLeft,
        attemptsLeft
    )
}

@Composable
private fun formatLockTime(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60

    return when {
        minutes > 0 && secs > 0 -> {
            stringResource(
                R.string.lock_time_minutes_seconds,
                minutes,
                secs
            )
        }

        minutes > 0 -> {
            stringResource(R.string.lock_time_minutes, minutes)
        }

        else -> {
            stringResource(R.string.lock_time_seconds, secs)
        }
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