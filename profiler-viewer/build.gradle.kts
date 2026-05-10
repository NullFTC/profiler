plugins {
	application
	id("org.openjfx.javafxplugin")
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(17))
	}
}

dependencies {
	implementation(project(":profiler-core"))
}

javafx {
	version = "21.0.4"
	modules = listOf("javafx.controls")
}

application {
	mainClass.set("dev.nullftc.profiler.viewer.ProfilerViewerApp")
}

val sampleTrace = rootProject.layout.projectDirectory.file("samples/test-profiler.csv")
val generatedSampleTrace = layout.buildDirectory.file("generated/samples/test-profiler.csv")

tasks.register<Copy>("generateViewerTestCsv") {
	group = "verification"
	description = "Generates the sample profiler CSV used by viewer CI checks."
	from(sampleTrace)
	into(generatedSampleTrace.map { it.asFile.parentFile })
}

tasks.register<JavaExec>("validateViewerSampleTrace") {
	group = "verification"
	description = "Loads the sample profiler CSV through the viewer's offline import and analyzer path."
	dependsOn("classes", "generateViewerTestCsv")
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("dev.nullftc.profiler.viewer.TraceValidationMain")
	args(generatedSampleTrace.get().asFile.absolutePath)
}

tasks.named<Zip>("distZip") {
	dependsOn("validateViewerSampleTrace")
}

tasks.register<JavaExec>("runViewerSample") {
	group = "application"
	description = "Runs the JavaFX viewer with the generated sample profiler CSV."
	dependsOn("classes", "generateViewerTestCsv")
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set(application.mainClass)
	args(generatedSampleTrace.get().asFile.absolutePath)
}

tasks.register<Zip>("viewerReleaseCandidate") {
	group = "distribution"
	description = "Builds a runnable release-candidate zip for the offline profiler viewer."
	dependsOn("installDist", "generateViewerTestCsv", "validateViewerSampleTrace")
	archiveFileName.set("ftc-profiler-viewer-${project.version}-rc.zip")
	destinationDirectory.set(layout.buildDirectory.dir("release-candidates"))
	from(layout.buildDirectory.dir("install/profiler-viewer")) {
		into("profiler-viewer")
	}
	from(generatedSampleTrace) {
		into("profiler-viewer/samples")
	}
}

tasks.register("viewerCi") {
	group = "verification"
	description = "Compiles, validates sample trace loading, and builds the viewer release candidate."
	dependsOn("validateViewerSampleTrace", "viewerReleaseCandidate")
}
