import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "com.dauvalter.geoflare"
version = "0.2.0"

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    androidLibrary {
        namespace = "com.dauvalter.geoflare.firestore"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withJava()
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain.dependencies {
            api(project(":geoflare-core"))
            api(libs.firebase.firestore)
            api(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        jvmTest.dependencies {
            implementation("org.mockito:mockito-core:5.20.0")
        }
    }
}

// Disable native test linking for firestore module as FirebaseCore.framework is provided by the consuming app/CocoaPods
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink>().configureEach {
    enabled = false
}
tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest>().configureEach {
    enabled = false
}

mavenPublishing {
    publishToMavenCentral()

    val hasSigningKey = project.hasProperty("signingInMemoryKey") ||
        System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null ||
        project.hasProperty("signing.keyId")
    if (hasSigningKey) {
        signAllPublications()
    }

    coordinates(group.toString(), "geoflare-firestore", version.toString())

    pom {
        name = "GeoFlare Firestore"
        description = "Cloud Firestore geospatial query extensions and flow-based querying using GeoFlare and GitLive Firebase SDK"
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
