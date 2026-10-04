import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.JETBRAINS)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.JETBRAINS)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":shared:ui:common"))
    implementation(libs.compose.material3)
    implementation(libs.jbr.api)
    implementation(libs.jna)
    implementation(libs.jna.platform)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.compose.uiToolingPreview)
}

val desktopWindowJvmArgs = buildList {
    add("--enable-native-access=ALL-UNNAMED")
    if (System.getProperty("os.name").startsWith("Linux", ignoreCase = true)) {
        add("-Dawt.toolkit.name=XToolkit")
        add("--add-opens=java.desktop/java.awt=ALL-UNNAMED")
        add("--add-opens=java.desktop/sun.awt=ALL-UNNAMED")
        add("--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED")
    }
}

compose.desktop {
    application {
        mainClass = "org.orev.nahidka.MainKt"
        // jpackage expands APPDIR after installation, independent of the working directory.
        jvmArgs("-splash:\$APPDIR/resources/splash.png", *desktopWindowJvmArgs.toTypedArray())

        val jbrToolchain = project.extensions.getByType<JavaToolchainService>().launcherFor {
            languageVersion.set(JavaLanguageVersion.of(25))
            vendor.set(JvmVendorSpec.JETBRAINS)
        }

        javaHome = jbrToolchain.get().metadata.installationPath.asFile.absolutePath

        buildTypes.release.proguard {
            // The plugin's default ProGuard 7.7 cannot read our JDK 25 class files.
            version.set("7.9.1")
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            modules("jdk.unsupported")
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.orev.nahidka"
            packageVersion = "1.0.0"
            description = "Nahidka"
            appResourcesRootDir.set(layout.projectDirectory.dir("resources"))
            linux {
                shortcut = true
                appCategory = "Utility"
            }
            windows {
                shortcut = true
                menuGroup = "Nahidka"
            }
        }
    }
}

// Development launches use the same image, but do not have jpackage's APPDIR macro.
tasks.withType<JavaExec>().configureEach {
    val developmentSplash = layout.projectDirectory.file("resources/common/splash.png").asFile.absolutePath
    doFirst {
        setJvmArgs((jvmArgs.orEmpty().filterNot { it.startsWith("-splash:") } + desktopWindowJvmArgs + "-splash:$developmentSplash").distinct())
    }
}
