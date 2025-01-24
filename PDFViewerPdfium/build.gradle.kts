import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose)
    alias(libs.plugins.publish)
}

android {
    namespace = "io.github.veselyjan92.pdfviewer"
    compileSdk = 35

    defaultConfig {
        minSdk = 23
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    api(project(":PDFViewer"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.pdfiumandroid)
}

mavenPublishing {
    coordinates("io.github.veselyjan92", "pdfviewer-pdfium",  libs.versions.libVersion.get())

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)

    signAllPublications()

    pom {
        name.set("Compose PDF Viewer")
        description.set("Compose PDF Viewer, backwards compatible, anntations, zoom, scroll")
        inceptionYear.set("2024")
        url.set("https://github.com/VeselyJan92/PDFViewer")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                name.set("Jan Veselý")
                url.set("jan.vesely92@gmail.com")
            }
        }
        scm {
            url.set("https://github.com/VeselyJan92/PDFViewer")
        }
    }
}


