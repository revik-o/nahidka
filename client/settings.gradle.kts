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
include(":shared:core:lib:api")
include(":shared:core:common")
include(":shared:core:feature:tasks")
include(":shared:core:feature:financial-management")
include(":shared:core:feature:social-battery")
include(":shared:core:feature:goals")
include(":shared:core:feature:settings")
include(":shared:core:feature:dashboard")
include(":webApp")
include(":shared:ui:common")
include(":shared:ui:models")
include(":shared:ui:task")
include(":shared:ui:goal")
include(":shared:ui:socialbattery")
include(":shared:ui:dashboard")
include(":shared:ui:settings")
include(":shared:ui:financialmanagement")
include(":shared:ui:notification")
include(":shared:ui:di")
