# Compose PDFViewer library for Android
- Backwards compatible thanks to [PdfiumAndroidKt](https://github.com/johngray1965/PdfiumAndroidKt)
- Supports PDF annotations
- Supported gestures: pinch to zoom, tap to zoom, scroll
- Scrolling and dragging supports velocity drag

## Native vs PdfiumAndroidKt version

### PdfiumAndroidKt renderer
This renderer uses [PdfiumAndroidKt](https://github.com/johngray1965/PdfiumAndroidKt) and is backwards compatible but app needs to bundle `PDFium` native libraries. 

```
implementation("io.github.veselyjan92:pdfviewer-pdfium:1.0.1")
```
```
val state = rememberPDFViewerPdfiumState(file)

PDFViewer(
    state = state,
)
```

### PdfiumAndroidKt renderer
This renderer uses [PdfRenderer](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer). There are some limitations such as lack of support for annotated PDFs on Android api levels less than 31. [Read more here](https://issuetracker.google.com/issues/365693423)

```
implementation("io.github.veselyjan92:pdfviewer:1.0.1")
```
```
val state = rememberPDFViewerAndroidState(file)

PDFViewer(
    state = state,
)
```


## Try with preview
Put a sample PDF [apollo.pdf](https://www.nasa.gov/wp-content/uploads/static/apollo50th/pdf/a11final-fltpln.pdf) in the assets folder.
```
@Composable
@Preview
private fun PdfViewerAndroidRendererPreview() {
    val context = LocalContext.current

    val file =  File.createTempFile("tmp", "pdf", context.cacheDir).apply {
        mkdirs()
        deleteOnExit()
        writeBytes(context.assets.open("apollo.pdf").readBytes())
    }
    
    val state = rememberPDFViewerAndroidState(file)

    PDFViewer(
        state = state,
        modifier = Modifier.fillMaxSize()
    )
}
```


## Check out how this library works
- [Compose layout](https://github.com/VeselyJan92/PDFViewer/blob/master/PDFViewer/src/main/java/io/github/veselyjan92/pdfviewer/PDFViewer.kt)
- [Android renderer](https://github.com/VeselyJan92/PDFViewer/blob/master/PDFViewer/src/main/java/io/github/veselyjan92/pdfviewer/PDFViewerRendererAndroid.kt)
- [PdfiumAndroidKt renderer](https://github.com/VeselyJan92/PDFViewer/blob/master/PDFViewerPdfium/src/main/java/io/github/veselyjan92/pdfviewer/PdfiumRenderer.kt)










