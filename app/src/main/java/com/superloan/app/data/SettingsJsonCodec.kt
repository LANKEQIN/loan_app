package com.superloan.app.data

import org.json.JSONObject

/**
 * 设置项的 JSON 编解码（与账单一样使用 Android 内置 org.json，零额外依赖）。
 */
object SettingsJsonCodec {

    fun encode(settings: AppSettings): String {
        val root = JSONObject()
        root.put("themeMode", settings.themeMode.name)
        root.put("paletteId", settings.paletteId)
        root.put("billListExpanded", settings.billListExpanded)
        root.put("newBillStartMonthCurrent", settings.newBillStartMonthCurrent)
        root.put("defaultRepaymentType", settings.defaultRepaymentType)
        return root.toString()
    }

    fun decode(json: String?): AppSettings {
        if (json.isNullOrBlank()) return AppSettings()
        return try {
            val root = JSONObject(json)
            AppSettings(
                themeMode = ThemeMode.fromName(root.optString("themeMode", ThemeMode.SYSTEM.name)),
                paletteId = root.optString("paletteId", AppSettings.DEFAULT_PALETTE),
                billListExpanded = root.optBoolean("billListExpanded", false),
                newBillStartMonthCurrent = root.optBoolean("newBillStartMonthCurrent", true),
                defaultRepaymentType = root.optString(
                    "defaultRepaymentType",
                    AppSettings.DEFAULT_REPAYMENT_TYPE
                )
            )
        } catch (e: Exception) {
            // 数据损坏时回退到默认设置，保证应用仍可正常启动
            AppSettings()
        }
    }
}
