package ru.devasn.config

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class LockPasswordUiConfig @JvmOverloads constructor(
    var lightColors: ru.devasn.config.LockPasswordColorsConfig = _root_ide_package_.ru.devasn.config.LockPasswordDefaults.lightColors(),
    var darkColors: ru.devasn.config.LockPasswordColorsConfig = _root_ide_package_.ru.devasn.config.LockPasswordDefaults.darkColors(),
    var sizes: ru.devasn.config.LockPasswordSizesConfig = _root_ide_package_.ru.devasn.config.LockPasswordDefaults.sizes()
) : Parcelable