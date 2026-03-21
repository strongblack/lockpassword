package com.app.lockpassword.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LockPasswordThemeColors(
    val primary: Int,
    val onPrimary: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
    val secondary: Int,
    val onSecondary: Int,
    val background: Int,
    val onBackground: Int,
    val surface: Int,
    val onSurface: Int,
    val surfaceVariant: Int,
    val onSurfaceVariant: Int,
    val outline: Int,
    val error: Int,
    val onError: Int,
    val errorContainer: Int
) : Parcelable