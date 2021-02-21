package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SaveReminderFragment : BaseFragment() {

    companion object {
        private const val DEFAULT_GEOFENCE_RADIUS_IN_METERS = 100f
        internal const val ACTION_GEOFENCE_EVENT = "RemindersActivity.action.ACTION_GEOFENCE_EVENT"
    }

    // Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)
        binding.viewModel = _viewModel

        setDisplayHomeAsUpEnabled(true)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        // Obtain a reference to a Geofencing Client
        geofencingClient = LocationServices.getGeofencingClient(activity!!)

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            // Save the reminder
            val reminder = ReminderDataItem(title, description, location, latitude, longitude)
            _viewModel.validateAndSaveReminder(reminder)

            // Create the Geofence
            val geofence = createGeofence(reminder.latitude!!, reminder.longitude!!, reminder.id)

            // Create Geofence request
            val geofencingRequest = createGeofenceRequest(geofence)

            // Create Geofence pending intent
            val geofencePendingIntent = createGeofencePendingIntent()

            // Add the Geofence
            addGeofence(geofencingRequest, geofencePendingIntent)
        }
    }

    /**
     * Create a [Geofence] with the given coordinates and id.
     */
    private fun createGeofence(latitude: Double, longitude: Double, id: String): Geofence {
        return Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(latitude, longitude, DEFAULT_GEOFENCE_RADIUS_IN_METERS)
            .setExpirationDuration(TimeUnit.DAYS.toMillis(1))
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()
    }

    /**
     * Creates a [GeofencingRequest] for the given [Geofence].
     */
    private fun createGeofenceRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
    }

    /**
     * Constructs a [PendingIntent] for the [Geofence].
     */
    private fun createGeofencePendingIntent(): PendingIntent {
        val intent = Intent(activity, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        return PendingIntent.getBroadcast(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /**
     * Adds a [Geofence] using the given [GeofencingRequest] and [PendingIntent].
     */
    @SuppressLint("MissingPermission")
    private fun addGeofence(geofencingRequest: GeofencingRequest, geofencePendingIntent: PendingIntent) {
        geofencingClient.removeGeofences(geofencePendingIntent)?.run {
            addOnCompleteListener {
                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                    addOnSuccessListener {
                        activity?.let {
                            _viewModel.showToast.value = "Added Geofence!"
                        }
                    }
                    addOnFailureListener {
                        activity?.let {
                            _viewModel.showToast.value = getString(R.string.geofences_not_added)
                        }
                        if ((it.message != null)) {
                            Timber.e("Failure encountered adding Geofence: %s", it.message.toString())
                        }
                    }
                }
            }
        }
    }

    /**
     * Lifecycle callback when this [BaseFragment] is about to be destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        // Make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
