package com.example.widgetest 

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
            }
            return WidgetTask(
                id = json.getString("id"),
                title = json.getString("title"),
                isCompleted = json.getBoolean("isCompleted"),
                createdAt = parsedDate
            )
        }
    }

    fun isConsideredToday(): Boolean {
        return createdAt?.toLocalDate()?.isEqual(LocalDate.now()) ?: false
    }

    fun hasReminder(): Boolean {
        return isConsideredToday() 
    }
}