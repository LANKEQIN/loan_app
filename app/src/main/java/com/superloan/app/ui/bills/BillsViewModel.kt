package com.superloan.app.ui.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.superloan.app.data.Bill
import com.superloan.app.data.BillRepository
import com.superloan.app.data.BillStoreData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 分期账单模块的状态持有者。
 */
class BillsViewModel(private val repository: BillRepository) : ViewModel() {

    private val _store = MutableStateFlow(BillStoreData())

    /** 账单与还款记录 */
    val store: StateFlow<BillStoreData> = _store.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            repository.data.collect { _store.value = it }
        }
    }

    /** 新增或更新账单 */
    fun save(bill: Bill) {
        viewModelScope.launch { repository.upsert(bill) }
    }

    /** 删除账单（连带删除还款记录） */
    fun remove(billId: String) {
        viewModelScope.launch { repository.delete(billId) }
    }

    /** 勾选 / 取消勾选某一期 */
    fun setPaid(billId: String, month: String, isPaid: Boolean) {
        viewModelScope.launch { repository.setPaid(billId, month, isPaid) }
    }

    companion object {
        fun factory(repository: BillRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { BillsViewModel(repository) }
        }
    }
}
