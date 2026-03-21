package com.app.lockpassword.ui

import androidx.annotation.StringRes
import com.app.lockpassword.R
import com.app.lockpassword.api.LockPasswordSecurityPreset
import com.app.lockpassword.model.LockPasswordError
import com.app.lockpassword.model.LockPasswordMode

object LockPasswordTextResolver {

    @StringRes
    fun getTitleRes(mode: LockPasswordMode): Int {
        return when (mode) {
            LockPasswordMode.CREATE -> R.string.lock_title_create
            LockPasswordMode.CONFIRM -> R.string.lock_title_repeat
            LockPasswordMode.ENTER -> R.string.lock_title_enter
        }
    }

    @StringRes
    fun getErrorRes(error: LockPasswordError): Int {
        return when (error) {
            LockPasswordError.WRONG_PIN -> R.string.lock_error_wrong_pin
            LockPasswordError.PIN_MISMATCH -> R.string.lock_error_pin_mismatch
            LockPasswordError.LOCKED -> R.string.lock_error_locked
            else -> R.string.lock_error_unknown
        }
    }

    @StringRes
    fun getSecurityPresetTitleRes(preset: LockPasswordSecurityPreset): Int {
        return when (preset) {
            LockPasswordSecurityPreset.FAST -> R.string.lock_security_preset_fast_title
            LockPasswordSecurityPreset.BALANCED -> R.string.lock_security_preset_balanced_title
            LockPasswordSecurityPreset.STRONG -> R.string.lock_security_preset_strong_title
            LockPasswordSecurityPreset.CUSTOM -> R.string.lock_security_preset_custom_title
        }
    }

    @StringRes
    fun getSecurityPresetDescriptionRes(preset: LockPasswordSecurityPreset): Int {
        return when (preset) {
            LockPasswordSecurityPreset.FAST -> R.string.lock_security_preset_fast_description
            LockPasswordSecurityPreset.BALANCED -> R.string.lock_security_preset_balanced_description
            LockPasswordSecurityPreset.STRONG -> R.string.lock_security_preset_strong_description
            LockPasswordSecurityPreset.CUSTOM -> R.string.lock_security_preset_custom_description
        }
    }

    @StringRes
    fun getSecurityPresetWarningRes(preset: LockPasswordSecurityPreset): Int? {
        return when (preset) {
            LockPasswordSecurityPreset.STRONG -> R.string.lock_security_strong_warning
            else -> null
        }
    }
}