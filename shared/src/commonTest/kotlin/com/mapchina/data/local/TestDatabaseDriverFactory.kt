package com.mapchina.data.local

import app.cash.sqldelight.db.SqlDriver

expect class TestDatabaseDriverFactory() {
    fun createDriver(): SqlDriver
}
