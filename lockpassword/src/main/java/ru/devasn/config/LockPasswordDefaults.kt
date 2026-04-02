package ru.devasn.config

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object LockPasswordDefaults {

    @JvmStatic
    fun lightColors(): ru.devasn.config.LockPasswordColorsConfig {
        return _root_ide_package_.ru.devasn.config.LockPasswordColorsConfig(
            screenBackgroundColor = Color(0xFFF8FAFC).toArgb(),
            titleColor = Color(0xFF111827).toArgb(),

            keypadButtonBackgroundColor = Color(0xFFE0E7FF).toArgb(),
            keypadDigitColor = Color(0xFF1E3A8A).toArgb(),

            lockOuterCircleColor = Color(0xFFE5E7EB).toArgb(),
            lockInnerCircleColor = Color(0xFFE0E7FF).toArgb(),
            lockIconColor = Color(0xFF2563EB).toArgb(),

            dotEmptyColor = Color(0xFFE5E7EB).toArgb(),
            dotFilledColor = Color(0xFF2563EB).toArgb(),
            dotBorderColor = Color(0xFF9CA3AF).toArgb(),

            actionIconColor = Color(0xFF374151).toArgb(),
            actionTextColor = Color(0xFF374151).toArgb(),

            errorTextColor = Color(0xFFB3261E).toArgb(),
            errorBackgroundColor = Color(0xFFFFDAD6).toArgb(),

            messageTextColor = Color(0xFF374151).toArgb()
        )
    }

    @JvmStatic
    fun darkColors(): ru.devasn.config.LockPasswordColorsConfig {
        return _root_ide_package_.ru.devasn.config.LockPasswordColorsConfig(
            screenBackgroundColor = Color(0xFF0F1115).toArgb(),
            titleColor = Color(0xFFF9FAFB).toArgb(),

            keypadButtonBackgroundColor = Color(0xFF1F2937).toArgb(),
            keypadDigitColor = Color(0xFFF9FAFB).toArgb(),

            lockOuterCircleColor = Color(0xFF374151).toArgb(),
            lockInnerCircleColor = Color(0xFF1F2937).toArgb(),
            lockIconColor = Color(0xFF93C5FD).toArgb(),

            dotEmptyColor = Color(0xFF374151).toArgb(),
            dotFilledColor = Color(0xFF93C5FD).toArgb(),
            dotBorderColor = Color(0xFF6B7280).toArgb(),

            actionIconColor = Color(0xFFD1D5DB).toArgb(),
            actionTextColor = Color(0xFFD1D5DB).toArgb(),

            errorTextColor = Color(0xFFCF6679).toArgb(),
            errorBackgroundColor = Color(0xFF601410).toArgb(),

            messageTextColor = Color(0xFFD1D5DB).toArgb()
        )
    }

    @JvmStatic
    fun uiConfig(): ru.devasn.config.LockPasswordUiConfig {
        return _root_ide_package_.ru.devasn.config.LockPasswordUiConfig(
            lightColors = lightColors(),
            darkColors = darkColors(),
            sizes = sizes()
        )
    }

    @JvmStatic
    fun sizes(): ru.devasn.config.LockPasswordSizesConfig {
        return _root_ide_package_.ru.devasn.config.LockPasswordSizesConfig(
            buttonScale = 1.0f
        )
    }


    @JvmStatic
    fun security(): LockPasswordSecurityConfig {
        return _root_ide_package_.ru.devasn.config.LockPasswordSecurityConfig(
            pinLength = 6
        )
    }
}