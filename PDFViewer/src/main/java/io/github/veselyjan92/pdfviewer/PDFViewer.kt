@file:OptIn(ExperimentalFoundationApi::class)

package io.github.veselyjan92.pdfviewer

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs


@Composable
fun PDFViewer(
    state: PDFViewerState,
    modifier: Modifier = Modifier.fillMaxSize(),
    verticalSpacing: Dp = 4.dp,
    contentPadding: PaddingValues = PaddingValues(4.dp),
    pageBox: @Composable (page: @Composable () -> Unit) -> Unit = { page ->
        Box(
            Modifier.shadow(elevation = 2.dp)
        ) {
            page()
        }
    }
) {
    LaunchedEffect(Unit) {
        state.loadPages()
    }

    DisposableEffect(Unit) {
        onDispose {
            state.clear()
        }
    }

    val pages by state.pages.collectAsState(listOf())

    if (pages.isNotEmpty()) {
        PDF(
            state = state,
            modifier = modifier,
            pageBox = pageBox,
            verticalSpacing = verticalSpacing,
            contentPadding = contentPadding
        )
    }
}

@Composable
private fun PDF(
    modifier: Modifier,
    state: PDFViewerState,
    verticalSpacing: Dp,
    contentPadding: PaddingValues,
    pageBox: @Composable (page: @Composable () -> Unit) -> Unit
) {
    val itemProvider = remember(state.pages) {
        object : LazyLayoutItemProvider {

            override val itemCount: Int = state.pageCount

            @Composable
            override fun Item(index: Int, key: Any) {
                pageBox {
                    PDFPage(state, index)
                }
            }
        }
    }
    val verticalSpacingPx = with(LocalDensity.current) { verticalSpacing.toPx() }

    val direction = LocalLayoutDirection.current
    val contentPaddingPx = with(LocalDensity.current) {
        Rect(
            top = contentPadding.calculateTopPadding().toPx().toFloat(),
            left = contentPadding.calculateEndPadding(direction).toPx().toFloat(),
            bottom = contentPadding.calculateBottomPadding().toPx().toFloat(),
            right = contentPadding.calculateStartPadding(direction).toPx().toFloat(),
        )
    }

    var scale by remember { mutableFloatStateOf(1f) }

    LazyLayout(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clipToBounds()
            .pointerInput(Unit) {
                detectZoom { zoom ->
                    scale = (scale * zoom).coerceIn(1f, 2f)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapCenter ->
                        scale = if (scale > 1.0f) 1f else 2f
                    }
                )
            }
            .pointerInput(Unit) {
                val velocityTracker = VelocityTracker()
                coroutineScope {
                    detectDragGestures(
                        onDragEnd = {
                            var velocity = velocityTracker.calculateVelocity()

                            launch {
                                state.flingBy(velocity)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)

                            launch {
                                state.dragBy(dragAmount)
                            }
                        }
                    )
                }
            },
        itemProvider = { itemProvider },
    ) { constraints ->
        val viewport = Size(
            constraints.maxWidth.toFloat(),
            constraints.maxHeight.toFloat()
        )

        var cumulativePageYCoordinate = contentPaddingPx.top.toFloat()

        data class PageLocation(
            val index: Int,
            val x: Float,
            val y: Float,
            val width: Float,
            val height: Float,
        )

        val placeables = state.pages.value.mapIndexed { index, page ->
            //Calculate page coordinates

            val width = viewport.width - contentPaddingPx.left - contentPaddingPx.right
            val height = page.ration * width

            val item = PageLocation(
                index = index,
                x = contentPaddingPx.left,
                y = cumulativePageYCoordinate,
                width = width,
                height = height
            )

            cumulativePageYCoordinate += height + verticalSpacingPx

            item
        }.filter {
            //Filter out pages that are not visible

            it.y + it.height > state.translateY.value - 0.5 * viewport.height && it.y < state.translateY.value + 1.5 * viewport.height
        }.map { page ->
            //Translate page coordinates to scroll and drag offset

            page.copy(
                x = page.x - state.translateX.value,
                y = page.y - state.translateY.value,
            )
        }.map { page ->
            //map placeable base on fixed size

            measure(
                page.index,
                Constraints.Companion.fixed(
                    page.width.toInt(),
                    page.height.toInt()
                )
            ) to Offset(page.x, page.y)
        }

        //Update zoom bounds
        val shiftX = (viewport.width / 4f) * (1 - scale)
        val shiftY = viewport.height * ((1 / scale) - 1) / 2.0f

        state.updateBounds(
            left = shiftX,
            top = shiftY,
            right = -shiftX,
            bottom = cumulativePageYCoordinate - verticalSpacingPx + contentPaddingPx.bottom - viewport.height - shiftY
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { (itemPlaceables, position) ->
                itemPlaceables.first().placeRelative(
                    x = position.x.toInt(),
                    y = position.y.toInt(),
                )
            }
        }
    }
}

@Composable
private fun PDFPage(
    state: PDFViewerState,
    index: Int,
) {
    BoxWithConstraints {
        val width = this.constraints.maxWidth

        var bitmap by remember {
            mutableStateOf<Bitmap?>(
                null
            )
        }

        LaunchedEffect(index) {
            withContext(Dispatchers.Default) {
                bitmap = state.loadPage(index, (width * 1.5f).toInt())
            }
        }

        DisposableEffect(key1 = Unit) {
            //Help with garbage collection
            onDispose {
                bitmap?.recycle()
                bitmap = null
            }
        }

        bitmap?.let {
            Image(
                modifier = Modifier.Companion.fillMaxSize(),
                bitmap = it.asImageBitmap(),
                contentDescription = null,
            )
        }
    }
}

/**
 * Detect only two finger zoom
 */
suspend fun PointerInputScope.detectZoom(onGesture: (zoom: Float) -> Unit) {
    awaitEachGesture {
        var zoom = 1f
        var pastTouchSlop = false

        val touchSlop = viewConfiguration.touchSlop

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { it.isConsumed } || (event.changes.size == 1)

            if (!canceled) {
                val zoomChange = event.calculateZoom()

                if (!pastTouchSlop) {
                    zoom *= zoomChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize

                    if (zoomMotion > touchSlop) {
                        pastTouchSlop = true
                    }
                }

                if (zoomChange != 1f) {
                    onGesture(zoomChange)
                }

                event.changes.fastForEach {
                    if (it.positionChanged()) {
                        it.consume()
                    }
                }

            }
        } while (!canceled && event.changes.fastAny { it.pressed })
    }
}


