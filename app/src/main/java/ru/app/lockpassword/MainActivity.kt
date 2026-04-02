package ru.app.lockpassword

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import ru.devasn.api.LockPasswordActivity
import ru.devasn.api.LockPasswordLauncher

import ru.app.lockpassword.ui.theme.LockPasswordTheme
import ru.devasn.api.LockPasswordResult
import ru.devasn.config.LockPasswordDefaults
import ru.devasn.config.LockPasswordSecurityConfig
import ru.devasn.config.LockPasswordSecurityPreset

class MainActivity : ComponentActivity() {


    private val tag = "LockPasswordDemo"

    private val lockPasswordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val lockResult = LockPasswordResult.fromActivityResult(result)

        when (lockResult.code) {
            LockPasswordResult.Code.SUCCESS -> {
                Log.d(tag, "Authentication succeeded")
            }

            LockPasswordResult.Code.PIN_CREATED -> {
                Log.d(tag, "PIN created")
            }

            LockPasswordResult.Code.CANCELLED -> {
                Log.d(tag, "Cancelled by user")
            }

            LockPasswordResult.Code.ERROR -> {

                Log.d(tag, "Error. message=${lockResult.message}")
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LockPasswordTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {

                                // FULL EXAMPLE

                                val uiConfig = LockPasswordDefaults.uiConfig().apply {
                                    sizes.buttonScale = 1.08f

                                    lightColors.screenBackgroundColor = Color(0xFFF8FAFC).toArgb()
                                    lightColors.titleColor = Color(0xFF111827).toArgb()
                                    lightColors.keypadButtonBackgroundColor = Color(0xFFE0E7FF).toArgb()
                                    lightColors.keypadDigitColor = Color(0xFF1E3A8A).toArgb()
                                    lightColors.lockOuterCircleColor = Color(0xFFE5E7EB).toArgb()
                                    lightColors.lockInnerCircleColor = Color(0xFFE0E7FF).toArgb()
                                    lightColors.lockIconColor = Color(0xFF2563EB).toArgb()
                                    lightColors.dotEmptyColor = Color(0xFFE5E7EB).toArgb()
                                    lightColors.dotFilledColor = Color(0xFF2563EB).toArgb()
                                    lightColors.dotBorderColor = Color(0xFF9CA3AF).toArgb()
                                    lightColors.actionIconColor = Color(0xFF374151).toArgb()
                                    lightColors.actionTextColor = Color(0xFF374151).toArgb()
                                    lightColors.errorTextColor = Color(0xFFB3261E).toArgb()
                                    lightColors.errorBackgroundColor = Color(0xFFFFDAD6).toArgb()
                                    lightColors.messageTextColor = Color(0xFF374151).toArgb()

                                    darkColors.screenBackgroundColor = Color(0xFF0F1115).toArgb()
                                    darkColors.titleColor = Color(0xFFF9FAFB).toArgb()
                                    darkColors.keypadButtonBackgroundColor = Color(0xFF1F2937).toArgb()
                                    darkColors.keypadDigitColor = Color(0xFFF9FAFB).toArgb()
                                    darkColors.lockOuterCircleColor = Color(0xFF374151).toArgb()
                                    darkColors.lockInnerCircleColor = Color(0xFF1F2937).toArgb()
                                    darkColors.lockIconColor = Color(0xFF93C5FD).toArgb()
                                    darkColors.dotEmptyColor = Color(0xFF374151).toArgb()
                                    darkColors.dotFilledColor = Color(0xFF93C5FD).toArgb()
                                    darkColors.dotBorderColor = Color(0xFF6B7280).toArgb()
                                    darkColors.actionIconColor = Color(0xFFD1D5DB).toArgb()
                                    darkColors.actionTextColor = Color(0xFFD1D5DB).toArgb()
                                    darkColors.errorTextColor = Color(0xFFCF6679).toArgb()
                                    darkColors.errorBackgroundColor = Color(0xFF601410).toArgb()
                                    darkColors.messageTextColor = Color(0xFFD1D5DB).toArgb()
                                }

                                val intent = LockPasswordLauncher.createIntent(
                                    context = this@MainActivity,
                                    biometricEnabled = true,
                                    uiConfig = uiConfig,
                                    securityConfig = LockPasswordSecurityConfig(
                                        pinLength = 6,
                                        securityPreset = LockPasswordSecurityPreset.BALANCED,
                                        useKeystoreWrapping = true,
                                        saltSizeBytes = 16,
                                        derivedKeySizeBytes = 32,
                                        maxFailedAttemptsBeforeLockout = 5,
                                        lockoutScheduleMs = listOf(
                                            30_000L,
                                            60_000L,
                                            120_000L,
                                            300_000L,
                                            900_000L
                                        )
                                    )
                                )

                                lockPasswordLauncher.launch(intent)


                            }
                        ) {
                            Text("Open LockPassword")
                        }
                    }
                }
            }
        }
    }
}