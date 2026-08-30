.PHONY: build build-desktop build-android build-ios build-web run-desktop run-web test

build: build-desktop build-android build-ios build-web

build-desktop:
	./gradlew :desktopApp:packageDistributionForCurrentOS

build-android:
	./gradlew :androidApp:assembleDebug

build-ios:
	./gradlew :shared:assembleXCFramework

build-web:
	./gradlew :webApp:jsBrowserDistribution

run-desktop:
	./gradlew :desktopApp:run

run-web:
	./gradlew :webApp:jsBrowserRun

test:
	./gradlew allTests
