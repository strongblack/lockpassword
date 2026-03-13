package com.app.lockpassword.presentation

import com.app.lockpassword.domain.LockPasswordMode

data class LockPasswordUiState(
    val title: String = "LockPassword",
    val description: String = "Первый шаг архитектуры готов. Дальше добавим ввод PIN, валидацию и результат.",
    val mode: LockPasswordMode = LockPasswordMode.CHECK,
)