plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

// The convention plugins apply these Gradle plugins by id, so their jars must be on build-logic's
// classpath here.
dependencies {
    implementation(libs.spring.boot.gradle.plugin)
    implementation(libs.dependency.management.plugin)
    implementation(libs.pitest.gradle.plugin)
}
