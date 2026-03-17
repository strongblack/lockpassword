package com.app.lockpassword.api

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.app.lockpassword.biometric.BiometricAuthManager
import com.app.lockpassword.storage.LockPasswordPrefsRepository
import com.app.lockpassword.ui.LockPasswordRoute
import com.app.lockpassword.ui.LockPasswordViewModel

class LockPasswordActivity : AppCompatActivity() {

    private lateinit var viewModel: LockPasswordViewModel
    private lateinit var biometricAuthManager: BiometricAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = LockPasswordPrefsRepository(this)
        val biometricEnabled = intent.getBooleanExtra(
            LockPasswordLauncher.EXTRA_BIOMETRIC_ENABLED,
            false
        )

        biometricAuthManager = BiometricAuthManager(this)

        viewModel = LockPasswordViewModel(
            repository = repository,
            isBiometricAvailable = biometricEnabled && biometricAuthManager.isBiometricAvailable()
        )

        setContent {
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
                        }

                        is LockPasswordResult.Locked -> {
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
                        },
                        onFailed = {
                        }
                    )
                }
            )
        }
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