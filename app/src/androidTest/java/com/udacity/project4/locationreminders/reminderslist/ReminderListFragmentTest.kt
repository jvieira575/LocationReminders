package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.FakeRemindersLocalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@MediumTest
class ReminderListFragmentTest {

    private lateinit var repository: FakeRemindersLocalRepository
    private lateinit var applicationContext: Application
    private lateinit var reminderListViewModel: RemindersListViewModel
    private lateinit var reminderDTO : ReminderDTO

    @Before
    fun setup() {
        applicationContext = ApplicationProvider.getApplicationContext()
        repository = FakeRemindersLocalRepository()
        reminderListViewModel = RemindersListViewModel(applicationContext, repository)

        stopKoin()
        val myModule = module {
            single {
                reminderListViewModel
            }
        }
        startKoin {
            modules(listOf(myModule))
        }

        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Test
    fun testError() = runBlockingTest{
        repository.setShouldReturnError(true)
        repository.getReminders() as Result.Error
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // Snackbar should show fake error message from repository
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText("Fake error encountered...")))
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun testEmptyList() = runBlockingTest {
        repository.setShouldReturnError(false)
        repository.getReminders()
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun testList() = runBlockingTest{
        reminderDTO = ReminderDTO("Reminder Title", "Reminder Description","Null Island", 0.0, 0.0)
        repository.saveReminder(reminderDTO)

        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        onView(withText(reminderDTO.title)).check(matches(isDisplayed()))
        onView(withText(reminderDTO.description)).check(matches(isDisplayed()))
        onView(withText(reminderDTO.location)).check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationToSaveReminderFragment() {
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = Mockito.mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withId(R.id.addReminderFAB)).perform(click())
        Mockito.verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder()
        )
    }
}