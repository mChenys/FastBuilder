plugins {
    kotlin("jvm") version "1.3.61"
    java
    `java-gradle-plugin`
    id("maven-publish")
    id("com.gradle.plugin-publish") version "0.16.0"
}

val artifactId="FastBuilder"
val groupName = "io.github.tiyateam.fastbuilder"
val artifactVersion = "1.0.62"
group = groupName
version = artifactVersion

pluginBundle {
    website = "https://github.com/TIYATEAM/FastBuilder"
    vcsUrl = "https://github.com/TIYATEAM/FastBuilder"
    tags = listOf("Android", "aar", "compile", "fast")
}

repositories {
    google()
    mavenLocal()
    mavenCentral()
}

gradlePlugin {
    plugins {
        create(artifactId) {
            id = groupName
            displayName = artifactId
            description = "Replace Android module dependency with AAR dependency at compile time."
            implementationClass = "org.lizhi.tiya.plugin.FastBuilderPlugin"
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
//    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
//    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    implementation("com.android.tools.build:gradle:3.6.0")
    implementation("org.jooq:joor-java-8:0.9.13")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.60")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}