plugins {
    java
    id("com.gradleup.shadow") version "9.3.1"
}

allprojects {
    apply(plugin = "java")


    group = "dev.cosmojar.StellarityPaper"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()

        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
        maven { url = uri("https://repo.papermc.io/repository/maven-snapshots/") }
        maven { url = uri("https://maven.enginehub.org/repo/") }
        maven { url = uri("https://repo.auxilor.io/repository/maven-public/") }

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

    tasks.matching { it.name == "reobfJar" }.configureEach {
        enabled = false
    }
}

dependencies {
    implementation("org.bstats:bstats-bukkit:3.2.1")
}

tasks.shadowJar {
    archiveBaseName.set("StellarityPaper")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())

    includeEmptyDirs = false
    mergeServiceFiles()
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }


    from(project(":core").sourceSets.main.get().output)
    from(project(":impl-v1_21_4").sourceSets.main.get().output)
    from(project(":impl-v1_21_5").sourceSets.main.get().output)
    from(project(":impl-v1_21_6").sourceSets.main.get().output)
    from(project(":impl-v1_21_7").sourceSets.main.get().output)
    from(project(":impl-v1_21_8").sourceSets.main.get().output)
    from(project(":impl-v1_21_9").sourceSets.main.get().output)
    from(project(":impl-v1_21_10").sourceSets.main.get().output)
    from(project(":impl-v26_1_1").sourceSets.main.get().output)
    from(project(":impl-v26_1_2").sourceSets.main.get().output)
    from(project(":impl-v26_2").sourceSets.main.get().output)
    from(project(":impl-v26_3").sourceSets.main.get().output)
    from(project(":api").sourceSets.main.get().output)

    relocate("org.bstats", "dev.cosmojar.StellarityPaper.bstats")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
