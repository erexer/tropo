package io.github.erexer.tropo.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import io.github.erexer.tropo.ui.common.AppLinks

@Composable
@Suppress("FunctionNaming")
internal fun AboutTropo(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var linkError by remember { mutableStateOf(false) }
    val open: (String) -> Unit = { linkError = !AppLinks.open(context, it) }
    val version =
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            .orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tropo $version") },
        text = {
            Column {
                Text("Developer: erexer")
                TextButton(onClick = { open(AppLinks.SUPPORT) }) { Text(AppLinks.EMAIL) }
                TextButton(onClick = { open(AppLinks.PRIVACY) }) { Text("Privacy policy") }
                TextButton(onClick = { open(AppLinks.WEATHER) }) { Text("Weather data by Open-Meteo") }
                TextButton(onClick = { open(AppLinks.DATA_LICENSE) }) { Text("Data license: CC BY 4.0") }
                TextButton(onClick = { open(AppLinks.GEONAMES) }) { Text("Location data: GeoNames") }
                TextButton(onClick = { open(AppLinks.COPERNICUS) }) { Text("Air quality: Copernicus CAMS") }
                TextButton(onClick = { open(AppLinks.CREDITS) }) { Text("Credits and open-source licenses") }
                if (linkError) {
                    Text("No app available to open this link.", color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { AppLinks.copy(context, AppLinks.EMAIL) }) { Text("Copy support address") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
@Suppress("FunctionNaming")
internal fun IssueReportDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val info = remember { context.packageManager.getPackageInfo(context.packageName, 0) }
    var report by rememberSaveable {
        mutableStateOf(
            AppLinks.reportText(
                info.versionName.orEmpty(),
                PackageInfoCompat.getLongVersionCode(info),
                android.os.Build.VERSION.RELEASE,
                android.os.Build.VERSION.SDK_INT,
            ),
        )
    }
    var noEmail by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report an issue") },
        text = {
            Column {
                OutlinedTextField(
                    value = report,
                    onValueChange = { report = it },
                    label = { Text("Issue report") },
                    modifier = Modifier.heightIn(min = 120.dp, max = 260.dp),
                )
                Text(AppLinks.EMAIL, style = MaterialTheme.typography.bodySmall)
                if (noEmail) {
                    Text("No email app available.", color = MaterialTheme.colorScheme.error)
                }
                Row {
                    TextButton(onClick = {
                        AppLinks.copy(context, AppLinks.EMAIL)
                        copied = true
                    }) { Text("Copy address") }
                    TextButton(onClick = {
                        AppLinks.copy(context, report)
                        copied = true
                    }) { Text("Copy report") }
                }
                if (copied) {
                    Text("Copied", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (AppLinks.sendReport(context, report)) {
                    onDismiss()
                } else {
                    noEmail = true
                }
            }) { Text("Open email") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
