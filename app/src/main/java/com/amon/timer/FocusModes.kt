package com.amon.timer

enum class FocusModeType(val title: String) {
    TIMER("Timer"),
    STOPWATCH("Stopwatch"),
    POMODORO("Pomodoro")
}

data class PomodoroConfig(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val totalRounds: Int = 4,
    val autoStartNextFocus: Boolean = true
)
