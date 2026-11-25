plugins {
    id("com.azuredoom.hytale-tools")
}

group = project.property("group").toString()

// Its own key: the release sets OpenQuests' version with -Pversion, which would override a plain `version` here
version = property("livingworldevents_version").toString()

hytaleTools {
    javaVersion = property("java_version").toString().toInt()
    hytaleVersion = property("hytale_version").toString()
    manifestServerVersion = property("manifestServerVersion").toString()
    manifestGroup = property("manifest_group").toString()
    modId = property("mod_id").toString()
    modDescription = property("mod_description").toString()
    modUrl = property("mod_url").toString()
    mainClass = property("main_class").toString()
    modCredits = property("mod_author").toString()
    manifestDependencies = property("manifest_dependencies").toString()
    manifestOptionalDependencies = property("manifest_opt_dependencies").toString()
    curseforgeId = property("curseforgeID").toString()
    disabledByDefault = property("disabled_by_default").toString().toBoolean()
    includesPack = property("includes_pack").toString().toBoolean()
    patchline = property("patchline").toString()
    injectServerJavadocsIntoSources = property("injectServerJavadocsIntoSources").toString().toBoolean()
    generateAssetsBinary = property("generateAssetsBinary").toString().toBoolean()
}


// A content pack: no plugin class, so no Main in its manifest
tasks.named("validateManifest") {
    enabled = false
}

tasks.named("updatePluginManifest") {
    val manifest = file("src/main/resources/manifest.json")
    doLast {
        @Suppress("UNCHECKED_CAST")
        val fields = groovy.json.JsonSlurper().parse(manifest) as MutableMap<String, Any?>
        fields.remove("Main")
        fields.remove("UpdateChecker")
        if ((fields["OptionalDependencies"] as? Map<*, *>)?.isEmpty() == true) {
            fields.remove("OptionalDependencies")
        }
        manifest.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(fields)) + "\n")
    }
}

val packZip by tasks.registering(Zip::class) {
    dependsOn("updatePluginManifest")
    archiveBaseName.set("LivingWorldQuests")
    archiveVersion.set(project.version.toString())
    from("src/main/resources")
}

tasks.named("assemble") {
    dependsOn(packZip)
}

// The required mod itself, pinned to the version the pack is tested against, for the dev run.
val devMods by configurations.creating

dependencies {
    devMods("curse.maven:living-world-events-1761204:${property("lwe_dev_file")}")
}

// Staged only with -PwithLivingWorldQuests; any staged copy is dropped first, so a run without it stays clean.
val devModsDirectory = rootProject.layout.projectDirectory.dir("run/mods")

val unstageDevMods by tasks.registering(Delete::class) {
    delete(provider { devModsDirectory.asFile.listFiles { file -> file.name.startsWith("living-world-events-1761204-") }.orEmpty() })
}

val stageDevMods by tasks.registering(Copy::class) {
    dependsOn(unstageDevMods)
    enabled = providers.gradleProperty("withLivingWorldQuests").isPresent
    from(devMods)
    into(devModsDirectory)
}

rootProject.tasks.matching { it.name == "runAllMods" }.configureEach {
    dependsOn(stageDevMods)
}
