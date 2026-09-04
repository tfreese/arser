plugins {
    id("application")
}

val mainClazz = "de.freese.arser.server.JreHttpServerApplication"

application {
    mainClass = mainClazz
}

dependencies {
    implementation(project(":arser-core"))

    runtimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl")
}
