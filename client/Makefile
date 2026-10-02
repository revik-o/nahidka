.PHONY: build build-desktop build-desktop-image build-android build-ios build-web run-desktop launch-desktop run-web test

build: build-desktop build-android build-ios build-web

build-desktop:
	./gradlew :desktopApp:packageReleaseDistributionForCurrentOS

build-desktop-image:
	./gradlew :desktopApp:createReleaseDistributable

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
