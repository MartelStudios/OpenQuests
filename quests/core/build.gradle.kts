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

tasks.named<Jar>("jar") {
    archiveBaseName.set(project.property("mod_id").toString())
    archiveVersion.set(project.property("version").toString())
}

// The persistence backends are the one part of the core that can be exercised off a running
// server: a codec, a connection and nothing else. The server jar is compileOnly, so the tests ask
// for it explicitly.
dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // In-memory, so the JDBC backend is covered without a database to start. The PostgreSQL run is
    // the same test against -Dopenquests.jdbc.url, which the Docker compose file below sets up.
    testImplementation("com.h2database:h2:2.3.232")
    testImplementation("org.postgresql:postgresql:42.7.4")

    // Spells its upsert exactly as PostgreSQL does, so the ON CONFLICT statement is covered
    // without a database to start
    testImplementation("org.xerial:sqlite-jdbc:3.47.1.0")

    testImplementation(files(configurations.named("vineServerJar")))
}

tasks.named<Test>("test") {
    useJUnitPlatform()

    // HytaleLogger installs its own LogManager and refuses to load if something got there first
    systemProperty("java.util.logging.manager", "com.hypixel.hytale.logger.backend.HytaleLogManager")

    systemProperty("openquests.jdbc.url", providers.gradleProperty("openquests.jdbc.url").getOrElse(""))
    systemProperty("openquests.jdbc.user", providers.gradleProperty("openquests.jdbc.user").getOrElse(""))
    systemProperty("openquests.jdbc.password", providers.gradleProperty("openquests.jdbc.password").getOrElse(""))

    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
