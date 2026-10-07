plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

group = "dev.cosmojar.StellarityPaper"
version = "1.0.0"

dependencies {
    implementation("org.bstats:bstats-bukkit:3.2.1")
    implementation(project(":api"))
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:24.1.0")
    compileOnly("com.google.code.gson:gson:2.11.0")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.0")
    compileOnly("com.sk89q.worldguard:worldguard-core:7.0.9")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
    compileOnly(files("../Важное/eco-2026.33-modrinth.jar"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.withType<Zip>().configureEach {
    entryCompression = ZipEntryCompression.DEFLATED
}

