package com.superloan.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** 设置数据在 DataStore 中的键 */
private val SETTINGS_KEY = stringPreferencesKey("app_settings_json")

/**
 * 设置仓库：与账单仓库共用同一个 DataStore 文件，只是键不同。
 */
class SettingsRepository(appContext: Context) {

    private val store: DataStore<Preferences> = appContext.appDataStore

    /** 响应式设置流 */
    val settings: Flow<AppSettings> = store.data.map { prefs ->
        SettingsJsonCodec.decode(prefs[SETTINGS_KEY])
    }

    /** 读取当前设置快照 */
    suspend fun current(): AppSettings = settings.first()

    /** 以「读改写」方式原子更新设置 */
    suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs ->
            val current = SettingsJsonCodec.decode(prefs[SETTINGS_KEY])
            prefs[SETTINGS_KEY] = SettingsJsonCodec.encode(transform(current))
        }
    }
}
