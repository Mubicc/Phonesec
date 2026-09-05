package com.phonesec.broke.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phonesec.broke.game.Asset
import com.phonesec.broke.game.DayReport
import com.phonesec.broke.game.Drain
import com.phonesec.broke.game.GameState
import com.phonesec.broke.game.GameStatus
import com.phonesec.broke.game.LogEntry
import com.phonesec.broke.game.asEuro
import com.phonesec.broke.game.asPercent
import com.phonesec.broke.game.format1

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state = viewModel.state

    if (state.status == GameStatus.LOST) {
        GameOverScreen(state = state, onRestart = viewModel::restart)
        return
    }

    RunningGame(viewModel = viewModel, state = state)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RunningGame(viewModel: GameViewModel, state: GameState) {
    val snackbarHost = remember { SnackbarHostState() }
    var chosenTab by rememberSaveable { mutableIntStateOf(0) }

    // Während des Tutorials bestimmt der Schritt, was zu sehen ist.
    val step = viewModel.tutorialStep?.let { Tutorial.steps[it] }
    val tab = step?.tab ?: chosenTab

    viewModel.message?.let { message ->
        LaunchedEffect(message) {
            snackbarHost.showSnackbar(message)
            viewModel.dismissMessage()
        }
    }

    viewModel.lastReport?.let { report ->
        DayReportDialog(report = report, onDismiss = viewModel::dismissReport)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tag ${state.day}", fontWeight = FontWeight.Bold)
                        Text(
                            "Rekord: Tag ${state.bestDay}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    ActionPointDots(state.actionPoints)
                    TextButton(
                        onClick = viewModel::restartTutorial,
                        modifier = Modifier.testTag("help"),
                    ) {
                        Text("?", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            EndDayBar(state = state, onEndDay = viewModel::endDay)
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            BalanceCard(state)

            TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                Tab(
                    selected = tab == 0,
                    onClick = { chosenTab = 0 },
                    text = { Text("Kosten") },
                    modifier = Modifier.testTag("tab-kosten"),
                )
                Tab(
                    selected = tab == 1,
                    onClick = { chosenTab = 1 },
                    text = { Text("Anlagen") },
                    modifier = Modifier.testTag("tab-anlagen"),
                )
                Tab(
                    selected = tab == 2,
                    onClick = { chosenTab = 2 },
                    text = { Text("Ausbau") },
                    modifier = Modifier.testTag("tab-ausbau"),
                )
                Tab(
                    selected = tab == 3,
                    onClick = { chosenTab = 3 },
                    text = { Text("Verlauf") },
                    modifier = Modifier.testTag("tab-verlauf"),
                )
            }

            when (tab) {
                0 -> DrainList(
                    state = state,
                    onCancel = viewModel::cancelDrain,
                    onNegotiate = viewModel::negotiate,
                    onSideGig = viewModel::sideGig,
                )
                1 -> AssetList(
                    state = state,
                    onBuy = viewModel::buyAsset,
                    onSell = viewModel::sellAsset,
                )
                2 -> UpgradeList(state = state, onBuy = viewModel::buyUpgrade)
                else -> LogList(state.log)
            }
        }
    }

    if (step != null) {
        TutorialOverlay(
            step = step,
            index = viewModel.tutorialStep ?: 0,
            total = Tutorial.steps.size,
            onNext = viewModel::nextTutorialStep,
            onSkip = viewModel::skipTutorial,
        )
    }
}

@Composable
private fun ActionPointDots(points: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 16.dp),
    ) {
        Text(
            "Aktionen",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        repeat(points.coerceAtMost(6)) {
            Box(
                Modifier
                    .padding(horizontal = 2.dp)
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
        if (points == 0) {
            Text("—", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BalanceCard(state: GameState) {
    val progress = if (state.goal <= 0) 1f else {
        (state.projectedWorth.toFloat() / state.goal.toFloat()).coerceIn(0f, 1f)
    }
    val barColor by animateColorAsState(
        if (state.onTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        label = "goalBar",
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Vermögen",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                state.netWorth.asEuro(),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                "davon ${state.cash.asEuro()} liquide auf dem Konto",
                style = MaterialTheme.typography.labelSmall,
                color = if (state.projectedCash < 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tagesziel (Vermögen)", style = MaterialTheme.typography.labelMedium)
                Text(
                    state.goal.asEuro(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (state.onTrack) {
                    "Heute Abend ${state.projectedWorth.asEuro()} — Ziel geschafft"
                } else {
                    "Heute Abend ${state.projectedWorth.asEuro()} — es fehlen ${state.shortfall.asEuro()}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = barColor,
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(12.dp))

            MoneyRow("Zinsen (${state.baseInterest.asPercent()})", state.passiveIncome, positive = true)
            MoneyRow("Anlagen", state.assetIncome, positive = true)
            if (state.bonusIncome > 0) {
                MoneyRow("Nebenjob (heute)", state.bonusIncome, positive = true)
            }
            MoneyRow("Steuern", -state.taxCost, positive = false)
            MoneyRow("Fixkosten & Abos", -state.fixedCost, positive = false)

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Netto pro Tag", fontWeight = FontWeight.Bold)
                Text(
                    state.netIncome.asEuro(),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (state.netIncome >= 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
        }
    }
}

@Composable
private fun MoneyRow(label: String, amount: Long, positive: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            amount.asEuro(),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun DrainList(
    state: GameState,
    onCancel: (String) -> Unit,
    onNegotiate: (String) -> Unit,
    onSideGig: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.testTag("drain-list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            OutlinedButton(
                onClick = onSideGig,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.actionPoints > 0,
            ) {
                Text("Nebenjob annehmen (1 Aktion)")
            }
        }
        items(state.drains, key = { it.id }) { drain ->
            DrainCard(
                drain = drain,
                day = state.day,
                gross = state.grossIncome,
                actionsLeft = state.actionPoints,
                onCancel = { onCancel(drain.id) },
                onNegotiate = { onNegotiate(drain.id) },
            )
        }
    }
}

@Composable
private fun DrainCard(
    drain: Drain,
    day: Int,
    gross: Long,
    actionsLeft: Int,
    onCancel: () -> Unit,
    onNegotiate: () -> Unit,
) {
    val locked = drain.isLocked(day)

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(drain.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (drain.isTax) {
                            "${drain.type.label} · ${drain.incomeRate.asPercent()} der Einnahmen"
                        } else {
                            drain.type.label
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "-${drain.costFor(gross).asEuro()}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (locked) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Mindestlaufzeit bis Tag ${drain.lockedUntilDay}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onNegotiate,
                    enabled = actionsLeft > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Verhandeln ${(drain.negotiationChance * 100).format1()} %", fontSize = 12.sp)
                }
                Button(
                    onClick = onCancel,
                    enabled = drain.cancellable && !locked && actionsLeft > 0,
                    modifier = Modifier.weight(1f).testTag("cancel-${drain.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        if (drain.cancelFee > 0) "Kündigen ${drain.cancelFee.asEuro()}" else "Kündigen",
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun AssetList(state: GameState, onBuy: (String) -> Unit, onSell: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.testTag("asset-list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "Angelegt: ${state.investedValue.asEuro()} · liquide: ${state.cash.asEuro()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(state.assets, key = { it.id }) { asset ->
            AssetCard(
                asset = asset,
                cash = state.cash,
                yieldMultiplier = state.yieldMultiplier,
                onBuy = { onBuy(asset.id) },
                onSell = { onSell(asset.id) },
            )
        }
    }
}

@Composable
private fun AssetCard(
    asset: Asset,
    cash: Long,
    yieldMultiplier: Double,
    onBuy: () -> Unit,
    onSell: () -> Unit,
) {
    val affordable = cash >= asset.nextPrice

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(asset.name, fontWeight = FontWeight.SemiBold)
                if (asset.owned > 0) {
                    Text(
                        "${asset.owned}×",
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                asset.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Rendite ${(asset.dailyYield * yieldMultiplier).asPercent()}/Tag",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Risiko ${asset.risk.asPercent()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (asset.risk > 0.2) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Text(
                if (asset.sellRate >= 1.0) {
                    "Verkauf: voll auszahlbar"
                } else {
                    "Verkauf: nur ${(asset.sellRate * 100).format1()} % zurück"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (asset.owned > 0) {
                Text(
                    "bringt aktuell ${(asset.dailyIncome() * yieldMultiplier).toLong().asEuro()}/Tag",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onBuy,
                    enabled = affordable,
                    modifier = Modifier.weight(1f).testTag("buy-${asset.id}"),
                ) {
                    Text("Kaufen ${asset.nextPrice.asEuro()}", fontSize = 12.sp)
                }
                if (asset.owned > 0) {
                    OutlinedButton(
                        onClick = onSell,
                        modifier = Modifier.weight(1f).testTag("sell-${asset.id}"),
                    ) {
                        Text("Verkaufen ${asset.sellValue.asEuro()}", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun UpgradeList(state: GameState, onBuy: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.testTag("upgrade-list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "Ausbauten bringen keine Rendite — sie ändern die Regeln. " +
                    "Dasselbe Geld könnte auch als Anlage arbeiten.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(state.upgrades, key = { it.id }) { upgrade ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(upgrade.name, fontWeight = FontWeight.SemiBold)
                        if (upgrade.owned) {
                            Text(
                                "aktiv",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    Text(
                        upgrade.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!upgrade.owned) {
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { onBuy(upgrade.id) },
                            enabled = state.cash >= upgrade.price,
                            modifier = Modifier.fillMaxWidth().testTag("upgrade-${upgrade.id}"),
                        ) {
                            Text("Kaufen für ${upgrade.price.asEuro()}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogList(log: List<LogEntry>) {
    val listState = rememberLazyListState()
    LaunchedEffect(log.size) {
        if (log.isNotEmpty()) listState.animateScrollToItem(log.size - 1)
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(log) { entry ->
            Row {
                Text(
                    "T${entry.day}",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(40.dp),
                )
                Text(
                    entry.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (entry.tone) {
                        LogEntry.Tone.GOOD -> MaterialTheme.colorScheme.primary
                        LogEntry.Tone.BAD -> MaterialTheme.colorScheme.error
                        LogEntry.Tone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun EndDayBar(state: GameState, onEndDay: () -> Unit) {
    Column(
        Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        if (!state.onTrack) {
            Text(
                if (state.projectedCash < 0) {
                    "Achtung: dein Konto läuft heute leer."
                } else {
                    "Achtung: so wie es jetzt steht, verlierst du heute."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = onEndDay,
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("end-day"),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("Tag ${state.day} abschließen", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DayReportDialog(report: DayReport, onDismiss: () -> Unit) {
    if (!report.survived) return

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Weiter") } },
        title = { Text("Tag ${report.day} abgerechnet") },
        text = {
            Column {
                MoneyRow("Einnahmen", report.gross, positive = true)
                MoneyRow("Steuern", -report.taxes, positive = false)
                MoneyRow("Fixkosten", -report.fixed, positive = false)
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Vermögen (Ziel ${report.goal.asEuro()})", fontWeight = FontWeight.Bold)
                    Text(
                        report.worthAfter.asEuro(),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Netto heute",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        report.net.asEuro(),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        },
    )
}

@Composable
private fun GameOverScreen(state: GameState, onRestart: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "BROKE",
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (state.cash < 0) {
                    "An Tag ${state.day} war dein Konto leer — Rechnungen unbezahlbar."
                } else {
                    "Du hast das Tagesziel an Tag ${state.day} verfehlt."
                },
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                state.netWorth.asEuro(),
                fontSize = 28.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "gebraucht hättest du ${state.goal.asEuro()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
            Text("Rekord: Tag ${state.bestDay}", color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Nochmal", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Legt sich über das laufende Spiel und erklärt Schritt für Schritt. Der
 * Hintergrund bleibt sichtbar, weil der Text sich auf genau das bezieht, was
 * darunter zu sehen ist.
 */
@Composable
private fun TutorialOverlay(
    step: TutorialStep,
    index: Int,
    total: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(24.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().testTag("tutorial-card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Schritt ${index + 1} von $total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(4.dp))
                Text(step.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(step.body, style = MaterialTheme.typography.bodyMedium)

                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onSkip, modifier = Modifier.testTag("tutorial-skip")) {
                        Text("Überspringen")
                    }
                    Button(onClick = onNext, modifier = Modifier.testTag("tutorial-next")) {
                        Text(if (index + 1 >= total) "Los geht's" else "Weiter")
                    }
                }
            }
        }
    }
}
