package com.app.lockpassword.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class LockPasswordPrefsRepository(
    context: Context
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasPin(): Boolean {
        return !getPin().isNullOrEmpty()
    }

    fun savePin(pin: String) {
        prefs.edit { putString(KEY_PIN, pin) }
    }

    fun getPin(): String? {
        return prefs.getString(KEY_PIN, null)
    }

    fun clearPin() {
        prefs.edit { remove(KEY_PIN) }
    }

    fun getFailedAttempts(): Int {
        return prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
    }

    fun setFailedAttempts(value: Int) {
        prefs.edit { putInt(KEY_FAILED_ATTEMPTS, value) }
    }

    fun getLockoutLevel(): Int {
        return prefs.getInt(KEY_LOCKOUT_LEVEL, 0)
    }

    fun setLockoutLevel(value: Int) {
        prefs.edit { putInt(KEY_LOCKOUT_LEVEL, value) }
    }

    fun getLockedUntilTimestamp(): Long {
        return prefs.getLong(KEY_LOCKED_UNTIL_TIMESTAMP, 0L)
    }

    fun setLockedUntilTimestamp(value: Long) {
        prefs.edit { putLong(KEY_LOCKED_UNTIL_TIMESTAMP, value) }
    }

    fun clearLockedUntilTimestamp() {
        prefs.edit { remove(KEY_LOCKED_UNTIL_TIMESTAMP) }
    }

    fun clearSecurityState(resetLockoutLevel: Boolean = true) {
        prefs.edit {
            putInt(KEY_FAILED_ATTEMPTS, 0)
            remove(KEY_LOCKED_UNTIL_TIMESTAMP)

            if (resetLockoutLevel) {
                putInt(KEY_LOCKOUT_LEVEL, 0)
            }
        }
    }

    private companion object {
        const val PREFS_NAME = "lock_password_prefs"
        const val KEY_PIN = "key_pin"
        const val KEY_FAILED_ATTEMPTS = "key_failed_attempts"
        const val KEY_LOCKOUT_LEVEL = "key_lockout_level"
        const val KEY_LOCKED_UNTIL_TIMESTAMP = "key_locked_until_timestamp"
    }
}