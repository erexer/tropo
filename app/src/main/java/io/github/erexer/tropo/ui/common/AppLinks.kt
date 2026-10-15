package io.github.erexer.tropo.ui.common

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

internal object AppLinks {
    const val PRIVACY = "https://erexer.github.io/tropo/privacy.html"
    const val WEATHER = "https://open-meteo.com/"
    const val DATA_LICENSE = "https://creativecommons.org/licenses/by/4.0/"
    const val GEONAMES = "https://www.geonames.org/"
    const val COPERNICUS = "https://atmosphere.copernicus.eu/"
    const val EMAIL = "tropodev@gmail.com"
    const val SUPPORT = "mailto:$EMAIL"
    const val CREDITS = "https://erexer.github.io/tropo/credits.html"

    fun open(
        context: Context,
        url: String,
    ): Boolean = launch(context, Intent(Intent.ACTION_VIEW, url.toUri()))

    fun sendReport(
        context: Context,
        report: String,
    ): Boolean =
        launch(
            context,
            Intent(Intent.ACTION_SENDTO, SUPPORT.toUri())
                .putExtra(Intent.EXTRA_SUBJECT, "Tropo issue report")
                .putExtra(Intent.EXTRA_TEXT, report),
        )

    fun reportText(
        version: String,
        code: Long,
        android: String,
        api: Int,
    ): String =
        """
        What happened:

        Expected behavior:

        Steps to reproduce:

        Tropo $version ($code)
        Android $android (API $api)
        """.trimIndent()

    fun copy(
        context: Context,
        text: String,
    ) {
        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Tropo", text))
    }

    private fun launch(
        context: Context,
        intent: Intent,
    ): Boolean =
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
}
