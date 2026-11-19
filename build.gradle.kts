plugins {
    idea
    id("com.azuredoom.hytale-workspace") version "1.+"
}

tasks.withType<Javadoc>().configureEach {
    (options as org.gradle.external.javadoc.StandardJavadocDocletOptions).addStringOption("Xdoclint:-missing", "-quiet")
}

group = project.property("group").toString()

hytaleWorkspace {
    modProjects = listOf(":quests:core", ":quests:extension", ":dialogues", ":mods:mmoskilltree", ":mods:openquestsdialogues")
    hostProject = ":quests:extension"

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
// command line: PowerShell splits an argument holding a dot before gradlew.bat sees it.
tasks.matching { it.name == "runAllMods" }.configureEach {
    val configPath = providers.gradleProperty("openquests.config")
    if (configPath.isPresent) {
        (this as JavaExec).environment("OPENQUESTS_CONFIG", file(configPath.get()).absolutePath)
    }
}

// The Adventure Quests pack stays out of modProjects: the server loads every manifest on the
// workspace classpath as a Java plugin, and the pack has no Main. AssetModule reads it from run/mods
// through a junction to its sources, so hot reload and the Asset Editor work on the tracked files.
// A name the workspace does not track: its cleanup deletes what it staged, recursively.
// shortcut: Windows only (mklink /J), add a symlink branch if the dev server runs elsewhere.
val linkAdventureQuests by tasks.registering(Exec::class) {
    val link = layout.projectDirectory.dir("run/mods/AdventureQuests").asFile
    val target = project(":mods:adventurequests").file("src/main/resources")
    dependsOn(":mods:adventurequests:updatePluginManifest")
    mustRunAfter("stageAllModAssets")
    // rmdir without /s only drops a junction or an empty folder, never the files behind it
    commandLine("cmd", "/c", "(if exist \"$link\" rmdir \"$link\") && mklink /J \"$link\" \"$target\"")
}

tasks.matching { it.name == "runAllMods" }.configureEach {
    dependsOn(linkAdventureQuests)
}
