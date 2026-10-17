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
    // Separate plugins at runtime, never bundled in. Only their public API is used here, the way a
    // modder outside this repository would.
    compileOnly(project(":core"))
    compileOnly(project(":extension"))

    // MMO Skill Tree 1.7.2 and the CommonLib it requires, for the dev run and decompiled sources
    vineImplementation("curse.maven:mmo-skill-tree-1431313:9215826")
    vineImplementation("curse.maven:ziggfreeds-commonlib-1580260:9215812")
}

tasks.named<Jar>("jar") {
    archiveBaseName.set(project.property("mod_id").toString())
    archiveVersion.set(project.property("version").toString())
}
