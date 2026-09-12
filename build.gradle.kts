plugins {
	kotlin("jvm") version "2.4.10"
	id("com.gradleup.shadow") version "8.3.0"
}

group = "io.klartnet"

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.slf4j:slf4j-simple:2.0.16")
	implementation("net.minestom:minestom:2026.08.28-26.2")
	implementation("dev.hollowcube:polar:1.16.0")
	implementation("io.github.togar2:MinestomPvP:2026.05.30-26.1.1")
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(25))
	}
}
kotlin {
	jvmToolchain(25)
}

tasks {
	val mainClass = "$group.kcp.MainKt"
	val outDir = layout.projectDirectory.dir("run")
	val outName = "server.jar"
	
	jar {
		enabled = false
		manifest {
			attributes["Main-Class"] = mainClass
		}
	}
	shadowJar {
		mergeServiceFiles()
		archiveClassifier = ""
		
		destinationDirectory = outDir
		archiveFileName = outName
	}
	
	build {
		dependsOn(shadowJar)
	}
}