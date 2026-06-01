package com.mapchina.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import kotlin.random.Random

actual class TestDatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val name = "test_${Random.nextLong()}.db"
        return NativeSqliteDriver(MapChinaDatabase.Schema, name)
    }
}
