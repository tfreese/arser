plugins {}

allprojects {
    plugins.apply("base")

    // ext["version_slf4j"] = "2.0.18"
    // ext["version_gradleToolingApi"] = "9.7.0"
    // ext["version_junit"] = "6.1.3"
}

subprojects {
    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(26))
            }
        }

        // dependencies {
        //     add("testImplementation", "org.junit.jupiter:junit-jupiter:" + property("version_junit"))
        //     add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher:" + property("version_junit"))
        // }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.isDebug = true
        }

        tasks.withType<Test>().configureEach {
            isEnabled = true
            ignoreFailures = false
            useJUnitPlatform()
        }

        tasks.withType<Javadoc>().configureEach {
            isEnabled = false
        }
    }
}
