package com.app.lockpassword.api

import android.content.Context
import android.content.Intent

object LockPasswordLauncher {

    const val EXTRA_BIOMETRIC_ENABLED = "extra_biometric_enabled"
    const val EXTRA_RESULT_CODE = "extra_result_code"
    const val EXTRA_UI_CONFIG = "extra_ui_config"

    const val RESULT_SUCCESS = 1
    const val RESULT_BIOMETRIC_SUCCESS = 2
    const val RESULT_CANCELLED = 3
    const val RESULT_ERROR = 4

    @JvmStatic
    @JvmOverloads
    fun createIntent(
        context: Context,
        biometricEnabled: Boolean = false,
        uiConfig: LockPasswordUiConfig = LockPasswordDefaults.uiConfig()
    ): Intent {
        return Intent(context, LockPasswordActivity::class.java).apply {
            putExtra(EXTRA_BIOMETRIC_ENABLED, biometricEnabled)
            putExtra(EXTRA_UI_CONFIG, uiConfig)
        }
    }
}