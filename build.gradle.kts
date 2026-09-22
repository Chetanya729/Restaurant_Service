plugins {
    java
    application            // ← adds the `run` task
}

group = "org.example"
version = "1.0-SNAPSHOT"
description = "Restaurant_Service"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)    // ← replaces sourceCompatibility = VERSION_1_8
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.org.hibernate.hibernate.core)
    implementation(libs.org.hibernate.orm.hibernate.hikaricp)
    runtimeOnly(libs.com.h2database.h2)
    runtimeOnly(libs.org.slf4j.slf4j.simple)
}

application {
    mainClass = "org.example.Main"
}

tasks.register<JavaExec>("runHello") {
    group = "application"
    description = "Runs the HelloHibernate CRUD demo"
    mainClass = "org.example.HelloHibernate"
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("runRaceDemo") {
    group = "application"
    description = "Runs the race condition demo"
    mainClass = "org.example.Service.RaceDemo"
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}