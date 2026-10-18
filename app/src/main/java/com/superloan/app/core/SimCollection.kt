package com.superloan.app.core

import java.time.LocalDate
import kotlin.math.absoluteValue

/**
 * 「作死模式」的催收演出层：短信、来电、亲友消息。
 *
 * 设计要点：消息与来电都是**事件**，一旦投递就永久留在存档里，不会因为
 * 后来把欠款还清而凭空消失。投递靠一个「水位线」控制：
 * 每次只生成 (水位, 当前虚拟天] 区间内的事件，因此无论快进 1 天还是 90 天，
 * 结果都完全一致，也不会重复投递。
 *
 * id 是**确定性**的（由 `借款 id + 期数 + 事件类型` 拼成），
 * 所以即使水位线逻辑出岔子，也可以在写入时按 id 去重，保证永不重复。
 */

// ---------------------------------------------------------------------------
// 事件模型
// ---------------------------------------------------------------------------

/** 催收消息类型 */
enum class SimMessageKind(val title: String) {
    /** 还款提醒（到期前 3 天） */
    REMIND("还款提醒"),

    /** 出账通知（到期当天） */
    BILL("出账通知"),

    /** 逾期第 1 天提醒 */
    OVERDUE("逾期提醒"),

    /** 进入催收流程 */
    COLLECT("催收通知"),

    /** 已联系紧急联系人 */
    CONTACT("联系人通知"),

    /** 已向通讯录好友群发 */
    BLAST("通讯录通知"),

    /** 法务诉讼警告 */
    LEGAL("法务通知"),

    /** 亲友发来的消息 */
    FAMILY("亲友消息");

    companion object {
        fun of(name: String?): SimMessageKind = entries.firstOrNull { it.name == name } ?: COLLECT
    }
}

/** 来电处理状态 */
enum class SimCallStatus(val title: String) {
    /** 还没处理，进来会直接弹来电界面 */
    PENDING("未处理"),
    ANSWERED("已接听"),
    REJECTED("已拒接");

    companion object {
        fun of(name: String?): SimCallStatus = entries.firstOrNull { it.name == name } ?: PENDING
    }
}

/** 一条催收消息 */
data class SimMessage(
    val id: String,
    val kind: SimMessageKind,
    /** 发件人展示名，如「闪电借」「表姐」 */
    val sender: String,
    val text: String,
    /** 到达日（虚拟天序号） */
    val arriveDay: Int,
    /** 到达日期 YYYY-MM-DD */
    val arriveDate: String,
    /** 到达时刻（伪随机的确定性时间，如 10:23） */
    val arriveClock: String
)

/** 一通来电 */
data class SimCall(
    val id: String,
    /** 来电方展示名 */
    val caller: String,
    val arriveDay: Int,
    val arriveDate: String,
    val arriveClock: String,
    val status: SimCallStatus
)

// ---------------------------------------------------------------------------
// 事件参数
// ---------------------------------------------------------------------------

/** 逾期后的催收短信：相对该期到期日的偏移天数 -> 消息类型 */
private val OVERDUE_MESSAGE_OFFSETS: List<Pair<Int, SimMessageKind>> = listOf(
    1 to SimMessageKind.OVERDUE,
    3 to SimMessageKind.COLLECT,
    7 to SimMessageKind.CONTACT,
    15 to SimMessageKind.BLAST,
    30 to SimMessageKind.LEGAL
)

/** 逾期后的来电轰炸：相对该期到期日的偏移天数 -> 来电方 */
private val OVERDUE_CALL_OFFSETS: List<Pair<Int, String>> = listOf(
    3 to "未知号码",
    6 to "400-669-8888",
    9 to "催收专线",
    20 to "某某金融 · 法务部"
)

/** 亲友被骚扰：相对「最早一笔逾期」的偏移天数（递进式：关心 → 追问 → 社死） */
private val FAMILY_OFFSETS: List<Pair<Int, Pair<String, String>>> = listOf(
    7 to ("表姐" to "弟，最近还好吗？刚才有个陌生号码打给我，问你在不在，说你欠钱不还…你是不是遇到什么事了？"),
    15 to ("老同学 · 阿哲" to "我这个月已经接了 6 个电话找你要钱，到底什么情况？有困难就说话，别硬扛。"),
    30 to ("同事 · 小刘" to "今天有人打电话到公司前台，自称是「法务部」，说找你核实欠款…全办公室都听见了。")
)

// ---------------------------------------------------------------------------
// 生成
// ---------------------------------------------------------------------------

/** 伪随机但确定性的到达时刻，让收件箱看起来像真的 */
private fun clockOf(id: String): String {
    val seed = id.hashCode().absoluteValue
    val hour = seed % 23 + 1
    val minute = seed / 23 % 60
    return "%02d:%02d".format(hour, minute)
}

/** 逾期阶段名，用于文案里的「已逾期 N 天」 */
private fun stageText(days: Int): String = if (days <= 1) "1 天" else "$days 天"

/**
 * 生成 (fromDay, toDay] 区间内应投递的催收短信。
 *
 * @param loanStatuses 各笔借款的实时状态（由 [simStatus] 得出）
 * @param startDate    开局日期
 * @param dayIndex     当前虚拟天序号（用于换算「最早一笔逾期」）
 * @param fromDay      已投递水位（不含）
 * @param toDay        投递目标天（含）
 */
