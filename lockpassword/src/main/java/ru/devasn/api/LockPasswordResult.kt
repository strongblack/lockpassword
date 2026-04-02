package ru.devasn.api

import android.content.Intent
import androidx.activity.result.ActivityResult

class LockPasswordResult @JvmOverloads constructor(
    val code: Code,
    val message: String? = null
) {

    enum class Code {
        SUCCESS,
        PIN_CREATED,
        CANCELLED,
        ERROR
    }

    fun toIntent(): Intent {
        return Intent().apply {
            putExtra(EXTRA_RESULT_CODE, code.name)

            message?.let {
                putExtra(EXTRA_MESSAGE, it)
            }
        }
    }

    companion object {
        private const val EXTRA_RESULT_CODE = "ru.devasn.extra.RESULT_CODE"
        private const val EXTRA_MESSAGE = "ru.devasn.extra.MESSAGE"

        @JvmStatic
        fun success(): LockPasswordResult {
            return LockPasswordResult(code = Code.SUCCESS)
        }

        @JvmStatic
        fun pinCreated(): LockPasswordResult {
            return LockPasswordResult(code = Code.PIN_CREATED)
        }

        @JvmStatic
        fun cancelled(): LockPasswordResult {
            return LockPasswordResult(code = Code.CANCELLED)
        }

        @JvmStatic
        @JvmOverloads
        fun error(message: String? = null): LockPasswordResult {
            return LockPasswordResult(
                code = Code.ERROR,
                message = message
            )
        }

        @JvmStatic
        fun fromActivityResult(result: ActivityResult?): LockPasswordResult {
            return fromIntent(result?.data)
        }

        @JvmStatic
        fun fromIntent(data: Intent?): LockPasswordResult {
            if (data == null) {
                return error()
            }

            val code = parseCode(data.getStringExtra(EXTRA_RESULT_CODE))
            val message = data.getStringExtra(EXTRA_MESSAGE)

            return LockPasswordResult(
                code = code,
                message = message
            )
        }

        private fun parseCode(rawCode: String?): Code {
            return try {
                if (rawCode.isNullOrBlank()) {
                    Code.ERROR
                } else {
                    Code.valueOf(rawCode)
                }
            } catch (_: IllegalArgumentException) {
                Code.ERROR
            }
        }
    }
}