plugins {
  kotlin("jvm") version "2.0.20"
  application
}

group = "org.example"

version = "1.2"

repositories { mavenCentral() }

application { mainClass = "org.example.MainKt" }

dependencies {
  val logbackVersion = "1.5.6"
  implementation("ch.qos.logback:logback-classic:$logbackVersion")

  val ktorVersion = "2.3.12"
  implementation("io.ktor:ktor-client-core:$ktorVersion")
  implementation("io.ktor:ktor-client-cio:$ktorVersion")

  testImplementation(kotlin("test"))

  val seleniumVersion = "4.25.0"
  implementation("org.seleniumhq.selenium:selenium-java:$seleniumVersion")
  implementation("org.seleniumhq.selenium:selenium-chrome-driver:$seleniumVersion")
  implementation("org.seleniumhq.selenium:selenium-devtools-v129:$seleniumVersion")
  implementation("io.github.bonigarcia:webdrivermanager:5.9.2")

  implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
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
