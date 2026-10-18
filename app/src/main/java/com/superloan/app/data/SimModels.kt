package com.superloan.app.data

import com.superloan.app.core.SimCall
import com.superloan.app.core.SimCallStatus
import com.superloan.app.core.SimLoan
import com.superloan.app.core.SimMessage
import com.superloan.app.core.SimPurchase
import kotlin.random.Random

/**
 * 「作死模式」模拟器的持久化数据结构。
 *
 * 独立占用一个 DataStore 键，与真实账单完全隔离：
 * 破产清零只会清空这里，绝不会动到真实账单。
 *
 * @param startedToday 开局日期（真实世界），格式 YYYY-MM-DD；空串表示还没开局
 * @param fastForwardDays 手动累计快进的天数
 * @param wallet 手上余额
 * @param bankruptcyCount 历史破产次数（战绩）
 * @param messages 已投递的催收短信（事件一旦投递就永久保留）
 * @param calls 已投递的来电记录
 * @param readMessageCount 已读消息条数，未读数 = 消息总数 − 它
 * @param watermarkDay 消息/来电的投递水位：已投递到第几个虚拟天
 */
data class SimStoreData(
    val startedToday: String = "",
    val fastForwardDays: Int = 0,
    val wallet: Double = 0.0,
    val loans: List<SimLoan> = emptyList(),
    val purchases: List<SimPurchase> = emptyList(),
    val bankruptcyCount: Int = 0,
    val messages: List<SimMessage> = emptyList(),
    val calls: List<SimCall> = emptyList(),
    val readMessageCount: Int = 0,
    val watermarkDay: Int = 0
) {
    /** 是否已经开过局 */
    val started: Boolean get() = startedToday.isNotBlank()

    /** 未读消息数 */
    val unreadCount: Int get() = (messages.size - readMessageCount).coerceAtLeast(0)

    /** 待处理来电（会弹来电界面的那些） */
    val pendingCalls: List<SimCall> get() = calls.filter { it.status == SimCallStatus.PENDING }
}

/**
 * 生成模拟器借款 id：sim_ + 时间戳 36 进制 + 6 位随机串
 */
fun newSimLoanId(): String {
    val timestamp = System.currentTimeMillis().toString(36)
    val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
    val randomPart = buildString {
        repeat(6) { append(alphabet[Random.nextInt(alphabet.length)]) }
    }
    return "sim_$timestamp$randomPart"
}
