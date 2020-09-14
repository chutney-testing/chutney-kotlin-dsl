import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.0"
}

group = "me.iguissouma"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven {
        setUrl("https://dl.bintray.com/s1m0nw1/KtsRunner")
    }
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.5")
    testImplementation(kotlin("test-junit"))
    testImplementation("com.gregwoodfill.assert:kotlin-json-assert:0.1.0")
    testImplementation("de.swirtz:ktsRunner:0.0.8")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.test {
  testLogging {
    events("passed", "failed", "skipped")
    showStandardStreams = true
  }
}
