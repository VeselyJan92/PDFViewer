package com.github.veselyjan92.pdfviewer

import android.graphics.Bitmap

interface PDFViewerRender {
    suspend fun loadPDF(): List<PDFViewerState.Page>
    suspend fun loadPage(page: PDFViewerState.Page, viewportWidth: Int): Bitmap
    fun close()
}