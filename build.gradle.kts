import groovy.lang.MissingPropertyException

plugins {
    java
    kotlin("jvm") version("1.6.21")
    application
    val dgt = "1.2.1"
    id("xyz.deftu.gradle.tools") version(dgt)
    id("xyz.deftu.gradle.tools.blossom") version(dgt)
    id("xyz.deftu.gradle.tools.shadow") version(dgt)
}

repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://maven.kotlindiscord.com/repository/maven-public/")
}

dependencies {
    // Language
    shade(implementation(kotlin("stdlib-jdk8"))!!)

    // Discord
    shade(implementation("com.kotlindiscord.kord.extensions:kord-extensions:${libs.versions.kordex.get()}")!!)

    // Storage
    shade(implementation("org.xerial:sqlite-jdbc:${libs.versions.sqlitejdbc.get()}")!!)
    shade(implementation("org.influxdb:influxdb-java:${libs.versions.influxdb.get()}")!!)

    // Utility
    shade(api("xyz.deftu.deftils:Deftils:${libs.versions.deftils.get()}")!!)
    shade(api("com.squareup.okhttp3:okhttp:${libs.versions.okhttp.get()}")!!)
    shade(implementation("com.google.code.gson:gson:${libs.versions.gson.get()}")!!)
    shade(implementation("com.google.guava:guava:${libs.versions.guava.get()}")!!)

    // Logging
    shade(implementation("org.apache.logging.log4j:log4j-api:${libs.versions.log4j.get()}")!!)
    shade(implementation("org.apache.logging.log4j:log4j-core:${libs.versions.log4j.get()}")!!)
    shade(implementation("org.apache.logging.log4j:log4j-slf4j-impl:${libs.versions.log4j.get()}")!!)
}

application {
    mainClass.set(extra["project.mainClass"]?.toString() ?: throw MissingPropertyException("No main class specified"))
}

tasks {
    named<Jar>("fatJar") {
        archiveClassifier.set("")
        from("LICENSE")
    }
}
