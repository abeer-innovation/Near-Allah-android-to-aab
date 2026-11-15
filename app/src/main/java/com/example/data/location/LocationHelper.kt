package com.example.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import com.example.data.calculation.CalculationMethod
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume

object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(context: Context): Result<CityLocation> = withContext(Dispatchers.IO) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        val isNetworkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

        if (!isGpsEnabled && !isNetworkEnabled) {
            return@withContext Result.failure(Exception("Location providers are disabled. Please enable GPS on your device."))
        }

        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            val location: Location? = suspendCancellableCoroutine { continuation ->
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            continuation.resume(loc)
                        } else {
                            // Try last known location fallback
                            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                                continuation.resume(lastLoc)
                            }.addOnFailureListener {
                                continuation.resume(null)
                            }
                        }
                    }
                    .addOnFailureListener {
                        continuation.resume(null)
                    }

                continuation.invokeOnCancellation {
                    cts.cancel()
                }
            }

            if (location == null) {
                return@withContext Result.failure(Exception("Unable to acquire current location. Please verify device GPS or select your city manually."))
            }

            val lat = location.latitude
            val lng = location.longitude
            val currentTimeZone = TimeZone.getDefault().id

            var cityName = "Current Location"
            var countryName = String.format(Locale.US, "%.3f°, %.3f°", lat, lng)

            // Attempt Geocoding
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val locality = address.locality ?: address.subAdminArea ?: address.adminArea
                    val country = address.countryName
                    if (!locality.isNullOrBlank()) {
                        cityName = locality
                    }
                    if (!country.isNullOrBlank()) {
                        countryName = country
                    }
                }
            } catch (e: Exception) {
                // Geocoding network or service failure; retain coordinate fallback
            }

            val isPakistan = (lat in 23.5..37.0 && lng in 60.5..77.5) ||
                    currentTimeZone.contains("Karachi", ignoreCase = true) ||
                    countryName.contains("Pakistan", ignoreCase = true)

            val recMethod = when {
                isPakistan -> CalculationMethod.KARACHI
                currentTimeZone.contains("Riyadh", ignoreCase = true) || countryName.contains("Saudi", ignoreCase = true) -> CalculationMethod.UMM_AL_QURA
                currentTimeZone.contains("Cairo", ignoreCase = true) || countryName.contains("Egypt", ignoreCase = true) -> CalculationMethod.EGYPTIAN
                currentTimeZone.contains("America", ignoreCase = true) || countryName.contains("United States", ignoreCase = true) -> CalculationMethod.ISNA
                else -> CalculationMethod.MUSLIM_WORLD_LEAGUE
            }

            val recAsr = if (isPakistan) com.example.data.calculation.AsrMethod.HANAFI else com.example.data.calculation.AsrMethod.STANDARD

            val resultCity = CityLocation(
                name = cityName,
                country = countryName,
                latitude = lat,
                longitude = lng,
                timezoneId = currentTimeZone,
                recommendedMethod = recMethod,
                recommendedAsrMethod = recAsr
            )

            Result.success(resultCity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
