import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Instant
import java.time.format.DateTimeFormatter

plugins {
    `maven-publish`
    kotlin("jvm") version "1.4.0"
    id("com.jfrog.bintray") version "1.8.5"
    id("org.ajoberstar.reckon") version "0.12.0"
}

val group = "com.chutneytesting"
val timestamp: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
val gitHubUrl = "https://github.com/chutney-testing/${project.name}"
val publicationName = "default";

reckon {
    scopeFromProp()
    snapshotFromProp()
}

bintray {
    user = System.getenv("BINTRAY_SERVER_USERNAME")
    key = System.getenv("BINTRAY_SERVER_PASSWORD")
    publish = true
    setPublications(publicationName)
    pkg(delegateClosureOf<com.jfrog.bintray.gradle.BintrayExtension.PackageConfig> {
        repo = "maven"
        name = project.name
        userOrg = "chutney-testing"
        vcsUrl = gitHubUrl
        setLicenses("Apache-2.0")
        desc = description
    })
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.5")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.0")
    implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.4.0")

    testImplementation(kotlin("test-junit"))
    testImplementation("com.gregwoodfill.assert:kotlin-json-assert:0.1.0")
    testImplementation("org.springframework:spring-expression:5.1.5.RELEASE")
    testImplementation(kotlin("scripting-jsr223"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks {
    test {
        testLogging {
            events("passed", "failed", "skipped")
            showStandardStreams = true
        }
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    publications {
        create<MavenPublication>(publicationName) {
            from(components["java"])
            artifact(sourcesJar.get())
            groupId = group
            artifactId = project.name
            version = project.version.toString()
            pom {
                url.set(gitHubUrl)
                inceptionYear.set("2020")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                scm {
                    url.set("${gitHubUrl}.git")
                    connection.set("scm:git:git@github.com:chutney-testing/${project.name}.git")
                    developerConnection.set("scm:git:git@github.com:chutney-testing/${project.name}.git")
                    tag.set(project.version.toString().takeUnless { it.endsWith("SNAPSHOT") })
                }
                issueManagement {
                    system.set("github")
                    url.set("${gitHubUrl}/issues")
                }
                ciManagement {
                    system.set("github-ci")
                    url.set("${gitHubUrl}/actions")
                }
            }
        }
    }
}
