package com.udacity.project4.authentication

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.firebase.ui.auth.AuthUI
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity
import timber.log.Timber

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    companion object {
        const val SIGN_IN_RESULT_CODE = 99
    }

    private val viewModel by viewModels<AuthenticationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityAuthenticationBinding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_authentication
        )
        binding.loginButton.setOnClickListener { launchSignInFlow() }

        // If the user was authenticated, send user to RemindersActivity
        viewModel.authenticationState.observe(this, Observer {
            when (it) {
                AuthenticationViewModel.AuthenticationState.AUTHENTICATED -> {
                    Timber.i("AuthenticationState is AUTHENTICATED. Starting RemindersActivity...")
                    val intent = Intent(this, RemindersActivity::class.java).apply {

                        // Start new activity in new task
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                }
                AuthenticationViewModel.AuthenticationState.UNAUTHENTICATED -> {
                    Timber.i("AuthenticationState is UNAUTHENTICATED. Remaining on AuthenticationActivity...")
                }
                AuthenticationViewModel.AuthenticationState.INVALID_AUTHENTICATION -> {
                    Timber.i("AuthenticationState is INVALID_AUTHENTICATION. Remaining on AuthenticationActivity...")
                }
                else -> {
                    Timber.i("AuthenticationState is null. Remaining on AuthenticationActivity...")
                }
            }
        })
    }

    /**
     * Launches the sign in flow of the application.
     */
    private fun launchSignInFlow() {
        // Give users the option to sign in / register with their email or Google account. If users
        // choose to register with their email, they will need to create a password as well.
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // Create and launch sign-in intent. We listen to the response of this activity with the
        // SIGN_IN_RESULT_CODE code.
        startActivityForResult(
            AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(
                providers
            ).build(), SIGN_IN_RESULT_CODE
        )
    }
}
