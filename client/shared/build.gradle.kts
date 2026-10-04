import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.metro)
}

kotlin {
    jvmToolchain { languageVersion.set(JavaLanguageVersion.of(25)); vendor.set(JvmVendorSpec.JETBRAINS) }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    android {
       namespace = "org.orev.nahidka.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()

       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core:feature:tasks"))
            api(project(":shared:core:feature:financial-management"))
            api(project(":shared:core:feature:social-battery"))
            api(project(":shared:core:feature:goals"))
            api(project(":shared:core:feature:settings"))
            api(project(":shared:ui:dashboard"))
            implementation(project(":shared:ui:common"))
            implementation(project(":shared:ui:financial-management"))
            implementation(project(":shared:ui:settings"))
            implementation(project(":shared:ui:tasks"))
            implementation(project(":shared:ui:goal"))
            implementation(project(":shared:ui:social-battery"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.metro.runtime)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        jvmTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(compose.desktop.currentOs)
            implementation(compose.desktop.uiTestJUnit4)
        }

        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
    }
}
