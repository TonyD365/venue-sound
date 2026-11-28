pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // One source tree for every Minecraft version: https://stonecutter.kikugie.dev/
    id("dev.kikugie.stonecutter") version "0.10"
    // Applies fabric-loom-remap (obfuscated 1.20.1 and 1.21.1) or fabric-loom (unobfuscated 26.x)
    // from one build script: https://codeberg.org/KikuGie/loom-back-compat
    id("dev.kikugie.loom-back-compat") version "0.4.3"
    // Lets Gradle download a JDK a version's toolchain needs (17, 21 or 25) when none is installed
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // Each version has its own settings in versions/<version>/gradle.properties.
        versions("1.20.1", "1.21.1", "26.3")
        // src/ is committed in this version's form; the others are generated from it at build time.
        vcsVersion = "1.20.1"
    }
}

rootProject.name = "venue-sound"
include("workbench")
