package com.example.widgetest

import android.util.Log
import org.json.JSONObject
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class WidgetTask(
    val id: String,
    val title: String,
    val isCompleted: Boolean,
    val createdAt: LocalDateTime?,
    val deadline: LocalDateTime?
) {
    companion object {
        fun fromJson(json: JSONObject): WidgetTask {
            var parsedCreatedAt: LocalDateTime? = null
            try {
                if (json.has("createdAt") && !json.isNull("createdAt")) {
                    parsedCreatedAt = LocalDateTime.parse(json.getString("createdAt"))
                }
            } catch (e: DateTimeParseException) {
                Log.e("WidgetTask", "Error parsing createdAt: ${json.optString("createdAt")}", e)
            }

            var parsedDeadline: LocalDateTime? = null
            try {
                if (json.has("deadline") && !json.isNull("deadline")) {
                    parsedDeadline = LocalDateTime.parse(json.getString("deadline"))
                }
            } catch (e: DateTimeParseException) {
                 Log.e("WidgetTask", "Error parsing deadline: ${json.optString("deadline")}", e)
            }

            return WidgetTask(
                id = json.getString("id"),
                title = json.getString("title"),
                isCompleted = json.getBoolean("isCompleted"),
                createdAt = parsedCreatedAt,
                deadline = parsedDeadline
            )
        }
    }

    fun isDeadlineToday(): Boolean {
        if (deadline == null) return false
        return deadline.toLocalDate().isEqual(LocalDate.now())
    }

    fun isDeadlinePassed(): Boolean {
        if (deadline == null) return false
        return deadline.isBefore(LocalDateTime.now())
    }

    fun getFormattedDeadlineForWidget(): String? {
        if (deadline == null) return null

        val now = LocalDateTime.now()
        val todayDate = now.toLocalDate()
        val deadlineDateTime = deadline
        val deadlineDate = deadlineDateTime.toLocalDate()

        val hasSpecificTime = deadlineDateTime.toLocalTime().hour != 0 || deadlineDateTime.toLocalTime().minute != 0

        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM")
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        return when {
            deadlineDate.isEqual(todayDate) -> {
                val difference = Duration.between(now, deadlineDateTime)

                if (difference.isNegative) { 
                    val absDifference = difference.abs()
                    val hours = absDifference.toHours()
                    val minutes = absDifference.toMinutes() % 60

                    when {
                        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m ago"
                        hours > 0 -> "${hours}h ago"
                        minutes > 0 -> "${minutes}m ago"
                        else -> "Just now"
                    }
                } else { 
                    val hours = difference.toHours()
                    val minutes = difference.toMinutes() % 60
                    when {
                        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m left"
                        hours > 0 -> "${hours}h left"
                        minutes > 0 -> "${minutes}m left"
                        else -> "Due now"
                    }
                }
            }
            deadlineDateTime.isBefore(now) && !isCompleted -> {
                "Overdue: ${deadlineDate.format(dateFormatter)}${if (hasSpecificTime) ", ${deadlineDateTime.toLocalTime().format(timeFormatter)}" else ""}"
            }
            deadlineDateTime.isBefore(now) && isCompleted -> {
                "Done (Was ${deadlineDate.format(dateFormatter)}${if (hasSpecificTime) ", ${deadlineDateTime.toLocalTime().format(timeFormatter)}" else ""})"
            }
            deadlineDate.isEqual(todayDate.plusDays(1)) -> {
                if (hasSpecificTime) "Tomorrow, ${deadlineDateTime.toLocalTime().format(timeFormatter)}"
                else "Tomorrow"
            }
            else -> {
                "${deadlineDate.format(dateFormatter)}${if (hasSpecificTime) ", ${deadlineDateTime.toLocalTime().format(timeFormatter)}" else ""}"
            }
        }
    }

    fun hasReminderForWidget(): Boolean {
        return deadline != null && !isCompleted && (isDeadlineToday() || isDeadlinePassed())
    }
}