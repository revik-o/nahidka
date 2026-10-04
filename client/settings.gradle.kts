rootProject.name = "Nahidka"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":androidApp")
include(":desktopApp")
include(":shared")
include(":shared:core:feature:common")
include(":shared:core:feature:tasks")
include(":shared:core:feature:financial-management")
include(":shared:core:feature:social-battery")
include(":shared:core:feature:goals")
include(":shared:core:feature:settings")
include(":shared:core:feature:dashboard")
include(":webApp")
include(":shared:ui:common")
include(":shared:ui:tasks")
include(":shared:ui:goal")
include(":shared:ui:social-battery")
include(":shared:ui:dashboard")
include(":shared:ui:settings")
include(":shared:ui:financial-management")
include(":shared:ui:notification")
