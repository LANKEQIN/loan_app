package com.superloan.app.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

/**
 * 「作死模式」模拟器的纯逻辑层。
 *
 * 这里只做三件事，全部是无副作用纯函数：
 *  1. 模拟借贷产品 / 商城商品的静态目录；
 *  2. 由「开局日期 + 已快进天数」推导虚拟日期与每笔借款的到期情况；
 *  3. 计算罚息、征信分、破产判定等派生指标。
 *
 * 该玩法与真实账单模块完全隔离，不写八竿子打不着的真实数据。
 */

// ---------------------------------------------------------------------------
// 常量
// ---------------------------------------------------------------------------

/** 逾期达到该期数即触发破产结局 */
const val SIM_BANKRUPT_OVERDUE_PERIODS = 3

/** 虚拟天数上限，防止快进到极端值导致界面异常 */
const val SIM_MAX_DAY_INDEX = 3650

/** 单次快进的最大天数 */
const val SIM_MAX_FF_STEP = 90

/** 罚息日利率下限：日万分之五 */
private const val SIM_MIN_PENALTY_DAILY = 0.0005

/** 罚息在年化日利率基础上的上浮倍数 */
private const val SIM_PENALTY_MULTIPLIER = 1.5

/** 征信分满分 */
const val SIM_MAX_CREDIT = 100

/** 每逾期一期扣掉的征信分 */
private const val SIM_CREDIT_PENALTY_PER_PERIOD = 15

// ---------------------------------------------------------------------------
// 目录：模拟借贷产品
// ---------------------------------------------------------------------------

/**
 * 模拟借贷产品。
 *
 * @param annualRate 真实年化利率（小数），评级直接复用全项目共用的 rateLevelOf()
 */
data class SimProduct(
    val id: String,
    val name: String,
    val slogan: String,
    val annualRate: Double,
    val termMonths: Int,
    val minAmount: Double,
    val maxAmount: Double,
    /** 借款页底部的一句吐槽 */
    val comment: String
)

/** 五个画风各异的「平台」，年化从 0% 一路排到 34.8% */
val SIM_PRODUCTS: List<SimProduct> = listOf(
    SimProduct(
        id = "p_relatives",
        name = "亲友贷（免息版）",
        slogan = "免息 0 成本 · 但要还人情",
        annualRate = 0.0,
        termMonths = 1,
        minAmount = 100.0,
        maxAmount = 5000.0,
        comment = "唯一真正免息的选项。代价是过年要被问三遍「有对象了吗」。"
    ),
    SimProduct(
        id = "p_bank",
        name = "某某银行 · 信用快贷",
        slogan = "正规银行资金 · 年化低至 6.2%",
        annualRate = 0.062,
        termMonths = 24,
        minAmount = 5000.0,
        maxAmount = 50000.0,
        comment = "全场最像样的产品。缺点是每次借款都会在征信上留一条记录。"
    ),
    SimProduct(
        id = "p_baitiao",
        name = "某东白条 · 临时提额",
        slogan = "额度已提升 · 超 20 万用户的选择",
        annualRate = 0.138,
        termMonths = 6,
        minAmount = 1000.0,
        maxAmount = 15000.0,
        comment = "「临时提额」的意思是：提的时候很爽，还的时候很真。"
    ),
    SimProduct(
        id = "p_life",
        name = "某团生活费",
        slogan = "今天花明天的钱 · 吃饭也能分期",
        annualRate = 0.204,
        termMonths = 12,
        minAmount = 500.0,
        maxAmount = 30000.0,
        comment = "点外卖的时候顺手点一下，年化就上 20% 了。"
    ),
    SimProduct(
        id = "p_flash",
        name = "闪电借",
        slogan = "3 分钟到账 · 日息低至 0.03%",
        annualRate = 0.348,
        termMonths = 12,
        minAmount = 1000.0,
        maxAmount = 20000.0,
        comment = "页面只写「日息 0.03%」，折算下来年化 34.8%，评级「拉完了」。"
    )
)

// ---------------------------------------------------------------------------
// 目录：商城商品（正经 + 沙雕）
// ---------------------------------------------------------------------------

/** 商城商品 */
data class SimShopItem(
    val id: String,
    val name: String,
    val emoji: String,
    val price: Double,
    /** 买完之后弹出的吐槽文案 */
    val funnyLine: String
)

