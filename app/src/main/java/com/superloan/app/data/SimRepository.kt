package com.superloan.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.superloan.app.core.SIM_MAX_DAY_INDEX
import com.superloan.app.core.SimCallStatus
import com.superloan.app.core.SimLoan
import com.superloan.app.core.SimProduct
import com.superloan.app.core.SimPurchase
import com.superloan.app.core.SimShopItem
import com.superloan.app.core.daysBetween
import com.superloan.app.core.parseIsoDate
import com.superloan.app.core.simMonthlyPayment
import com.superloan.app.core.simPendingCalls
import com.superloan.app.core.simPendingMessages
import com.superloan.app.core.simStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** 模拟器存档在 DataStore 中的键（与账单、设置同文件、不同键） */
private val SIM_KEY = stringPreferencesKey("sim_store_json")

/**
 * 「作死模式」模拟器仓库。
 *
 * 所有数据只落在本地 DataStore，且与真实账单完全隔离。
 */
class SimRepository(appContext: Context) {

    private val store: DataStore<Preferences> = appContext.appDataStore

    /** 响应式存档流 */
    val data: Flow<SimStoreData> = store.data.map { prefs ->
        SimJsonCodec.decode(prefs[SIM_KEY])
    }

    /** 读取当前存档快照 */
    suspend fun current(): SimStoreData = data.first()

    /**
     * 保证已开局：首次进入模拟器时写入开局日期。
     * 开局日期是「真实时间轴」的锚点，虚拟日期 = 开局日期 + 真实流逝天数 + 快进天数。
     */
    suspend fun ensureStarted() {
        store.edit { prefs ->
            val current = SimJsonCodec.decode(prefs[SIM_KEY])
            if (current.started) return@edit
            prefs[SIM_KEY] = SimJsonCodec.encode(
                current.copy(startedToday = LocalDate.now().toString())
            )
        }
    }

    /**
     * 借一笔钱：余额增加本金，同时记下一笔模拟借款。
     *
     * 月供按等额本息计算；月利率与日利率都沿用全项目统一的复利折算口径。
     *
     * @param virtualDate 当前虚拟日期（真实日期 + 快进天数）
     * @param dayIndex    当前虚拟天序号，作为这笔借款的起始天
     */
    suspend fun borrow(
        product: SimProduct,
        amount: Double,
        virtualDate: LocalDate,
        dayIndex: Int
    ) {
        if (amount <= 0.0) return
        store.edit { prefs ->
            val current = SimJsonCodec.decode(prefs[SIM_KEY])
            val monthly = simMonthlyPayment(amount, product.annualRate, product.termMonths)
            val loan = SimLoan(
                id = newSimLoanId(),
                productId = product.id,
                productName = product.name,
                annualRate = product.annualRate,
                principal = amount,
                termMonths = product.termMonths,
                monthlyPayment = monthly,
                startDay = dayIndex,
                // 还款日跟随虚拟借款日，限制在 1-28 以免月末越界
                dueDayOfMonth = virtualDate.dayOfMonth.coerceIn(1, 28),
                paidPeriods = 0,
                borrowedDate = virtualDate.toString()
            )
            prefs[SIM_KEY] = SimJsonCodec.encode(
                current.copy(
                    wallet = current.wallet + amount,
                    loans = current.loans + loan
                )
            )
        }
    }

    /** 买一件商品：余额扣款并记录购买 */
    suspend fun buy(item: SimShopItem, virtualDate: LocalDate) {
        store.edit { prefs ->
            val current = SimJsonCodec.decode(prefs[SIM_KEY])
            if (current.wallet < item.price) return@edit
            prefs[SIM_KEY] = SimJsonCodec.encode(
                current.copy(
                    wallet = current.wallet - item.price,
                    purchases = current.purchases + SimPurchase(
                        itemId = item.id,
                        name = item.name,
                        emoji = item.emoji,
                        price = item.price,
                        boughtDate = virtualDate.toString()
                    )
                )
            )
        }
    }

    /**
     * 归还指定借款的下一期。
     * 金额由 ViewModel 按「月供 + 该期罚息」算好后传入；余额不足或已还完则不改动。
     */
    suspend fun repayPeriod(loanId: String, amount: Double) {
        store.edit { prefs ->
            val current = SimJsonCodec.decode(prefs[SIM_KEY])
            val index = current.loans.indexOfFirst { it.id == loanId }
            if (index < 0) return@edit
            val loan = current.loans[index]
            if (loan.paidPeriods >= loan.termMonths) return@edit
            if (current.wallet < amount) return@edit

            val loans = current.loans.toMutableList()
            loans[index] = loan.copy(paidPeriods = loan.paidPeriods + 1)
            prefs[SIM_KEY] = SimJsonCodec.encode(
                current.copy(wallet = current.wallet - amount, loans = loans)
            )
        }
    }

