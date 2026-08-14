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
