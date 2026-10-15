package io.github.erexer.tropo.ui.common

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import io.github.erexer.tropo.R
import io.github.erexer.tropo.ui.locations.LocationsViewModel

@Composable
@Suppress("FunctionNaming")
internal fun CurrentLocationAction(
    model: LocationsViewModel,
    onSelected: () -> Unit,
    style: LocationActionStyle,
) {
    val context = LocalContext.current
    val busy by model.usingDeviceLocation.collectAsState()
    var denied by rememberSaveable { mutableStateOf(false) }
    val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    val activity =
        generateSequence(context) { (it as? ContextWrapper)?.baseContext }.filterIsInstance<Activity>().firstOrNull()
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants.values.any { it }) {
                model.useDeviceLocation(onSelected)
            } else {
                denied = true
                model.locationPermissionDenied()
            }
        }
    val request = {
        val granted = hasLocationPermission(context, permissions)
        val previouslyDenied =
            activity?.let { host ->
                permissions.any { ActivityCompat.shouldShowRequestPermissionRationale(host, it) }
            } == true
        when {
            granted -> {
                denied = false
                model.useDeviceLocation(onSelected)
            }
            denied || previouslyDenied -> {
                denied = true
                model.locationPermissionDenied()
                if (style == LocationActionStyle.ICON) {
                    openLocationSettings(context)
                }
            }
            else -> launcher.launch(permissions)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (style == LocationActionStyle.ICON) {
            IconButton(
                enabled = !busy,
                onClick = request,
                modifier =
                    Modifier.semantics {
                        contentDescription = if (denied) "Location permission settings" else "Use current location"
                    },
            ) { LocationActionIcon(busy) }
        } else {
            TextButton(enabled = !busy, onClick = request) {
                LocationActionIcon(busy)
                Text("Use current location", modifier = Modifier.padding(start = 6.dp))
            }
        }
        if (denied && style == LocationActionStyle.LABEL) {
            TextButton(onClick = { openLocationSettings(context) }) { Text("App settings") }
        }
    }
}

private fun openLocationSettings(context: Context) {
    val packageUri = "package:${context.packageName}".toUri()
    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri))
}

private fun hasLocationPermission(
    context: Context,
    permissions: Array<String>,
): Boolean =
    permissions.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

@Composable
@Suppress("FunctionNaming")
private fun LocationActionIcon(busy: Boolean) {
    if (busy) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
    } else {
        Icon(painterResource(R.drawable.ic_location_target), "Use current location", modifier = Modifier.size(18.dp))
    }
}
