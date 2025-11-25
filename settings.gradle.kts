pluginManagement {
    val springBootVersion: String by settings
    val flywayVersion: String by settings

    plugins {
        id("java")
        id("io.spring.dependency-management") version "1.1.7"
        id("org.springframework.boot") version springBootVersion
        id("io.freefair.lombok") version "9.0.0"
        id("org.unbroken-dome.test-sets") version "4.1.0"
        id("com.google.cloud.tools.jib") version "3.5.1"
        id("org.flywaydb.flyway") version flywayVersion
    }

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "fireblocks-external-key-store"

include("fireblocks-application")
include("fireblocks-service")
include("fireblocks-datamodel")
