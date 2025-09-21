pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

toolchainManagement {
    jvm {
        javaRepositories {
            repository("foojay") {
                vendor = JvmVendorSpec.AZUL
            }
        }
    }
}

rootProject.name = "MenuApp"
include(":app")
