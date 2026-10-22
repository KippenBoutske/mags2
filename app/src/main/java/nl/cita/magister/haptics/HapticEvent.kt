package nl.cita.magister.haptics

/**
 * Subtiele Pixel-achtige haptics. Geen lange buzz.
 */
enum class HapticEvent {
    TabSwitch,
    DaySnap,
    WeekChip,
    TodayJump,
    HomeworkThreshold,
    HomeworkDone,
    HomeworkUndo,
    RefreshThreshold,
    RefreshDone,
    RefreshError,
    GradeOpen,
    GradeFail,
    CalculatorStep,
    LongPress,
    LoginSuccess,
    LoginError,
}

fun hapticForGrade(sufficient: Boolean): HapticEvent =
    if (sufficient) HapticEvent.GradeOpen else HapticEvent.GradeFail
