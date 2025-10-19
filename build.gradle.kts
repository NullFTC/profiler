plugins {
	id("dev.frozenmilk.android-library") version "10.1.1-0.1.3"
	id("dev.frozenmilk.publish") version "0.0.4"
	id("dev.frozenmilk.doc") version "0.0.4"
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

publishing {
	repositories {
		maven {
			name = "nullftc"
			url = uri("https://maven.nullftc.dev/releases")
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
