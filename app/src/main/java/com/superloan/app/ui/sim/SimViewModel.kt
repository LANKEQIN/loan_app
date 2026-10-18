package com.superloan.app.ui.sim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.superloan.app.core.SIM_MAX_FF_STEP
import com.superloan.app.core.SimCall
import com.superloan.app.core.SimCallStatus
import com.superloan.app.core.SimProduct
import com.superloan.app.core.SimShopItem
import com.superloan.app.core.SimStatus
import com.superloan.app.core.daysBetween
import com.superloan.app.core.formatAmount
import com.superloan.app.core.formatPercent
import com.superloan.app.core.parseIsoDate
import com.superloan.app.core.simStatus
import com.superloan.app.data.SimRepository
import com.superloan.app.data.SimStoreData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 「作死模式」模拟器的状态持有者。
 *
 * 职责划分：
 *  - 存档（余额 / 借款 / 消费 / 快进天数 / 催收记录）由仓库读写；
 *  - 到期期数、罚息、征信分、破产判定等全部由 [simStatus] 纯函数推导，不落库；
 *  - 催收短信与来电是「事件」，靠仓库里的投递水位增量补投，一旦投递就永久保留。
 */
class SimViewModel(private val repository: SimRepository) : ViewModel() {

    /** 存档快照 */
    private val _store = MutableStateFlow(SimStoreData())
    val store: StateFlow<SimStoreData> = _store.asStateFlow()

    /** 当前真实日期，回到前台时刷新，用于「跟随现实时间」 */
    private val _today = MutableStateFlow(LocalDate.now())

    /** 一次性提示（Snackbar 文案） */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** 派生出来的模拟器实时状态 */
    val status: StateFlow<SimStatus> = combine(repository.data, _today) { data, today ->
        deriveStatus(data, today)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, deriveStatus(SimStoreData(), LocalDate.now()))

    /** 待处理的来电队列：会一通接一通地弹来电界面 */
    val pendingCalls: StateFlow<List<SimCall>> = repository.data
        .map { it.pendingCalls }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 未读催收短信数 */
    val unreadCount: StateFlow<Int> = repository.data
        .map { it.unreadCount }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    init {
        viewModelScope.launch {
            repository.ensureStarted()
            // 开局先把截止到今天的催收事件补投一遍（真实时间可能已经过了几天）
            repository.deliverCollection(LocalDate.now())
            repository.data.collect { _store.value = it }
        }
    }

    /** 回到前台时刷新真实日期，让虚拟时间跟着现实走，并把这段时间的催收补投出来 */
    fun refreshToday() {
        _today.value = LocalDate.now()
        deliver()
    }

    /** 借一笔钱 */
    fun borrow(product: SimProduct, amount: Double) {
        val min = product.minAmount
        val max = product.maxAmount
        if (amount < min || amount > max) {
            _message.value = "${product.name} 可借区间：¥${formatAmount(min)} ~ ¥${formatAmount(max)}"
            return
        }
        val snapshot = status.value
        viewModelScope.launch {
            repository.borrow(product, amount, snapshot.virtualDate, snapshot.dayIndex)
            repository.deliverCollection(LocalDate.now())
            _message.value = "放款成功：¥${formatAmount(amount)} 已到账（年化 ${formatPercent(product.annualRate)}）"
        }
    }

    /** 买一件商品 */
    fun buy(item: SimShopItem) {
        val wallet = _store.value.wallet
        if (wallet < item.price) {
            val gap = item.price - wallet
            _message.value = "余额不足，还差 ¥${formatAmount(gap)} —— 要不要先去借点？"
            return
        }
        viewModelScope.launch {
            repository.buy(item, status.value.virtualDate)
            _message.value = item.funnyLine
        }
    }

    /** 归还某笔借款的下一期（金额含该期已产生的罚息） */
    fun repayNext(loanId: String) {
        val loanStatus = status.value.loans.firstOrNull { it.loan.id == loanId }
        if (loanStatus == null) return
        val amount = loanStatus.nextDueAmount
        if (amount <= 0.0) {
            _message.value = "这笔已经还完了，别再给自己加戏了"
            return
        }
        if (_store.value.wallet < amount) {
            val gap = amount - _store.value.wallet
            _message.value = "余额还差 ¥${formatAmount(gap)}，本期需还 ¥${formatAmount(amount)} —— 要不，再借一笔？"
            return
        }
        viewModelScope.launch {
            repository.repayPeriod(loanId, amount)
            repository.deliverCollection(LocalDate.now())
            _message.value = "已归还 1 期，还款 ¥${formatAmount(amount)}"
        }
    }

    /** 手动快进：单次最多 90 天，避免一跳到宇宙尽头 */
    fun fastForward(days: Int) {
        val step = days.coerceIn(1, SIM_MAX_FF_STEP)
        viewModelScope.launch {
            repository.fastForward(step)
            // 快进之后立刻补投：一次性欠下的来电会排队弹出来，这就是「轰炸」
            repository.deliverCollection(LocalDate.now())
            _message.value = "时间快进了 $step 天"
        }
    }

    /** 回到现实，重新跟随真实日期 */
    fun backToReality() {
        viewModelScope.launch {
            repository.backToReality()
            _message.value = "已回到现实，时间重新跟随真实日期"
        }
    }

    /** 破产清零、从头再来 */
    fun resetRound() {
        viewModelScope.launch {
            repository.resetRound(LocalDate.now())
            _today.value = LocalDate.now()
            _message.value = "已清零重来，祝你这次能活着走出来"
        }
    }

    /** 收件箱全部标为已读 */
    fun markMessagesRead() {
        viewModelScope.launch { repository.markMessagesRead() }
    }

    /** 处理一通来电：接听或拒接 */
    fun handleCall(callId: String, status: SimCallStatus) {
        viewModelScope.launch { repository.setCallStatus(callId, status) }
    }

    /** 一键拒接所有待处理来电 */
    fun rejectAllPendingCalls() {
        viewModelScope.launch {
            repository.rejectAllPendingCalls()
            _message.value = "已一键拒接全部来电（手机安静下来的感觉真好）"
        }
    }

    /** 消费掉一次性提示 */
    fun consumeMessage() {
        _message.value = null
    }

    /** 把截止到今天的催收事件补投一遍 */
    private fun deliver() {
        viewModelScope.launch { repository.deliverCollection(LocalDate.now()) }
    }

    /** 由存档 + 真实日期推导模拟器状态 */
    private fun deriveStatus(data: SimStoreData, today: LocalDate): SimStatus {
        val anchor = parseIsoDate(data.startedToday) ?: today
        val realElapsed = if (data.started) daysBetween(anchor, today) else 0
        return simStatus(
            startedToday = data.startedToday,
            fastForwardDays = data.fastForwardDays,
            today = today,
            wallet = data.wallet,
            loans = data.loans,
            purchases = data.purchases,
            realElapsedDays = realElapsed
        )
    }

    companion object {
        fun factory(repository: SimRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { SimViewModel(repository) }
        }
    }
}
