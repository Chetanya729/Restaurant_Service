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
    testImplementation(platform("org.junit:junit-bom:6.1.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

tasks.register<JavaExec>("runDemo") {
    group = "application"
    description = "Runs the 10-customer demo without the interactive menu"
    mainClass = "org.example.Main"
    classpath = sourceSets["main"].runtimeClasspath
    args("--demo")
}

tasks.named<JavaExec>("run") {
    // the menu reads from the console
    doFirst { standardInput = System.`in` }
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "failed", "skipped") }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}
