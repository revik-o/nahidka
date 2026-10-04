import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.metro)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { it.binaries.framework { baseName = "financialmanagement"; isStatic = true } }
    jvm()
    js {
        browser {
            testTask { useMocha { timeout = "10s" } }
        }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }
    android {
       namespace = "org.orev.nahidka.feature.financial"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
       compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core:feature:common"))
            implementation(project(":shared:core:lib:api"))
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.collections.immutable)
            api(libs.kotlinx.datetime)
            implementation(libs.metro.runtime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jsMain.dependencies {
            implementation(npm("@js-joda/timezone", "2.23.0"))
        }
        wasmJsMain.dependencies {
            implementation(npm("@js-joda/timezone", "2.23.0"))
        }
    }
}
