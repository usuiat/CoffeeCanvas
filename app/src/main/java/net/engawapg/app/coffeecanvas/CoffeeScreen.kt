package net.engawapg.app.coffeecanvas

import androidx.compose.animation.core.EaseInQuad
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
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

enum class DripState {
    WAITING,
    POURING,
    FINISHED,
}

@Composable
fun CoffeeScreen() {
    var dripState by remember { mutableStateOf(DripState.WAITING) }
    LaunchedEffect(true) {
        var counter = 0
        while (true) {
            delay(1000)
            counter++
            when(counter) {
                1 -> dripState = DripState.POURING
                3 -> dripState = DripState.FINISHED
                4 -> {
                    dripState = DripState.WAITING
                    counter = 0
                }
            }
        }
    }
    CoffeeCanvas(dripState)
}

private class CoffeeTransitionData(
    potAngle: State<Float>,
    waterTop: State<Float>,
    waterBottom: State<Float>,
) {
    val potAngle by potAngle
    val waterTop by waterTop
    val waterBottom by waterBottom
}

@Composable
private fun updateCoffeeTransitionData(dripState: DripState): CoffeeTransitionData {
    val transition = updateTransition(dripState, "DripStateTransition")

    val potAngle = transition.animateFloat(
        label = "PotAngleAnimation",
        transitionSpec = { tween(500) }
    ) { state ->
        if (state == DripState.POURING) -30f else 0f
    }

    val waterTop = transition.animateFloat(
        label = "WaterTopAnimation",
        transitionSpec = { tween(durationMillis = 250, easing = EaseInQuad) }
    ) { state ->
        if (state == DripState.FINISHED) WATER_RECT.bottom else WATER_RECT.top - WATER_WIDTH
    }

    val waterBottom = transition.animateFloat(
        label = "WaterBottomAnimation",
        transitionSpec = { tween(durationMillis = 250, easing = EaseInQuad) }
    ) { state ->
        if (state == DripState.WAITING) WATER_RECT.top - WATER_WIDTH else WATER_RECT.bottom
    }

    return remember(transition) {
        CoffeeTransitionData(potAngle, waterTop, waterBottom)
    }
}

@Composable
fun CoffeeCanvas(dripState: DripState) {
    val transitionData = updateCoffeeTransitionData(dripState = dripState)
    val potImage = ImageBitmap.imageResource(id = R.drawable.pot)
    val dripperImage = ImageBitmap.imageResource(id = R.drawable.dripper)
    val serverImage = ImageBitmap.imageResource(id = R.drawable.server)
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
            clipRect(top = transitionData.waterTop, bottom = transitionData.waterBottom) {
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
                degrees = transitionData.potAngle,
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
