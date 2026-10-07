import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import org.jetbrains.compose.desktop.application.tasks.AbstractRunDistributableTask
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

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

abstract class ClassDataSharingArchiveCreation : DefaultTask() {

    @get:Internal
    abstract val applicationDirectory: DirectoryProperty

    @get:Internal
    abstract val launcherName: Property<String>

    @TaskAction
    fun createArchive() {
        if (System.getenv("DISPLAY").isNullOrEmpty()) {
            logger.warn("Skipping the class data sharing archive: recording the loaded classes needs an X11 DISPLAY")
            return
        }

        val applicationDirectoryFile = applicationDirectory.get().asFile
        val launcher = applicationDirectoryFile.resolve("bin/${launcherName.get()}")
        val launcherConfiguration = applicationDirectoryFile.resolve("lib/app/${launcherName.get()}.cfg")
        val originalLauncherConfiguration = launcherConfiguration.readText()

        applicationDirectoryFile.resolve("lib/app/$ARCHIVE_FILE_NAME").delete()
        try {
            launcherConfiguration.writeText(originalLauncherConfiguration + javaOptions("-XX:DumpLoadedClassList=$CLASS_LIST_PATH"))
            recordLoadedClasses(launcher)
            launcherConfiguration.writeText(originalLauncherConfiguration + javaOptions("-Xshare:dump", "-XX:SharedClassListFile=$CLASS_LIST_PATH"))
            dumpArchive(launcher)
        } finally {
            launcherConfiguration.writeText(originalLauncherConfiguration)
            applicationDirectoryFile.resolve("lib/app/$CLASS_LIST_FILE_NAME").delete()
        }
    }

    private fun recordLoadedClasses(launcher: File) {
        val trainingProcess = ProcessBuilder(launcher.absolutePath)
            .redirectErrorStream(true)
            .apply { environment()[STARTUP_TRACE_VARIABLE] = "1" }
            .start()

        try {
            val dashboardFrameReached = CompletableFuture.supplyAsync {
                trainingProcess
                    .inputReader()
                    .useLines { outputLines -> outputLines.any { outputLine -> outputLine.startsWith(DASHBOARD_FRAME_MARKER) } }
            }
            check(dashboardFrameReached.get(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                "The application exited before drawing the dashboard"
            }
        } finally {
            trainingProcess.destroy()
            trainingProcess.waitFor()
        }
    }

    private fun dumpArchive(launcher: File) {
        val dumpProcess = ProcessBuilder(launcher.absolutePath)
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start()

        check(dumpProcess.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS) && dumpProcess.exitValue() == 0) {
            "Could not dump the class data sharing archive"
        }
    }

    private fun javaOptions(vararg javaOptions: String): String =
        javaOptions.joinToString(separator = "") { javaOption -> "java-options=$javaOption\n" }

    companion object {
        const val ARCHIVE_FILE_NAME = "nahidka.jsa"
        const val CLASS_LIST_FILE_NAME = "nahidka.classlist"
        const val CLASS_LIST_PATH = "\$APPDIR/$CLASS_LIST_FILE_NAME"
        const val STARTUP_TRACE_VARIABLE = "NAHIDKA_STARTUP_TRACE"
        const val DASHBOARD_FRAME_MARKER = "NAHIDKA_STARTUP dashboard_frame"
        const val PROCESS_TIMEOUT_SECONDS = 120L
    }
}

val linuxBuildHost = System.getProperty("os.name").startsWith("Linux", ignoreCase = true)

val desktopWindowJvmArgs = buildList {
    add("--enable-native-access=ALL-UNNAMED")
    if (linuxBuildHost) {
        add("-Dawt.toolkit.name=XToolkit")
        add("--add-opens=java.desktop/java.awt=ALL-UNNAMED")
        add("--add-opens=java.desktop/sun.awt=ALL-UNNAMED")
        add("--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED")
    }
}

val classDataSharingJvmArgs = buildList {
    if (linuxBuildHost) {
        add("-XX:SharedArchiveFile=\$APPDIR/${ClassDataSharingArchiveCreation.ARCHIVE_FILE_NAME}")
        add("-Xlog:cds=off")
        add("-Xlog:aot=off")
    }
}

compose.desktop {
    application {
        mainClass = "org.orev.nahidka.MainKt"
        jvmArgs(*(desktopWindowJvmArgs + classDataSharingJvmArgs).toTypedArray())

        val jbrToolchain = project.extensions.getByType<JavaToolchainService>().launcherFor {
            languageVersion.set(JavaLanguageVersion.of(25))
            vendor.set(JvmVendorSpec.JETBRAINS)
        }

        javaHome = jbrToolchain.get().metadata.installationPath.asFile.absolutePath

        buildTypes.release.proguard {
            version.set("7.9.1")
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            modules("jdk.unsupported")
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.orev.nahidka"
            packageVersion = "1.0.0"
            description = "Nahidka"
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

tasks.withType<JavaExec>().configureEach {
    doFirst {
        setJvmArgs((jvmArgs.orEmpty() - classDataSharingJvmArgs + desktopWindowJvmArgs).distinct())
    }
}

if (linuxBuildHost) {
    val classDataSharingArchiveCreations = listOf("createDistributable", "createReleaseDistributable")
        .associateWith { distributableTaskName ->
            tasks.register<ClassDataSharingArchiveCreation>("${distributableTaskName}ClassDataSharingArchive") {
                val distributableTask = tasks.named<AbstractJPackageTask>(distributableTaskName)

                applicationDirectory.set(distributableTask.flatMap { jpackageTask -> jpackageTask.destinationDir.dir(jpackageTask.packageName) })
                launcherName.set(distributableTask.flatMap(AbstractJPackageTask::packageName))
            }
        }

    tasks.withType<AbstractJPackageTask>().configureEach {
        classDataSharingArchiveCreations[name]?.let { classDataSharingArchiveCreation -> finalizedBy(classDataSharingArchiveCreation) }
    }

    tasks.withType<AbstractRunDistributableTask>().configureEach {
        mustRunAfter(classDataSharingArchiveCreations.values)
    }
}
