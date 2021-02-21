package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
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
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: SaveReminderViewModel
    private lateinit var dataSource: FakeDataSource
    private lateinit var applicationContext: Application
    private lateinit var reminderDataItem : ReminderDataItem

    @Before
    fun before() {
        stopKoin()
        applicationContext = ApplicationProvider.getApplicationContext()
        dataSource = FakeDataSource()
        viewModel = SaveReminderViewModel(applicationContext, dataSource)
        reminderDataItem = ReminderDataItem("Reminder Title", "Reminder Description","Null Island", 0.0, 0.0)
    }

    @Test
    fun check_loading() = mainCoroutineRule.runBlockingTest {
        mainCoroutineRule.pauseDispatcher()
        viewModel.saveReminder(reminderDataItem)
        assertTrue(viewModel.showLoading.value!!)
        mainCoroutineRule.resumeDispatcher()
        assertFalse(viewModel.showLoading.getOrAwaitValue())
        assertEquals(applicationContext.getString(R.string.reminder_saved), viewModel.showToast.getOrAwaitValue())
        assertEquals(NavigationCommand.Back, viewModel.navigationCommand.getOrAwaitValue())
    }

    @Test
    fun testOnClear() = mainCoroutineRule.runBlockingTest {
        mainCoroutineRule.pauseDispatcher()
        viewModel.onClear()
        mainCoroutineRule.resumeDispatcher()
        assertNull(viewModel.reminderTitle.getOrAwaitValue())
        assertNull(viewModel.reminderDescription.getOrAwaitValue())
        assertNull(viewModel.reminderSelectedLocationStr.getOrAwaitValue())
        assertNull(viewModel.selectedPOI.getOrAwaitValue())
        assertNull(viewModel.latitude.getOrAwaitValue())
        assertNull(viewModel.longitude.getOrAwaitValue())
    }

    @Test
    fun shouldReturnError() = mainCoroutineRule.runBlockingTest {
        reminderDataItem.title = null
        mainCoroutineRule.pauseDispatcher()
        val isValid = viewModel.validateEnteredData(reminderDataItem)
        assertFalse(isValid)
        mainCoroutineRule.resumeDispatcher()
        assertEquals(R.string.err_enter_title, viewModel.showSnackBarInt.getOrAwaitValue())
    }

    @Test
    fun shouldReturnErrorNoLocation() = mainCoroutineRule.runBlockingTest {
        reminderDataItem.location = null
        mainCoroutineRule.pauseDispatcher()
        val isValid = viewModel.validateEnteredData(reminderDataItem)
        assertFalse(isValid)
        mainCoroutineRule.resumeDispatcher()
        assertEquals(R.string.err_select_location, viewModel.showSnackBarInt.getOrAwaitValue())
    }

    @Test
    fun shouldReturnErrorValidateAndSaveReminder() = mainCoroutineRule.runBlockingTest {
        reminderDataItem.title = null
        mainCoroutineRule.pauseDispatcher()
        viewModel.validateAndSaveReminder(reminderDataItem)
        mainCoroutineRule.resumeDispatcher()
        assertEquals(R.string.err_enter_title, viewModel.showSnackBarInt.getOrAwaitValue())
    }

    @Test
    fun shouldReturnValidateAndSaveReminderErrorNoLocation() = mainCoroutineRule.runBlockingTest {
        reminderDataItem.location = null
        mainCoroutineRule.pauseDispatcher()
        viewModel.validateAndSaveReminder(reminderDataItem)
        mainCoroutineRule.resumeDispatcher()
        assertEquals(R.string.err_select_location, viewModel.showSnackBarInt.getOrAwaitValue())
    }
}