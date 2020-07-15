plugins {
    application
    kotlin("jvm") version "1.3.72"
}

group = "com.spadger"
version = "1.0-SNAPSHOT"

application {
    mainClassName = "com.spadger.opentracingtest.MainKt"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.apache.kafka", "kafka-streams", "2.5.0")
    implementation("ch.qos.logback", "logback-classic", "1.2.3")
    implementation("io.github.microutils", "kotlin-logging", "1.8.3")
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "11"
    languageVersion = "1.3"
}