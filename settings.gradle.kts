pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        // Necesario para KCEF / JCEF (navegador embebido)
        maven("https://jogamp.org/deployment/maven")
    }
}

rootProject.name = "AGTechDesktop"
