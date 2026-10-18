package com.superloan.app.ui.sim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.superloan.app.SuperLoanApp
import com.superloan.app.core.SimCallStatus
import com.superloan.app.core.SimStatus
import com.superloan.app.core.formatDateLabel
import com.superloan.app.core.formatMoney
import kotlinx.coroutines.delay

/** 体验服内部的四个页签 */
private enum class SimTab(val title: String, val icon: ImageVector) {
    MARKET("借钱", Icons.Filled.Add),
    DEBT("我的", Icons.Filled.Person),
    MESSAGES("短信", Icons.Filled.Email),
    SHOP("商城", Icons.Filled.ShoppingCart)
}

/**
 * 「作死模式」全屏容器。
 *
 * 这个页面完全覆盖主界面（连底部导航一起盖住），因此退出后 App 还是原来那个工具，
 * 不会因为多了一个玩法而改变主结构。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimRoot(onExit: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) {
        (context.applicationContext as SuperLoanApp).simRepository
    }
    val viewModel: SimViewModel = viewModel(factory = SimViewModel.factory(repository))

    val store by viewModel.store.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val pendingCalls by viewModel.pendingCalls.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()

    // 回到前台时刷新真实日期，虚拟时间才会继续「跟随现实」
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshToday()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        val text = message
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.consumeMessage()
        }
    }

    // 每次进入体验服都会重看一遍假风控（本身就是笑点的一部分）
    var auditDone by rememberSaveable { mutableStateOf(false) }
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }

    // ---------- 第一幕：假风控审核 ----------
    if (!auditDone && !status.bankrupt) {
        SimAuditPage(
            onForceLend = {
                auditDone = true
                tabIndex = 0
            },
            onExit = onExit
        )
        return
    }

    // ---------- 第二幕：破产结局，整屏接管 ----------
    if (status.bankrupt) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            SimBankruptPage(
                status = status,
                bankruptcyCount = store.bankruptcyCount,
                onReset = {
                    viewModel.resetRound()
                    tabIndex = 0
                },
                onExit = onExit,
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    // ---------- 第三幕：来电覆盖层（电话轰炸） ----------
    // 快进一次性欠下多通电话时，处理完一通立刻弹下一通；把 activeCallId 也当作
    // 依赖项，这样挂断之后下一通才会被自动接管
    var activeCallId by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(pendingCalls, activeCallId) {
        if (activeCallId.isBlank() && pendingCalls.isNotEmpty()) {
            activeCallId = pendingCalls.first().id
        }
    }

    val activeCall = store.calls.firstOrNull { it.id == activeCallId }
    if (activeCall != null) {
        // 只在「还在响」的时候放铃声与震动，一接听世界就安静了
        val ringing = activeCall.status == SimCallStatus.PENDING
        DisposableEffect(ringing, activeCall.id) {
            val ringtone = SimRingtone(context)
            if (ringing) ringtone.start()
            onDispose { ringtone.stop() }
        }

        SimCallOverlay(
            call = activeCall,
            pendingCount = pendingCalls.size,
            overdueDays = status.loans.maxOfOrNull { it.overdueDays } ?: 0,
            overdueAmount = status.overdueAmount,
            onAnswered = { viewModel.handleCall(activeCall.id, SimCallStatus.ANSWERED) },
            onRejected = {
                viewModel.handleCall(activeCall.id, SimCallStatus.REJECTED)
                activeCallId = ""
            },
            onRejectAll = {
                viewModel.rejectAllPendingCalls()
                activeCallId = ""
            },
            onHangUp = { activeCallId = "" }
        )
        return
    }

    // ---------- 第四幕：常规四页 ----------
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("超级借钱 · 体验服") },
                actions = {
                    TextButton(onClick = onExit) { Text("退出") }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                SimTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = tabIndex == index,
                        onClick = { tabIndex = index },
                        icon = {
                            BadgedBox(
                                badge = {
                                    // 「短信」页签挂未读红点：逾期之后它基本不会消停
                                    if (tab == SimTab.MESSAGES && unreadCount > 0) {
                                        Badge { Text(unreadCount.coerceAtMost(99).toString()) }
                                    }
                                }
                            ) {
                                Icon(tab.icon, contentDescription = tab.title)
                            }
                        },
                        label = { Text(tab.title) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SimStatusStrip(status = status)

            when (tabIndex) {
                0 -> SimMarketPage(
                    status = status,
                    onBorrow = { product, amount -> viewModel.borrow(product, amount) },
                    modifier = Modifier.weight(1f)
                )

                1 -> SimDebtPage(
                    status = status,
                    fastForwardDays = store.fastForwardDays,
                    purchases = store.purchases,
                    onFastForward = { viewModel.fastForward(it) },
                    onBackToReality = { viewModel.backToReality() },
                    onRepay = { viewModel.repayNext(it) },
                    modifier = Modifier.weight(1f)
                )

                2 -> SimMessagePage(
                    messages = store.messages,
                    calls = store.calls,
                    unreadCount = store.unreadCount,
                    onMarkRead = { viewModel.markMessagesRead() },
                    modifier = Modifier.weight(1f)
                )

                else -> SimShopPage(
                    status = status,
                    purchasesCount = store.purchases.size,
                    onBuy = { viewModel.buy(it) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** 顶部常驻状态条：虚拟日期 + 征信分 + 余额 */
@Composable
private fun SimStatusStrip(status: SimStatus) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDateLabel(status.virtualDate),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "第 ${status.dayIndex} 天 · 征信 ${status.creditScore} 分",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "余额",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatMoney(status.wallet),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 假风控审核页：进度条走完之后「审核不通过」，然后给一个「强制放款」按钮。
 *
 * 这一步存在的意义就是先礼后兵：先假装自己是正经金融 App，再当场翻脸。
 */
@Composable
private fun SimAuditPage(
    onForceLend: () -> Unit,
    onExit: () -> Unit
) {
    var checking by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val steps = 24
        repeat(steps) { index ->
            delay(50)
            progress = (index + 1).toFloat() / steps
        }
        checking = false
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (checking) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "风控审核中…",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "正在核验您的身份、收入与消费欲望",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "审核不通过",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "风控原因：系统检测到您近期「想买东西」的欲望异常旺盛，风险等级：极高。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "顺便说一句：本应用并不提供任何借贷服务，也不联网。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onForceLend,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("强制放款（反正也是假的）")
                    }
                    OutlinedButton(
                        onClick = onExit,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("算了，我退出")
                    }
                }
            }
        }
    }
}
