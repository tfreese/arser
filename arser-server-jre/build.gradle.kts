plugins {
    // id("java")
    id("application")
}

val mainClazz = "de.freese.arser.server.JreHttpServerApplication"

application {
    mainClass = mainClazz
}

dependencies {
    implementation(project(":arser-core"))

    runtimeOnly("org.slf4j:slf4j-simple")
}
