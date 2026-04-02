package ru.devasn.model

enum class LockPasswordError {
    PIN_MISMATCH,
    WRONG_PIN,
    LOCKED,
    UNKNOWN
}