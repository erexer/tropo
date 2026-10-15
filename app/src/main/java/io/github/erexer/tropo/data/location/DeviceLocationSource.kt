package io.github.erexer.tropo.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import io.github.erexer.tropo.data.local.LocationEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToLong

fun interface DeviceLocationSource {
    suspend fun locate(): LocationEntity
}

internal class AndroidDeviceLocation(
    private val context: Context,
) : DeviceLocationSource {
    override suspend fun locate(): LocationEntity =
        withTimeout(LOCATION_TIMEOUT) {
            val location = position()
            require(SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos <= MAX_AGE_NANOS) {
                "Device position is stale. Retry outdoors."
            }
            val address =
                try {
                    withTimeoutOrNull(ADDRESS_TIMEOUT) { address(location) }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    null
                }
            val latitudeKey = ((location.latitude + LATITUDE_OFFSET) * COORDINATE_SCALE).roundToLong()
            val longitudeKey = ((location.longitude + LONGITUDE_OFFSET) * COORDINATE_SCALE).roundToLong()
            LocationEntity(
                id = -((latitudeKey + 1) * LONGITUDE_SLOTS + longitudeKey + 1),
                name = address?.locality ?: address?.subAdminArea ?: "Current location",
                country = address?.countryName.orEmpty(),
                latitude = location.latitude,
                longitude = location.longitude,
                admin1 = address?.adminArea.orEmpty(),
            )
        }

    private suspend fun position(): Location =
        suspendCancellableCoroutine { continuation ->
            val manager = context.getSystemService(LocationManager::class.java)
            val fine =
                hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            val coarse =
                hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (!fine && !coarse) {
                continuation.resumeWithException(SecurityException("Location permission is required."))
                return@suspendCancellableCoroutine
            }
            val providers = manager.getProviders(true)
            val provider =
                when {
                    fine && LocationManager.GPS_PROVIDER in providers -> LocationManager.GPS_PROVIDER
                    LocationManager.NETWORK_PROVIDER in providers -> LocationManager.NETWORK_PROVIDER
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && LocationManager.FUSED_PROVIDER in providers ->
                        LocationManager.FUSED_PROVIDER
                    else -> null
                }
            if (provider == null) {
                continuation.resumeWithException(IllegalStateException("Turn on device location and retry."))
                return@suspendCancellableCoroutine
            }
            val cancellation = CancellationSignal()
            continuation.invokeOnCancellation { cancellation.cancel() }
            val executor = ContextCompat.getMainExecutor(context)
            try {
                LocationManagerCompat.getCurrentLocation(manager, provider, cancellation, executor) { fix ->
                    if (continuation.isActive) {
                        if (fix == null) {
                            continuation.resumeWithException(
                                IllegalStateException("Position unavailable. Retry outdoors."),
                            )
                        } else {
                            continuation.resume(fix)
                        }
                    }
                }
            } catch (error: SecurityException) {
                cancellation.cancel()
                if (continuation.isActive) {
                    continuation.resumeWithException(error)
                }
            }
        }

    @Suppress("DEPRECATION")
    private suspend fun address(location: Location): Address? {
        if (!Geocoder.isPresent()) {
            return null
        }
        val geocoder = Geocoder(context, Locale.getDefault())
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            withContext(Dispatchers.IO) {
                runCatching {
                    geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
                }.getOrNull()
            }
        } else {
            lookupAddress(geocoder, location)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun lookupAddress(
        geocoder: Geocoder,
        location: Location,
    ): Address? =
        suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocation(
                location.latitude,
                location.longitude,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (continuation.isActive) {
                            continuation.resume(addresses.firstOrNull())
                        }
                    }

                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                },
            )
        }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private const val LOCATION_TIMEOUT = 20_000L
private const val ADDRESS_TIMEOUT = 4_000L
private const val MAX_AGE_NANOS = 60_000_000_000L
private const val COORDINATE_SCALE = 10_000
private const val LATITUDE_OFFSET = 90
private const val LONGITUDE_OFFSET = 180
private const val LONGITUDE_SLOTS = 3_600_001L
