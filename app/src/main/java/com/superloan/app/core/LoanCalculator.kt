package com.superloan.app.core

/** 还款方式 */
enum class RepaymentType(val title: String) {
    /** 等额本息：每月还款额固定 */
    ANNUITY("等额本息"),

    /** 等额本金：每月归还固定本金，利息逐月递减 */
    PRINCIPAL("等额本金"),

    /** 等本等费：手续费按原始本金收取（花呗 / 信用卡账单分期的算法） */
    FEE("等本等费")
}

/** 利率类型 */
enum class RateType(val title: String) {
    ANNUAL("年利率"),
    MONTHLY("月利率")
}

/**
 * 把用户输入的利率换算为小数月利率。
 * - 年利率：月利率 = 输入值 / 100 / 12
 * - 月利率（月费率）：月利率 = 输入值 / 100
 */
fun toMonthlyRate(inputPercent: Double, rateType: RateType): Double = when (rateType) {
    RateType.ANNUAL -> inputPercent / 100.0 / 12.0
    RateType.MONTHLY -> inputPercent / 100.0
}

/** 还款计划中的一行 */
data class RepaymentRow(
    /** 期数，从 1 开始 */
    val period: Int,
    /** 当期月供 */
    val payment: Double,
    /** 当期归还本金 */
    val principalPart: Double,
    /** 当期利息 / 手续费 */
    val interest: Double,
    /** 当期还款后的剩余本金 */
    val remaining: Double
)

/**
 * 月供计算结果。
 *
 * @param monthlyPayment   等额本息 / 等本等费的每月固定还款额
 * @param firstPayment     等额本金的首月还款额
 * @param lastPayment      等额本金的末月还款额
 * @param monthlyDecrease  等额本金每月递减额（= 每月固定本金 × 月利率）
 * @param nominalAnnualRate 名义年化（小数）= 月利率 × 12
 * @param realAnnualRate   真实年化（小数），仅等本等费需要展示
 */
data class LoanResult(
    val type: RepaymentType,
    val monthlyRate: Double,
    val rows: List<RepaymentRow>,
    val totalPayment: Double,
    val totalInterest: Double,
    val monthlyPayment: Double?,
    val firstPayment: Double?,
    val lastPayment: Double?,
    val monthlyDecrease: Double?,
    val nominalAnnualRate: Double,
    val realAnnualRate: Double?,
    val interestFree: Boolean
)

/** 金额视为持平的阈值 */
private const val FLAT_THRESHOLD = 0.005

/** 浮点归零容差 */
private const val ZERO_TOLERANCE = 1e-9

/**
 * 计算月供与完整还款计划表。
 *
 * @param principal   本金
 * @param months      期数（月）
 * @param monthlyRate 小数形式的月利率
 * @param type        还款方式
 */
fun calculateLoan(
    principal: Double,
    months: Int,
    monthlyRate: Double,
    type: RepaymentType
): LoanResult {
    require(principal > 0.0) { "本金必须大于 0" }
    require(months >= 1) { "期数至少为 1" }
    require(monthlyRate >= 0.0) { "利率不能为负" }

    val rows = when (type) {
        RepaymentType.ANNUITY -> annuityRows(principal, months, monthlyRate)
        RepaymentType.PRINCIPAL -> principalRows(principal, months, monthlyRate)
        RepaymentType.FEE -> feeRows(principal, months, monthlyRate)
    }

    val totalPayment = rows.sumOf { it.payment }
    val totalInterest = rows.sumOf { it.interest }
    val interestFree = monthlyRate == 0.0 || totalInterest <= ZERO_TOLERANCE

    val monthlyPayment: Double? = when (type) {
        RepaymentType.ANNUITY, RepaymentType.FEE -> rows.first().payment
        RepaymentType.PRINCIPAL -> null
    }
    val firstPayment: Double? = if (type == RepaymentType.PRINCIPAL) rows.first().payment else null
    val lastPayment: Double? = if (type == RepaymentType.PRINCIPAL) rows.last().payment else null
    val monthlyDecrease: Double? =
        if (type == RepaymentType.PRINCIPAL) principal / months * monthlyRate else null

    // 等本等费的真实年化需要用 IRR 从每期月供反算
    val realAnnualRate: Double? = if (type == RepaymentType.FEE) {
        val monthlyIrr = IrrCalculator.solveMonthlyRate(principal, rows.first().payment, months)
        IrrCalculator.annualFromMonthly(monthlyIrr)
    } else null

    return LoanResult(
        type = type,
        monthlyRate = monthlyRate,
        rows = rows,
        totalPayment = totalPayment,
        totalInterest = totalInterest,
        monthlyPayment = monthlyPayment,
        firstPayment = firstPayment,
        lastPayment = lastPayment,
        monthlyDecrease = monthlyDecrease,
        nominalAnnualRate = monthlyRate * 12.0,
        realAnnualRate = realAnnualRate,
        interestFree = interestFree
    )
}

