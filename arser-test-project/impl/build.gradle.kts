plugins {
    id("java")
}

description = "Arser Test Project - IMPL"

dependencies {
    implementation("de.freese.arser.test:api:$version")

    runtimeOnly("org.slf4j:slf4j-simple:" + property("version_slf4j"))

    testImplementation("org.junit.jupiter:junit-jupiter:" + property("version_junit"))

    testRuntimeOnly("org.junit.platform:junit-platform-launcher:" + property("version_junit"))
}
