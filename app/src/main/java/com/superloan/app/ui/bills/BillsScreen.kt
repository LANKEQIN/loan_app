package com.superloan.app.ui.bills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.superloan.app.SuperLoanApp
import com.superloan.app.core.currentMonth
import com.superloan.app.data.Bill
import com.superloan.app.ui.LocalAppSettings
import com.superloan.app.ui.sim.SimAdBannerCard

/**
 * 模块一：分期账单。
 *
 * 列表底部藏了一张伪装成网贷广告的卡片，点它会开启「作死模式」模拟器。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreen(
    onOpenSim: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember(context) {
        (context.applicationContext as SuperLoanApp).billRepository
    }
    val viewModel: BillsViewModel = viewModel(factory = BillsViewModel.factory(repository))

    val state by viewModel.store.collectAsStateWithLifecycle()
    val settings = LocalAppSettings.current

    var query by rememberSaveable { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }
    var editingBill by remember { mutableStateOf<Bill?>(null) }
    var pendingDelete by remember { mutableStateOf<Bill?>(null) }

    // 按「平台 + 商品名」不区分大小写过滤
    val keyword = query.trim()
    val filtered = remember(state.bills, keyword) {
        if (keyword.isEmpty()) {
            state.bills
        } else {
            state.bills.filter {
                it.platform.contains(keyword, ignoreCase = true) ||
                    it.item.contains(keyword, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        // 底部导航由外层容器负责，这里不再重复消费底部安全区
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(title = { Text("分期账单") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingBill = null
                showEditor = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "新增账单")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("搜索平台 / 商品名") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "清空搜索")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Text(
                text = "找到 ${filtered.size} 笔 / 共 ${state.bills.size} 笔",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (filtered.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (state.bills.isEmpty()) {
                            "还没有账单，点右下角「+」新增一笔"
                        } else {
                            "没有匹配「$keyword」的账单"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                    SimAdBannerCard(
                        onClick = onOpenSim,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = filtered, key = { it.id }) { bill ->
                        BillCard(
                            bill = bill,
                            paidMonths = state.paidMonthsOf(bill.id),
                            defaultExpanded = settings.billListExpanded,
                            onTogglePaid = { month, isPaid ->
                                viewModel.setPaid(bill.id, month, isPaid)
                            },
                            onEdit = {
                                editingBill = bill
                                showEditor = true
                            },
                            onDelete = { pendingDelete = bill }
                        )
                    }
                    // 列表底部的「广告位」：点进去就是作死模式的入口
                    item {
                        SimAdBannerCard(
                            onClick = onOpenSim,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }

    // 删除二次确认
    val deleteTarget = pendingDelete
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除账单") },
            text = {
                Text(
                    "确定删除「${deleteTarget.platform} · ${deleteTarget.item}」吗？\n" +
                        "该账单的还款打卡记录会一并删除。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.remove(deleteTarget.id)
                    pendingDelete = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    // 新增 / 编辑表单
    if (showEditor) {
        BillEditorDialog(
            initial = editingBill,
            defaultStartMonth = if (settings.newBillStartMonthCurrent) currentMonth() else null,
            onDismiss = { showEditor = false },
            onSave = { bill ->
                viewModel.save(bill)
                showEditor = false
            }
        )
    }
}
