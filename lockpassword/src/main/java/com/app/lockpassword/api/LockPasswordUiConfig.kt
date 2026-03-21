package com.app.lockpassword.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class LockPasswordUiConfig @JvmOverloads constructor(
    var lightColors: LockPasswordColorsConfig = LockPasswordDefaults.lightColors(),
    var darkColors: LockPasswordColorsConfig = LockPasswordDefaults.darkColors(),
    var sizes: LockPasswordSizesConfig = LockPasswordDefaults.sizes()
) : Parcelable