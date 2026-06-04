plugins {
    kotlin("multiplatform") version "2.1.21" apply false
    kotlin("jvm") version "2.1.21" apply false
    kotlin("plugin.serialization") version "2.1.21" apply false
    kotlin("android") version "2.1.21" apply false
    id("com.android.application") version "8.9.3" apply false
    id("com.android.library") version "8.9.2" apply false
    id("org.jetbrains.compose") version "1.10.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21" apply false
    id("app.cash.sqldelight") version "2.0.2" apply false
    id("io.ktor.plugin") version "3.1.3" apply false
}
