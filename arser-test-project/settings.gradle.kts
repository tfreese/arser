pluginManagement {
    repositories {
        // mavenLocal()
        // mavenCentral()
        // gradlePluginPortal()
        maven {
            url = uri("http://localhost:8484/public")
            isAllowInsecureProtocol = true
        }
    }
}

rootProject.name = "arser-test-project"

dependencyResolutionManagement {
    repositories {
        // mavenLocal()
        // mavenCentral()
        maven {
            url = uri("http://localhost:8484/public")
            isAllowInsecureProtocol = true
            mavenContent {
                releasesOnly()
            }
        }
        maven {
            url = uri("http://localhost:8484/snapshots")
            isAllowInsecureProtocol = true
            mavenContent {
                snapshotsOnly()
            }
        }
        maven {
            url = uri("http://localhost:8484/releases")
            isAllowInsecureProtocol = true
            mavenContent {
                releasesOnly()
            }
        }
    }
}

include("api")
include("impl")
