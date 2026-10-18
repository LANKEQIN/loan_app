package com.superloan.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** DataStore 文件名：应用内所有本地数据都存在同一个文件里 */
private const val STORE_NAME = "super_loan"

/**
 * 全应用共用的 DataStore 实例。
 * 账单数据与设置数据都存放在这里，只是使用不同的 key。
 */
internal val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = STORE_NAME)
