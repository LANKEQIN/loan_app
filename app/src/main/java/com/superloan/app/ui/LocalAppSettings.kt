package com.superloan.app.ui

import androidx.compose.runtime.compositionLocalOf
import com.superloan.app.data.AppSettings

/**
 * 全局设置。
 * 由 Activity 顶层提供，各页面直接读取当前设置，无需层层传参。
 */
val LocalAppSettings = compositionLocalOf { AppSettings() }
