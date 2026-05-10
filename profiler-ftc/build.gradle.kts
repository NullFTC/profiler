import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.publish.maven.MavenPublication

plugins {
	id("dev.frozenmilk.android-library")
	id("dev.frozenmilk.publish")
	id("dev.frozenmilk.doc")
}

android.namespace = "dev.nullftc.profiler"

ftc {
	sdk {
		RobotCore
		FtcCommon {
			configurationNames += "testImplementation"
		}
	}
}

dependencies {
	api(project(":profiler-core"))
}

publishing {
	repositories {
		maven {
			name = "nullftcReleases"
			url = uri("https://maven.nullftc.dev/releases")
			credentials(PasswordCredentials::class)
		}
	}
	publications {
		register<MavenPublication>("release") {
			groupId = "dev.nullftc"
			artifactId = "Profiler"

			artifact(dairyDoc.dokkaHtmlJar)
			artifact(dairyDoc.dokkaJavadocJar)
			afterEvaluate {
				from(components["release"])
			}
		}
	}
}
