// Build logic for the app module. It compiles the app's own .OTips classes —
// the same source files, not a copy — so the extensions packed at build time
// are written by exactly the code the app uses to read them.
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    // Android provides org.json on the phone; the build needs the library.
    implementation("org.json:json:20240303")
}

kotlin {
    sourceSets.named("main") {
        kotlin.srcDir("../app/src/main/java")
        kotlin.include(
            "com/example/buildlogic/**",
            "com/example/data/extensions/otips/**",
            "com/example/data/extensions/ExtensionManifest.kt",
            "com/example/data/extensions/ExtensionPermission.kt",
            "com/example/data/extensions/ExtensionKind.kt",
            "com/example/data/extensions/OrthoExtension.kt"
        )
    }
}
