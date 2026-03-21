package com.app.lockpassword.api


import android.content.Context
import android.content.Intent


object LockPasswordLauncher {

    const val EXTRA_BIOMETRIC_ENABLED = "com.app.lockpassword.extra.BIOMETRIC_ENABLED"
    const val EXTRA_UI_CONFIG = "com.app.lockpassword.extra.UI_CONFIG"
    const val EXTRA_SECURITY_CONFIG = "com.app.lockpassword.extra.SECURITY_CONFIG"

    fun createIntent(
        context: Context,
        biometricEnabled: Boolean,
        uiConfig: LockPasswordUiConfig = LockPasswordUiConfig(),
        securityConfig: LockPasswordSecurityConfig = LockPasswordSecurityConfig()
    ): Intent {
        return Intent(context, LockPasswordActivity::class.java).apply {
            putExtra(EXTRA_BIOMETRIC_ENABLED, biometricEnabled)
            putExtra(EXTRA_UI_CONFIG, uiConfig)
            putExtra(EXTRA_SECURITY_CONFIG, securityConfig)
        }
    }
}