import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import groovy.lang.MissingPropertyException

plugins {
    id("com.github.johnrengelman.shadow") version("7.1.2")
    kotlin("jvm") version("1.7.10")
    application
}

group = extra["project.group"] ?: throw MissingPropertyException("The project group was not configured!")
version = extra["project.version"] ?: throw MissingPropertyException("The project version was not configured!")

val shade by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

repositories {
    mavenCentral()
}

dependencies {
    // Language
    shade(kotlin("stdlib-jdk8"))

    // Discord
    shade("net.dv8tion:JDA:5.0.0-alpha.17")
    shade("club.minnced:discord-webhooks:0.8.0")

    // Utility
    shade(api("xyz.deftu.deftils:Deftils:2.0.0")!!)
    shade(api("com.squareup.okhttp3:okhttp:4.9.3")!!)
    shade("com.google.code.gson:gson:2.9.0")
    shade("com.google.guava:guava:31.1-jre")

    // Logging
    shade("org.apache.logging.log4j:log4j-api:2.17.2")
    shade("org.apache.logging.log4j:log4j-core:2.17.2")
    shade("org.apache.logging.log4j:log4j-slf4j-impl:2.17.2")
}

application {
    mainClass.set(extra["project.mainClass"]?.toString() ?: throw MissingPropertyException("No main class specified"))
}

tasks {
    named<Jar>("shadowJar") {
        archiveClassifier.set("")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
