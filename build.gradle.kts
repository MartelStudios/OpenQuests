plugins {
    idea
    id("com.azuredoom.hytale-workspace") version "1.+"
}

tasks.withType<Javadoc>().configureEach {
    (options as org.gradle.external.javadoc.StandardJavadocDocletOptions).addStringOption("Xdoclint:-missing", "-quiet")
}

group = project.property("group").toString()

hytaleWorkspace {
    modProjects = listOf(":core", ":extension")
    hostProject = ":extension"

    // Shared
    manifestGroup = property("manifest_group").toString()
    hytaleVersion = property("hytale_version").toString()
    patchline = property("patchline").toString()
}

repositories {
    mavenCentral()
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

// The server reads its config from OPENQUESTS_CONFIG when set, which is how a dev run points at a
// JDBC config: the plugin data directory is deleted and re-linked on every stageAllModAssets, so
// nothing written there survives. Set openquests.config in gradle.properties rather than on the
// command line — PowerShell splits an argument holding a dot before gradlew.bat sees it.
tasks.matching { it.name == "runAllMods" }.configureEach {
    val configPath = providers.gradleProperty("openquests.config")
    if (configPath.isPresent) {
        (this as JavaExec).environment("OPENQUESTS_CONFIG", file(configPath.get()).absolutePath)
    }
}
