import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    id("java")
    id("maven-publish")
}

val artifactVersion = "0.0.1"
val artifact = "extremum-model-tools"
val extremumGroup = "io.extremum"
val releasesRepoUrl = "https://artifactory.extremum.monster/artifactory/extremum-releases/"
val snapshotsRepoUrl = "https://artifactory.extremum.monster/artifactory/extremum-snapshots/"

group = extremumGroup
version = artifactVersion
java.sourceCompatibility = JavaVersion.VERSION_17


allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            url = uri(snapshotsRepoUrl)
            credentials {
                username = System.getenv("ARTIFACTORY_USER")
                password = System.getenv("ARTIFACTORY_PASSWORD")
            }
            mavenContent {
                snapshotsOnly()
            }
        }

        maven {
            url = uri(releasesRepoUrl)
            credentials {
                username = System.getenv("ARTIFACTORY_USER")
                password = System.getenv("ARTIFACTORY_PASSWORD")
            }
        }
    }
}

subprojects {
    version = artifactVersion
    group = extremumGroup
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java-library")

    dependencies {
        implementation("io.extremum:extremum-shared-models:2.1.17-SNAPSHOT") {
            exclude("io.extremum", "extremum-mongo-db-factory-reactive")
            exclude("io.extremum", "extremum-mongo-db-factory-sync")
        }
        testImplementation(kotlin("test"))
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "17"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.named<Jar>("jar") {
        enabled = true
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}

project(":extremum-model-tools") {
    apply(plugin = "maven-publish")

    dependencies {
        testImplementation(project(":extremum-test-tools"))

        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.4")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.14.0")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.0")

        testImplementation("org.assertj:assertj-core:3.8.0")
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = extremumGroup
                artifactId = "extremum-model-tools"
                version = artifactVersion

                from(components["java"])
            }

            repositories {
                maven {
                    val isReleaseVersion = !(version as String).endsWith("-SNAPSHOT")
                    url = uri(if (isReleaseVersion) releasesRepoUrl else snapshotsRepoUrl)
                    credentials {
                        username = System.getenv("ARTIFACTORY_USER")
                        password = System.getenv("ARTIFACTORY_PASSWORD")
                    }
                }
            }
        }
    }
}

project(":extremum-test-tools") {
    apply(plugin = "maven-publish")

    dependencies {
        implementation(project(":extremum-model-tools"))
        implementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = extremumGroup
                artifactId = "extremum-test-tools"
                version = artifactVersion

                from(components["java"])
            }

            repositories {
                maven {
                    val isReleaseVersion = !(version as String).endsWith("-SNAPSHOT")
                    url = uri(if (isReleaseVersion) releasesRepoUrl else snapshotsRepoUrl)
                    credentials {
                        username = System.getenv("ARTIFACTORY_USER")
                        password = System.getenv("ARTIFACTORY_PASSWORD")
                    }
                }
            }
        }
    }
}

tasks.wrapper {
    gradleVersion = "7.5.1"
    distributionType = Wrapper.DistributionType.ALL
}

tasks.named<Jar>("jar") {
    enabled = false
}