package com.superloan.app.core

import java.time.LocalDate
import java.util.Locale

/**
 * 全局格式化工具。
 *
 * 约定：
 * - 金额：`¥` + 保留两位小数，如 ¥9.36
 * - 利率：百分数保留两位小数，如 13.15%
 * - 月份：内部统一使用 `YYYY-MM` 字符串，展示为 `2026 年 8 月`
 */

/** 金额展示：¥9.36 */
fun formatMoney(value: Double): String = "¥" + formatAmount(value)

/** 金额数字部分：9.36 */
fun formatAmount(value: Double): String = String.format(Locale.CHINA, "%.2f", value)

/**
 * 利率展示：13.15%
 * @param rate 小数形式的利率，例如 0.131541 -> 13.15%
 */
fun formatPercent(rate: Double): String = String.format(Locale.CHINA, "%.2f%%", rate * 100.0)

/** 拆分 `YYYY-MM`，非法返回 null */
fun splitMonth(month: String): Pair<Int, Int>? {
    val parts = month.split("-")
    if (parts.size != 2) return null
    val year = parts[0].toIntOrNull() ?: return null
    val mon = parts[1].toIntOrNull() ?: return null
    if (mon !in 1..12) return null
    return year to mon
}

/** 月份偏移：addMonths("2026-08", 23) -> "2028-07" */
fun addMonths(month: String, offset: Int): String {
    val (year, mon) = splitMonth(month) ?: return month
    val total = year * 12 + (mon - 1) + offset
    val newYear = Math.floorDiv(total, 12)
    val newMon = Math.floorMod(total, 12) + 1
    return String.format(Locale.CHINA, "%04d-%02d", newYear, newMon)
}

/** 由起始月与期数推导全部账单月份 */
fun monthRange(startMonth: String, months: Int): List<String> {
    if (months <= 0) return emptyList()
    return (0 until months).map { addMonths(startMonth, it) }
}

/** 当前月份 `YYYY-MM` */
fun currentMonth(): String {
    val now = LocalDate.now()
    return String.format(Locale.CHINA, "%04d-%02d", now.year, now.monthValue)
}

/** 月份展示：2026-08 -> 2026 年 8 月 */
fun formatMonthLabel(month: String): String {
    val (year, mon) = splitMonth(month) ?: return month
    return "$year 年 $mon 月"
}

/** 出账日 / 还款日展示：6 日；为空时显示占位符 */
fun formatDayLabel(day: Int?): String = if (day == null) "未设置" else "$day 日"

/** 百分比进度 0..1 展示：75.00% */
fun formatProgress(ratio: Double): String =
    String.format(Locale.CHINA, "%.2f%%", ratio.coerceIn(0.0, 1.0) * 100.0)
