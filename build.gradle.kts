import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `maven-publish`
    kotlin("jvm") version "1.4.0"
}

group = "com.chutneytesting"
version = "0.1-SNAPSHOT"

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

tasks {
    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

    artifacts { 
        archives(sourcesJar)
        archives(jar)
    }

    test {
        testLogging {
            events("passed", "failed", "skipped")
            showStandardStreams = true
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("default") {
            from(components["java"])
            pom {
                url.set("https://github.com/chutney-testing/chutney-kotlin-dsl")
                inceptionYear.set("2020")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                scm {
                    url.set("https://github.com/chutney-testing/chutney-kotlin-dsl.git")
                    connection.set("scm:git:git@github.com:chutney-testing/chutney-kotlin-dsl.git")
                    developerConnection.set("scm:git:git@github.com:chutney-testing/chutney-kotlin-dsl.git")
                    tag.set("HEAD")
                }
                issueManagement {
                    system.set("github")
                    url.set("https://github.com/chutney-testing/chutney-kotlin-dsl/issues")
                }
                ciManagement {
                    system.set("travis-ci")
                    url.set("https://travis-ci.org/chutney-testing/chutney-kotlin-dsl")
                }
            }
        }
    }
}
