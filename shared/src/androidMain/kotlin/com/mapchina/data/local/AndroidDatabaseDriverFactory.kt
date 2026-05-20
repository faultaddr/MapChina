package com.mapchina.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = MapChinaDatabase.Schema,
            context = context,
            name = "mapchina.db",
            callback = object : AndroidSqliteDriver.Callback(MapChinaDatabase.Schema) {
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    // 在 onUpgrade 中创建新表（旧数据库升级时）
                    db.execSQL("CREATE TABLE IF NOT EXISTS achievement_definition (achievement_id TEXT NOT NULL PRIMARY KEY, category TEXT NOT NULL, sub_category TEXT NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL, icon TEXT NOT NULL, rarity TEXT NOT NULL, trigger_type TEXT NOT NULL, trigger_condition TEXT NOT NULL, reward_score INTEGER NOT NULL, sort_order INTEGER NOT NULL, status TEXT NOT NULL DEFAULT 'active')")
                    db.execSQL("CREATE TABLE IF NOT EXISTS user_achievement (user_id TEXT NOT NULL, achievement_id TEXT NOT NULL, progress_value INTEGER NOT NULL DEFAULT 0, progress_target INTEGER NOT NULL, status TEXT NOT NULL DEFAULT 'locked', unlock_time INTEGER, PRIMARY KEY(user_id, achievement_id))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS user_score (user_id TEXT NOT NULL PRIMARY KEY, current_score INTEGER NOT NULL DEFAULT 0, current_level INTEGER NOT NULL DEFAULT 1, updated_at INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS atlas_definition (atlas_id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, description TEXT NOT NULL, cover_image TEXT NOT NULL DEFAULT '', status TEXT NOT NULL DEFAULT 'active')")
                    db.execSQL("CREATE TABLE IF NOT EXISTS atlas_item (atlas_id TEXT NOT NULL, attraction_id TEXT NOT NULL, item_name TEXT NOT NULL, province TEXT NOT NULL DEFAULT '', city TEXT NOT NULL DEFAULT '', PRIMARY KEY (atlas_id, attraction_id))")
                }
            }
        )
    }
}
