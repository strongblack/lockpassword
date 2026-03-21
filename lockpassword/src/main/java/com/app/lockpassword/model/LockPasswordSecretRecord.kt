package com.app.lockpassword.model

data class LockPasswordSecretRecord(
    val version: Int,
    val algorithm: String,
    val saltBase64: String,
    val iterations: Int,
    val derivedKeySizeBytes: Int,
    val verifierBase64: String,
    val failedAttempts: Int,
    val lockoutUntilEpochMs: Long
)