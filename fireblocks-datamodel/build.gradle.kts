// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0


plugins {
	id("java")
	id("io.freefair.lombok")
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("commons-codec:commons-codec")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-mysql")
	implementation("org.flywaydb:flyway-database-postgresql")

	runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
	runtimeOnly("com.h2database:h2")
	runtimeOnly("org.postgresql:postgresql")

	compileOnly("com.fasterxml.jackson.core:jackson-annotations")
}

