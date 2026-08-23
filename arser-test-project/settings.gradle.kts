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
    }
}

include("api")
include("impl")
