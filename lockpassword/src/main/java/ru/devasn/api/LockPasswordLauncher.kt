package ru.devasn.api

import android.content.Context
import android.content.Intent
import ru.devasn.config.LockPasswordDefaults
import ru.devasn.config.LockPasswordSecurityConfig
import ru.devasn.config.LockPasswordUiConfig

object LockPasswordLauncher {
    const val EXTRA_BIOMETRIC_ENABLED = "extra_biometric_enabled"
    const val EXTRA_UI_CONFIG = "extra_ui_config"
    const val EXTRA_SECURITY_CONFIG = "extra_security_config"

    @JvmStatic
    @JvmOverloads
    fun createIntent(
        context: Context,
        biometricEnabled: Boolean = false,
        uiConfig: LockPasswordUiConfig = LockPasswordDefaults.uiConfig(),
        securityConfig: LockPasswordSecurityConfig = LockPasswordDefaults.security()
    ): Intent {
        val safeSecurityConfig = if (securityConfig.isPinLengthValid()) {
            securityConfig
        } else {

            securityConfig.copy(
                pinLength = securityConfig.resolvedPinLength()
            )
        }

        return Intent(context, LockPasswordActivity::class.java).apply {
            putExtra(EXTRA_BIOMETRIC_ENABLED, biometricEnabled)
            putExtra(EXTRA_UI_CONFIG, uiConfig)
            putExtra(EXTRA_SECURITY_CONFIG, safeSecurityConfig)
        }
    }
}