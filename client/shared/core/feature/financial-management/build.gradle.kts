import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { it.binaries.framework { baseName = "financialmanagement"; isStatic = true } }
    jvm()
    js { browser() }
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
            implementation(project(":shared:core:lib:api"))
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }
    }
}
