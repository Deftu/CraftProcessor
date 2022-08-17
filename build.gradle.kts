import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import groovy.lang.MissingPropertyException

plugins {
    id("com.github.johnrengelman.shadow") version("7.1.2")
    kotlin("jvm") version("1.7.10")
    id("net.kyori.blossom") version("1.3.1")
    application
}

val name = extra["project.name"]?.toString() ?: throw MissingPropertyException("The project name was not configured!")
group = extra["project.group"] ?: throw MissingPropertyException("The project group was not configured!")
version = extra["project.version"] ?: throw MissingPropertyException("The project version was not configured!")

val shade by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

blossom {
    replaceToken("@NAME@", name)
    replaceToken("@VERSION@", version)
}

repositories {
    mavenCentral()
}

dependencies {
    // Language
    shade(kotlin("stdlib-jdk8"))

    // Discord
    shade("net.dv8tion:JDA:5.0.0-alpha.17") {
        exclude(module = "opus-java")
    }

    // Storage
    shade("org.xerial:sqlite-jdbc:3.36.0.3")

    // Utility
    shade(api("xyz.deftu.deftils:Deftils:2.0.0")!!)
    shade(api("com.squareup.okhttp3:okhttp:4.9.3")!!)
    shade("com.google.code.gson:gson:2.9.0")
    shade("com.google.guava:guava:31.1-jre")

    // Logging
    shade("org.apache.logging.log4j:log4j-api:2.18.0")
    shade("org.apache.logging.log4j:log4j-core:2.18.0")
    shade("org.apache.logging.log4j:log4j-slf4j-impl:2.18.0")
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
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
