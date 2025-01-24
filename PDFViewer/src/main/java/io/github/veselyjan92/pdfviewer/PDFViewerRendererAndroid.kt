package io.github.veselyjan92.pdfviewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import kotlin.use

class PdfRenderAndroid(private val file: File) : PDFViewerRender {

    /**
     * PdfRenderer can be run in parallel, we must create instance for each page
     */
    private fun <T> useRenderer(utilize: (renderer: PdfRenderer) -> T): T {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(descriptor)

        val result = utilize(renderer)

        renderer.close()
        descriptor.close()

        return result
    }

    override suspend fun loadPDF(): List<PDFViewerState.Page> = useRenderer { renderer ->
        List(renderer.pageCount) {
            PDFViewerState.Page(
                index = it,
                ratio = renderer.openPage(it).use { page ->
                    page.height.toFloat() / page.width.toFloat()
                }
            )
        }
    }

    override suspend fun loadPage(page: PDFViewerState.Page, viewportWidth: Int): Bitmap =
        useRenderer { renderer ->
            PdfRenderer(
                ParcelFileDescriptor.open(
                    file,
                    ParcelFileDescriptor.MODE_READ_ONLY
                )
            ).openPage(page.index).use { currentPage ->
                val height = (page.ratio * viewportWidth).toInt()
                val newBitmap = Bitmap.createBitmap(viewportWidth, height, Bitmap.Config.ARGB_8888)
                currentPage.render(newBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                newBitmap
            }
        }

    override fun close() {}
}