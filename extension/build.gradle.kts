import java.util.zip.ZipFile

plugins {
    id("com.azuredoom.hytale-tools")
}

group = project.property("group").toString()

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

repositories {
    mavenCentral()
}

dependencies {
    // Loaded as a separate plugin at runtime, so it is never bundled in
    compileOnly(project(":core"))
}

tasks.named<Jar>("jar") {
    archiveBaseName.set(project.property("mod_id").toString())
    archiveVersion.set(project.property("version").toString())
}

// The ship quests as content. They leave the jar and travel as an asset pack of
// their own, so a server owner keeps the quest types and drops the example chain by deleting one file.
val questExamples = listOf("Server/OpenQuests/**", "Server/Languages/*/quest.lang")

// Only the published jar loses them: stageAllModAssets and prepareRunServer link the dev run
// against src/main/resources rather than this output, so runAllMods keeps the quests, and keeps
// them editable live. Installing the pack into run/mods would just load the same assets twice.
tasks.named<Copy>("processResources") {
    exclude(questExamples)
}

val questExamplesManifest by tasks.registering {
    val manifest = layout.buildDirectory.file("questExamples/manifest.json")
    outputs.file(manifest)

    val group = project.property("manifest_group").toString()
    val modVersion = project.property("version").toString()
    val author = project.property("mod_author").toString()
    val url = project.property("mod_url").toString()
    val serverVersion = project.property("manifestServerVersion").toString()

    doLast {
        val file = manifest.get().asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            {
                "Group": "$group",
                "Name": "OpenQuestsExamples",
                "Version": "$modVersion",
                "Description": "The quest line shipped with OpenQuests. Delete to start from an empty quest list.",
                "Authors": [
                    {
                        "Name": "$author"
                    }
                ],
                "Website": "$url",
                "ServerVersion": "$serverVersion",
                "Dependencies": {
                    "$group:OpenQuests": "*"
                },
                "DisabledByDefault": false,
                "IncludesAssetPack": true
            }
            """.trimIndent() + "\n"
        )
    }
}

val questExamplesZip by tasks.registering(Zip::class) {
    archiveBaseName.set("OpenQuestsExamples")
    archiveVersion.set(project.property("version").toString())

    // Manifest at the root, the rest keeping the paths it has under src/main/resources
    from(questExamplesManifest)
    from("src/main/resources") {
        include(questExamples)
    }
}

tasks.named("assemble") {
    dependsOn(questExamplesZip)
}

fun archiveEntries(archive: File): Set<String> =
    ZipFile(archive).use { zip -> zip.entries().asSequence().map { it.name }.toSet() }

// Check the shipped content before releasing
val verifyReleaseArtifacts by tasks.registering {
    group = "verification"
    description = "Checks the two jars and the quest content pack that the release publishes."

    dependsOn(":core:jar", tasks.named("jar"), questExamplesZip)

    val modVersion = project.property("version").toString()
    val coreJars = fileTree(project(":core").layout.buildDirectory.dir("libs").get().asFile) { include("*.jar") }
    val extensionJars = fileTree(layout.buildDirectory.dir("libs").get().asFile) { include("*.jar") }
    val contentPacks = fileTree(layout.buildDirectory.dir("distributions").get().asFile) { include("*.zip") }
    val published = mapOf(
        "core/build/libs/*.jar" to coreJars,
        "extension/build/libs/*.jar" to extensionJars,
        "extension/build/distributions/*.zip" to contentPacks
    )

    val resourcesRoot = file("src/main/resources")
    val questSources = fileTree(resourcesRoot) { include(questExamples) }

    doLast {
        published.forEach { (glob, tree) ->
            val matched = tree.files.sortedBy { it.name }

            if (matched.isEmpty()) {
                throw GradleException("Nothing matches $glob, the release would fail on it.")
            }

            // A jar left over from an earlier version is matched by the same glob and shipped too
            if (matched.size > 1) {
                throw GradleException(
                    "$glob matches ${matched.joinToString { it.name }} and the release would " +
                        "publish them all. Run clean."
                )
            }

            val artifact = matched.single()
            if (!artifact.name.contains(modVersion)) {
                throw GradleException(
                    "$glob matches ${artifact.name}, not version $modVersion. Run clean."
                )
            }

            logger.lifecycle("Release artefact: ${artifact.name} (${artifact.length() / 1024} KiB)")
        }

        val quests = questSources.files
            .map { it.toRelativeString(resourcesRoot).replace(File.separatorChar, '/') }
            .toSet()

        val jar = extensionJars.singleFile
        val leaked = quests.intersect(archiveEntries(jar)).sorted()
        if (leaked.isNotEmpty()) {
            throw GradleException("${jar.name} still carries quest content: ${leaked.joinToString()}")
        }

        val pack = contentPacks.singleFile
        val packed = archiveEntries(pack)

        // AssetModule skips a pack with no manifest.json of its own, and only warns about it
        if (!packed.contains("manifest.json")) {
            throw GradleException("${pack.name} has no manifest.json, the server would skip it.")
        }

        val absent = (quests - packed).sorted()
        if (absent.isNotEmpty()) {
            throw GradleException("${pack.name} is missing quest content: ${absent.joinToString()}")
        }
    }
}

tasks.named("check") {
    dependsOn(verifyReleaseArtifacts)
}
