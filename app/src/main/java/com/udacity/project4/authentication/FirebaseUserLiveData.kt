package com.udacity.project4.authentication

import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/**
 * This class observes the current FirebaseUser. If there is no logged in user, FirebaseUser will
 * be null.
 **/
class FirebaseUserLiveData : LiveData<FirebaseUser?>() {

    // The FirebaseAuth instance (entry point into the Firebase Authentication instance the app is using)
    private val firebaseAuth = FirebaseAuth.getInstance()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->

        // Sets the value of this FireUserLiveData object by hooking it up to equal the value of the
        // current FirebaseUser
        value = firebaseAuth.currentUser
    }

    override fun onActive() {

        // Attach the authentication state listener
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onInactive() {

        // Remove the authentication state listener (prevents memory leaks)
        firebaseAuth.removeAuthStateListener(authStateListener)
    }
}