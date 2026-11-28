// Build script for one Minecraft version. Stonecutter applies it to each version in
// settings.gradle.kts, with that version's versions/<version>/gradle.properties.
plugins {
    // fabric-loom-remap on 1.20.1 and 1.21.1 (obfuscated, Mojang mappings), fabric-loom on 26.x
    id("dev.kikugie.loom-back-compat")
}

val mc = sc.current.version
fun prop(name: String) = property(name).toString()
val javaVersion = prop("java_version").toInt()

version = "${prop("mod_version")}+mc$mc"
group = prop("maven_group")

base {
    archivesName = prop("archives_base_name")
}

repositories {
    mavenCentral()
    maven("https://maven.maxhenkel.de/repository/public") { name = "Max Henkel" }
}

dependencies {
    minecraft("com.mojang:minecraft:$mc")
    // Mojang mappings on the obfuscated versions; 26.x ships with Mojang's names
    loomx.applyMojangMappings()
    // loom-back-compat turns mod* into the plain configurations on 26.x
    modImplementation("net.fabricmc:fabric-loader:${prop("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${prop("fabric_version")}")

    // Simple Voice Chat plugin API (provided at runtime by the Simple Voice Chat mod)
    compileOnly("de.maxhenkel.voicechat:voicechat-api:${prop("voicechat_api_version")}")

    // MP3 decoding for the media deck, bundled into the mod jar
    implementation("javazoom:jlayer:1.0.1")
    include("javazoom:jlayer:1.0.1")
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version.toString(),
        "minecraft" to prop("dep_minecraft"),
        "fabricloader" to prop("dep_fabricloader"),
        "voicechat" to prop("dep_voicechat"),
        "java" to ">=$javaVersion",
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") {
        expand(props)
    }
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
    // Each version compiles and runs on the Java it targets: 17 (1.20.1), 21 (1.21.1), 25 (26.x)
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = javaVersion
}

tasks.jar {
    val name = base.archivesName.get()
    inputs.property("archivesName", name)
    from(rootProject.file("LICENSE")) {
        rename { "${it}_$name" }
    }
}

loom {
    runs {
        named("client") {
            // run/<version>: each version needs its own Simple Voice Chat in run/<version>/mods
            runDir = rootProject.file("run/$mc").relativeTo(projectDir).path
            vmArg("-Dvenuesound.selftest=" + (findProperty("selftest") ?: "false"))
            vmArg("-Dvenuesound.photos=" + (findProperty("photos") ?: "false"))
            vmArg("-Dvenuesound.film=" + (findProperty("film") ?: "false"))
            programArgs("--username", "SelfTest")
        }
        named("server") {
            runDir = rootProject.file("run/$mc-server").relativeTo(projectDir).path
        }
    }
}