/** 等额本息：每月还款额固定 */
private fun annuityRows(principal: Double, months: Int, rate: Double): List<RepaymentRow> {
    val monthly = if (rate == 0.0) {
        principal / months
    } else {
        val pow = Math.pow(1.0 + rate, months.toDouble())
        principal * rate * pow / (pow - 1.0)
    }
    val rows = ArrayList<RepaymentRow>(months)
    var remaining = principal
    for (period in 1..months) {
        val interest = remaining * rate
        var principalPart = monthly - interest
        var payment = monthly
        if (period == months) {
            // 末期本金取剩余本金，消除浮点除不尽的误差
            principalPart = remaining
            payment = principalPart + interest
        }
        remaining -= principalPart
        if (remaining < ZERO_TOLERANCE) remaining = 0.0
        rows += RepaymentRow(period, payment, principalPart, interest, remaining)
    }
    return rows
}

/** 等额本金：每月归还固定本金，利息按剩余本金逐月递减 */
private fun principalRows(principal: Double, months: Int, rate: Double): List<RepaymentRow> {
    val monthlyPrincipal = principal / months
    val rows = ArrayList<RepaymentRow>(months)
    var remaining = principal
    for (period in 1..months) {
        val interest = remaining * rate
        val principalPart = if (period == months) remaining else monthlyPrincipal
        val payment = principalPart + interest
        remaining -= principalPart
        if (remaining < ZERO_TOLERANCE) remaining = 0.0
        rows += RepaymentRow(period, payment, principalPart, interest, remaining)
    }
    return rows
}

/** 等本等费：每月本金固定，手续费按原始本金收取且恒定不变 */
private fun feeRows(principal: Double, months: Int, rate: Double): List<RepaymentRow> {
    val monthlyPrincipal = principal / months
    val feePerMonth = principal * rate
    val rows = ArrayList<RepaymentRow>(months)
    var remaining = principal
    for (period in 1..months) {
        val principalPart = if (period == months) remaining else monthlyPrincipal
        remaining -= principalPart
        if (remaining < ZERO_TOLERANCE) remaining = 0.0
        rows += RepaymentRow(period, principalPart + feePerMonth, principalPart, feePerMonth, remaining)
    }
    return rows
}

/** 用于「方式对比」的对照还款方式 */
fun comparisonTarget(type: RepaymentType): RepaymentType = when (type) {
    RepaymentType.ANNUITY -> RepaymentType.PRINCIPAL
    RepaymentType.PRINCIPAL -> RepaymentType.ANNUITY
    RepaymentType.FEE -> RepaymentType.ANNUITY
}

/**
 * 本方式相对对照方式的利息差额。
 * 返回正值表示本方式多付利息，负值表示少付利息。
 * 绝对值小于 0.005 时视为基本持平。
 */
fun interestDifference(myInterest: Double, otherInterest: Double): Double =
    myInterest - otherInterest

/** 是否基本持平 */
fun isBasicallyFlat(difference: Double): Boolean = Math.abs(difference) < FLAT_THRESHOLD

/** 评级：免息返回 null，等本等费按真实年化，其余按名义年化 */
fun loanLevel(result: LoanResult): RateLevel? {
    if (result.interestFree) return null
    val annual = if (result.type == RepaymentType.FEE) {
        result.realAnnualRate ?: result.nominalAnnualRate
    } else {
        result.nominalAnnualRate
    }
    return rateLevelOf(annual)
}
