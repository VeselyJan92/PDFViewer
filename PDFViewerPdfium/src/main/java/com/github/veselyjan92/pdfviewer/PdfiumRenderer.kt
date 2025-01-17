package com.github.veselyjan92.pdfviewer

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.legere.pdfiumandroid.suspend.PdfDocumentKt
import io.legere.pdfiumandroid.suspend.PdfiumCoreKt
import kotlinx.coroutines.Dispatchers
import java.io.File

@Composable
fun rememberPDFViewerPdfiumState(file: File): PDFViewerState {
    return remember {
        PDFViewerState(
            PdfRenderPdfium(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
        )
    }
}

class PdfRenderPdfium(private val fileDescriptor: ParcelFileDescriptor) : PDFViewerRender {
    private val pdfiumCore = PdfiumCoreKt(Dispatchers.Default)
    lateinit var document: PdfDocumentKt

    override suspend fun loadPDF(): List<PDFViewerState.Page> {
        document = pdfiumCore.newDocument(fileDescriptor)

        return List(document.getPageCount()) {
            PDFViewerState.Page(
                index = it,
                ration = document.openPage(it).use { page ->
                    page.getPageHeightPoint().toFloat() / page.getPageWidthPoint().toFloat()
                }
            )
        }
    }

    override suspend fun loadPage(page: PDFViewerState.Page, viewportWidth: Int): Bitmap {
        document.openPage(page.index).use { currentPage ->
            val height = (page.ration * viewportWidth).toInt()
            val newBitmap = Bitmap.createBitmap(viewportWidth, height, Bitmap.Config.ARGB_8888)
            currentPage.renderPageBitmap(newBitmap, 0, 0, viewportWidth, height, renderAnnot = true)

            return newBitmap
        }
    }

    override fun close() {
        document.close()
        fileDescriptor.close()
    }
}