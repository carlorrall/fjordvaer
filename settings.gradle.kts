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
        // NetCDF-Java (GRIB parsing) is published by Unidata, not Maven Central.
        maven { url = uri("https://artifacts.unidata.ucar.edu/repository/unidata-all") }
    }
}

rootProject.name = "Fjordvaer"
include(":app")
