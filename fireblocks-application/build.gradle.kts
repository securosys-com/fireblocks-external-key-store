// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0


plugins {
    id("java")
    id("org.springframework.boot")
    id("com.google.cloud.tools.jib")
}

configurations {
    create("deployment")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation(project(":fireblocks-service"))

    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
}

tasks.named<Jar>("jar") {
    // Custom jar configuration if needed
}

tasks.named("assemble") {
    doLast {
        println("Copying resources")
        copy {
            from(project(":fireblocks-service").buildDir.resolve("resources/main/"))
            into("build/resources/main")
            include("logback.xml")
        }
    }
}

jib {
    val fromDockerRegistry = project.properties["fromDockerRegistry"] as String? ?: "registry-1.docker.io"
    val toDockerRegistry = project.properties["toDockerRegistry"] as String? ?:  "securosys.jfrog.io/external-key-store"
    from {
        image = "${fromDockerRegistry}/eclipse-temurin:21-jdk"
        platforms {
            platform {
                architecture = "amd64"
                os = "linux"
            }
            platform {
                architecture = "arm64"
                os = "linux"
            }
        }
    }
    to {
        image = "${toDockerRegistry}/fireblocks-external-key-store:${project.version}"
    }
    container {
        // current timestamp is not reproduible, but keeps track of build time in the image metadata
        creationTime = "USE_CURRENT_TIMESTAMP"
        jvmFlags = listOf(
            "-Djava.security.egd=file:/dev/./urandom",
            "--add-opens=java.base/sun.security.pkcs10=ALL-UNNAMED",
            "--add-opens=java.base/sun.security.x509=ALL-UNNAMED",
            "--add-opens=java.base/sun.security.pkcs=ALL-UNNAMED",
            "--add-opens=java.base/sun.security.util=ALL-UNNAMED",
            "-Dcom.securosys.primus.jce.logoutCloseReaderGracetimeMillis=0",
            "-Dcom.securosys.primus.jce.cutLmsSignaturePrefix=false",
            "-Dcom.securosys.primus.jce.fallBackToSignatureLessLogFetching=true",
            "-Dspring.config.additional-location=/etc/app/config/",
            "-Dlogging.config=/etc/app/config/logback.xml",
            "-XX:MinRAMPercentage=70",
            "-XX:InitialRAMPercentage=70",
            "-XX:MaxRAMPercentage=70"
        )
    }
    extraDirectories {
        paths {
            path {
                setFrom("src/main/resources")
                includes.add("logback.xml")
                includes.add("application.yml")
                into = "/etc/app/config"
            }
        }
    }
}
