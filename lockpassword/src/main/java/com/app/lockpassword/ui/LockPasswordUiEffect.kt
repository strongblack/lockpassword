package com.app.lockpassword.ui

sealed interface LockPasswordUiEffect {
    data object WrongPinVibration : LockPasswordUiEffect
    data object LockedVibration : LockPasswordUiEffect
}