package ru.devasn.api

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import ru.devasn.biometric.BiometricAuthManager
import ru.devasn.config.LockPasswordDefaults
import ru.devasn.config.LockPasswordSecurityConfig
import ru.devasn.config.LockPasswordUiConfig
import ru.devasn.ui.LockPasswordRoute
import ru.devasn.ui.LockPasswordViewModel
import ru.devasn.ui.theme.LockPasswordTheme

class LockPasswordActivity : FragmentActivity() {

    private lateinit var viewModel: LockPasswordViewModel
    private lateinit var biometricAuthManager: BiometricAuthManager

    private var autoBiometricStarted = false

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val uiConfig = readUiConfig()
        val securityConfig = readSecurityConfig()
        val biometricEnabled = intent.getBooleanExtra(
            LockPasswordLauncher.EXTRA_BIOMETRIC_ENABLED,
            false
        )

        val canUseBiometric = createDependencies(
            securityConfig = securityConfig,
            biometricEnabled = biometricEnabled
        )

        setupBackPressedHandler()
        setupContent(uiConfig)
        launchAutoBiometricIfNeeded(
            savedInstanceState = savedInstanceState,
            canUseBiometric = canUseBiometric
        )
    }

    private fun createDependencies(
        securityConfig: LockPasswordSecurityConfig,
        biometricEnabled: Boolean
    ): Boolean {
        val repository = _root_ide_package_.ru.devasn.storage.LockPasswordPrefsRepository(
            context = this,
            securityConfig = securityConfig
        )

        biometricAuthManager =
            _root_ide_package_.ru.devasn.biometric.BiometricAuthManager(this)

        val canUseBiometric = biometricEnabled &&
                repository.hasPin() &&
                biometricAuthManager.isBiometricAvailable()

        viewModel = _root_ide_package_.ru.devasn.ui.LockPasswordViewModel(
            repository = repository,
            isBiometricAvailable = canUseBiometric,
            configuredPinLength = securityConfig.resolvedPinLength()
        )

        return canUseBiometric
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finishWithResult(LockPasswordResult.cancelled())
                }
            }
        )
    }

    private fun setupContent(uiConfig: LockPasswordUiConfig) {
        setContent {
            _root_ide_package_.ru.devasn.ui.theme.LockPasswordTheme(uiConfig = uiConfig) {
                _root_ide_package_.ru.devasn.ui.LockPasswordRoute(
                    viewModel = viewModel,
                    onResult = ::finishWithResult,
                    onBiometricRequest = {
                        startBiometricIfNeeded(force = true)
                    }
                )
            }
        }
    }

    private fun launchAutoBiometricIfNeeded(
        savedInstanceState: Bundle?,
        canUseBiometric: Boolean
    ) {
        if (savedInstanceState == null && canUseBiometric) {
            window.decorView.post {
                startBiometricIfNeeded(force = false)
            }
        }
    }

    private fun startBiometricIfNeeded(force: Boolean) {
        if (!force && autoBiometricStarted) {
            return
        }

        if (!force) {
            autoBiometricStarted = true
        }

        biometricAuthManager.authenticate(
            onSuccess = {
                viewModel.onBiometricSuccess()
            },
            onError = { _, _ ->
                viewModel.onBiometricError()
            },
            onFailed = {
            }
        )
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
        } ?: LockPasswordDefaults.security()
    }

    private fun finishWithResult(result: LockPasswordResult) {
        setResult(RESULT_OK, result.toIntent())
        finish()
    }
}