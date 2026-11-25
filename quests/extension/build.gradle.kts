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
    compileOnly(project(":quests:core"))

    vineImplementation("curse.maven:opennavigation-1692713:9260487")

    // What a constraint decides from its own fields is exercised off a running server, the way the
    // core tests its backends. The server jar and the core are compileOnly, so the tests ask for both.
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation(project(":quests:core"))
    testImplementation(files(configurations.named("vineServerJar")))
}

tasks.named<Test>("test") {
    useJUnitPlatform()

    // HytaleLogger installs its own LogManager and refuses to load if something got there first
    systemProperty("java.util.logging.manager", "com.hypixel.hytale.logger.backend.HytaleLogManager")

    testLogging {
        events("passed", "failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.named<Jar>("jar") {
    archiveBaseName.set(project.property("mod_id").toString())
    archiveVersion.set(project.property("version").toString())
}

fun archiveEntries(archive: File): Set<String> =
    ZipFile(archive).use { zip -> zip.entries().asSequence().map { it.name }.toSet() }

// Check the shipped content before releasing
val verifyReleaseArtifacts by tasks.registering {
    group = "verification"
    description = "Checks the jars and the quest content pack that the release publishes."

    dependsOn(":quests:core:jar", tasks.named("jar"), ":mods:adventurequests:packZip", ":mods:livingworldevents:packZip", ":mods:mmoskilltree:jar")

    val modVersion = project.property("version").toString()
    // Read late: the packs are configured after this project and carry a version of their own
    val adventureQuestsVersion = provider { project(":mods:adventurequests").version.toString() }
    val livingWorldQuestsVersion = provider { project(":mods:livingworldevents").version.toString() }
    val packVersion = provider { project(":mods:mmoskilltree").version.toString() }
    val coreJars = fileTree(project(":quests:core").layout.buildDirectory.dir("libs").get().asFile) { include("*.jar") }
    val extensionJars = fileTree(layout.buildDirectory.dir("libs").get().asFile) { include("*.jar") }
    val contentPacks = fileTree(project(":mods:adventurequests").layout.buildDirectory.dir("distributions").get().asFile) { include("*.zip") }
    val livingWorldPacks = fileTree(project(":mods:livingworldevents").layout.buildDirectory.dir("distributions").get().asFile) { include("*.zip") }
    val packJars = fileTree(project(":mods:mmoskilltree").layout.buildDirectory.dir("libs").get().asFile) { include("*.jar") }
    val published = mapOf(
        "quests/core/build/libs/*.jar" to (coreJars to provider { modVersion }),
        "quests/extension/build/libs/*.jar" to (extensionJars to provider { modVersion }),
        "mods/adventurequests/build/distributions/*.zip" to (contentPacks to adventureQuestsVersion),
        "mods/livingworldevents/build/distributions/*.zip" to (livingWorldPacks to livingWorldQuestsVersion),
        "mods/mmoskilltree/build/libs/*.jar" to (packJars to packVersion)
    )

    doLast {
        published.forEach { (glob, artifacts) ->
            val (tree, version) = artifacts
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
            if (!artifact.name.contains(version.get())) {
                throw GradleException(
                    "$glob matches ${artifact.name}, not version ${version.get()}. Run clean."
                )
            }

            logger.lifecycle("Release artefact: ${artifact.name} (${artifact.length() / 1024} KiB)")
        }

        // AssetModule skips a pack with no manifest.json of its own, and only warns about it
        for ((packs, version) in listOf(contentPacks to adventureQuestsVersion, livingWorldPacks to livingWorldQuestsVersion)) {
            val pack = packs.singleFile
            if (!archiveEntries(pack).contains("manifest.json")) {
                throw GradleException("${pack.name} has no manifest.json, the server would skip it.")
            }

            // A content pack has no plugin class: a Main, even blank, makes the server look for one
            val manifest = ZipFile(pack).use { zip ->
                groovy.json.JsonSlurper().parse(zip.getInputStream(zip.getEntry("manifest.json"))) as Map<*, *>
            }
            if (manifest.containsKey("Main")) {
                throw GradleException("${pack.name} declares a Main in its manifest.json.")
            }
            if (manifest["Version"] != version.get()) {
                throw GradleException("${pack.name} has manifest version ${manifest["Version"]}, not ${version.get()}.")
            }
        }
    }
}

tasks.named("check") {
    dependsOn(verifyReleaseArtifacts)
}
