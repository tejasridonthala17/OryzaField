package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface LocationResult {
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val accuracyMeters: Float?
    ) : LocationResult

    data object PermissionDenied : LocationResult
    data class Unavailable(val reason: String) : LocationResult
}

class FieldLocationProvider(private val context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun isLocationEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentCoordinates(): LocationResult {
        if (!hasLocationPermission()) {
            return LocationResult.PermissionDenied
        }
        if (!isLocationEnabled()) {
            return LocationResult.Unavailable("Device location services (GPS) are disabled.")
        }

        return suspendCancellableCoroutine { continuation ->
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (!continuation.isActive) return@addOnSuccessListener
                    if (location != null) {
                        continuation.resume(
                            LocationResult.Success(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracyMeters = if (location.hasAccuracy()) location.accuracy else null
                            )
                        )
                    } else {
                        // Fall back to lastLocation if current fix timed out indoors
                        fusedClient.lastLocation
                            .addOnSuccessListener { lastLoc ->
                                if (!continuation.isActive) return@addOnSuccessListener
                                if (lastLoc != null) {
                                    continuation.resume(
                                        LocationResult.Success(
                                            latitude = lastLoc.latitude,
                                            longitude = lastLoc.longitude,
                                            accuracyMeters = if (lastLoc.hasAccuracy()) lastLoc.accuracy else null
                                        )
                                    )
                                } else {
                                    continuation.resume(
                                        LocationResult.Unavailable("No GPS signal available at this site.")
                                    )
                                }
                            }
                            .addOnFailureListener { err ->
                                if (continuation.isActive) {
                                    continuation.resume(
                                        LocationResult.Unavailable(
                                            err.localizedMessage ?: "Failed to read GPS location."
                                        )
                                    )
                                }
                            }
                    }
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) {
                        continuation.resume(
                            LocationResult.Unavailable(
                                error.localizedMessage ?: "Failed to capture high-accuracy GPS fix."
                            )
                        )
                    }
                }
        }
    }
}
