package ru.devasn.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import ru.devasn.config.LockPasswordColorsConfig
import ru.devasn.config.LockPasswordDefaults
import ru.devasn.config.LockPasswordUiConfig

@Immutable
data class LockPasswordResolvedColors(
    val screenBackground: Color,
    val title: Color,

    val keypadButtonBackground: Color,
    val keypadDigit: Color,

    val lockOuterCircle: Color,
    val lockInnerCircle: Color,
    val lockIcon: Color,

    val dotEmpty: Color,
    val dotFilled: Color,
    val dotBorder: Color,

    val actionIcon: Color,
    val actionText: Color,

    val errorText: Color,
    val errorBackground: Color,

    val messageText: Color
)

@Immutable
data class LockPasswordResolvedSizes(
    val buttonScale: Float
)

private val LocalLockPasswordColors = staticCompositionLocalOf {
    LockPasswordDefaults.lightColors().toResolvedColors()
}

private val LocalLockPasswordSizes = staticCompositionLocalOf {
    LockPasswordResolvedSizes(buttonScale = 1.0f)
}

object LockPasswordThemeTokens {
    val colors: LockPasswordResolvedColors
        @Composable
        get() = LocalLockPasswordColors.current

    val sizes: LockPasswordResolvedSizes
        @Composable
        get() = LocalLockPasswordSizes.current
}

@Composable
fun LockPasswordTheme(
    uiConfig: LockPasswordUiConfig = LockPasswordDefaults.uiConfig(),
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val resolvedColors = if (darkTheme) {
        uiConfig.darkColors.toResolvedColors()
    } else {
        uiConfig.lightColors.toResolvedColors()
    }

    val resolvedSizes = LockPasswordResolvedSizes(
        buttonScale = uiConfig.sizes.normalizedButtonScale()
    )

    val materialColors = if (darkTheme) {
        darkColorScheme(
            primary = resolvedColors.dotFilled,
            onPrimary = resolvedColors.screenBackground,
            primaryContainer = resolvedColors.keypadButtonBackground,
            onPrimaryContainer = resolvedColors.keypadDigit,
            secondary = resolvedColors.lockIcon,
            onSecondary = resolvedColors.screenBackground,
            background = resolvedColors.screenBackground,
            onBackground = resolvedColors.title,
            surface = resolvedColors.screenBackground,
            onSurface = resolvedColors.title,
            surfaceVariant = resolvedColors.lockOuterCircle,
            onSurfaceVariant = resolvedColors.messageText,
            outline = resolvedColors.dotBorder,
            error = resolvedColors.errorText,
            errorContainer = resolvedColors.errorBackground
        )
    } else {
        lightColorScheme(
            primary = resolvedColors.dotFilled,
            onPrimary = resolvedColors.screenBackground,
            primaryContainer = resolvedColors.keypadButtonBackground,
            onPrimaryContainer = resolvedColors.keypadDigit,
            secondary = resolvedColors.lockIcon,
            onSecondary = resolvedColors.screenBackground,
            background = resolvedColors.screenBackground,
            onBackground = resolvedColors.title,
            surface = resolvedColors.screenBackground,
            onSurface = resolvedColors.title,
            surfaceVariant = resolvedColors.lockOuterCircle,
            onSurfaceVariant = resolvedColors.messageText,
            outline = resolvedColors.dotBorder,
            error = resolvedColors.errorText,
            errorContainer = resolvedColors.errorBackground
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalLockPasswordColors provides resolvedColors,
        LocalLockPasswordSizes provides resolvedSizes
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = Typography(),
            content = content
        )
    }
}

private fun LockPasswordColorsConfig.toResolvedColors(): LockPasswordResolvedColors {
    return LockPasswordResolvedColors(
        screenBackground = Color(screenBackgroundColor),
        title = Color(titleColor),

        keypadButtonBackground = Color(keypadButtonBackgroundColor),
        keypadDigit = Color(keypadDigitColor),

        lockOuterCircle = Color(lockOuterCircleColor),
        lockInnerCircle = Color(lockInnerCircleColor),
        lockIcon = Color(lockIconColor),

        dotEmpty = Color(dotEmptyColor),
        dotFilled = Color(dotFilledColor),
        dotBorder = Color(dotBorderColor),

        actionIcon = Color(actionIconColor),
        actionText = Color(actionTextColor),

        errorText = Color(errorTextColor),
        errorBackground = Color(errorBackgroundColor),

        messageText = Color(messageTextColor)
    )
}