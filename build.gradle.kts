import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("net.fabricmc.fabric-loom")
    id("org.jetbrains.kotlin.jvm")
    `maven-publish`
    signing
}

val minecraftVersion = providers.gradleProperty("minecraft_version").get()
val loaderVersion = providers.gradleProperty("loader_version").get()
val fabricApiVersion = providers.gradleProperty("fabric_api_version").get()
val fabricKotlinVersion = providers.gradleProperty("fabric_kotlin_version").get()
val lwjglVersion = providers.gradleProperty("lwjgl_version").get()
val serializationVersion = providers.gradleProperty("serialization_version").get()
val modVersion = providers.gradleProperty("mod_version").get()
val mavenGroup = providers.gradleProperty("maven_group").get()
val archivesBaseName = providers.gradleProperty("archives_base_name").get()

val signingKey = providers.gradleProperty("signingKey")
    .orElse(providers.environmentVariable("ORG_GRADLE_PROJECT_signingKey"))
    .orElse(providers.environmentVariable("GPG_PRIVATE_KEY"))
val signingPassword = providers.gradleProperty("signingPassword")
    .orElse(providers.environmentVariable("ORG_GRADLE_PROJECT_signingPassword"))
    .orElse(providers.environmentVariable("GPG_PASSPHRASE"))

version = modVersion
group = mavenGroup

base {
    archivesName.set(archivesBaseName)
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    implementation("net.fabricmc:fabric-loader:$loaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    implementation("net.fabricmc:fabric-language-kotlin:$fabricKotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    compileOnly(files("libs/iris.jar"))

    val meshoptimizer = "org.lwjgl:lwjgl-meshoptimizer:$lwjglVersion"
    implementation(meshoptimizer)
    include(meshoptimizer)
    listOf(
        "natives-windows",
        "natives-windows-arm64",
        "natives-windows-x86",
        "natives-linux",
        "natives-macos",
        "natives-macos-arm64"
    ).forEach { classifier ->
        runtimeOnly("$meshoptimizer:$classifier")
        include("$meshoptimizer:$classifier")
    }
}

tasks.processResources {
    val version = project.version.toString()
    inputs.property("version", version)
    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("fabric")
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    jvmToolchain(25)
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(rootProject.file("LICENSE")) {
        into("META-INF")
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                groupId = mavenGroup
                artifactId = archivesBaseName
                version = modVersion
                artifact(tasks.named("jar")) {
                    classifier = ""
                }
                artifact(tasks.named("sourcesJar")) {
                    classifier = "sources"
                }
                artifact(javadocJar) {
                    classifier = "javadoc"
                }
                pom {
                    name.set("libgltf")
                    description.set("High-performance glTF 2.0 rendering library for Minecraft")
                    url.set("https://github.com/Micheanl/libglTF")
                    licenses {
                        license {
                            name.set("MIT")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("Micheanl")
                            name.set("Chen Micheanl")
                        }
                    }
                    scm {
                        connection.set("scm:git:https://github.com/Micheanl/libglTF.git")
                        developerConnection.set("scm:git:git@github.com:Micheanl/libglTF.git")
                        url.set("https://github.com/Micheanl/libglTF")
                    }
                }
            }
        }
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/Micheanl/libgltf")
                credentials {
                    username = (project.findProperty("gpr.user") as String?) ?: System.getenv("GITHUB_ACTOR").orEmpty()
                    password = (project.findProperty("gpr.key") as String?) ?: System.getenv("GITHUB_TOKEN").orEmpty()
                }
            }
        }
        if (signingKey.isPresent && signingPassword.isPresent) {
            signing {
                useInMemoryPgpKeys(signingKey.get(), signingPassword.get())
                sign(publishing.publications["mavenJava"])
            }
        }
    }
}
