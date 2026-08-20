plugins {
    id("java-library")
    id("maven-publish")
}

description = "Arser Test Project - API"

dependencies {
    // Only in https://repo.gradle.org/gradle/libs-releases
    api("org.gradle:gradle-tooling-api:" + property("version_gradleToolingApi"))

    api("org.slf4j:slf4j-api:" + property("version_slf4j"))
}

val publishGroup = project.group.toString()
val publishName = project.name
val publishVersion = project.version.toString()
val publishDescription = project.description

// Aufrufbar, aber wird nicht ausgeführt.
tasks.withType<PublishToMavenLocal>().configureEach {
    isEnabled = false
}

publishing {
    publications {
        create<MavenPublication>("testApi") {
            groupId = publishGroup
            artifactId = publishName
            version = publishVersion

            from(components["java"])

            pom {
                name = publishName
                description = publishDescription
            }
        }

        // remove(this.findByName("publishToMavenLocal"))
    }

    repositories {
        maven {
            url = uri("http://localhost:8484/snapshots")
            name = "arserSnapshots"
            isAllowInsecureProtocol = true
        }
    }
}
