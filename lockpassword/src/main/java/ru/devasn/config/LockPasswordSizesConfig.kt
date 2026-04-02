package ru.devasn.config

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class LockPasswordSizesConfig @JvmOverloads constructor(
    var buttonScale: Float = 1.0f
) : Parcelable {
    fun normalizedButtonScale(): Float {
        return buttonScale.coerceIn(0.85f, 1.15f)
    }
}