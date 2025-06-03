package com.example.widgetest // Make sure this matches your package name

import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

data class WidgetTask(
    val id: String,
    val title: String,
    val isCompleted: Boolean,
    val createdAt: LocalDateTime? // Make nullable to handle potential parsing errors gracefully
) {
    companion object {
        fun fromJson(json: JSONObject): WidgetTask {
            var parsedDate: LocalDateTime? = null
            try {
                parsedDate = LocalDateTime.parse(json.getString("createdAt"))
            } catch (e: DateTimeParseException) {
                // Log error or handle default date
            }
            return WidgetTask(
                id = json.getString("id"),
                title = json.getString("title"),
                isCompleted = json.getBoolean("isCompleted"),
                createdAt = parsedDate
            )
        }
    }

    // Simple check for "Today" based on createdAt date.
    // You might want more sophisticated logic depending on how "Today" is determined in your app.
    fun isConsideredToday(): Boolean {
        return createdAt?.toLocalDate()?.isEqual(LocalDate.now()) ?: false
    }

    // Placeholder for reminder logic - adapt as needed
    fun hasReminder(): Boolean {
        // return true // or based on some property of the task
        return isConsideredToday() // For demo, show bell if "Today"
    }
}