pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
    }
}

rootProject.name = "StellarityPaper-parent"

include("api")
project(":api").projectDir = file("api")

include("core")
project(":core").projectDir = file("core")

include("impl-v1_21_4")
project(":impl-v1_21_4").projectDir = file("impl/v1_21_4")

include("impl-v1_21_5")
project(":impl-v1_21_5").projectDir = file("impl/v1_21_5")

include("impl-v1_21_6")
project(":impl-v1_21_6").projectDir = file("impl/v1_21_6")

include("impl-v1_21_7")
project(":impl-v1_21_7").projectDir = file("impl/v1_21_7")

include("impl-v1_21_8")
project(":impl-v1_21_8").projectDir = file("impl/v1_21_8")

include("impl-v1_21_9")
project(":impl-v1_21_9").projectDir = file("impl/v1_21_9")

include("impl-v1_21_10")
project(":impl-v1_21_10").projectDir = file("impl/v1_21_10")

include("impl-v26_1_1")
project(":impl-v26_1_1").projectDir = file("impl/v26_1_1")

include("impl-v26_1_2")
project(":impl-v26_1_2").projectDir = file("impl/v26_1_2")

include("impl-v26_2")
project(":impl-v26_2").projectDir = file("impl/v26_2")

include("impl-v26_3")
project(":impl-v26_3").projectDir = file("impl/v26_3")
