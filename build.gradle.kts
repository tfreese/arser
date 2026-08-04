// Execute Tasks in SubModule: gradle MODUL:clean build
plugins {
    id("de.freese.gradle.conventions").apply(false)
    id("io.spring.dependency-management").apply(false)
}

allprojects {
    plugins.apply("base")
}

subprojects {
    plugins.apply("de.freese.gradle.conventions")
    plugins.apply("io.spring.dependency-management")

    extensions.configure(io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension::class.java) {
        imports {
//            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
            mavenBom("org.springframework.boot:spring-boot-dependencies:" + property("version_springBoot"))
        }

        dependencies {
            dependency("com.github.spotbugs:spotbugs-annotations:" + property("version_spotbugs"))
            dependency("dev.failsafe:failsafe:" + property("version_failsafe"))

            dependencySet("com.sun.xml.bind:" + property("version_jaxb")) {
                entry("jaxb-xjc")
                entry("jaxb-impl")
            }

            dependency("org.apiguardian:apiguardian-api:" + property("version_apiGuardian"))
        }
    }

    plugins.withType<JavaPlugin> {
        val mockitoAgent = configurations.create("mockitoAgent")

        dependencies {
            //add("implementation", platform("org.springframework.boot:spring-boot-dependencies:$version_springBoot"))

            add("testImplementation", "org.awaitility:awaitility")
            add("testImplementation", "org.junit.jupiter:junit-jupiter")

            add("testImplementation", "org.mockito:mockito-junit-jupiter")
            mockitoAgent("org.mockito:mockito-core") {
                isTransitive = false
            }

            // To avoid compiler warnings about @API annotations in Log4j Code.
            // add("testCompileOnly", "com.github.spotbugs:spotbugs-annotations")

            // To avoid compiler warnings about @API annotations in JUnit Code.
            add("testCompileOnly", "org.apiguardian:apiguardian-api")

            add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        }

        tasks.withType<Test>().configureEach {
            val mockitoFiles = mockitoAgent.asPath

            doFirst {
                jvmArgs.add("-javaagent:$mockitoFiles")
            }
        }
    }
}

