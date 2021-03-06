import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
    id("org.jetbrains.compose") version "1.0.0-rc3"
}

group = "me.ziomaleq"
version = "1.0"

repositories {
    jcenter()
    mavenCentral()
    maven(uri("https://maven.pkg.jetbrains.space/public/p/compose/dev"))
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("club.minnced:java-discord-rpc:2.0.2")
    implementation("org.jsoup:jsoup:1.14.3")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ReadMeAManga"
            packageVersion = "1.0.0"
        }
    }
}