package com.example.moveai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.amap.api.maps.model.Polyline
import com.amap.api.maps.model.PolylineOptions
import com.example.moveai.databinding.FragmentMapBinding
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.max

class MapFragment : Fragment(R.layout.fragment_map), AMapLocationListener, SensorEventListener {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private var aMap: AMap? = null
    private var locationClient: AMapLocationClient? = null
    private var isTracking = false
    private var isSaving = false
    private var timeSeconds = 0L

    private var gpsDistanceMeters = 0f
    private var stepCount = 0
    private val strideLength = 0.74f
    private var displayDistanceMeters = 0f

    private var currentActivityType = "Run"
    private var currentMetValue = 8.0
    private val userWeightKg = 70.0
    private var lastLocation: LatLng? = null
    private val pathPoints = mutableListOf<LatLng>()
    private var polyline: Polyline? = null

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isTracking) {
                timeSeconds++
                updateUI()
                timerHandler.postDelayed(this, 1000)
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) {
            initMap()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMapBinding.bind(view)
        binding.mapView.onCreate(savedInstanceState)

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        checkPermissionsAndInitMap()

        binding.btnStartPause.setOnClickListener { toggleTracking() }
        binding.tvActivitySelector.setOnClickListener { showActivityMenu() }

        binding.btnMyLocation.setOnClickListener {
            if (lastLocation != null) {
                aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(lastLocation!!, 17f))
            } else {
                val myLoc = aMap?.myLocation
                if (myLoc != null) {
                    val currentLatLng = LatLng(myLoc.latitude, myLoc.longitude)
                    aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
                    lastLocation = currentLatLng
                } else {
                    Toast.makeText(requireContext(), "Searching for GPS...", Toast.LENGTH_SHORT).show()
                    locationClient?.startLocation()
                }
            }
        }

        binding.btnHistory.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .hide(this@MapFragment).add(R.id.fragment_container, HistoryFragment())
                .addToBackStack(null).commit()
        }
    }

    private fun checkPermissionsAndInitMap() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            initMap()
        } else {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun initMap() {
        if (aMap == null) {
            aMap = binding.mapView.map
        }

        // This line sets the map to real view (Satellite mode)
        aMap?.mapType = AMap.MAP_TYPE_SATELLITE

        val style = MyLocationStyle()
        style.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
        style.interval(1000)
        style.showMyLocation(true)

        aMap?.myLocationStyle = style
        aMap?.isMyLocationEnabled = true
        aMap?.uiSettings?.isMyLocationButtonEnabled = false

        if (locationClient == null) {
            locationClient = AMapLocationClient(requireContext())
        }
        val opt = AMapLocationClientOption()
        opt.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        opt.interval = 1000
        opt.isSensorEnable = true
        locationClient?.setLocationOption(opt)
        locationClient?.setLocationListener(this)

        locationClient?.startLocation()
    }

    private fun showActivityMenu() {
        val popup = PopupMenu(requireContext(), binding.tvActivitySelector)
        popup.menu.add(0, 1, 0, "🏃‍♂️ Run")
        popup.menu.add(0, 2, 0, "🚶‍♂️ Walk")
        popup.menu.add(0, 3, 0, "🧗‍♂️ Hike")
        popup.menu.add(0, 4, 0, "🚴‍♂️ Ride")
        popup.menu.add(0, 5, 0, "🏊‍♂️ Swim")

        popup.setOnMenuItemClickListener { item ->
            binding.tvActivitySelector.text = "${item.title} ▼"
            currentActivityType = item.title.toString().replace(" ▼", "")
            currentMetValue = when(item.itemId) {
                1 -> 8.0
                2 -> 3.5
                3 -> 6.0
                4 -> 8.0
                5 -> 7.0
                else -> 8.0
            }
            true
        }
        popup.show()
    }

    private fun toggleTracking() {
        if (!isTracking) {
            isTracking = true
            binding.btnStartPause.setCardBackgroundColor(Color.parseColor("#E53935"))
            binding.ivStartPauseIcon.setImageResource(android.R.drawable.ic_media_pause)
            binding.rippleBg.startRippleAnimation()

            gpsDistanceMeters = 0f
            stepCount = 0
            displayDistanceMeters = 0f

            pathPoints.clear()
            aMap?.clear()
            polyline = null

            lastLocation?.let { pathPoints.add(it) }

            val style = MyLocationStyle()
            style.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
            style.showMyLocation(true)
            aMap?.myLocationStyle = style
            aMap?.isMyLocationEnabled = true

            stepSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST) }

            timerHandler.postDelayed(timerRunnable, 1000)
        } else {
            stopTracking()
        }
    }

    private fun stopTracking() {
        if (!isTracking) return
        isTracking = false
        binding.btnStartPause.setCardBackgroundColor(Color.parseColor("#5E35B1"))
        binding.ivStartPauseIcon.setImageResource(android.R.drawable.ic_media_play)
        binding.rippleBg.stopRippleAnimation()
        timerHandler.removeCallbacks(timerRunnable)

        sensorManager.unregisterListener(this)

        takeSnapshotAndSave()
    }

    private fun takeSnapshotAndSave() {
        if (displayDistanceMeters < 1) {
            Toast.makeText(requireContext(), "No movement detected", Toast.LENGTH_SHORT).show()
            resetTracker()
            return
        }

        isSaving = true
        aMap?.isMyLocationEnabled = false

        if (pathPoints.size < 2) {
            lastLocation?.let {
                aMap?.addMarker(MarkerOptions().position(it).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))
                aMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(it, 18f),
                    1000,
                    object : AMap.CancelableCallback {
                        override fun onFinish() { takeScreenshotWithDelay() }
                        override fun onCancel() { takeScreenshotWithDelay() }
                    }
                )
            } ?: takeScreenshotWithDelay()
            return
        }

        aMap?.addMarker(MarkerOptions().position(pathPoints.first()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))
        aMap?.addMarker(MarkerOptions().position(pathPoints.last()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))

        val boundsBuilder = LatLngBounds.Builder()
        for (p in pathPoints) boundsBuilder.include(p)

        try {
            aMap?.animateCamera(
                CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 150),
                1000,
                object : AMap.CancelableCallback {
                    override fun onFinish() { takeScreenshotWithDelay() }
                    override fun onCancel() { takeScreenshotWithDelay() }
                }
            )
        } catch (e: Exception) {
            takeScreenshotWithDelay()
        }
    }

    private fun takeScreenshotWithDelay() {
        Handler(Looper.getMainLooper()).postDelayed({
            aMap?.getMapScreenShot(object : AMap.OnMapScreenShotListener {
                override fun onMapScreenShot(bitmap: Bitmap?) {
                    if (isSaving) { bitmap?.let { saveData(it) }; isSaving = false }
                }
                override fun onMapScreenShot(b: Bitmap?, s: Int) {}
            })
        }, 800)
    }

    private fun saveData(bitmap: Bitmap?) {
        try {
            var filePath = ""
            if (bitmap != null) {
                val file = File(requireContext().filesDir, "map_snapshot_${System.currentTimeMillis()}.png")
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
                outputStream.close()
                filePath = file.absolutePath
            }

            val distKm = displayDistanceMeters / 1000f
            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", timeSeconds / 60, timeSeconds % 60)
            val paceStr = if (distKm > 0.05f) { val p = (timeSeconds / distKm).toInt(); String.format(Locale.getDefault(), "%d:%02d /km", p / 60, p % 60) } else "0:00 /km"
            val cal = (currentMetValue * userWeightKg * (timeSeconds / 3600.0)).toInt()

            HistoryManager.saveWorkout(requireContext(), currentActivityType, distKm, timeStr, paceStr, cal, filePath)
            Toast.makeText(requireContext(), "Workout Saved!", Toast.LENGTH_SHORT).show()
            resetTracker()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun resetTracker() {
        timeSeconds = 0L; gpsDistanceMeters = 0f; stepCount = 0; displayDistanceMeters = 0f
        pathPoints.clear(); aMap?.clear()

        val style = MyLocationStyle()
        style.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
        style.showMyLocation(true)
        aMap?.myLocationStyle = style
        aMap?.isMyLocationEnabled = true

        polyline = null; updateUI()
    }

    override fun onLocationChanged(amapLocation: AMapLocation?) {
        if (amapLocation != null && amapLocation.errorCode == 0) {

            val currentLatLng = LatLng(amapLocation.latitude, amapLocation.longitude)

            if (lastLocation == null) {
                aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
                lastLocation = currentLatLng
            }

            if (amapLocation.accuracy > 30f) {
                lastLocation = currentLatLng
                return
            }

            if (isTracking) {
                if (pathPoints.isEmpty()) {
                    pathPoints.add(currentLatLng)
                } else if (lastLocation != null) {
                    val distance = AMapUtils.calculateLineDistance(lastLocation, currentLatLng)
                    if (distance > 1.0 && distance < 50.0) {
                        gpsDistanceMeters += distance
                        pathPoints.add(currentLatLng)
                        drawPolyline()
                    }
                }
            }
            lastLocation = currentLatLng
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isTracking && event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
            stepCount++
            updateDistanceLogic()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun updateDistanceLogic() {
        val stepDistance = stepCount * strideLength
        displayDistanceMeters = max(gpsDistanceMeters, stepDistance)
        updateUI()
    }

    private fun drawPolyline() {
        if (polyline == null) polyline = aMap?.addPolyline(PolylineOptions().width(18f).color(Color.parseColor("#06B6D4")).lineJoinType(PolylineOptions.LineJoinType.LineJoinRound))
        polyline?.points = pathPoints
    }

    private fun updateUI() {
        binding.tvTime.text = String.format(Locale.getDefault(), "%02d:%02d", timeSeconds / 60, timeSeconds % 60)
        binding.tvDistance.text = if (displayDistanceMeters < 1000) "${displayDistanceMeters.toInt()} m" else String.format(Locale.getDefault(), "%.2f km", displayDistanceMeters / 1000f)
        val d = displayDistanceMeters / 1000f
        binding.tvPace.text = if (d > 0.01f) { val p = (timeSeconds / d).toInt(); String.format(Locale.getDefault(), "%d:%02d /km", p / 60, p % 60) } else "0:00 /km"
        binding.tvCalories.text = "${(currentMetValue * 70.0 * (timeSeconds / 3600.0)).toInt()} kcal"
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (aMap == null || locationClient == null) {
                initMap()
            } else {
                aMap?.isMyLocationEnabled = true
                locationClient?.startLocation()
            }
        }
    }

    override fun onPause() { super.onPause(); binding.mapView.onPause() }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapView.onDestroy()
        locationClient?.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
        sensorManager.unregisterListener(this)
        _binding = null
    }
}