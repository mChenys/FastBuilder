plugins {
    kotlin("jvm") version "1.3.61"
    java
    `java-gradle-plugin`
    id("maven-publish")
    id("com.gradle.plugin-publish") version "0.16.0"
}

val artifactId="FastBuilder"
val groupName = "org.lizhi.tiya"
val artifactVersion = "1.0.0"
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
            description = "Improve the efficiency of compiling Android.Package module as AAR and cache it."
            implementationClass = "org.lizhi.tiya.plugin.FastBuilderPlugin"
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
//    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
//    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    implementation("com.android.tools.build:gradle:3.6.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}