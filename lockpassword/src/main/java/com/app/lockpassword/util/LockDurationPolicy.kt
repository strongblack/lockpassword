package com.app.lockpassword.util

object LockDurationPolicy {

    fun getDurationMillis(errorCycle: Int): Long {
        return when (errorCycle) {
            1 -> 1 * 60 * 1000L
            2 -> 5 * 60 * 1000L
            3 -> 30 * 60 * 1000L
            else -> 60 * 60 * 1000L
        }
    }

    fun getRemainingMinutes(
        lockTimestamp: Long,
        errorCycle: Int,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Long? {
        if (lockTimestamp <= 0L) return null

        val duration = getDurationMillis(errorCycle)
        val remaining = duration - (currentTimeMillis - lockTimestamp)

        if (remaining <= 0L) return null

        val minutes = remaining / (60 * 1000L)
        val seconds = (remaining % (60 * 1000L)) / 1000L

        return if (seconds > 0) minutes + 1 else minutes
    }
}