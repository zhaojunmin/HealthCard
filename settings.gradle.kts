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
        // 高德地图 SDK 托管仓库
        maven("https://maven.aliyun.com/repository/public")
    }
}
rootProject.name = "HealthCard"
include(":app")
