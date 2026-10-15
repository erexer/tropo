package io.github.erexer.tropo.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import androidx.test.core.app.ApplicationProvider
import io.github.erexer.tropo.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LauncherIconTest {
    private val cloudCenters = listOf(34 to 94, 43 to 86, 54 to 81, 67 to 75)

    @Test
    fun oceanCloudsAreSolidAndMonochromeCloudsAreCutouts() {
        val foreground = render(R.drawable.ic_launcher_foreground)
        val monochrome = render(R.drawable.ic_launcher_monochrome)
        cloudCenters.forEach { (horizontal, vertical) ->
            val pixel = foreground.getPixel(horizontal, vertical)
            val bright = Color.red(pixel) > 200 && Color.green(pixel) > 200 && Color.blue(pixel) > 200
            assertTrue("cloud $horizontal,$vertical", bright)
            assertEquals("cutout $horizontal,$vertical", 0, Color.alpha(monochrome.getPixel(horizontal, vertical)))
        }
        assertEquals(255, Color.alpha(monochrome.getPixel(54, 95)))
        listOf(70 to 90, 75 to 96, 60 to 100).forEach { (horizontal, vertical) ->
            val pixel = foreground.getPixel(horizontal, vertical)
            assertTrue("clear ocean $horizontal,$vertical", Color.blue(pixel) > Color.green(pixel) * 2)
        }
        val background = render(R.drawable.ic_launcher_background)
        listOf(43 to 27, 54 to 28, 65 to 30, 53 to 36, 50 to 43).forEach { (horizontal, vertical) ->
            assertTrue("star $horizontal,$vertical", Color.red(background.getPixel(horizontal, vertical)) > 100)
            assertTrue("themed star $horizontal,$vertical", Color.alpha(monochrome.getPixel(horizontal, vertical)) > 100)
        }
        System.getenv("TROPO_STORE_ICON")?.let { destination ->
            val context = ApplicationProvider.getApplicationContext<Context>()
            val icon = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(icon)
            listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground).forEach { resource ->
                val layer = requireNotNull(context.getDrawable(resource))
                layer.setBounds(0, 0, icon.width, icon.height)
                layer.draw(canvas)
            }
            export(icon, destination)
        }
    }

    @Test
    fun launcherMasksKeepFrontAndStars() {
        val preview = Bitmap.createBitmap(576, 336, Bitmap.Config.ARGB_8888)
        val previewCanvas = Canvas(preview)
        var top = 0
        listOf(192, 96, 48).forEach { size ->
            val bounds = RectF(0f, 0f, size.toFloat(), size.toFloat())
            val circle = Path().apply { addOval(bounds, Path.Direction.CW) }
            val squircle = Path().apply { addRoundRect(bounds, size * 0.2f, size * 0.2f, Path.Direction.CW) }
            val colorLayers = listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground)
            val variants = listOf(circle to colorLayers, squircle to colorLayers, circle to listOf(R.drawable.ic_launcher_monochrome))
            variants.forEachIndexed { index, (mask, layers) ->
                val icon = renderMasked(size, mask, layers)
                val star = icon.getPixel(size / 2, size * 10 / 72)
                assertTrue("star survives mask at $size", Color.red(star) > 100)
                val cloud = icon.getPixel(size / 2, size * 63 / 72)
                if (index == 2) {
                    assertEquals("themed front at $size", 0, Color.alpha(cloud))
                } else {
                    assertTrue("front survives mask at $size", Color.red(cloud) > 180)
                }
                assertEquals("masked corner at $size", 0, Color.alpha(icon.getPixel(0, 0)))
                previewCanvas.drawBitmap(icon, (index * 192).toFloat(), top.toFloat(), null)
            }
            top += size
        }
        System.getenv("TROPO_ICON_PREVIEW")?.let { export(preview, it) }
    }

    private fun renderMasked(size: Int, mask: Path, resources: List<Int>): Bitmap {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.clipPath(mask)
        resources.forEach { resource ->
            val drawable = requireNotNull(context.getDrawable(resource))
            drawable.setBounds(-size / 4, -size / 4, size * 5 / 4, size * 5 / 4)
            drawable.draw(canvas)
        }
        return bitmap
    }

    private fun export(bitmap: Bitmap, destination: String) {
        val file = File(destination)
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun render(resource: Int): Bitmap {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val drawable = requireNotNull(context.getDrawable(resource))
        val bitmap = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, bitmap.width, bitmap.height)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }
}
