import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Instant
import java.time.format.DateTimeFormatter

plugins {
    `maven-publish`
    signing
    kotlin("jvm") version "1.4.0"
    id("com.jfrog.bintray") version "1.8.5"
    id("org.ajoberstar.reckon") version "0.12.0"
}

val group = "com.chutneytesting"
val timestamp: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
val githubUrl = "https://github.com/chutney-testing/${project.name}"
val publicationName = "chutneyKotlinDSL"

reckon {
    scopeFromProp()
    snapshotFromProp()
}

repositories {
    mavenCentral()
    jcenter() // JCenter will be able to resolve dependencies until February 1, 2022 without changes. (https://blog.gradle.org/jcenter-shutdown)
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

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.javadoc {
    if (JavaVersion.current().isJava11Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}

publishing {
    publications {
        create<MavenPublication>(publicationName) {
            groupId = group
            artifactId = project.name
            version = project.version.toString()
            from(components["java"])
            pom {
                name.set("Chutney Kotlin DSL")
                description.set("Generates Chutney scenarios using Kotlin.")
                inceptionYear.set("2020")
                url.set(githubUrl)
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                scm {
                    url.set("${githubUrl}.git")
                    connection.set("scm:git:git@github.com:chutney-testing/${project.name}.git")
                    developerConnection.set("scm:git:git@github.com:chutney-testing/${project.name}.git")
                    tag.set(project.version.toString().takeUnless { it.endsWith("SNAPSHOT") })
                }
                issueManagement {
                    system.set("github")
                    url.set("${githubUrl}/issues")
                }
                ciManagement {
                    system.set("github-ci")
                    url.set("${githubUrl}/actions")
                }
                developers {
                    developer {
                        id.set("iguissouma ")
                        name.set("Issam Guissouma")
                    }
                    developer {
                        id.set("boddissattva")
                        name.set("Matthieu Gensollen")
                    }
                    developer {
                        id.set("bessonm")
                        name.set("Mael Besson")
                    }
                    developer {
                        id.set("nbrouand")
                        name.set("Nicolas Brouand")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "OSSRH"

            val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots"

            val ossrhUsername = System.getenv("OSSRH_USERNAME") // Use token ; https://s01.oss.sonatype.org/#profile;User%20Token
            val ossrhPassword = System.getenv("OSSRH_PASSWORD") // Use token

            url = uri(releasesRepoUrl)
            credentials {
                username = ossrhUsername
                password = ossrhPassword
            }
        }
    }
}

signing {
    //useGpgCmd()

    // Format: "0x12345678" ; gpg --list-keys --keyid-format 0xSHORT
    val signingKeyId: String? = System.getenv("CHUTNEY_GPG_KEY_ID")

    // gpg -a --export-secret-subkeys KEY_ID
    val signingKey: String? = System.getenv("CHUTNEY_GPG_KEY")
    val signingPassword: String? = System.getenv("CHUTNEY_GPG_PASSPHRASE")
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(publishing.publications[publicationName])
}
