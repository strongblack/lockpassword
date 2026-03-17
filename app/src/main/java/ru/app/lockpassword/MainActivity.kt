package ru.app.lockpassword

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.app.lockpassword.api.LockPasswordResult
import com.app.lockpassword.storage.LockPasswordPrefsRepository
import com.app.lockpassword.ui.LockPasswordRoute
import com.app.lockpassword.ui.LockPasswordViewModel
import ru.app.lockpassword.ui.theme.LockPasswordTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: LockPasswordViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = LockPasswordPrefsRepository(this)

        viewModel = LockPasswordViewModel(
            repository = repository,
            isBiometricAvailable = false
        )

        setContent {
            LockPasswordTheme {
                LockPasswordRoute(
                    viewModel = viewModel,
                    onResult = { result ->
                        when (result) {
                            LockPasswordResult.Success -> {
                                Toast.makeText(
                                    this,
                                    "PIN верный",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            LockPasswordResult.BiometricSuccess -> {
                                Toast.makeText(
                                    this,
                                    "Успех по биометрии",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            LockPasswordResult.Cancelled -> {
                                finish()
                            }

                            is LockPasswordResult.InvalidPin -> {
                                Toast.makeText(
                                    this,
                                    "Неверный PIN",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            is LockPasswordResult.Locked -> {
                                Toast.makeText(
                                    this,
                                    "Блокировка на ${result.remainingMinutes} мин.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            is LockPasswordResult.Error -> {
                                Toast.makeText(
                                    this,
                                    "Ошибка",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    onBiometricRequest = {
                        // пока биометрия отключена
                    }
                )
            }
        }
    }
}