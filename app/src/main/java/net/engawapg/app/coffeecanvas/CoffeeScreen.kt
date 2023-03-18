package net.engawapg.app.coffeecanvas

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.sin

private val FRAME_SIZE = Size(500f, 650f)
private val POT_RECT = Rect(Offset(-110f, -10f), Size(558f, 374f))
private val DRIPPER_RECT = Rect(Offset(32f, 402f), Size(185f, 98f))
private val WATER_RECT = Rect(
    top = POT_RECT.center.y, bottom = DRIPPER_RECT.center.y,
    left = DRIPPER_RECT.center.x, right = POT_RECT.center.x
)
private val SERVER_RECT = Rect(Offset(26f, 500f), Size(226f, 150f))
private const val WATER_WIDTH = 8f
private val BG_COLOR = Color(0xffFFF6E9)
private val WATER_COLOR = Color(0xffD1C9C7)

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
        transitionSpec = {
            if (DripState.POURING isTransitioningTo DripState.FINISHED)
                tween(durationMillis = 250, easing = EaseInQuad)
            else
                snap()
        }
    ) { state ->
        if (state == DripState.FINISHED) WATER_RECT.bottom else WATER_RECT.top - WATER_WIDTH
    }

    val waterBottom = transition.animateFloat(
        label = "WaterBottomAnimation",
        transitionSpec = {
            if (DripState.WAITING isTransitioningTo DripState.POURING)
                tween(durationMillis = 250, delayMillis = 200, easing = EaseInQuad)
            else
                snap()
        }
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

    // Time counter for water flow physical simulation.
    var time by remember { mutableStateOf(0) }
    LaunchedEffect(true) {
        while (true) {
            delay(20)
            time++
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
            drawWaterFlow(
                start = WATER_RECT.topRight,
                end = WATER_RECT.bottomLeft,
                width = WATER_WIDTH,
                color = WATER_COLOR,
                transitionData = transitionData,
                time = time
            )

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

private fun DrawScope.drawWaterFlow(
    start: Offset,
    end: Offset,
    width: Float,
    color: Color,
    transitionData: CoffeeTransitionData,
    time: Int
) {
    // Draw a water flow. To create fluctuation in width, draw two lines with different phases.
    val waterYRange = transitionData.waterTop .. transitionData.waterBottom
    val nPoints = 30
    // Definition of coefficients / A2 * n^2 + A1 * n + A0
    val xA0 = start.x
    val xA1 = (end.x - start.x) / nPoints
    val yA0 = start.y
    val yA2 = (end.y - start.y) / nPoints / nPoints

    val theta1 = 0.3f * time // Phase of fluctuation
    val points1 = (0..nPoints).map { n -> Offset(
        x = xA0 + xA1 * n + sin(0.3f * n - theta1) * (1f + n.toFloat() / nPoints),
        y = yA0 + yA2 * n * n
    ) }.filter { it.y in waterYRange }
    drawPoints(
        points = points1,
        pointMode = PointMode.Polygon,
        color = color,
        strokeWidth = width,
    )

    val theta2 = 0.2f * time + 1f // Phase of fluctuation
    val points2 = (0..nPoints).map { n -> Offset(
        x = xA0 + xA1 * n + sin(0.12f * n - theta2) * (1f + n.toFloat() * 1.3f / nPoints),
        y = yA0 + yA2 * n * n
    ) }.filter { it.y in waterYRange }
    drawPoints(
        points = points2,
        pointMode = PointMode.Polygon,
        color = color,
        strokeWidth = width,
    )

    // Additional line to make the spout look natural.
    val startPoints = listOf(
        start, start + Offset(10f, -3f),
    ).filter { it.y in waterYRange }
    drawPoints(
        points = startPoints,
        pointMode = PointMode.Polygon,
        color = color,
        strokeWidth = width,
    )
}
