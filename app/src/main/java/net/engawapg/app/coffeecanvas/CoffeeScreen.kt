package net.engawapg.app.coffeecanvas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInQuad
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

private val FRAME_SIZE = Size(500f, 650f)
private val POT_RECT = Rect(Offset(-110f, -10f), Size(558f, 374f))
private val WATER_RECT = Rect(Offset(133f, 174f), Size(-43f, 228f))
private val DRIPPER_RECT = Rect(Offset(32f, 402f), Size(185f, 98f))
private val SERVER_RECT = Rect(Offset(26f, 500f), Size(226f, 150f))
private const val WATER_WIDTH = 8f
private val BG_COLOR = Color(0xffFFF6E9)
private val COFFEE_COLOR = Color(0xb4280A00)

internal fun Size.toInt() = IntSize(width.toInt(), height.toInt())
internal fun Offset.toInt() = IntOffset(x.toInt(), y.toInt())

@Composable
fun CoffeeScreen() {
    val potImage = ImageBitmap.imageResource(id = R.drawable.pot)
    val dripperImage = ImageBitmap.imageResource(id = R.drawable.dripper)
    val serverImage = ImageBitmap.imageResource(id = R.drawable.server)
    val potAngle = remember { Animatable(0f) }
    val waterBottom = remember { Animatable(WATER_RECT.top - WATER_WIDTH) }
    val waterTop = remember { Animatable(WATER_RECT.top - WATER_WIDTH) }
    LaunchedEffect(potAngle, waterBottom, waterTop) {
        launch {
            delay(500)
            potAngle.animateTo(-30f, animationSpec = tween(500))
            delay(1000)
            potAngle.animateTo(0f, animationSpec = tween(500))
        }
        launch {
            delay(700)
            waterBottom.animateTo(WATER_RECT.bottom, animationSpec = tween(durationMillis = 250, easing = EaseInQuad))
        }
        launch {
            delay(2200)
            waterTop.animateTo(WATER_RECT.bottom, animationSpec = tween(durationMillis = 250, easing = EaseInQuad))
        }
    }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(BG_COLOR)
    ) {
        val scale = min((size.width / FRAME_SIZE.width), (size.height / FRAME_SIZE.height))
        withTransform({
            translate(left = center.x - FRAME_SIZE.center.x, top = center.y - FRAME_SIZE.center.y)
            scale(scale, scale, pivot = FRAME_SIZE.center)
        }) {
            clipRect(top = waterTop.value, bottom = waterBottom.value) {
                val path = Path().apply {
                    moveTo(POT_RECT.center.x, POT_RECT.center.y)
                    relativeQuadraticBezierTo(WATER_RECT.width * 0.7f, 0f, WATER_RECT.width, WATER_RECT.height)
                    moveTo(POT_RECT.center.x, POT_RECT.center.y)
                    relativeLineTo(10f, -3f)
                }
                drawPath(
                    path = path,
                    color = COFFEE_COLOR,
                    style = Stroke(WATER_WIDTH)
                )
            }

            rotate(
                degrees = potAngle.value,
                pivot = POT_RECT.center
            ) {
                drawImage(
                    image = potImage,
                    dstOffset = POT_RECT.topLeft.toInt(),
                    dstSize = POT_RECT.size.toInt(),
                )
            }
            drawImage(
                image = dripperImage,
                dstOffset = DRIPPER_RECT.topLeft.toInt(),
                dstSize = DRIPPER_RECT.size.toInt(),
            )
            drawImage(
                image = serverImage,
                dstOffset = SERVER_RECT.topLeft.toInt(),
                dstSize = SERVER_RECT.size.toInt(),
            )
        }
    }
}
