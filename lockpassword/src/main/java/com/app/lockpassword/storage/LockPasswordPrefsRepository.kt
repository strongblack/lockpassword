package com.app.lockpassword.storage

import android.content.Context
import android.content.SharedPreferences

class LockPasswordPrefsRepository(
    context: Context
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasPin(): Boolean {
        return !getPin().isNullOrEmpty()
    }

    fun savePin(pin: String) {
        prefs.edit().putString(KEY_PIN, pin).apply()
    }

    fun getPin(): String? {
        return prefs.getString(KEY_PIN, null)
    }

    fun clearPin() {
        prefs.edit().remove(KEY_PIN).apply()
    }

    fun getErrorCount(): Int {
        return prefs.getInt(KEY_ERROR_COUNT, 0)
    }

    fun setErrorCount(value: Int) {
        prefs.edit().putInt(KEY_ERROR_COUNT, value).apply()
    }

    fun saveLockTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LOCK_TIMESTAMP, timestamp).apply()
    }

    fun getLockTimestamp(): Long {
        return prefs.getLong(KEY_LOCK_TIMESTAMP, 0L)
    }

    fun clearLockTimestamp() {
        prefs.edit().remove(KEY_LOCK_TIMESTAMP).apply()
    }

    private companion object {
        const val PREFS_NAME = "lock_password_prefs"
        const val KEY_PIN = "key_pin"
        const val KEY_ERROR_COUNT = "key_error_count"
        const val KEY_LOCK_TIMESTAMP = "key_lock_timestamp"
    }
}