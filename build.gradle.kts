plugins {
  kotlin("jvm") version "2.0.20"
  application
}

group = "org.example"

version = "0.1"

repositories { mavenCentral() }

application { mainClass = "org.example.MainKt" }

dependencies {
  val logbackVersion = "1.5.6"
  implementation("ch.qos.logback:logback-classic:$logbackVersion")

  val ktorVersion = "2.3.12"
  implementation("io.ktor:ktor-client-core:$ktorVersion")
  implementation("io.ktor:ktor-client-cio:$ktorVersion")

  testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }

kotlin { jvmToolchain(17) }

tasks {
  val fatJar =
      register<Jar>("fatJar") {
        dependsOn.addAll(
            listOf(
                "compileJava",
                "compileKotlin",
                "processResources")) // We need this for Gradle optimization to work
        archiveClassifier.set("standalone") // Naming the jar
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
          attributes(mapOf("Main-Class" to application.mainClass))
        } // Provided we set it up in the application plugin configuration
        val sourcesMain = sourceSets.main.get()
        val contents =
            configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) } +
                sourcesMain.output
        from(contents)
      }
  build {
    dependsOn(fatJar) // Trigger fat jar creation during build
  }
}