/** 商城清单 · 正经区：真会让人心动的数码家电 */
val SIM_SHOP_ITEMS_SERIOUS: List<SimShopItem> = listOf(
    SimShopItem("s_phone", "iPhone 22 Pro Max", "📱", 12999.0, "分期一时爽，还款火葬场。"),
    SimShopItem("s_keyboard", "客制化机械键盘", "⌨️", 899.0, "敲起来很爽，还钱的时候也很响。"),
    SimShopItem("s_gpu", "二手显卡（矿卡）", "🖥️", 2399.0, "卖家说「只跑了三个月」，你信了。"),
    SimShopItem("s_drone", "大疆无人机", "🚁", 4999.0, "拍的是风景，涨的是账单。"),
    SimShopItem("s_headphone", "降噪耳机", "🎧", 1999.0, "降噪了，但降不掉还款提醒。"),
    SimShopItem("s_chair", "人体工学椅", "💺", 3999.0, "腰舒服了，心在滴血。"),
    SimShopItem("s_camera", "微单相机", "📷", 6899.0, "买了就吃灰，这是自然规律。"),
    SimShopItem("s_fridge", "高端风冷冰箱", "🧊", 5999.0, "装满的不是食物，是负债。")
)

/** 商城清单 · 整活区：纯属虚构，买了就是图一乐 */
val SIM_SHOP_ITEMS_FUN: List<SimShopItem> = listOf(
    SimShopItem("s_wife", "虚拟老婆体验卡", "💕", 1999.0, "有效期 30 天，到期自动收回，概不退款。"),
    SimShopItem("s_merit", "赛博功德", "🏆", 66.0, "捐了这份功德，明天运气 +1（不保证）。"),
    SimShopItem("s_praise", "同事的早八赞", "☕", 520.0, "买断一位同事连续一个月对你的夸奖。"),
    SimShopItem("s_aircoin", "空气币（成分：空气）", "💰", 8888.0, "价值 0，但名字很贵，这就是币圈。"),
    SimShopItem("s_erase", "前任回忆清除服务", "🧠", 999.0, "清除 99%，剩下 1% 归你自己保管。"),
    SimShopItem("s_luck", "一夜暴富电子符", "🍀", 9.9, "恭喜，你并不是那个幸运儿。")
)

/** 商城全部商品 */
val SIM_SHOP_ITEMS: List<SimShopItem> = SIM_SHOP_ITEMS_SERIOUS + SIM_SHOP_ITEMS_FUN

// ---------------------------------------------------------------------------
// 领域模型
// ---------------------------------------------------------------------------

/**
 * 模拟器里的一笔借款（只属于模拟器，与真实账单无关）。
 *
 * @param startDay    借款当天的虚拟天序号（开局当天为 0）
 * @param dueDayOfMonth 每月还款日 1-28
 * @param paidPeriods 已还期数
 */
data class SimLoan(
    val id: String,
    val productId: String,
    val productName: String,
    val annualRate: Double,
    val principal: Double,
    val termMonths: Int,
    val monthlyPayment: Double,
    val startDay: Int,
    val dueDayOfMonth: Int,
    val paidPeriods: Int,
    /** 借款日期展示用 YYYY-MM-DD */
    val borrowedDate: String
)

/** 模拟器里的一次购物 */
data class SimPurchase(
    val itemId: String,
    val name: String,
    val emoji: String,
    val price: Double,
    val boughtDate: String
)

/** 单笔借款的实时状态（全部由日期推导，不落库） */
data class SimLoanStatus(
    val loan: SimLoan,
    /** 已到期期数（含已还） */
    val duePeriods: Int,
    /** 已到期但未还的期数 */
    val unpaidPeriods: Int,
    /** 最早一笔未还欠款的逾期天数，未逾期为 0 */
    val overdueDays: Int,
    /** 累计罚息与违约金 */
    val penalty: Double,
    /** 下一期应还金额（含该期已产生的罚息），已还完为 0 */
    val nextDueAmount: Double,
    /** 下一期还款日，已还完为 null */
    val nextDueDate: LocalDate?,
    /** 剩余未还期数（含未到期的） */
    val remainingPeriods: Int,
    /** 剩余应还本金总额（不含罚息） */
    val remainingAmount: Double,
    /** 还款进度 0..1 */
    val progress: Double
) {
    /** 是否处于逾期状态 */
    val isOverdue: Boolean get() = unpaidPeriods > 0
}

/** 模拟器整体状态（派生量） */
data class SimStatus(
    /** 当前虚拟天序号 */
    val dayIndex: Int,
    /** 当前虚拟日期 */
    val virtualDate: LocalDate,
    val loans: List<SimLoanStatus>,
    /** 手上余额 */
    val wallet: Double,
    /** 累计借到手 */
    val totalBorrowed: Double,
    /** 累计购物花掉 */
    val totalSpent: Double,
    /** 累计已还 */
    val totalRepaid: Double,
    /** 当前逾期未还（含罚息） */
    val overdueAmount: Double,
    /** 剩余应还总额（含未到期，含罚息） */
    val remainingAmount: Double,
    /** 累计罚息 */
    val totalPenalty: Double,
    /** 逾期期数合计 */
    val unpaidPeriods: Int,
    /** 征信分 0..100 */
    val creditScore: Int,
    /** 是否已触发破产结局 */
    val bankrupt: Boolean,
    /** 净资产 = 余额 − 剩余应还总额 */
    val netWorth: Double
) {
    /** 是否已借过钱 */
    val hasLoan: Boolean get() = loans.isNotEmpty()
}

