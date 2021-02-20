package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    companion object {
        private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
        private const val LOCATION_PERMISSION_INDEX = 0
        private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
        private const val NULL_ISLAND_LATITUDE = 0.0
        private const val NULL_ISLAND_LONGITUDE = 0.0
        private const val DEFAULT_MAP_ZOOM = 16.0f
    }

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    // Stores the selected map location values
    private var mapMarker: Marker? = null
    private var selectedMapLocation: LatLng? = null
    private var selectedMapLocationName: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)
        binding.viewModel = _viewModel
        binding.lifecycleOwner = this
        binding.saveButton.setOnClickListener { onLocationSelected() }

        // Construct a FusedLocationProviderClient. Used to zoom to actual location
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Obtain the SupportMapFragment and get notified when the map is ready to be used. Since this is a
        // Fragment within a Fragment we need to use childFragmentManager
        val mapFragment = childFragmentManager.findFragmentById(R.id.google_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        return binding.root
    }

    /**
     * Implementation of [OnMapReadyCallback]. Invoked when the Map is ready to use.
     */
    override fun onMapReady(googleMap: GoogleMap?) {
        if (googleMap != null) {
            map = googleMap
            checkPermissions()
        }
    }

    /**
     * Initializes the [GoogleMap].
     */
    private fun initializeMap() {
        setMapLongClick(map)
        setPoiClick(map)
        setMapStyle(map)
        setCurrentLocation()
    }

    /**
     * Checks for required permissions before configuring map.
     */
    private fun checkPermissions() {

        if (hasRequiredPermissions()) {
            checkDeviceLocationSettings()
        } else {
            requestRequiredPermissions()
        }
    }

    /**
     * Determines whether the app has the required foregraund and background permissions.
     */
    @TargetApi(29)
    private fun hasRequiredPermissions(): Boolean {

        // First, check if the ACCESS_FINE_LOCATION permission is granted
        val foregroundLocationApproved = (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION))

        // If the device is running Q or higher, check that the ACCESS_BACKGROUND_LOCATION permission is granted
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                // Device is lower than Q so return true
                true
            }

        // Return true if the permissions are granted and false if not
        Timber.i("Foreground approved: $foregroundLocationApproved Background approved: $backgroundPermissionApproved")
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    /**
     * Requests required permissions. This method will request ACCESS_FINE_LOCATION for all API levels
     * and if the device is on Android 10 (Q) or later it will also request background permissions.
     */
    @TargetApi(29)
    private fun requestRequiredPermissions() {
        // If the permissions have already been approved, you don’t need to ask again. Return out of the method.
        if (hasRequiredPermissions()) {
            Timber.i("requestRequiredPermissions called. hasRequirePermissions is true, returning")
            checkPermissions()
        }

        // Build the permissions array. ACCESS_FINE_LOCATION is needed on all API levels...
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        // Specify the result code needed to request permissions
        val resultCode = when {
            runningQOrLater -> {
                // If running Q or later, we need to request the background location permission too
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        // Request permissions passing in the current fragment, the permissions array and the result code.
        Timber.i("Requesting permissions: ${permissionsArray.joinToString(" ")}")

        // Need to call this Fragment's requestPermissions method instead of the Activity's
        requestPermissions(permissionsArray, resultCode)
    }

    /**
     * Uses the Location Client to check the current state of location settings, and gives the user
     * the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationSettings(resolve: Boolean = true) {

        // First, create a LocationRequest, a LocationSettingsRequest Builder.
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }

        // Next, use LocationServices to get the Settings Client
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        // Since the case we are most interested in here is finding out if the location settings are
        // not satisfied, add an onFailureListener() to the locationSettingsResponseTask.
        locationSettingsResponseTask.addOnFailureListener { exception ->

            // Check if the exception is of type ResolvableApiException and if so, try calling the
            // startResolutionForResult() method in order to prompt the user to turn on device location.
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(requireActivity(), REQUEST_TURN_DEVICE_LOCATION_ON)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // If calling startResolutionForResult enters the catch block, print a log
                    Timber.e("Error getting location settings resolution: %s", sendEx.message)
                }
            } else {

                // If the exception is not of type ResolvableApiException, present a snackbar that
                // alerts the user that location needs to be enabled
                Snackbar.make(view!!, R.string.location_required_error, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok) {
                    checkDeviceLocationSettings()
                }.show()
            }
        }

        // If the locationSettingsResponseTask does complete, check that it is successful
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                Timber.i("LocationSettingsResponseTask successful. Initializing map...")
                initializeMap()
            }
        }
    }

    /**
     * Sets the current location on the [GoogleMap].
     */
    @SuppressLint("MissingPermission")
    private fun setCurrentLocation() {
        map.isMyLocationEnabled = true
        val lastLocation = fusedLocationProviderClient.lastLocation

        lastLocation?.addOnCompleteListener(activity!!) { task ->
            if (task.isSuccessful) {
                val location = task.result
                location?.let {
                    selectedMapLocation = LatLng(it.latitude, it.longitude)
                    selectedMapLocationName = getLocationSnippet(selectedMapLocation!!)
                    addMapMarker(selectedMapLocation!!)
                }
            } else {
                Timber.e("LocationResult unsuccessful. Apply default location...")
                addMapMarker(LatLng(NULL_ISLAND_LATITUDE, NULL_ISLAND_LONGITUDE))
                map.uiSettings?.isMyLocationButtonEnabled = false
            }
        }
    }

    /**
     * Helper function to create a Location snippet formatted text.
     */
    private fun getLocationSnippet(latLng: LatLng): String {
        return String.format(Locale.getDefault(), getString(R.string.lat_long_snippet), latLng.latitude, latLng.longitude)
    }

    /**
     * Adds a [PointOfInterest] [Marker] and updates the selected location when a user clicks on a
     * [PointOfInterest].
     */
    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->

            // Move to the POI coordinates
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(poi.latLng, DEFAULT_MAP_ZOOM))

            // Remove any previous map Marker
            mapMarker?.remove()

            // Create a new map Marker and show the info window
            mapMarker = map.addMarker(MarkerOptions().position(poi.latLng).title(poi.name))
            mapMarker?.showInfoWindow()

            // Store the selected POI as the selected map location
            selectedMapLocation = poi.latLng
            selectedMapLocationName = poi.name
            Timber.i("Point of Interest selected: $selectedMapLocation $selectedMapLocationName")
        }
    }

    /**
     * Sets a custom [GoogleMap] style.
     */
    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object
            val success = map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style))

            if (!success) {
                Timber.e("Custom Map style parsing failed. Could not apply custom style.")
            }
        } catch (e: Resources.NotFoundException) {
            Timber.e("Can't find custom Map style. Error: %s", e.toString())
        }
    }

    /**
     * Adds a [PointOfInterest] [Marker] and updates the selected location when a user clicks on a
     * location in the [GoogleMap].
     */
    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            // Add a Marker to the map
            addMapMarker(latLng)
        }
    }

    /**
     * Helper method to add a [Marker] to the [GoogleMap].
     */
    private fun addMapMarker(latLng: LatLng) {
        // Move to the map marker coordinates
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_MAP_ZOOM))

        // Remove any previous map Marker
        mapMarker?.remove()

        // Create a new map Marker and show the info window
        mapMarker = this.map.addMarker(MarkerOptions().position(latLng).title(getString(R.string.dropped_pin)).snippet(getLocationSnippet(latLng)))
        mapMarker?.showInfoWindow()

        // Store the selected map location
        selectedMapLocation = latLng
        selectedMapLocationName = getString(R.string.dropped_pin)
        Timber.i("Map location selected: $selectedMapLocation $selectedMapLocationName")
    }

    /**
     * Processes the selected location when the user has clicked to save the selected location.
     */
    private fun onLocationSelected() {
        // Check to ensure the user has selected a location before saving
        if (selectedMapLocation != null && selectedMapLocationName != null) {
            _viewModel.longitude.value = selectedMapLocation!!.longitude
            _viewModel.latitude.value = selectedMapLocation!!.latitude
            _viewModel.reminderSelectedLocationStr.value = selectedMapLocationName
            _viewModel.navigationCommand.postValue(NavigationCommand.Back)
        } else {
            Timber.e("User has not entered a location. Displaying error message.")
            _viewModel.showErrorMessage.value = getString(R.string.err_select_location)
        }
    }

    /**
     * Callback when a request for permission has been made and processed.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Permissions can be denied in a few ways:
        // If the grantResults array is empty, then the interaction was interrupted and the permission request was cancelled.
        // If the grantResults array’s value at the LOCATION_PERMISSION_INDEX has a PERMISSION_DENIED it means that the user denied foreground permissions.
        // If the request code equals REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE and the BACKGROUND_LOCATION_PERMISSION_INDEX is denied it means that the device is running API 29 or above and that background permissions were denied.
        if (grantResults.isEmpty() || grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED)) {
            Timber.e( "Permissions have not granted. Displaying message notifying user to enable them.")

            // This app has very little use when permissions are not granted so present a snackbar
            // explaining that the user needs location permissions in order to play.
            Snackbar.make(requireView(), R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.settings) {
                    // Start activity for user to enable location
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            // Permissions have been granted so proceed
            Timber.i( "Permissions have been granted. Proceeding with other checks... ")
            checkPermissions()
        }
    }

    /**
     * Creates the menu of the fragment.
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    /**
     * Handles menu item clicks. Currently used to change [GoogleMap] map types.
     */
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
