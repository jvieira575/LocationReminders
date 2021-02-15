package com.udacity.project4.authentication

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.udacity.project4.base.BaseViewModel

/**
 * [ViewModel] class attached to the [AuthenticationActivity]. Responsible for observing authentication
 * state, determining if the user is authenticated or unauthenticated.
 */
class AuthenticationViewModel(app: Application) : BaseViewModel(app) {

    // Enum to store different states of authentication
    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED, INVALID_AUTHENTICATION
    }

    val authenticationState = FirebaseUserLiveData().map { user ->
        if (user != null) {
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }
}