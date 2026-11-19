plugins {
    id("com.azuredoom.hytale-tools")
}

group = project.property("group").toString()

// Its own key: the release sets OpenQuests' version with -Pversion, which would override a plain `version` here
version = property("adventurequests_version").toString()

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
        manifest.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(fields)) + "\n")
    }
}

val packZip by tasks.registering(Zip::class) {
    dependsOn("updatePluginManifest")
    archiveBaseName.set("AdventureQuests")
    archiveVersion.set(project.version.toString())
    from("src/main/resources")
}

tasks.named("assemble") {
    dependsOn(packZip)
}
