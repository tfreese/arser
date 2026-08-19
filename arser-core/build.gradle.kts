plugins {
    id("java-library")
}

configurations.create("jaxb") {
    // extendsFrom(configurations.implementation.get())

    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    // api(project(":arser-configuration"))
    add("jaxb", "com.sun.xml.bind:jaxb-impl")
    add("jaxb", "com.sun.xml.bind:jaxb-xjc") // xsd -> java
    add("jaxb", "com.sun.xml.bind:jaxb-jxc") // java -> xsd
    //    compile(files(genJaxb.classesDir).builtBy(genJaxb))

    api("org.slf4j:slf4j-api")
    api("org.slf4j:jul-to-slf4j")
    api("dev.failsafe:failsafe")
//    api("org.apache.ivy:ivy:2.5.3")
    api("jakarta.xml.bind:jakarta.xml.bind-api")
    api("tools.jackson.core:jackson-databind")
    api("tools.jackson.dataformat:jackson-dataformat-xml")
    // api("tools.jackson.datatype:jackson-datatype-jakarta-jsonp")

    runtimeOnly("org.glassfish.jaxb:jaxb-runtime")
    // runtimeOnly("org.hsqldb:hsqldb")

    testImplementation("com.h2database:h2")
    testImplementation("com.zaxxer:HikariCP")
    testImplementation("org.apache.derby:derby")
    testImplementation("org.hsqldb:hsqldb")

    testRuntimeOnly("org.slf4j:slf4j-simple")
}

// Trigger JAXB generation.
// compileJava.dependsOn(":arser-configuration:build")

val destDirJaxb = layout.buildDirectory.get().dir("generated").dir("jaxb")

sourceSets {
    main {
        java {
            srcDir(destDirJaxb)
        }
    }
}

tasks.register("genJaxb") {
    group = "arser"
    description = "Generate Classes from Schema"

    val resourcesDir = layout.projectDirectory.dir("src").dir("main").dir("resources").dir("xml")
    val schema = resourcesDir.file("arser-config.xsd")
    inputs.file(schema)

    val binding = resourcesDir.file("binding.xjb")
    inputs.file(binding)

    outputs.dir(destDirJaxb)

    mkdir(destDirJaxb)

    val compileJavaTask = tasks.named<JavaCompile>("compileJava").get()
    val currentEncoding = compileJavaTask.options.encoding

    val xjcToolsClasspath = configurations["jaxb"].asPath

    ant.withGroovyBuilder {
        "taskdef"(
            "name" to "xjc", "classname" to "com.sun.tools.xjc.XJCTask", "classpath" to xjcToolsClasspath
        )

        "xjc"(
            "destdir" to destDirJaxb,
            "schema" to schema,
            "binding" to binding,
            "package" to "de.freese.arser.config.xml",
            "encoding" to currentEncoding,
            "extension" to true,
            "removeOldOutput" to true
        ) {
            "arg"("value" to "-npa")
            // schema(dir: destDir, includes: "*.xsd")

            // XJC has an own Build-Cache.
            // Files specified as the schema files and binding files are automatically added to the "depends" set as well,
            // but if those schemas are including/importing other schemas, you have to use a nested <depends> elements.

            // Avoid Warning: Consider using <depends>/<produces> so that XJC won't do unnecessary compilation

            // depends(file: binding)
            // depends(dir: schemaDir.dir("GolfCountryClub"), includes: "**/*.xsd")
            // produces(dir: destdir, includes: "**/*.java")
            "produces"("dir" to destDirJaxb, "includes" to "**/*.java")
        }

//            taskdef(name: "jxc", classname: "com.sun.tools.jxc.SchemaGenTask", classpath: configurations.jaxb.asPath)
//
//            jxc(srcdir: destDir,
//                    destdir: layout.buildDirectory.get(),
//                    includeAntRuntime: "false",
//                    verbose: false) {
//                //schema(file: "CustomSchema.xsd", namespace: "")
//            }
//         }
    }
}
// tasks.named("compileJava").get().dependsOn("genJaxb")
