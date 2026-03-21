package com.app.lockpassword.api

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.app.lockpassword.biometric.BiometricAuthManager
import com.app.lockpassword.storage.LockPasswordPrefsRepository
import com.app.lockpassword.ui.LockPasswordRoute
import com.app.lockpassword.ui.LockPasswordViewModel
import com.app.lockpassword.ui.theme.LockPasswordTheme

class LockPasswordActivity : FragmentActivity() {

    private lateinit var viewModel: LockPasswordViewModel
    private lateinit var biometricAuthManager: BiometricAuthManager

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val biometricEnabled = intent.getBooleanExtra(
            LockPasswordLauncher.EXTRA_BIOMETRIC_ENABLED,
            false
        )

        val uiConfig = readUiConfig()
        val securityConfig = readSecurityConfig()

        biometricAuthManager = BiometricAuthManager(this)

        val repository = LockPasswordPrefsRepository(
            context = applicationContext,
            securityConfig = securityConfig
        )

        viewModel = LockPasswordViewModel(
            repository = repository,
            isBiometricAvailable = biometricEnabled && biometricAuthManager.isBiometricAvailable()
        )

        setContent {
            LockPasswordTheme(uiConfig = uiConfig) {
                LockPasswordRoute(
                    viewModel = viewModel,
                    onResult = { result ->
                        when (result) {
                            LockPasswordResult.Success -> {
                                finishWithResult(RESULT_SUCCESS)
                            }

                            LockPasswordResult.BiometricSuccess -> {
                                finishWithResult(RESULT_BIOMETRIC_SUCCESS)
                            }

                            LockPasswordResult.Cancelled -> {
                                finishWithResult(RESULT_CANCELLED)
                            }

                            is LockPasswordResult.InvalidPin -> {
                                // Ничего не делаем.
                                // Ошибка уже обрабатывается внутри UI/ViewModel.
                            }

                            is LockPasswordResult.Locked -> {
                                // Ничего не делаем.
                                // Состояние блокировки уже показывается на экране.
                            }

                            is LockPasswordResult.Error -> {
                                finishWithResult(RESULT_ERROR)
                            }
                        }
                    },
                    onBiometricRequest = {
                        biometricAuthManager.authenticate(
                            onSuccess = {
                                viewModel.onBiometricSuccess()
                            },
                            onError = { _, _ ->
                                viewModel.onBiometricError()
                            },
                            onFailed = {
                                // BiometricPrompt сам показывает failed state,
                                // а экран остаётся в режиме ввода PIN.
                            }
                        )
                    }
                )
            }
        }
    }

    private fun readUiConfig(): LockPasswordUiConfig {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                LockPasswordLauncher.EXTRA_UI_CONFIG,
                LockPasswordUiConfig::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(LockPasswordLauncher.EXTRA_UI_CONFIG)
        } ?: LockPasswordDefaults.uiConfig()
    }

    private fun readSecurityConfig(): LockPasswordSecurityConfig {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                LockPasswordLauncher.EXTRA_SECURITY_CONFIG,
                LockPasswordSecurityConfig::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(LockPasswordLauncher.EXTRA_SECURITY_CONFIG)
        } ?: LockPasswordSecurityConfig()
    }

    private fun finishWithResult(resultCodeValue: Int) {
        val data = Intent().apply {
            putExtra(EXTRA_RESULT_CODE, resultCodeValue)
        }
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    companion object {
        const val EXTRA_RESULT_CODE = "extra_result_code"

        const val RESULT_SUCCESS = 1
        const val RESULT_BIOMETRIC_SUCCESS = 2
        const val RESULT_CANCELLED = 3
        const val RESULT_ERROR = 4
    }
}