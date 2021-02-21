package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest{

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: RemindersListViewModel
    private lateinit var dataSource: FakeDataSource
    private lateinit var applicationContext: Application

    @Before
    fun before() {
        stopKoin()
        applicationContext = ApplicationProvider.getApplicationContext()
        dataSource = FakeDataSource(mutableListOf(ReminderDTO("Reminder Title", "Reminder Description","Null Island", 0.0, 0.0)))
        viewModel = RemindersListViewModel(applicationContext, dataSource)
    }

    @Test
    fun check_loading() = mainCoroutineRule.runBlockingTest {
        dataSource.setShouldReturnError(false)
        mainCoroutineRule.pauseDispatcher()
        viewModel.loadReminders()
        assertTrue(viewModel.showLoading.value!!)
        mainCoroutineRule.resumeDispatcher()
        val remindersList = viewModel.remindersList.getOrAwaitValue()
        assertEquals(1, remindersList.size)
        assertEquals("Reminder Title", remindersList.first().title)
        assertEquals("Reminder Description", remindersList.first().description)
        assertEquals("Null Island", remindersList.first().location)
        assertEquals(0.0, remindersList.first().longitude)
        assertEquals(0.0, remindersList.first().latitude)
        assertFalse(viewModel.showLoading.getOrAwaitValue())
        assertFalse(viewModel.showNoData.getOrAwaitValue())
    }

    @Test
    fun check_loading_empty_list() = mainCoroutineRule.runBlockingTest {
        dataSource = FakeDataSource(mutableListOf())
        dataSource.setShouldReturnError(false)
        viewModel = RemindersListViewModel(applicationContext, dataSource)
        mainCoroutineRule.pauseDispatcher()
        viewModel.loadReminders()
        assertTrue(viewModel.showLoading.getOrAwaitValue())
        mainCoroutineRule.resumeDispatcher()
        val remindersList = viewModel.remindersList.getOrAwaitValue()
        assertEquals(0, remindersList.size)
        assertFalse(viewModel.showLoading.getOrAwaitValue())
        assertTrue(viewModel.showNoData.getOrAwaitValue())
    }

    @Test
    fun shouldReturnError() = mainCoroutineRule.runBlockingTest {
        dataSource.setShouldReturnError(true)
        mainCoroutineRule.pauseDispatcher()
        viewModel.loadReminders()
        assertTrue(viewModel.showLoading.getOrAwaitValue())
        mainCoroutineRule.resumeDispatcher()
        assertEquals("Fake error encountered...", viewModel.showSnackBar.getOrAwaitValue())
        assertTrue(viewModel.showNoData.getOrAwaitValue())
    }
}