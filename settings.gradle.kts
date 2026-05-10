pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
		google()
		maven("https://repo.dairy.foundation/releases")
	}
}

dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
	repositories {
		mavenCentral()
		google()
		maven("https://repo.dairy.foundation/releases")
	}
}

rootProject.name = "profiler"

include(":profiler-core")
include(":profiler-ftc")
include(":profiler-viewer")
