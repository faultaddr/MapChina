package com.mapchina.data.repository

import com.mapchina.data.local.MapChinaDatabase

class SettingsRepository(private val database: MapChinaDatabase) {

    fun getString(key: String): String? {
        return database.appSettingQueries.selectByKey(key).executeAsOneOrNull()?.value_
    }

    fun setString(key: String, value: String) {
        database.appSettingQueries.upsert(key, value)
    }

    fun getInt(key: String, default: Int = 0): Int {
        return getString(key)?.toIntOrNull() ?: default
    }

    fun setInt(key: String, value: Int) {
        setString(key, value.toString())
    }

    fun getLong(key: String, default: Long = 0L): Long {
        return getString(key)?.toLongOrNull() ?: default
    }

    fun setLong(key: String, value: Long) {
        setString(key, value.toString())
    }
}
