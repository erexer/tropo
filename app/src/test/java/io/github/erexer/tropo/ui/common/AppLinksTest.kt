package io.github.erexer.tropo.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppLinksTest {
    @Test
    fun missingHandlersDoNotCrash() {
        val context = RecordingContext(ActivityNotFoundException())
        assertFalse(AppLinks.open(context, AppLinks.PRIVACY))
        assertFalse(AppLinks.sendReport(context, "reviewed report"))
    }

    @Test
    fun blockedHandlersDoNotCrash() {
        assertFalse(AppLinks.open(RecordingContext(SecurityException()), AppLinks.SUPPORT))
    }

    @Test
    fun reportOpensDraftNotAutomaticSend() {
        val context = RecordingContext()
        assertTrue(AppLinks.sendReport(context, "reviewed report"))
        assertEquals(Intent.ACTION_SENDTO, context.intent?.action)
        assertEquals(AppLinks.SUPPORT, context.intent?.data.toString())
        assertEquals("reviewed report", context.intent?.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun templateContainsOnlyExplicitMetadata() {
        val report = AppLinks.reportText("1.0.0", 2L, "17", 37)
        assertTrue(report.contains("Tropo 1.0.0 (2)"))
        assertTrue(report.contains("Android 17 (API 37)"))
        assertTrue(report.contains("What happened:"))
        assertTrue(report.contains("Expected behavior:"))
        assertTrue(report.contains("Steps to reproduce:"))
        assertFalse(report.contains("latitude", ignoreCase = true))
        assertFalse(report.contains("longitude", ignoreCase = true))
        assertFalse(report.contains("identifier", ignoreCase = true))
    }

    private class RecordingContext(
        private val failure: RuntimeException? = null,
    ) : ContextWrapper(ApplicationProvider.getApplicationContext<Context>()) {
        var intent: Intent? = null

        override fun startActivity(intent: Intent) {
            failure?.let { throw it }
            this.intent = intent
        }
    }
}
