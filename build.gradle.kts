import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    java
}

val artifactVersion = "3.2.0-rc.1"
val extremumGroup = "io.extremum"
val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"

group = extremumGroup
version = artifactVersion
java.sourceCompatibility = JavaVersion.VERSION_17

allprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "java")

    repositories {
        mavenCentral()
        mavenLocal()
    }

    java {
        withJavadocJar()
        withSourcesJar()
    }

    configure<PublishingExtension> {
        if (project.path == ":") {
            // Do not publish "root" project
            return@configure
        }
        // Ignore projects without "artifact" property for publishing
        val projectArtifact = project.findProperty("artifact")?.let { it as String }
            ?: return@configure
        publications {
            create<MavenPublication>(project.name) {
                artifactId = projectArtifact
                version = rootProject.version.toString()
                description = project.description
                from(components["java"])

                pom {
                    project.property("artifact.name")?.let { name.set(it as String) }
                    description.set(project.description)
                    url.set("https://github.com/smekalka/extremum-tools")
                    inceptionYear.set("2022")

                    scm {
                        url.set("https://github.com/smekalka/extremum-tools")
                        connection.set("scm:https://github.com/smekalka/extremum-tools.git")
                        developerConnection.set("scm:git://github.com/smekalka/extremum-tools.git")
                    }

                    licenses {
                        license {
                            name.set("Business Source License 1.1")
                            url.set("https://github.com/smekalka/extremum-tools/blob/develop/LICENSE.md")
                            distribution.set("repo")
                        }
                    }

                    developers {
                        developer {
                            id.set("SherbakovaMA")
                            name.set("Maria Sherbakova")
                            email.set("m.sherbakova@smekalka.com")
                        }
                    }
                }
            }

            repositories {
                maven {
                    name = "OSSRH"
                    val isReleaseVersion = !(version as String).endsWith("-SNAPSHOT")
                    url = uri(if (isReleaseVersion) releasesRepoUrl else snapshotsRepoUrl)
                    credentials {
                        username = System.getProperty("ossrhUsername")
                        password = System.getProperty("ossrhPassword")
                    }
                }
            }
        }
    }

    configure<SigningExtension> {
        val publishing: PublishingExtension by project
        sign(publishing.publications)
    }

    tasks.javadoc {
        if (JavaVersion.current().isJava9Compatible) {
            (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
        }
    }
}

subprojects {
    version = artifactVersion
    group = extremumGroup
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        implementation("io.extremum:extremum-shared-models:3.0.0") {
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
}

project(":extremum-model-tools") {
    dependencies {
        testImplementation(project(":extremum-test-tools"))

        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.4")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.14.0")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.0")

        testImplementation("org.assertj:assertj-core:3.8.0")
        testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.2")
    }
}

project(":extremum-test-tools") {
    dependencies {
        implementation(project(":extremum-model-tools"))
        implementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
        implementation("org.assertj:assertj-core:3.8.0")
        implementation("de.cronn:reflection-util:2.14.0")
    }
}

tasks.wrapper {
    gradleVersion = "7.6"
    distributionType = Wrapper.DistributionType.ALL
}