package com.apagon.rhythm.ui.util

import com.apagon.rhythm.core.time.DateTimeFormatter
import com.apagon.rhythm.core.time.System
import com.apagon.rhythm.core.time.ZoneId
import com.apagon.rhythm.core.time.atStartOfDay
import com.apagon.rhythm.core.time.atZone
import com.apagon.rhythm.core.time.ofEpochMilli
import com.apagon.rhythm.core.time.plusDays
import com.apagon.rhythm.data.model.Todo
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

internal val REMINDER_INPUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

internal fun LocalDate.toDayStartEndMillis(): Pair<Long, Long> {
    val zoneId = runCatching { ZoneId.systemDefault() }.getOrElse { ZoneId.of("UTC") }
    val dayStart = runCatching { this.atStartOfDay(zoneId).toEpochMilli() }.getOrDefault(System.currentTimeMillis())
    val dayEnd = runCatching { this.plusDays(1).atStartOfDay(zoneId).toEpochMilli() }.getOrDefault(System.currentTimeMillis() + 86400000L)
    return dayStart to dayEnd
}

internal fun Todo.getDueDateAsLocalDate(): LocalDate? {
    val dueDateStr = dueDate.ifEmpty {
        runCatching {
            Instant.ofEpochMilli(createdAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString()
        }.getOrDefault("")
    }
    return runCatching { LocalDate.parse(dueDateStr.substringBefore(" ")) }.getOrNull()
}