    /** 手动快进若干天 */
    suspend fun fastForward(days: Int) {
        if (days <= 0) return
        store.edit { prefs ->
            val current = SimJsonCodec.decode(prefs[SIM_KEY])
            prefs[SIM_KEY] = SimJsonCodec.encode(
                current.copy(fastForwardDays = current.fastForwardDays + days)
            )
        }
    }

    /** 回到现实：清掉手动快进的天数，让时间重新跟随真实日期 */
    suspend fun backToReality() {
        store.edit { prefs ->
            val current = SimJsonCodec.decode(prefs[SIM_KEY])
            prefs[SIM_KEY] = SimJsonCodec.encode(current.copy(fastForwardDays = 0))
        }
    }

    /**
     * 推进催收投递水位：把 (水位, 当前虚拟天] 之间产生的催收短信与来电信件
     * 一次性补投进存档。
     *
     * 这一步是幂等的 —— 水位线保证同一事件只会投递一次，
     * 且消息/来电的 id 是确定性的，写库前还会按 id 去重一次，双保险。
     */
    suspend fun deliverCollection(today: LocalDate) {
        store.edit { prefs ->
            val current = SimJsonCodec.decode(prefs[SIM_KEY])
            if (!current.started) return@edit
            val startDate = parseIsoDate(current.startedToday) ?: return@edit

            val realElapsed = daysBetween(startDate, today)
            val dayIndex = (realElapsed + current.fastForwardDays).coerceIn(0, SIM_MAX_DAY_INDEX)
            val from = current.watermarkDay
            // 时间可以「回到现实」往后退，此时不补投也不回退水位
            if (dayIndex <= from) return@edit

            val status = simStatus(
                startedToday = current.startedToday,
                fastForwardDays = current.fastForwardDays,
                today = today,
                wallet = current.wallet,
                loans = current.loans,
                purchases = current.purchases,
                realElapsedDays = realElapsed
            )

            val existingMessageIds = current.messages.mapTo(HashSet()) { it.id }
            val existingCallIds = current.calls.mapTo(HashSet()) { it.id }

            val newMessages = simPendingMessages(status.loans, startDate, dayIndex, from, dayIndex)
                .filterNot { it.id in existingMessageIds }
            val newCalls = simPendingCalls(status.loans, startDate, from, dayIndex)
                .filterNot { it.id in existingCallIds }

            prefs[SIM_KEY] = SimJsonCodec.encode(
                current.copy(
                    messages = current.messages + newMessages,
                    calls = current.calls + newCalls,
                    watermarkDay = dayIndex
                )
            )
        }
    }

    /** 把收件箱全部标为已读 */
    suspend fun markMessagesRead() {
        store.edit { prefs ->
            val current = SimJsonCodec.decode(prefs[SIM_KEY])
            if (current.readMessageCount >= current.messages.size) return@edit
            prefs[SIM_KEY] = SimJsonCodec.encode(
                current.copy(readMessageCount = current.messages.size)
            )
        }
    }

    /** 更新某通来电的处理状态（接听 / 拒接） */
    suspend fun setCallStatus(callId: String, status: SimCallStatus) {
        store.edit { prefs ->
            val current = SimJsonCodec.decode(prefs[SIM_KEY])
            val calls = current.calls.map { if (it.id == callId) it.copy(status = status) else it }
            prefs[SIM_KEY] = SimJsonCodec.encode(current.copy(calls = calls))
        }
    }

    /** 一键拒接所有待处理来电（快进过后一通接一通弹，总得给人留个活路） */
    suspend fun rejectAllPendingCalls() {
        store.edit { prefs ->
            val current = SimJsonCodec.decode(prefs[SIM_KEY])
            if (current.calls.none { it.status == SimCallStatus.PENDING }) return@edit
            val calls = current.calls.map {
                if (it.status == SimCallStatus.PENDING) it.copy(status = SimCallStatus.REJECTED) else it
            }
            prefs[SIM_KEY] = SimJsonCodec.encode(current.copy(calls = calls))
        }
    }

    /**
     * 破产清零、从头再来：清空借款、消费、余额与全部催收记录，并把开局日期重置为今天。
     * 只累计破产次数，真实账单数据不受任何影响。
     */
    suspend fun resetRound(today: LocalDate) {
        store.edit { prefs ->
            val current = SimJsonCodec.decode(prefs[SIM_KEY])
            prefs[SIM_KEY] = SimJsonCodec.encode(
                SimStoreData(
                    startedToday = today.toString(),
                    fastForwardDays = 0,
                    wallet = 0.0,
                    loans = emptyList(),
                    purchases = emptyList(),
                    bankruptcyCount = current.bankruptcyCount + 1
                )
            )
        }
    }
}
