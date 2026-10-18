// 根构建脚本：仅声明插件版本，不在此处应用
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.4.21" apply false
    // Kotlin 2.0 之后 Compose 编译器作为独立的 Kotlin 插件提供
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
