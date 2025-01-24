package io.github.veselyjan92.pdfviewer

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val file = getFile()
            val state = rememberPDFViewerAndroidState(file)

            PDFViewer(
                state = state,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun getFile(): File {
    val context = LocalContext.current

     return File.createTempFile("tmp", "pdf", context.cacheDir).apply {
        mkdirs()
        deleteOnExit()
        writeBytes(context.assets.open("apollo.pdf").readBytes())
    }
}

@Composable
@Preview
private fun PdfViewerAndroidRendererPreview() {
    val file = getFile()
    val state = rememberPDFViewerAndroidState(file)

    PDFViewer(
        state = state,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
@Preview
private fun PdfViewerPdfiumRendererPreview() {
    val file = getFile()
    val state = rememberPDFViewerPdfiumState(file)

    PDFViewer(
        state = state,
    )
}