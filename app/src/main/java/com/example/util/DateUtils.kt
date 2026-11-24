package com.example.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private fun isoDateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
    }

    private fun displayDateFormat() = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    private fun monthYearFormat() = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private fun dayNameShortFormat() = SimpleDateFormat("EEE", Locale.getDefault())
    private fun dayNumberFormat() = SimpleDateFormat("d", Locale.getDefault())
    private fun timeFormat24() = SimpleDateFormat("HH:mm", Locale.US).apply {
        isLenient = false
    }
    private fun timeFormatDisplay() = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun todayIso(): String = isoDateFormat().format(Date())

    fun millisToIsoDate(millis: Long): String = isoDateFormat().format(Date(millis))

    fun isoDateToMillis(isoDate: String): Long {
        return try {
            isoDateFormat().parse(isoDate)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    fun offsetDaysIso(baseIso: String, deltaDays: Int): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = isoDateToMillis(baseIso)
        cal.add(Calendar.DAY_OF_YEAR, deltaDays)
        return isoDateFormat().format(cal.time)
    }

    fun weekDaysAround(anchorIso: String): List<String> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = isoDateToMillis(anchorIso)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val diffToMonday = if (dayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - dayOfWeek
        cal.add(Calendar.DAY_OF_YEAR, diffToMonday)
        return List(7) { idx ->
            val iso = isoDateFormat().format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            iso
        }
    }

    fun formatDisplayDate(isoDate: String): String {
        val today = todayIso()
        val tomorrow = offsetDaysIso(today, 1)
        val yesterday = offsetDaysIso(today, -1)
        return when (isoDate) {
            today -> "Today · ${displayDateFormat().format(Date(isoDateToMillis(isoDate)))}"
            tomorrow -> "Tomorrow · ${displayDateFormat().format(Date(isoDateToMillis(isoDate)))}"
            yesterday -> "Yesterday · ${displayDateFormat().format(Date(isoDateToMillis(isoDate)))}"
            else -> displayDateFormat().format(Date(isoDateToMillis(isoDate)))
        }
    }

    fun formatMonthYear(isoDate: String): String =
        monthYearFormat().format(Date(isoDateToMillis(isoDate)))

    fun formatDayOfWeekShort(isoDate: String): String =
        dayNameShortFormat().format(Date(isoDateToMillis(isoDate)))

    fun formatDayNumber(isoDate: String): String =
        dayNumberFormat().format(Date(isoDateToMillis(isoDate)))

    fun formatTimeDisplay(timeHhMm: String): String {
        if (timeHhMm.isBlank()) return "All day"
        return try {
            val parsed = timeFormat24().parse(timeHhMm)
            if (parsed != null) timeFormatDisplay().format(parsed) else timeHhMm
        } catch (_: Exception) {
            timeHhMm
        }
    }

    fun formatMillisTime(millis: Long): String = timeFormatDisplay().format(Date(millis))

    fun parseDateAndTimeToMillis(isoDate: String, timeHhMm: String, defaultHour: Int = 9): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = isoDateToMillis(isoDate)
        val parts = timeHhMm.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: defaultHour
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        cal.set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        cal.set(Calendar.MINUTE, minute.coerceIn(0, 59))
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun dayRangeMillis(isoDate: String): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = isoDateToMillis(isoDate)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val end = cal.timeInMillis - 1L
        return start to end
    }

    fun formatDuration(minutes: Long): String {
        return if (minutes < 60) {
            "${minutes}m"
        } else {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0L) "${h}h" else "${h}h ${m}m"
        }
    }

    fun formatCurrency(amount: Double, symbol: String = "$"): String {
        val df = DecimalFormat("#,##0.##")
        return "$symbol${df.format(amount)}"
    }

    fun isDateBeforeToday(isoDate: String): Boolean {
        if (isoDate.length != 10) return false
        return isoDate < todayIso()
    }

    fun doesRecurrenceApplyOnDate(scheduledDate: String, recurrenceRaw: String, targetDate: String): Boolean {
        if (targetDate < scheduledDate) return false
        if (scheduledDate == targetDate) return true
        return when (recurrenceRaw) {
            "DAILY" -> true
            "WEEKDAYS" -> {
                val cal = Calendar.getInstance().apply { timeInMillis = isoDateToMillis(targetDate) }
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                dow != Calendar.SATURDAY && dow != Calendar.SUNDAY
            }
            "WEEKLY" -> {
                val calStart = Calendar.getInstance().apply { timeInMillis = isoDateToMillis(scheduledDate) }
                val calTarget = Calendar.getInstance().apply { timeInMillis = isoDateToMillis(targetDate) }
                calStart.get(Calendar.DAY_OF_WEEK) == calTarget.get(Calendar.DAY_OF_WEEK)
            }
            else -> false
        }
    }
}