// ---------------------------------------------------------------------------
// 利率换算：全部沿用项目统一的复利折算口径
// ---------------------------------------------------------------------------

/** 由年化复利折算月利率：(1 + 年化)^(1/12) − 1 */
fun simMonthlyRate(annualRate: Double): Double =
    if (annualRate <= 0.0) 0.0 else Math.pow(1.0 + annualRate, 1.0 / 12.0) - 1.0

/** 由年化复利折算日利率：(1 + 年化)^(1/365) − 1 */
fun simDailyRate(annualRate: Double): Double =
    if (annualRate <= 0.0) 0.0 else Math.pow(1.0 + annualRate, 1.0 / 365.0) - 1.0

/** 等额本息月供 */
fun simMonthlyPayment(principal: Double, annualRate: Double, termMonths: Int): Double {
    if (termMonths <= 0) return 0.0
    val rate = simMonthlyRate(annualRate)
    if (rate <= 0.0) return principal / termMonths
    val pow = Math.pow(1.0 + rate, termMonths.toDouble())
    return principal * rate * pow / (pow - 1.0)
}

/** 首期还款日：借款日之后第一个出现的还款日（还款日固定在 1-28，不会越界） */
internal fun firstDueDate(startDate: LocalDate, dueDayOfMonth: Int): LocalDate {
    val monthStart = startDate.withDayOfMonth(1)
    val sameMonth = monthStart.withDayOfMonth(dueDayOfMonth)
    return if (sameMonth.isAfter(startDate)) sameMonth else sameMonth.plusMonths(1)
}

/**
 * 第 period 期的到期日（虚拟天序号，开局当天为 0）。
 * 供催收时间线把「第几天该收到哪条消息」算成绝对天数。
 */
fun simDueDayIndex(loan: SimLoan, startDate: LocalDate, period: Int): Int {
    val borrowed = startDate.plusDays(loan.startDay.toLong())
    val firstDue = firstDueDate(borrowed, loan.dueDayOfMonth)
    return ChronoUnit.DAYS.between(startDate, firstDue.plusMonths((period - 1).toLong())).toInt()
}

// ---------------------------------------------------------------------------
// 派生计算
// ---------------------------------------------------------------------------

/** 开局日期 + 虚拟天序号 -> 具体日期 */
fun simDateAt(startDate: LocalDate, dayIndex: Int): LocalDate =
    startDate.plusDays(dayIndex.toLong())

/**
 * 推导单笔借款的实时状态。
 *
 * 逾期罚息按「每期各自起算」累计，因此一次性快进 90 天也能算出准确金额。
 */
fun simLoanStatus(loan: SimLoan, startDate: LocalDate, dayIndex: Int): SimLoanStatus {
    val borrowedDate = startDate.plusDays(loan.startDay.toLong())
    val firstDue = firstDueDate(borrowedDate, loan.dueDayOfMonth)
    val penaltyDaily = max(simDailyRate(loan.annualRate) * SIM_PENALTY_MULTIPLIER, SIM_MIN_PENALTY_DAILY)

    var duePeriods = 0
    var penalty = 0.0
    var earliestOverdueDay = 0

    for (period in 1..loan.termMonths) {
        val dueDate = firstDue.plusMonths((period - 1).toLong())
        val dueIndex = ChronoUnit.DAYS.between(startDate, dueDate).toInt()
        if (dueIndex > dayIndex) break
        duePeriods++
        if (period > loan.paidPeriods) {
            if (earliestOverdueDay == 0) earliestOverdueDay = dueIndex
            penalty += loan.monthlyPayment * penaltyDaily * (dayIndex - dueIndex)
        }
    }

    val unpaidPeriods = max(0, duePeriods - loan.paidPeriods)
    val remainingPeriods = max(0, loan.termMonths - loan.paidPeriods)
    val nextPeriod = loan.paidPeriods + 1
    val nextDueDate = if (nextPeriod > loan.termMonths) null else firstDue.plusMonths((nextPeriod - 1).toLong())
    val nextDueIndex = nextDueDate?.let { ChronoUnit.DAYS.between(startDate, it).toInt() }
    // 下一期若已逾期，还款时要连该期罚息一起还
    val nextPeriodPenalty = if (nextDueIndex != null && nextDueIndex <= dayIndex) {
        loan.monthlyPayment * penaltyDaily * (dayIndex - nextDueIndex)
    } else 0.0

    return SimLoanStatus(
        loan = loan,
        duePeriods = duePeriods,
        unpaidPeriods = unpaidPeriods,
        overdueDays = if (unpaidPeriods > 0) max(0, dayIndex - earliestOverdueDay) else 0,
        penalty = penalty,
        nextDueAmount = if (remainingPeriods <= 0) 0.0 else loan.monthlyPayment + nextPeriodPenalty,
        nextDueDate = nextDueDate,
        remainingPeriods = remainingPeriods,
        remainingAmount = remainingPeriods * loan.monthlyPayment,
        progress = if (loan.termMonths <= 0) 1.0 else loan.paidPeriods.toDouble() / loan.termMonths
    )
}

