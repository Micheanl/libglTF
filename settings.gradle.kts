pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("net.fabricmc.fabric-loom") version providers.gradleProperty("loom_version").get()
        id("org.jetbrains.kotlin.jvm") version providers.gradleProperty("kotlin_version").get()
    }
}

plugins {
    id("com.gradleup.nmcp.settings") version "1.6.1"
}

nmcpSettings {
    centralPortal {
        username = providers.gradleProperty("ossrhUsername")
            .orElse(providers.environmentVariable("OSSRH_USERNAME"))
            .getOrNull() ?: ""
        password = providers.gradleProperty("ossrhPassword")
            .orElse(providers.environmentVariable("OSSRH_PASSWORD"))
            .getOrNull() ?: ""
    }
}

rootProject.name = "libgltf"
