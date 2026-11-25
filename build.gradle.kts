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

// The server needs the game's Assets.zip, which CI does not have: run this locally after a codec change and
// commit the result. Only our schemas are kept, with the shared definitions they reference.
val assetSchemaDir = layout.projectDirectory.dir("agents/openquests-author/schemas")
val generateAssetSchemas by tasks.registering(JavaExec::class) {
    group = "hytale"
    description = "Writes the JSON schemas of the OpenQuests and OpenDialogues assets."
    // runAllMods replaces its args on exec, so this borrows its setup instead of reusing the task
    val run = tasks.named<com.azuredoom.gradle.hytale.RunAllModsTask>("runAllMods").get()
    dependsOn(run.dependsOn)
    classpath = run.classpath
    mainClass = run.mainClass
    jvmArgs = run.jvmArgs
    workingDir = run.runDirectory.get().asFile
    val raw = layout.buildDirectory.dir("asset-schemas").get().asFile
    argumentProviders.add(CommandLineArgumentProvider {
        listOf("--assets=" + run.fallbackAssetsZip.get().asFile.absolutePath, "--bare", "--disable-sentry", "--generate-asset-schema", raw.absolutePath)
    })
    doFirst { raw.deleteRecursively() }
    doLast {
        val slurper = groovy.json.JsonSlurper()
        val source = raw.resolve("Schema")
        @Suppress("UNCHECKED_CAST")
        val definitions = (slurper.parse(source.resolve("common.json")) as Map<String, Any?>)["definitions"] as Map<String, Any?>
        val kept = sortedMapOf<String, Any?>()
        val files = source.listFiles { file -> file.name.startsWith("Open") }!!.map { it.name }.toMutableSet()
        val pending = ArrayDeque(files)
        fun walk(node: Any?) {
            when (node) {
                is Map<*, *> -> node.forEach { (key, value) ->
                    if (key == "\$ref" && value is String) {
                        val file = value.substringBefore("#")
                        val name = value.substringAfter("#/definitions/", "")
                        if (name.isNotEmpty()) {
                            if (kept.put(name, definitions[name]) == null) walk(definitions[name])
                        } else if (file.isNotEmpty() && files.add(file)) pending.add(file)
                    } else walk(value)
                }
                is List<*> -> node.forEach(::walk)
            }
        }
        val target = assetSchemaDir.asFile
        target.deleteRecursively()
        target.mkdirs()
        while (pending.isNotEmpty()) {
            val file = source.resolve(pending.removeFirst())
            walk(slurper.parse(file))
            file.copyTo(target.resolve(file.name))
        }
        target.resolve("common.json").writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(mapOf("definitions" to kept))) + "\n")
    }
}

// This content pack needs a third-party mod, so it is linked only with -PwithLivingWorldQuests.
val linkLivingWorldQuests by tasks.registering(Exec::class) {
    val link = layout.projectDirectory.dir("run/mods/LivingWorldQuests").asFile
    val target = project(":mods:livingworldevents").file("src/main/resources")
    dependsOn(":mods:livingworldevents:updatePluginManifest")
    mustRunAfter("stageAllModAssets")
    // rmdir without /s only drops a junction or an empty folder, never the files behind it
    val removeLink = "(if exist \"$link\" rmdir \"$link\")"
    val command = if (providers.gradleProperty("withLivingWorldQuests").isPresent) {
        "$removeLink && mklink /J \"$link\" \"$target\""
    } else {
        removeLink
    }
    commandLine("cmd", "/c", command)
}

tasks.matching { it.name == "runAllMods" }.configureEach {
    dependsOn(linkLivingWorldQuests)
}
