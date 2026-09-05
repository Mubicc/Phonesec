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
import com.phonesec.broke.game.Hints
import com.phonesec.broke.game.LogEntry
import com.phonesec.broke.game.asEuro

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
                        Text("Tag ${state.day}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            "Bester Tag bisher: ${state.bestDay}",
                            style = MaterialTheme.typography.labelMedium,
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
                        Text("?", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = { EndDayBar(state = state, onEndDay = viewModel::endDay) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            MoneyCard(state)

            TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                Tab(
                    selected = tab == 0,
                    onClick = { chosenTab = 0 },
                    text = { Text("Ausgaben", fontSize = 13.sp) },
                    modifier = Modifier.testTag("tab-kosten"),
                )
                Tab(
                    selected = tab == 1,
                    onClick = { chosenTab = 1 },
                    text = { Text("Anlegen", fontSize = 13.sp) },
                    modifier = Modifier.testTag("tab-anlagen"),
                )
                Tab(
                    selected = tab == 2,
                    onClick = { chosenTab = 2 },
                    text = { Text("Extras", fontSize = 13.sp) },
                    modifier = Modifier.testTag("tab-ausbau"),
                )
                Tab(
                    selected = tab == 3,
                    onClick = { chosenTab = 3 },
                    text = { Text("Verlauf", fontSize = 13.sp) },
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
        modifier = Modifier.padding(end = 4.dp),
    ) {
        Text(
            "Du kannst noch",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        if (points == 0) {
            Text("nichts", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        } else {
            repeat(points.coerceAtMost(6)) {
                Box(
                    Modifier
                        .padding(horizontal = 2.dp)
                        .size(12.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }
    }
}

/** Die Kopfkarte: erst wie viel Geld da ist, dann ob der Tag reicht, dann warum. */
@Composable
private fun MoneyCard(state: GameState) {
    val progress = if (state.goal <= 0) 1f else {
        (state.projectedWorth.toFloat() / state.goal.toFloat()).coerceIn(0f, 1f)
    }
    val statusColor by animateColorAsState(
        if (state.onTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        label = "status",
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "Dein Geld",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                state.netWorth.asEuro(),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                "davon ${state.cash.asEuro()} auf dem Konto",
                style = MaterialTheme.typography.labelMedium,
                color = if (state.projectedCash < 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            Spacer(Modifier.height(14.dp))

            Text(
                if (state.onTrack) "Heute schaffst du es" else "Heute reicht es noch nicht",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(12.dp),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Heute brauchst du ${state.goal.asEuro()}. " +
                    "Am Abend hast du ${state.projectedWorth.asEuro()}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(12.dp))

            BigMoneyRow("Heute kommt rein", state.grossIncome, good = true)
            BigMoneyRow("Heute geht raus", -state.totalDrain, good = false)
            Spacer(Modifier.height(4.dp))
            BigMoneyRow("Das bleibt übrig", state.netIncome, good = state.netIncome >= 0, bold = true)
        }
    }
}

@Composable
private fun BigMoneyRow(label: String, amount: Long, good: Boolean, bold: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            fontSize = if (bold) 16.sp else 15.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (bold) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Text(
            amount.asEuro(),
            fontSize = if (bold) 16.sp else 15.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (good) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}

/** Der Satz, der sagt, was jetzt zu tun ist — das eigentliche Hilfsmittel für Anfänger. */
@Composable
private fun HintCard(state: GameState) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("hint"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Tipp", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(12.dp))
            Text(Hints.forState(state), fontSize = 14.sp)
        }
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
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { HintCard(state) }
        item {
            Text(
                "Das nimmt dir jeden Tag Geld weg:",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        item {
            OutlinedButton(
                onClick = onSideGig,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = state.actionPoints > 0,
            ) {
                Text("Arbeiten gehen (bringt heute Geld)", fontSize = 14.sp)
            }
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
                    Text(drain.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        if (drain.isTax) {
                            "Der Staat nimmt sich einen Teil von allem, was du verdienst."
                        } else {
                            "Das kostet dich jeden Tag."
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "-${drain.costFor(gross).asEuro()}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (locked) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Das kannst du erst ab Tag ${drain.lockedUntilDay} loswerden.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (!drain.cancellable) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Das wirst du nie ganz los — nur billiger machen.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onNegotiate,
                    enabled = actionsLeft > 0,
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Text("Billiger machen", fontSize = 13.sp)
                }
                Button(
                    onClick = onCancel,
                    enabled = drain.cancellable && !locked && actionsLeft > 0,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("cancel-${drain.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        if (drain.cancelFee > 0) "Weg damit (${drain.cancelFee.asEuro()})" else "Weg damit",
                        fontSize = 13.sp,
                    )
                }
            }
            Text(
                Hints.chanceWord(drain.negotiationChance),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AssetList(state: GameState, onBuy: (String) -> Unit, onSell: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.testTag("asset-list"),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { HintCard(state) }
        item {
            Text(
                "Hier arbeitet dein Geld für dich. Aber pass auf: " +
                    "Was du anlegst, liegt nicht mehr auf dem Konto.",
                fontSize = 14.sp,
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
    // Was ein einzelnes Stück täglich abwirft — greifbarer als eine Prozentzahl.
    val perUnit = (asset.nextPrice * asset.dailyYield * yieldMultiplier).toLong()

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(asset.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (asset.owned > 0) {
                    Text(
                        "${asset.owned} Stück",
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Text(
                asset.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(6.dp))
            Text(
                "Bringt dir ${perUnit.asEuro()} am Tag.",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                Hints.riskWord(asset.risk),
                style = MaterialTheme.typography.labelMedium,
                color = if (asset.risk > 0.2) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                Hints.sellWord(asset.sellRate),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (asset.owned > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Deine bringen zusammen " +
                        "${(asset.dailyIncome() * yieldMultiplier).toLong().asEuro()} am Tag.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onBuy,
                    enabled = affordable,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("buy-${asset.id}"),
                ) {
                    Text("Kaufen ${asset.nextPrice.asEuro()}", fontSize = 13.sp)
                }
                if (asset.owned > 0) {
                    OutlinedButton(
                        onClick = onSell,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("sell-${asset.id}"),
                    ) {
                        Text("Verkaufen ${asset.sellValue.asEuro()}", fontSize = 13.sp)
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
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "Extras bringen dir kein Geld. Sie machen das Spiel für dich leichter. " +
                    "Dein Geld bleibt dabei erhalten.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(state.upgrades, key = { it.id }) { upgrade ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(upgrade.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (upgrade.owned) {
                            Text(
                                "hast du",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Text(
                        upgrade.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!upgrade.owned) {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onBuy(upgrade.id) },
                            enabled = state.cash >= upgrade.price,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("upgrade-${upgrade.id}"),
                        ) {
                            Text("Kaufen ${upgrade.price.asEuro()}", fontSize = 14.sp)
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
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(log) { entry ->
            Row {
                Text(
                    "Tag ${entry.day}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(64.dp),
                )
                Text(
                    entry.text,
                    fontSize = 13.sp,
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
            .padding(12.dp)
    ) {
        if (!state.onTrack) {
            Text(
                if (state.projectedCash < 0) {
                    "Wenn du jetzt aufhörst, ist dein Konto leer."
                } else {
                    "Wenn du jetzt aufhörst, verlierst du."
                },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = onEndDay,
            modifier = Modifier.fillMaxWidth().height(60.dp).testTag("end-day"),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("Tag ${state.day} fertig", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun DayReportDialog(report: DayReport, onDismiss: () -> Unit) {
    if (!report.survived) return

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Weiter", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        title = { Text("Tag ${report.day} geschafft!", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                BigMoneyRow("Reingekommen", report.gross, good = true)
                BigMoneyRow("Rausgegangen", -(report.taxes + report.fixed), good = false)
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                Text(
                    "Du hast jetzt ${report.worthAfter.asEuro()}.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Gebraucht hättest du ${report.goal.asEuro()}.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "BROKE",
                fontSize = 60.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (state.cash < 0) {
                    "An Tag ${state.day} war dein Konto leer. Du konntest nichts mehr bezahlen."
                } else {
                    "An Tag ${state.day} hattest du zu wenig Geld."
                },
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Du hattest ${state.netWorth.asEuro()}",
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Gebraucht hättest du ${state.goal.asEuro()}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
            Text(
                "Dein bester Tag: ${state.bestDay}",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Nochmal", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(20.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().testTag("tutorial-card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "${index + 1} von $total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(4.dp))
                Text(step.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(step.body, fontSize = 16.sp)

                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onSkip, modifier = Modifier.testTag("tutorial-skip")) {
                        Text("Überspringen")
                    }
                    Button(
                        onClick = onNext,
                        modifier = Modifier.height(50.dp).testTag("tutorial-next"),
                    ) {
                        Text(
                            if (index + 1 >= total) "Los geht's" else "Weiter",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
