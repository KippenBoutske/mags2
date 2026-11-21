package nl.cita.magister.haptics

/**
 * Pixel-achtige haptics. Geen lange buzz.
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
    GradeCelebrate,
    GradeFail,
    CalculatorStep,
    LongPress,
    LoginSuccess,
    LoginError,
    ScrollTick,
    EdgeBump,
    CardPress,
    FilterSelect,
    SheetOpen,
}

enum class HapticStrength(val id: String, val label: String, val scale: Float) {
    Light("light", "Licht", 0.55f),
    Normal("normal", "Normaal", 0.8f),
    Strong("strong", "Sterk", 1.0f),
    ;

    companion object {
        fun fromId(id: String?): HapticStrength =
            entries.firstOrNull { it.id == id } ?: Strong
    }
}

fun hapticForGrade(sufficient: Boolean, numericValue: Double? = null): HapticEvent = when {
    numericValue != null && numericValue >= 7.5 -> HapticEvent.GradeCelebrate
    sufficient -> HapticEvent.GradeOpen
    else -> HapticEvent.GradeFail
}
