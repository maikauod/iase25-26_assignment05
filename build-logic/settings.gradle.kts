// dependencyResolutionManagement is @Incubating, so IDEs flag it "unstable"; it is Gradle's
// recommended way to declare repositories centrally.
dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
