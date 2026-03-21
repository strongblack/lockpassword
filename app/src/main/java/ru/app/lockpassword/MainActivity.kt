package ru.app.lockpassword

import android.content.Intent
import android.os.Bundle
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
import com.app.lockpassword.api.LockPasswordActivity
import com.app.lockpassword.api.LockPasswordLauncher
import ru.app.lockpassword.ui.theme.LockPasswordTheme

class MainActivity : ComponentActivity() {

    private val lockLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val resultCode = result.data?.getIntExtra(
            LockPasswordActivity.EXTRA_RESULT_CODE,
            -1
        )

        when (resultCode) {
            LockPasswordActivity.RESULT_SUCCESS -> {
                Toast.makeText(this, "PIN верный", Toast.LENGTH_SHORT).show()
            }

            LockPasswordActivity.RESULT_BIOMETRIC_SUCCESS -> {
                Toast.makeText(this, "Успех по биометрии", Toast.LENGTH_SHORT).show()
            }

            LockPasswordActivity.RESULT_CANCELLED -> {
                Toast.makeText(this, "Отмена", Toast.LENGTH_SHORT).show()
            }

            LockPasswordActivity.RESULT_ERROR -> {
                Toast.makeText(this, "Ошибка", Toast.LENGTH_SHORT).show()
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
                                val intent: Intent = LockPasswordLauncher.createIntent(
                                    context = this@MainActivity,
                                    biometricEnabled = true
                                )
                                lockLauncher.launch(intent)
                            }
                        ) {
                            Text("Открыть lockpassword")
                        }
                    }
                }
            }
        }
    }
}