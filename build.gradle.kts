plugins {
    kotlin("jvm") version "2.3.10"
    java
    application
}

group = "one.empty3.corps3"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("one.empty3:empty3-library-mp:2026.10.11")
    implementation("one.empty3.libs:partial-desktop:0.0.39-17")
    implementation("one.empty3.libs:commons-mp:0.0.17-17")
    implementation("com.google.code.gson:gson:2.10.1")
}

kotlin {
    jvmToolchain(25)
}

application {
    mainClass.set("one.empty3.corps3.Problem")
}

