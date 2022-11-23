import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import groovy.lang.MissingPropertyException
import java.io.ByteArrayOutputStream

plugins {
    java
    id("com.github.johnrengelman.shadow") version("7.1.2")
    kotlin("jvm") version("1.6.21")
    id("net.kyori.blossom") version("1.3.0")
    application
}

val gitData = GitData.from(project)

group = extra["project.group"] ?: throw MissingPropertyException("The project group was not configured!")
version = extra["project.version"] ?: throw MissingPropertyException("The project version was not configured!")

val shade by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

blossom {
    replaceToken("@NAME@", name)
    replaceToken("@VERSION@", version)
    replaceToken("@GIT_BRANCH@", gitData.branch)
    replaceToken("@GIT_COMMIT@", gitData.commit)
}

repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://maven.kotlindiscord.com/repository/maven-public/")
    maven("https://jitpack.io/")
    mavenCentral()
}

dependencies {
    // Language
    shade(kotlin("stdlib-jdk8"))

    // Discord
    shade("com.kotlindiscord.kord.extensions:kord-extensions:${libs.versions.kordex.get()}")

    // Storage
    shade("org.xerial:sqlite-jdbc:${libs.versions.sqlitejdbc.get()}")
    shade("org.influxdb:influxdb-java:${libs.versions.influxdb.get()}")

    // Utility
    shade(api("xyz.deftu.deftils:Deftils:${libs.versions.deftils.get()}")!!)
    shade(api("com.squareup.okhttp3:okhttp:${libs.versions.okhttp.get()}")!!)
    shade("com.google.code.gson:gson:${libs.versions.gson.get()}")
    shade("com.google.guava:guava:${libs.versions.guava.get()}")

    // Logging
    shade("org.apache.logging.log4j:log4j-api:${libs.versions.log4j.get()}")
    shade("org.apache.logging.log4j:log4j-core:${libs.versions.log4j.get()}")
    shade("org.apache.logging.log4j:log4j-slf4j-impl:${libs.versions.log4j.get()}")
}

application {
    mainClass.set(extra["project.mainClass"]?.toString() ?: throw MissingPropertyException("No main class specified"))
}

tasks {
    named<Jar>("shadowJar") {
        archiveClassifier.set("")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from("LICENSE")
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}

data class GitData(
    val branch: String,
    val commit: String,
    val url: String
) {
    companion object {
        private val debug: Boolean
            get() = System.getProperty("debug.git", "false").toBoolean()
        private val errorOutput: java.io.OutputStream?
            get() = if (debug) System.err else null

        fun Project.propertyOr(key: String, default: String? = null): String {
            return (project.findProperty(key)
                ?: System.getProperty(key)
                ?: default) as String?
                ?: throw GradleException("No default property for key \"$key\" found. Set it in gradle.properties, environment variables or in the system properties.")
        }

        @JvmStatic
        fun from(project: Project): GitData {
            val extension = project.extensions.findByName("gitData") as GitData?
            if (extension != null) return extension

            val branch = project.propertyOr("GITHUB_REF_NAME", fetchCurrentBranch(project) ?: "LOCAL")
            val commit = project.propertyOr("GITHUB_SHA", fetchCurrentCommit(project) ?: "LOCAL")
            val url = fetchCurrentUrl(project) ?: "NONE"
            val data = GitData(branch, commit, url)
            project.extensions.add("gitData", data)
            return data
        }

        @JvmStatic
        fun fetchCurrentBranch(project: Project): String? {
            return try {
                val output = ByteArrayOutputStream()
                project.exec {
                    commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
                    standardOutput = output
                    errorOutput = this@Companion.errorOutput
                }
                val string = output.toString().trim()
                if (string.isEmpty() || string.startsWith("fatal")) null else string
            } catch (e: Exception) {
                "LOCAL"
            }
        }

        @JvmStatic
        fun fetchCurrentCommit(project: Project): String? {
            return try {
                val output = ByteArrayOutputStream()
                project.exec {
                    commandLine("git", "rev-parse", "HEAD")
                    standardOutput = output
                    errorOutput = this@Companion.errorOutput
                }
                val string = output.toString().trim()
                if (string.isEmpty() || string.startsWith("fatal")) "LOCAL" else string.substring(0, 7)
            } catch (e: Exception) {
                "LOCAL"
            }
        }

        @JvmStatic
        fun fetchCurrentUrl(project: Project): String? {
            return try {
                val output = ByteArrayOutputStream()
                project.exec {
                    commandLine("git", "config", "--get", "remote.origin.url")
                    standardOutput = output
                    errorOutput = this@Companion.errorOutput
                }
                val string = output.toString().trim()
                if (string.isEmpty() || string.startsWith("fatal")) "LOCAL" else string
            } catch (e: Exception) {
                "LOCAL"
            }
        }
    }
}
