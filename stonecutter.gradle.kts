plugins {
    id("dev.kikugie.stonecutter")
}

// The version src/ is switched to. Stonecutter rewrites the version comments in src/ in place when
// it changes ("Set active project to ..." tasks); commit with it set back to the vcsVersion in
// settings.gradle.kts (./gradlew "Reset active project").
stonecutter active "1.20.1"
