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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

private val FRAME_SIZE = Size(500f, 650f)
private val POT_RECT = Rect(Offset(-110f, -10f), Size(558f, 374f))
private val DRIPPER_RECT = Rect(Offset(32f, 402f), Size(185f, 98f))
private val WATER_RECT = Rect(
    top = POT_RECT.center.y, bottom = DRIPPER_RECT.center.y,
    left = DRIPPER_RECT.center.x, right = POT_RECT.center.x
)
private val SERVER_RECT = Rect(Offset(26f, 500f), Size(226f, 150f))
private val DROP_START = DRIPPER_RECT.bottomCenter
private val DROP_END = Offset(DROP_START.x, SERVER_RECT.bottom)
private const val WATER_WIDTH = 8f
private val BG_COLOR = Color(0xffFFF6E9)
private val WATER_COLOR = Color(0xffD1C9C7)
private val COFFEE_COLOR = Color(0xff1D100C)
private const val COFFEE_LEVEL = 112f
private const val COFFEE_LEVEL_FULL = 38f

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
    waterMask: State<Rect>,
) {
    val potAngle by potAngle
    val waterMask by waterMask
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

    val waterMask = transition.animateRect(
        label = "WaterRectAnimation",
        transitionSpec = {
            when {
                DripState.WAITING isTransitioningTo DripState.POURING ->
                    tween(durationMillis = 250, delayMillis = 200, easing = EaseInQuad)
                DripState.POURING isTransitioningTo DripState.FINISHED ->
                    tween(durationMillis = 250, easing = EaseInQuad)
                else ->
                    snap()
            }
        }
    ) { state ->
        val rect = WATER_RECT.inflate(10f)
        when(state) {
            DripState.WAITING -> rect.translate(0f, -rect.height)
            DripState.POURING -> rect
            DripState.FINISHED -> rect.translate(0f, rect.height)
        }
    }

    return remember(transition) {
        CoffeeTransitionData(potAngle, waterMask)
    }
}

data class CoffeeDrop(
    val startTime: Long,
    var y: Float = 0f,
    var reached: Boolean = false,
)

