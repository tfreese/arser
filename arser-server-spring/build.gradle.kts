plugins {
    id("application")
}

val mainClazz = "de.freese.arser.spring.SpringServerApplication"

application {
    mainClass = mainClazz
}

// Variante 1.
// Global aus allen Configurations entfernen.
// configurations.configureEach {
//     resolutionStrategy {
//         exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
//     }
// }

dependencies {
    // Variante 2.
    // "org.springframework.boot:spring-boot-starter-logging" ist immer noch als Dependency in der Gradle-View zu sehen, wird aber nicht verwendet.
    modules {
        module("org.springframework.boot:spring-boot-starter-logging") {
            replacedBy("org.springframework.boot:spring-boot-starter-log4j2", "Use Log4j2 instead of Logback")
        }
    }

    implementation(project(":arser-core"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc") {
        // Variante 3.
        // "spring-boot-starter-logging" ist nun auch nicht mehr in der Gradle-View zu sehen.
        // exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }

    implementation("org.springframework.boot:spring-boot-starter-webflux") {
        // Variante 3.
        // exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }

    runtimeOnly("org.springframework.boot:spring-boot-starter-log4j2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
}

tasks.named<ProcessResources>("processResources") {
    val map = mapOf(
        "project_description" to project.description, "project_artifactId" to project.name, "project_version" to project.version
    )

    filesMatching("application.yml") {
        // expand(map)
        filter(
            mapOf("tokens" to map), org.apache.tools.ant.filters.ReplaceTokens::class.java
        )
    }
}

tasks.register<Copy>("copyLibsServerSpring") {
    group = "arser"
    description = "Copy all runtime dependencies to build/libs"

    into(layout.buildDirectory)

    into("libs") {
        from(configurations.runtimeClasspath)
    }
}