fun simPendingMessages(
    loanStatuses: List<SimLoanStatus>,
    startDate: LocalDate,
    dayIndex: Int,
    fromDay: Int,
    toDay: Int
): List<SimMessage> {
    val result = ArrayList<SimMessage>()
    if (toDay <= fromDay) return result

    /** 该天是否落在本次投递区间内 */
    fun arrived(day: Int): Boolean = day > fromDay && day <= toDay

    fun emit(id: String, kind: SimMessageKind, sender: String, text: String, day: Int) {
        result += SimMessage(
            id = id,
            kind = kind,
            sender = sender,
            text = text,
            arriveDay = day,
            arriveDate = simDateAt(startDate, day).toString(),
            arriveClock = clockOf(id)
        )
    }

    loanStatuses.forEach { status ->
        val loan = status.loan
        val platform = loan.productName
        val money = formatMoney(loan.monthlyPayment * (loan.paidPeriods + 1))

        for (period in 1..loan.termMonths) {
            val due = simDueDayIndex(loan, startDate, period)

            // ---------- 信息类：无论还不还都会收到 ----------
            if (arrived(due - 3)) {
                emit(
                    "m_${loan.id}_${period}_remind",
                    SimMessageKind.REMIND,
                    platform,
                    "【$platform】您本期账单 $money 将于 ${formatDateLabel(simDateAt(startDate, due))} 到期，请提前准备好还款资金。",
                    due - 3
                )
            }
            if (arrived(due)) {
                emit(
                    "m_${loan.id}_${period}_bill",
                    SimMessageKind.BILL,
                    platform,
                    "【$platform】您本期账单已出，应还 $money，请于今日 24:00 前完成还款。",
                    due
                )
            }

            // ---------- 催收类：仅在「投递当天该期仍未还」时才发 ----------
            // 已投递的消息不会因为日后还清而消失，这是刻意为之
            if (period > loan.paidPeriods) {
                OVERDUE_MESSAGE_OFFSETS.forEach { (offset, kind) ->
                    val day = due + offset
                    if (!arrived(day)) return@forEach
                    val text = when (kind) {
                        SimMessageKind.OVERDUE ->
                            "【$platform】您的账单已逾期 ${stageText(offset)}，应还 $money。为避免影响征信，请尽快还款。"
                        SimMessageKind.COLLECT ->
                            "【$platform】您的欠款已进入催收流程，本金与罚息仍在计算中，请保持通讯畅通配合处理。"
                        SimMessageKind.CONTACT ->
                            "【$platform】已联系您的紧急联系人，请尽快处理，避免造成不良影响。"
                        SimMessageKind.BLAST ->
                            "【$platform】已向您的 12 位通讯录好友发送协助还款通知，请尽快处理。"
                        else ->
                            "【$platform · 法务部】如仍未还款，我方将依法提起诉讼，届时产生的费用由您承担。"
                    }
                    emit("m_${loan.id}_${period}_${kind.name}", kind, platform, text, day)
                }
            }
        }
    }

    // ---------- 亲友消息：跟着全局「最早一笔逾期」走，每人只发一次 ----------
    val earliestOverdueDay = loanStatuses
        .filter { it.isOverdue }
        .minOfOrNull { dayIndex - it.overdueDays }

    if (earliestOverdueDay != null) {
        FAMILY_OFFSETS.forEach { (offset, payload) ->
            val day = earliestOverdueDay + offset
            if (!arrived(day)) return@forEach
            val (sender, text) = payload
            emit("m_family_$offset", SimMessageKind.FAMILY, sender, text, day)
        }
    }

    return result.sortedBy { it.arriveDay }
}

/**
 * 生成 (fromDay, toDay] 区间内应「响」起来的来电。
 *
 * 一次性快进多天时这里会一次返回好几通，于是来电界面会一通接一通地弹，
 * 这就是字面意义上的电话轰炸。
 */
fun simPendingCalls(
    loanStatuses: List<SimLoanStatus>,
    startDate: LocalDate,
    fromDay: Int,
    toDay: Int
): List<SimCall> {
    val result = ArrayList<SimCall>()
    if (toDay <= fromDay) return result

    loanStatuses.forEach { status ->
        val loan = status.loan
        for (period in 1..loan.termMonths) {
            if (period <= loan.paidPeriods) continue
            val due = simDueDayIndex(loan, startDate, period)
            OVERDUE_CALL_OFFSETS.forEach { (offset, caller) ->
                val day = due + offset
                if (day <= fromDay || day > toDay) return@forEach
                val id = "c_${loan.id}_${period}_$offset"
                result += SimCall(
                    id = id,
                    caller = caller,
                    arriveDay = day,
                    arriveDate = simDateAt(startDate, day).toString(),
                    arriveClock = clockOf(id),
                    status = SimCallStatus.PENDING
                )
            }
        }
    }

    return result.sortedBy { it.arriveDay }
}

// ---------------------------------------------------------------------------
// 接听话术
// ---------------------------------------------------------------------------

/**
 * 接听后逐句显示的催收话术。
 * 纯文字对白，不放音频 —— 怕真的把自己吓一跳。
 */
fun simCallScript(caller: String, overdueDays: Int, overdueAmount: Double): List<String> = listOf(
    "喂，您好，请问是机主本人吗？我是$caller 这边的。",
    "系统显示您有一笔 ${formatMoney(overdueAmount)} 的账款已经逾期，方便现在处理一下吗？",
    "已经拖了 ${stageText(overdueDays)} 了，再这样下去我们只能联系您的紧急联系人。",
    "我们也理解您的难处，那您今天先还一部分也行，您看能还多少？",
    "您这样我们很难往上交代，只能按流程走下一步了。",
    "喂？喂？您还在听吗……"
)
