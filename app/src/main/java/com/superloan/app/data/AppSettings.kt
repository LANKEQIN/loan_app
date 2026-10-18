package com.superloan.app.data

/** 主题模式 */
enum class ThemeMode(val title: String, val desc: String) {
    /** 跟随系统深色开关 */
    SYSTEM("跟随系统", "随系统深色开关自动切换"),

    /** 始终浅色 */
    LIGHT("浅色", "始终使用浅色外观"),

    /** 始终深色 */
    DARK("深色", "始终使用深色外观");

    companion object {
        fun fromName(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

/**
 * 应用设置项。
 *
 * 这里只存「值」，不依赖任何 UI 类型，方便持久化与迁移。
 * 配色方案只存 id 字符串，具体颜色由 ui.theme.PaletteId 映射。
 */
data class AppSettings(
    /** 主题模式 */
    val themeMode: ThemeMode = ThemeMode.SYSTEM,

    /** 配色方案 id，对应 ui.theme.PaletteId.id */
    val paletteId: String = DEFAULT_PALETTE,

    /** 账单列表默认展开全部期数 */
    val billListExpanded: Boolean = false,

    /** 新增账单时，起始月默认取当前月 */
    val newBillStartMonthCurrent: Boolean = true,

    /** 月供计算器默认还款方式（RepaymentType.name） */
    val defaultRepaymentType: String = DEFAULT_REPAYMENT_TYPE
) {
    companion object {
        /** 默认配色：品牌绿 */
        const val DEFAULT_PALETTE = "brand_green"

        /** 默认还款方式：等额本息 */
        const val DEFAULT_REPAYMENT_TYPE = "ANNUITY"
    }
}
