plugins {
    id("com.azuredoom.hytale-tools")
}

group = project.property("group").toString()

// Its own key: the release sets OpenQuests' version with -Pversion, which would override a plain `version` here
version = property("openquestsdialogues_version").toString()

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
    // Separate plugins at runtime, never bundled in. Only their public API is used here.
    compileOnly(project(":quests:core"))
    compileOnly(project(":quests:extension"))
    compileOnly(project(":dialogues"))

    testImplementation(project(":quests:core"))
    testImplementation(project(":quests:extension"))
    testImplementation(project(":dialogues"))
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(files(configurations.named("vineServerJar")))
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    systemProperty("java.util.logging.manager", "com.hypixel.hytale.logger.backend.HytaleLogManager")

    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.named<Jar>("jar") {
    archiveBaseName.set(project.property("mod_id").toString())
    archiveVersion.set(project.version.toString())
}
