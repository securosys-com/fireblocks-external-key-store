import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    id("java")
    id("io.spring.dependency-management")
    id("org.springframework.boot") apply false
    id("io.freefair.lombok") apply false
    id("org.unbroken-dome.test-sets") apply false
    id("com.google.cloud.tools.jib") apply false
    id("org.flywaydb.flyway") apply false
}

group = "com.securosys.fireblocks"
version = findProperty("VERSION") ?: "unspecified"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    configurations {
            compileOnly {
                    extendsFrom(configurations.annotationProcessor.get())
            }
    }

    repositories {
        mavenCentral()
        maven {
            url = uri("https://splunk.jfrog.io/splunk/ext-releases-local")
        }
        maven {
            url = uri("https://securosys.jfrog.io/artifactory/jce-maven")
            credentials {
                username = findProperty("artifactory_user") as String? ?: System.getenv("ARTIFACTORY_USER")
                password = findProperty("artifactory_password") as String? ?: System.getenv("ARTIFACTORY_PASSWORD")
            }
        }
    }

    dependencies {
        implementation(enforcedPlatform(SpringBootPlugin.BOM_COORDINATES))

        implementation("org.springframework.boot:spring-boot-starter")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    dependencyManagement {
        val bouncyCastleVersion = project.properties["bouncyCastleVersion"] as String
        val flywayVersion = project.properties["flywayVersion"] as String
        val springBootVersion = project.properties["springBootVersion"] as String

        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
        }

        dependencies {
            dependency("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14")
            dependency("org.mariadb.jdbc:mariadb-java-client:3.5.6")
            dependency("org.postgresql:postgresql:42.7.8")
            dependency("org.hibernate.validator:hibernate-validator:9.0.1.Final")
            dependency("com.h2database:h2:2.4.240")
            dependency("org.flywaydb:flyway-core:$flywayVersion")
            dependency("org.flywaydb:flyway-mysql:$flywayVersion")
            dependency("commons-codec:commons-codec:1.19.0")
            dependency("org.flywaydb:flyway-database-postgresql:$flywayVersion")
            dependency("org.bouncycastle:bcprov-jdk18on:$bouncyCastleVersion")
            dependency("org.bouncycastle:bcpkix-jdk18on:$bouncyCastleVersion")
            dependency("org.bouncycastle:bcmail-jdk18on:$bouncyCastleVersion")
            dependency("jakarta.validation:jakarta.validation-api:3.1.1")
            dependency("jakarta.persistence:jakarta.persistence-api:3.1.0")
            dependency("com.nimbusds:oauth2-oidc-sdk:11.29.2")
            //dependency("com.nimbusds:nimbus-jose-jwt:10.5")
            dependency("com.splunk.logging:splunk-library-javalogging:1.11.8")
            dependency("org.apache.commons:commons-lang3:3.19.0")
            dependency("org.apache.httpcomponents.client5:httpclient5:5.5")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    doFirst {
        val activeProfile = System.getProperty("activeTestProfile") ?: "test"
        systemProperty("spring.profiles.active", activeProfile)
        systemProperty("jdk.sunec.disableNative", "false")
        environment("jdk.sunec.disableNative", "false")
        environment("SPRING_PROFILES_ACTIVE", activeProfile)
        jvmArgs = listOf(
            "--add-opens=java.base/sun.security.pkcs10=ALL-UNNAMED",
            "--add-opens=java.base/sun.security.x509=ALL-UNNAMED",
            "--add-opens=java.base/sun.security.pkcs=ALL-UNNAMED",
            "--add-opens=java.base/sun.security.util=ALL-UNNAMED"
        )
        println("Running $project tests with profile: $activeProfile")
        finalizedBy(rootProject.tasks.named("aggregateTestReports"))
    }
    reports {
        junitXml.required.set(true)
        junitXml.outputLocation.set(file("${rootProject.buildDir}/reports/tests/${project.name}/xml"))
        html.outputLocation.set(file("${rootProject.buildDir}/reports/tests/${project.name}/html"))
    }
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "Securosys SA"
            )
        )
    }
}
