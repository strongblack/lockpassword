package com.app.lockpassword.api

import android.content.Context
import android.content.Intent

object LockPasswordLauncher {

    const val EXTRA_BIOMETRIC_ENABLED = "extra_biometric_enabled"

    @JvmStatic
    fun createIntent(
        context: Context,
        biometricEnabled: Boolean = false
    ): Intent {
        return Intent(context, LockPasswordActivity::class.java).apply {
            putExtra(EXTRA_BIOMETRIC_ENABLED, biometricEnabled)
        }
    }
}