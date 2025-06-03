package com.example.widgetest 

import android.util.Log
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle

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
        return deadline.toLocalDate().isBefore(LocalDate.now())
    }

    fun getFormattedDeadlineForWidget(): String? {
        if (deadline == null) return null

        val today = LocalDate.now()
        val deadlineDate = deadline.toLocalDate()
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM") 

        return when {
            deadlineDate.isEqual(today) -> "Ends Today" 
            deadlineDate.isEqual(today.plusDays(1)) -> "Ends Tomorrow"
            deadlineDate.isBefore(today) && !isCompleted -> "Overdue: ${deadlineDate.format(dateFormatter)}"
            deadlineDate.isBefore(today) && isCompleted -> "Done (Overdue: ${deadlineDate.format(dateFormatter)})" 
            else -> deadlineDate.format(dateFormatter) 
        }
    }

    fun hasReminderForWidget(): Boolean {
        return deadline != null && !isCompleted && (isDeadlineToday() || isDeadlinePassed())
    }
}