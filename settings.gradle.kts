pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// dependencyResolutionManagement is @Incubating, so IDEs flag it "unstable"; it is Gradle's
// recommended way to declare repositories centrally.
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "campus-coffee"

include("domain", "api", "data", "application", "coverage")
