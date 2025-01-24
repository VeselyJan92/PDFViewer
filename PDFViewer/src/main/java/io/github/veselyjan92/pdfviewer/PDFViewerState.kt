package io.github.veselyjan92.pdfviewer

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun rememberPDFViewerAndroidState(file: File): PDFViewerState {
    return remember {
        PDFViewerState(
            PdfRenderAndroid(file)
        )
    }
}

@Stable
class PDFViewerState(val pdfRender: PDFViewerRender) {

    val pages = MutableStateFlow<List<Page>>(listOf())

    val pageCount: Int
        get() = pages.value.size

    internal var translateX: Animatable<Float, AnimationVector1D> = Animatable(0f)
    internal var translateY: Animatable<Float, AnimationVector1D> = Animatable(0f)

    class Page(val index: Int, val ration: Float)

    suspend fun loadPages() {
        pages.value = pdfRender.loadPDF()
    }

    suspend fun loadPage(index: Int, viewportWidth: Int): Bitmap {
        require(index < pages.value.size) { "First load pages with loadPages(), index: $index, pages: $pageCount" }

        return pdfRender.loadPage(pages.value[index], viewportWidth)
    }

    fun clear() {
        pages.value = listOf<Page>()
        pdfRender.close()
    }

    fun updateBounds(left: Float, top: Float, right: Float, bottom: Float) {
        translateX.updateBounds(lowerBound = left, upperBound = right)
        translateY.updateBounds(lowerBound = top, upperBound = bottom)
    }

    suspend fun dragBy(value: Offset) {
        coroutineScope {
            launch {
                translateX.snapTo(translateX.value - value.x)
            }
            launch {
                translateY.snapTo(translateY.value - value.y)
            }
        }
    }

    suspend fun animateTo(x: Float = translateX.value, y: Float = translateY.value) {
        coroutineScope {
            launch {
                translateX.animateTo(x)
            }
            launch {
                translateY.animateTo(y)
            }
        }
    }


    suspend fun snapTo(x: Float = translateX.value, y: Float = translateY.value) {
        coroutineScope {
            launch {
                translateX.snapTo(x)
            }
            launch {
                translateY.snapTo(y)
            }
        }
    }

    suspend fun flingBy(velocity: Velocity) {
        coroutineScope {
            launch {
                translateX.animateDecay(-velocity.x, exponentialDecay())
            }
            launch {
                translateY.animateDecay(-velocity.y, exponentialDecay())
            }
        }
    }
}