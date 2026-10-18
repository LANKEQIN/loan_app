package com.superloan.app.core

/**
 * IRR（内部收益率）求解器。
 *
 * 现金流视角：第 0 期收到本金，第 1..n 期每期支付 payment。
 * 求解 NPV = 0 的月利率：
 *
 *     NPV(r) = -principal + Σ(t=1..n) payment / (1+r)^t
 *
 * 二分法参数（与需求文档一致）：
 * - 初始区间 low = 0，high = 1（即 100% 月利率）
 * - 若 NPV(high) > 0 则 high 不断翻倍（最多 50 次保护）直到两端变号
 * - 若两端同号（无解）返回 0
 * - 最多迭代 64 次；|NPV(mid)| < 1e-10 或区间宽 < 1e-10 时提前退出
 */
object IrrCalculator {

    /** 收敛阈值 */
    private const val EPSILON = 1e-10

    /** 最大迭代次数 */
    private const val MAX_ITERATIONS = 64

    /** high 翻倍的次数上限，防止死循环 */
    private const val MAX_DOUBLING = 50

    /** 视为免息的容差（覆盖浮点除法误差） */
    private const val FREE_TOLERANCE = 1e-9

    /** 总还款额不超过本金即视为免息 */
    fun isInterestFree(principal: Double, totalRepayment: Double): Boolean =
        totalRepayment <= principal + FREE_TOLERANCE

    /** 同法：每期等额还款时是否免息 */
    fun isInterestFree(principal: Double, paymentPerMonth: Double, months: Int): Boolean =
        isInterestFree(principal, paymentPerMonth * months)

    /**
     * 求解月利率（小数形式）。
     *
     * @return 月利率，例如 0.010352 表示 1.0352%/月；无解或免息时返回 0
     */
    fun solveMonthlyRate(principal: Double, paymentPerMonth: Double, months: Int): Double {
        if (months <= 0 || principal <= 0.0 || paymentPerMonth <= 0.0) return 0.0
        // 总还款不超过本金：免息，直接返回 0
        if (isInterestFree(principal, paymentPerMonth, months)) return 0.0

        var low = 0.0
        var high = 1.0
        var npvLow = npv(principal, paymentPerMonth, months, low)
        var npvHigh = npv(principal, paymentPerMonth, months, high)

        // 两端同号时向右扩大区间，直到变号
        var doubled = 0
        while (npvLow * npvHigh > 0.0 && doubled < MAX_DOUBLING) {
            high *= 2.0
            npvHigh = npv(principal, paymentPerMonth, months, high)
            doubled++
        }
        // 仍然同号说明区间内无解
        if (npvLow * npvHigh > 0.0) return 0.0

        var mid = low
        repeat(MAX_ITERATIONS) {
            mid = (low + high) / 2.0
            val npvMid = npv(principal, paymentPerMonth, months, mid)
            if (Math.abs(npvMid) < EPSILON || (high - low) < EPSILON) return mid
            if (npvLow * npvMid < 0.0) {
                high = mid
                npvHigh = npvMid
            } else {
                low = mid
                npvLow = npvMid
            }
        }
        return mid
    }

    /** 净现值 */
    private fun npv(principal: Double, payment: Double, months: Int, rate: Double): Double {
        var sum = -principal
        for (t in 1..months) {
            sum += payment / Math.pow(1.0 + rate, t.toDouble())
        }
        return sum
    }

    /** 月利率折算年化：年化 = (1 + 月IRR)^12 − 1（复利折算，不是简单 ×12） */
    fun annualFromMonthly(monthlyRate: Double): Double {
        if (monthlyRate <= 0.0) return 0.0
        return Math.pow(1.0 + monthlyRate, 12.0) - 1.0
    }
}

/**
 * IRR 计算结果。
 *
 * @param monthlyRate    月利率（小数）
 * @param annualRate     年化利率（小数，复利折算）
 * @param totalRepayment 总还款额
 * @param totalInterest  总息费 = 总还款额 − 本金
 * @param interestFree   免息标记（总还款额 ≤ 本金），此时不显示评级
 */
data class IrrResult(
    val monthlyRate: Double,
    val annualRate: Double,
    val totalRepayment: Double,
    val totalInterest: Double,
    val interestFree: Boolean
) {
    /** 免息时返回 null，表示不显示评级 */
    val level: RateLevel? get() = if (interestFree) null else rateLevelOf(annualRate)
}

/**
 * 按「本金 / 期数 / 每期还款金额」计算 IRR。
 *
 * 总还款额 = 每期还款 × 期数。
 */
fun calculateIrr(principal: Double, paymentPerMonth: Double, months: Int): IrrResult {
    val totalRepayment = paymentPerMonth * months
    val interestFree = IrrCalculator.isInterestFree(principal, totalRepayment)
    val monthlyRate = IrrCalculator.solveMonthlyRate(principal, paymentPerMonth, months)
    val annualRate = IrrCalculator.annualFromMonthly(monthlyRate)
    return IrrResult(
        monthlyRate = monthlyRate,
        annualRate = annualRate,
        totalRepayment = totalRepayment,
        totalInterest = totalRepayment - principal,
        interestFree = interestFree
    )
}
