package nl.cita.magister.ui.homework

import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.InfoType
import nl.cita.magister.ui.localDateOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val nl = Locale("nl", "NL")
private val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM", nl)

enum class HwFilter { Open, Done, All }

sealed class HomeworkBucket {
    data class Overdue(val items: List<HomeworkItem>) : HomeworkBucket()
    data class Day(val date: LocalDate, val label: String, val items: List<HomeworkItem>) : HomeworkBucket()
}

fun isToets(type: InfoType): Boolean = when (type) {
    InfoType.Test, InfoType.Written, InfoType.Oral -> true
    else -> false
}

fun homeworkDayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Vandaag"
    today.plusDays(1) -> "Morgen"
    today.minusDays(1) -> "Gisteren"
    else -> date.format(dayFmt).replaceFirstChar { it.titlecase(nl) }
}

fun groupHomework(
    items: List<HomeworkItem>,
    today: LocalDate,
    filter: HwFilter,
): List<HomeworkBucket> {
    val filtered = when (filter) {
        HwFilter.Open -> items.filter { !it.isDone }
        HwFilter.Done -> items.filter { it.isDone }
        HwFilter.All -> items
    }
    val overdue = if (filter == HwFilter.Done) {
        emptyList()
    } else {
        filtered.filter { item ->
            !item.isDone && (localDateOf(item.dueIso)?.isBefore(today) == true)
        }.sortedBy { it.dueIso }
    }
    val rest = filtered.filterNot { item -> overdue.any { it.id == item.id } }
        .groupBy { localDateOf(it.dueIso) ?: today }
        .toSortedMap()
        .map { (date, dayItems) ->
            HomeworkBucket.Day(date, homeworkDayLabel(date, today), dayItems.sortedBy { it.dueIso })
        }
    return buildList {
        if (overdue.isNotEmpty()) add(HomeworkBucket.Overdue(overdue))
        addAll(rest)
    }
}
