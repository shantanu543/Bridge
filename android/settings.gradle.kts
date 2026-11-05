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

rootProject.name = "Bridge"
include(":app", ":core:datastore", ":core:designsystem", ":core:network", ":feature:auth", ":feature:onboarding")
project(":core:designsystem").projectDir = file("core/designsystem")
project(":core:datastore").projectDir = file("core/datastore")
project(":core:network").projectDir = file("core/network")
project(":feature:auth").projectDir = file("feature/auth")
project(":feature:onboarding").projectDir = file("feature/onboarding")
