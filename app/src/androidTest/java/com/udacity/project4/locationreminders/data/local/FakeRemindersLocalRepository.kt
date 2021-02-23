package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

class FakeRemindersLocalRepository(var reminders: MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {

    private var shouldReturnError = false

    fun setShouldReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Fake error encountered...")
        } else {
            reminders?.let {
                return Result.Success(ArrayList(it))
            }
            return Result.Error("No reminders found")
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error("Fake error encountered...")
        } else {
            val reminder = reminders?.first { it.id == id }
            return if (reminder != null)
                Result.Success(reminder)
            else
                Result.Error("Reminder with id of $id not found")
        }
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }
}