// 插件仓库与依赖仓库统一在此声明，子模块不再单独声明仓库
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // 禁止子模块自行声明仓库，保证依赖来源统一
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SuperLoan"
include(":app")
