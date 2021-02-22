package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MainAndroidCoroutineRule
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@MediumTest
class RemindersLocalRepositoryTest {

    @get:Rule
    var mainAndroidCoroutineRule = MainAndroidCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var remindersRepository: RemindersLocalRepository
    private lateinit var remindersDatabase: RemindersDatabase
    private lateinit var remindersDAO: RemindersDao
    private lateinit var reminderDTO : ReminderDTO

    @Before
    fun setup() {
        remindersDatabase = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), RemindersDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        remindersDAO = remindersDatabase.reminderDao()
        remindersRepository = RemindersLocalRepository(remindersDAO, Dispatchers.Main)
        reminderDTO = ReminderDTO("Reminder Title", "Reminder Description","Null Island", 0.0, 0.0)
    }

    @After
    fun closeDB() = remindersDatabase.close()

    @Test
    fun testGetReminders() = mainAndroidCoroutineRule.runBlockingTest {
        remindersRepository.saveReminder(reminderDTO)
        val reminders = remindersRepository.getReminders() as Result.Success<List<ReminderDTO>>
        assertEquals(1, reminders.data.size)
        assertEquals(reminderDTO.id, reminders.data.first().id)
        assertEquals(reminderDTO.description, reminders.data.first().description)
        assertEquals(reminderDTO.location, reminders.data.first().location)
        assertEquals(reminderDTO.latitude, reminders.data.first().latitude)
        assertEquals(reminderDTO.longitude, reminders.data.first().longitude)
    }

    @Test
    fun testSaveReminders() = mainAndroidCoroutineRule.runBlockingTest {
        remindersRepository.saveReminder(reminderDTO)
        val reminders = remindersRepository.getReminders() as Result.Success<List<ReminderDTO>>
        assertEquals(1, reminders.data.size)
        assertEquals(reminderDTO.id, reminders.data.first().id)
        assertEquals(reminderDTO.description, reminders.data.first().description)
        assertEquals(reminderDTO.location, reminders.data.first().location)
        assertEquals(reminderDTO.latitude, reminders.data.first().latitude)
        assertEquals(reminderDTO.longitude, reminders.data.first().longitude)
    }

    @Test
    fun testGetReminder() = mainAndroidCoroutineRule.runBlockingTest {
        remindersRepository.saveReminder(reminderDTO)
        val reminder = remindersRepository.getReminder(reminderDTO.id) as Result.Success<ReminderDTO>
        assertNotNull(reminder)
        assertEquals(reminderDTO.id, reminder.data.id)
        assertEquals(reminderDTO.description, reminder.data.description)
        assertEquals(reminderDTO.location, reminder.data.location)
        assertEquals(reminderDTO.latitude, reminder.data.latitude)
        assertEquals(reminderDTO.longitude, reminder.data.longitude)
    }

    @Test
    fun testGetReminderNotFound() = mainAndroidCoroutineRule.runBlockingTest {
        remindersRepository.saveReminder(reminderDTO)
        val reminder = remindersRepository.getReminder("unknown id") as Result.Error
        assertNotNull(reminder)
        assertEquals("Reminder not found!", reminder.message)
        assertNull(reminder.statusCode)
    }

    @Test
    fun testDeleteAllReminders() = mainAndroidCoroutineRule.runBlockingTest{
        remindersRepository.saveReminder(reminderDTO)
        remindersRepository.deleteAllReminders()
        val reminders = remindersRepository.getReminders() as Result.Success<List<ReminderDTO>>
        assertEquals(0, reminders.data.size)
    }
}