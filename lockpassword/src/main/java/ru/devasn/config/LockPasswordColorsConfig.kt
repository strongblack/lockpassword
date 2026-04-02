package ru.devasn.config

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.parcelize.Parcelize

@Parcelize
class LockPasswordColorsConfig @JvmOverloads constructor(

    var screenBackgroundColor: Int = Color(0xFFF8FAFC).toArgb(),
    var titleColor: Int = Color(0xFF111827).toArgb(),

    var keypadButtonBackgroundColor: Int = Color(0xFFE0E7FF).toArgb(),
    var keypadDigitColor: Int = Color(0xFF1E3A8A).toArgb(),

    var lockOuterCircleColor: Int = Color(0xFFE5E7EB).toArgb(),
    var lockInnerCircleColor: Int = Color(0xFFE0E7FF).toArgb(),
    var lockIconColor: Int = Color(0xFF2563EB).toArgb(),

    var dotEmptyColor: Int = Color(0xFFE5E7EB).toArgb(),
    var dotFilledColor: Int = Color(0xFF2563EB).toArgb(),
    var dotBorderColor: Int = Color(0xFF9CA3AF).toArgb(),

    var actionIconColor: Int = Color(0xFF374151).toArgb(),
    var actionTextColor: Int = Color(0xFF374151).toArgb(),

    var errorTextColor: Int = Color(0xFFB3261E).toArgb(),
    var errorBackgroundColor: Int = Color(0xFFFFDAD6).toArgb(),

    var messageTextColor: Int = Color(0xFF374151).toArgb()
) : Parcelable