package com.app.lockpassword.api

import java.io.Serializable

enum class LockPasswordSecurityPreset : Serializable {
    FAST,
    BALANCED,
    STRONG,
    CUSTOM
}