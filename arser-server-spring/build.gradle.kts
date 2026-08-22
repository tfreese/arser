plugins {
    id("java")
}

dependencies {
    implementation(project(":arser-core"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
    // testImplementation(project(path = ":arser-server-jre", configuration = "testRuntime"))

    // testRuntimeOnly("org.slf4j:slf4j-simple")
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