/**
 * 由存档数据推导模拟器的全部展示指标。
 *
 * @param startedToday 开局日期（真实世界），格式 YYYY-MM-DD
 * @param fastForwardDays 累计快进天数
 * @param today 当前真实日期
 */
fun simStatus(
    startedToday: String,
    fastForwardDays: Int,
    today: LocalDate,
    wallet: Double,
    loans: List<SimLoan>,
    purchases: List<SimPurchase>,
    realElapsedDays: Int
): SimStatus {
    val startDate = parseIsoDate(startedToday) ?: today
    val dayIndex = (realElapsedDays + fastForwardDays).coerceIn(0, SIM_MAX_DAY_INDEX)

    val loanStatuses = loans.map { simLoanStatus(it, startDate, dayIndex) }

    val totalBorrowed = loans.sumOf { it.principal }
    val totalSpent = purchases.sumOf { it.price }
    val totalRepaid = loans.sumOf { it.monthlyPayment * it.paidPeriods }
    val totalPenalty = loanStatuses.sumOf { it.penalty }
    val overdueAmount = loanStatuses.sumOf { it.unpaidPeriods * it.loan.monthlyPayment } + totalPenalty
    val remainingAmount = loanStatuses.sumOf { it.remainingAmount } + totalPenalty
    val unpaidPeriods = loanStatuses.sumOf { it.unpaidPeriods }

    val creditScore = (SIM_MAX_CREDIT - unpaidPeriods * SIM_CREDIT_PENALTY_PER_PERIOD).coerceIn(0, SIM_MAX_CREDIT)

    return SimStatus(
        dayIndex = dayIndex,
        virtualDate = simDateAt(startDate, dayIndex),
        loans = loanStatuses,
        wallet = wallet,
        totalBorrowed = totalBorrowed,
        totalSpent = totalSpent,
        totalRepaid = totalRepaid,
        overdueAmount = overdueAmount,
        remainingAmount = remainingAmount,
        totalPenalty = totalPenalty,
        unpaidPeriods = unpaidPeriods,
        creditScore = creditScore,
        bankrupt = unpaidPeriods >= SIM_BANKRUPT_OVERDUE_PERIODS,
        netWorth = wallet - remainingAmount
    )
}

/** 解析 YYYY-MM-DD，非法返回 null */
fun parseIsoDate(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()

/** 两个日期之间的天数差（b − a） */
fun daysBetween(a: LocalDate, b: LocalDate): Int = ChronoUnit.DAYS.between(a, b).toInt()

/** 日期展示：2026-10-18 */
fun formatIsoDate(date: LocalDate): String = date.toString()

/** 日期展示（中文）：2026 年 10 月 18 日 */
fun formatDateLabel(date: LocalDate): String =
    "${date.year} 年 ${date.monthValue} 月 ${date.dayOfMonth} 日"

// ---------------------------------------------------------------------------
// 催收短信：破产结局页的演出文案
// ---------------------------------------------------------------------------

/** 按逾期期数生成催收短信，越往后越离谱 */
fun simCollectionMessages(unpaidPeriods: Int, unpaidAmount: Double): List<String> {
    val amount = formatMoney(unpaidAmount)
    val all = listOf(
        "【闪电借】尊敬的用户，您的账单已逾期 1 天，应还 $amount。为避免影响征信，请尽快还款。",
        "【某团生活费】用户您好，逾期将影响您的出行、住宿与外卖优惠券使用权益。",
        "【某某银行】您的欠款已进入催收流程，请保持通讯畅通。",
        "【闪电借】已联系您的紧急联系人，请及时处理，避免造成不良影响。",
        "【催收部 · 小王】哥，我这边也有业绩指标，帮帮忙还一点吧。",
        "【某某银行】如仍未还款，我行将依法提起诉讼，届时费用由您承担。",
        "【催收部 · 小李】最后一次通知：明天安排上门（开玩笑的，但真的）。"
    )
    val count = (unpaidPeriods + 3).coerceIn(3, all.size)
    return all.take(count)
}
