import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "com.dauvalter.geoflare"
version = "0.1.0"

kotlin {
    jvm()
    androidLibrary {
        namespace = "com.dauvalter.geoflare.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withJava() // enable java compilation support
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    linuxX64()

    sourceSets {
        commonMain.dependencies {
            // zero external dependencies
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    val hasSigningKey = project.hasProperty("signingInMemoryKey") ||
        System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null ||
        project.hasProperty("signing.keyId")
    if (hasSigningKey) {
        signAllPublications()
    }

    coordinates(group.toString(), "geoflare-core", version.toString())

    pom {
        name = "GeoFlare Core"
        description = "Pure Kotlin Multiplatform library for geospatial queries and geohashing"
        inceptionYear = "2026"
        url = "https://github.com/adauvalter/geoflare-kmp"
        licenses {
            license {
                name = "Apache-2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "adauvalter"
                name = "Anton Dauwalter"
                url = "https://github.com/adauvalter"
            }
        }
        scm {
            url = "https://github.com/adauvalter/geoflare-kmp"
            connection = "scm:git:git://github.com/adauvalter/geoflare-kmp.git"
            developerConnection = "scm:git:ssh://github.com:adauvalter/geoflare-kmp.git"
        }
    }
}
