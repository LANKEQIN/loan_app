package com.superloan.app.data

import com.superloan.app.core.addMonths
import com.superloan.app.core.monthRange
import kotlin.random.Random

/**
 * 分期账单数据模型。
 *
 * 说明：endMonth 不落库，始终由 startMonth + (months − 1) 推导。
 */
data class Bill(
    /** 自动生成的 id */
    val id: String,
    /** 平台，如「京东白条」 */
    val platform: String,
    /** 商品 / 用途 */
    val item: String,
    /** 本金 */
    val principal: Double,
    /** 总还款额（含息 / 含费），不小于本金 */
    val totalAmount: Double,
    /** 分期期数 */
    val months: Int,
    /** 首期账单月份 YYYY-MM */
    val startMonth: String,
    /** 每月出账日 1-31，可空 */
    val billingDay: Int? = null,
    /** 每月还款日 1-31，可空 */
    val repaymentDay: Int? = null
) {
    /** 末月：startMonth + (months - 1) */
    val endMonth: String get() = addMonths(startMonth, months - 1)

    /** 每期等额月供 = 总还款额 / 期数 */
    val monthlyPayment: Double get() = if (months <= 0) 0.0 else totalAmount / months

    /** 全部账单月份 */
    val monthList: List<String> get() = monthRange(startMonth, months)
}

/**
 * 生成本地账单 id：bill_ + 时间戳 36 进制 + 6 位随机串
 */
fun newBillId(): String {
    val timestamp = System.currentTimeMillis().toString(36)
    val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
    val randomPart = buildString {
        repeat(6) { append(alphabet[Random.nextInt(alphabet.length)]) }
    }
    return "bill_$timestamp$randomPart"
}

/**
 * 本地持久化的完整数据结构。
 *
 * @param paid paid[billId] = ["2026-08", "2026-09"] 已还月份集合
 * @param seeded 是否已经写入过种子数据（避免用户清空账单后被反复写入）
 */
data class BillStoreData(
    val bills: List<Bill> = emptyList(),
    val paid: Map<String, List<String>> = emptyMap(),
    val seeded: Boolean = false
) {
    /** 取某笔账单的已还月份 */
    fun paidMonthsOf(billId: String): List<String> = paid[billId] ?: emptyList()
}