@Composable
fun CoffeeCanvas(dripState: DripState) {
    val transitionData = updateCoffeeTransitionData(dripState = dripState)
    val potImage = ImageBitmap.imageResource(id = R.drawable.pot)
    val dripperImage = ImageBitmap.imageResource(id = R.drawable.dripper)
    val serverImage = ImageBitmap.imageResource(id = R.drawable.server)
    val coffeeImage = ImageBitmap.imageResource(id = R.drawable.coffee_in_server)

    var isDropping by remember { mutableStateOf(false) }
    LaunchedEffect(dripState) {
        delay(800)
        isDropping = dripState == DripState.POURING
    }

    val coffeeLevel = remember { Animatable(SERVER_RECT.height - 4) }

    var dropYn by remember { mutableStateOf(listOf<Float>()) }
    val updatedDropping by rememberUpdatedState(isDropping)
    var time by remember { mutableStateOf(0L) }
    var landingTimes by remember { mutableStateOf(listOf<Long>()) }
    LaunchedEffect(true) {
        val interval = 250L
        val num = 5
        val a = (DROP_END.y - DROP_START.y) / 1_000_000.0f
        val drops = mutableListOf<CoffeeDrop>()
        while (true) {
            withFrameMillis { t ->
                time = t
                if (updatedDropping) {
                    if (drops.isEmpty() || (t > drops.last().startTime + interval)) {
                        drops.add(CoffeeDrop(t))
                    }
                }
                if (drops.isNotEmpty() && (t > drops.first().startTime + (interval * num))) {
                    drops.removeAt(0)
                }
                drops.forEach {
                    it.y = a * (it.startTime - t) * (it.startTime - t)
                    if (!it.reached && (it.y > coffeeLevel.value)) {
                        it.reached = true
                        launch {
                            coffeeLevel.animateTo(coffeeLevel.value - (COFFEE_LEVEL / 8 / 5))
                        }
                        landingTimes = listOf(t) + if (landingTimes.size == 5) landingTimes.dropLast(1) else landingTimes
                    }
                }
                if (dropYn.isNotEmpty() || drops.isNotEmpty()) {
                    dropYn = drops.map { a * (it.startTime - t) * (it.startTime - t) }
                }
                if (!updatedDropping && dropYn.isEmpty() && (coffeeLevel.value < COFFEE_LEVEL_FULL)) {
                    launch {
                        coffeeLevel.snapTo(SERVER_RECT.height)
                    }
                }
            }
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
                image = coffeeImage,
                dstOffset = SERVER_RECT.topLeft.toInt(),
                dstSize = SERVER_RECT.size.toInt(),
            )

            val yBase = SERVER_RECT.top + coffeeLevel.value
            val nPoints = 20
            val dx = SERVER_RECT.width / 2 / nPoints
            val omega = 2f * PI.toFloat() / 300
            val points = (0 .. nPoints).map { n ->
                var y = yBase
                for (lt in landingTimes) {
                    val t = max(0, time - lt - (n * 1000 / nPoints))
                    val amp = 2f * max(0, 2000 - t) / 2000f
                    y -= amp * sin(omega * t)
                }
                Offset(dx * n, y)
            }
            val path = Path().apply {
                moveTo(DROP_START.x - points.last().x, SERVER_RECT.top)
                points.reversed().forEach { lineTo(DROP_START.x - it.x, it.y)}
                points.forEach { lineTo(DROP_START.x + it.x, it.y) }
                lineTo(DROP_START.x + points.last().x, SERVER_RECT.top)
                close()
            }
            drawPath(
                path = path,
                color = BG_COLOR,
            )

            drawCoffeeDrops(
                start = DROP_START,
                end = DROP_END,
                yn = dropYn,
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
    time: Long
) {
    clipRect(top = transitionData.waterMask.top, bottom = transitionData.waterMask.bottom) {
        // Draw a water flow. To create fluctuation in width, draw two lines with different phases.
        val nPoints = 30
        // Definition of coefficients / A2 * n^2 + A1 * n + A0
        val xA0 = start.x
        val xA1 = (end.x - start.x) / nPoints
        val yA0 = start.y
        val yA2 = (end.y - start.y) / nPoints / nPoints

        val theta1 = 0.015f * time // Phase of fluctuation
        val points1 = (0..nPoints).map { n ->
            Offset(
                x = xA0 + xA1 * n + sin(0.3f * n - theta1) * (1f + n.toFloat() / nPoints),
                y = yA0 + yA2 * n * n
            )
        }
        drawPoints(
            points = points1,
            pointMode = PointMode.Polygon,
            color = color,
            strokeWidth = width,
        )

        val theta2 = 0.01f * time + 1f // Phase of fluctuation
        val points2 = (0..nPoints).map { n ->
            Offset(
                x = xA0 + xA1 * n + sin(0.12f * n - theta2) * (1f + n.toFloat() * 1.3f / nPoints),
                y = yA0 + yA2 * n * n
            )
        }
        drawPoints(
            points = points2,
            pointMode = PointMode.Polygon,
            color = color,
            strokeWidth = width,
        )

        // Additional line to make the spout look natural.
        val startPoints = listOf(
            start, start + Offset(10f, -3f),
        )
        drawPoints(
            points = startPoints,
            pointMode = PointMode.Polygon,
            color = color,
            strokeWidth = width,
        )
    }
}

private fun DrawScope.drawCoffeeDrops(
    start: Offset,
    end: Offset,
    yn: List<Float>,
) {
    clipRect(top = start.y, bottom = end.y) {
        for (y in yn) {
            val dropPath = Path().apply {
                val r = 6f // Radius of the arc.
                val t = 12f // distance from the center of the arc to the top.
                val c1 = 4.8f // distance from the edge of the arc to the control point.
                val c2 = 3f // distance from the top to the control point.
                addArc(Rect(center = Offset.Zero, radius = r), 0f, 180f)
                relativeCubicTo(0f, -c1, r, -(t - c2), r, -t)
                relativeCubicTo(0f, c2, r, t - c1, r, t)
                translate(Offset(start.x, start.y + y))
            }
            drawPath(
                path = dropPath,
                color = COFFEE_COLOR,
            )
        }
    }
}
