package com.phonesec.broke.game

import kotlin.random.Random

/**
 * Die komplette Spiellogik als reine Funktionen: jede Aktion nimmt einen Zustand
 * und gibt einen neuen zurück. Kein Android, keine Seiteneffekte — dadurch testbar.
 */
object GameEngine {

    sealed interface Outcome {
        data class Ok(val state: GameState) : Outcome
        data class Rejected(val reason: String) : Outcome
    }

    fun newGame(bestDay: Int = 1): GameState = Balancing.newGame().copy(bestDay = bestDay)

    // ---------------------------------------------------------------- Aktionen

    /** Abo kündigen. Kostet ggf. eine Gebühr und ist während der Mindestlaufzeit gesperrt. */
    fun cancel(state: GameState, drainId: String): Outcome {
        val drain = state.drains.find { it.id == drainId }
            ?: return Outcome.Rejected("Das gibt es nicht mehr.")
        if (!drain.cancellable) return Outcome.Rejected("\"${drain.name}\" wirst du nicht los.")
        if (drain.isLocked(state.day)) {
            return Outcome.Rejected("Das geht erst ab Tag ${drain.lockedUntilDay}.")
        }
        if (state.actionPoints < 1) return Outcome.Rejected("Heute kannst du nichts mehr machen.")
        if (state.cash < drain.cancelFee) {
            return Outcome.Rejected("Dafür brauchst du ${drain.cancelFee.asEuro()} auf dem Konto.")
        }

        val note = if (drain.cancelFee > 0) " Hat ${drain.cancelFee.asEuro()} gekostet." else ""
        return Outcome.Ok(
            state.copy(
                cash = state.cash - drain.cancelFee,
                drains = state.drains - drain,
                actionPoints = state.actionPoints - 1,
                log = state.log + LogEntry(
                    state.day,
                    "${drain.name} ist weg.$note",
                    LogEntry.Tone.GOOD,
                ),
            )
        )
    }

    /**
     * Kosten runterhandeln. Klappt nicht immer, und mit jedem Versuch am selben
     * Posten sinkt die Chance — irgendwann musst du kündigen statt feilschen.
     */
    fun negotiate(state: GameState, drainId: String, rng: Random): Outcome {
        val drain = state.drains.find { it.id == drainId }
            ?: return Outcome.Rejected("Posten existiert nicht mehr.")
        if (state.actionPoints < 1) return Outcome.Rejected("Heute kannst du nichts mehr machen.")

        val spent = state.copy(actionPoints = state.actionPoints - 1)
        val chance = if (state.hasUpgrade("netzwerk")) {
            (drain.negotiationChance + 0.25).coerceAtMost(0.95)
        } else {
            drain.negotiationChance
        }
        if (rng.nextDouble() > chance) {
            return Outcome.Ok(
                spent.copy(
                    drains = spent.drains.map { if (it.id == drainId) it.bumpNegotiations() else it },
                    log = spent.log + LogEntry(
                        state.day,
                        "Bei ${drain.name} hat es nicht geklappt.",
                        LogEntry.Tone.BAD,
                    ),
                )
            )
        }

        val reduction = 0.15 + rng.nextDouble() * 0.2
        val updated = if (drain.isTax) {
            drain.copy(
                incomeRate = (drain.incomeRate * (1 - reduction)).coerceAtLeast(0.05),
                negotiations = drain.negotiations + 1,
            )
        } else {
            drain.copy(
                dailyCost = (drain.dailyCost * (1 - reduction)).toLong(),
                negotiations = drain.negotiations + 1,
            )
        }

        return Outcome.Ok(
            spent.copy(
                drains = spent.drains.map { if (it.id == drainId) updated else it },
                log = spent.log + LogEntry(
                    state.day,
                    "${drain.name} ist jetzt billiger.",
                    LogEntry.Tone.GOOD,
                ),
            )
        )
    }

    /** Anteil einer Anlage kaufen. Kostet Geld, aber keine Aktion. */
    fun buyAsset(state: GameState, assetId: String): Outcome {
        val asset = state.assets.find { it.id == assetId }
            ?: return Outcome.Rejected("Das gibt es nicht.")
        val price = asset.nextPrice
        if (state.cash < price) return Outcome.Rejected("Dafür hast du nicht genug Geld.")

        val bought = asset.copy(owned = asset.owned + 1)
        var next = state.copy(
            cash = state.cash - price,
            assets = state.assets.map { if (it.id == assetId) bought else it },
            log = state.log + LogEntry(
                state.day,
                "${asset.name} gekauft für ${price.asEuro()}.",
                LogEntry.Tone.GOOD,
            ),
        )

        // Die Wohnung bringt einmalig ihre eigene Steuer mit ins Spiel.
        if (assetId == "immo" && next.drains.none { it.id == "grundsteuer" }) {
            next = next.copy(
                drains = next.drains + Drain(
                    id = "grundsteuer",
                    name = "Steuer für die Wohnung",
                    type = DrainType.FIXKOSTEN,
                    dailyCost = 900L,
                    cancellable = false,
                ),
                log = next.log + LogEntry(
                    state.day,
                    "Für die Wohnung zahlst du jetzt 9,00 € Steuer am Tag.",
                    LogEntry.Tone.BAD,
                ),
            )
        }
        return Outcome.Ok(next)
    }

    /**
     * Anteil zu Geld machen. Kostet keine Aktion, aber je nach Anlage bleibt ein
     * ordentlicher Teil auf der Strecke — der einzige Weg, aus einer Liquiditäts-
     * klemme zu kommen, und deshalb eine Entscheidung mit Preisschild.
     */
    fun sellAsset(state: GameState, assetId: String): Outcome {
        val asset = state.assets.find { it.id == assetId }
            ?: return Outcome.Rejected("Das gibt es nicht.")
        if (asset.owned == 0) return Outcome.Rejected("Das hast du gar nicht.")

        val payout = asset.sellValue
        val sold = asset.copy(owned = asset.owned - 1)
        val note = if (asset.sellRate < 1.0) " Etwas ist dabei verloren gegangen." else ""

        return Outcome.Ok(
            state.copy(
                cash = state.cash + payout,
                assets = state.assets.map { if (it.id == assetId) sold else it },
                log = state.log + LogEntry(
                    state.day,
                    "${asset.name} verkauft. Du bekommst ${payout.asEuro()}.$note",
                    LogEntry.Tone.NEUTRAL,
                ),
            )
        )
    }

    /** Dauerhaften Ausbau kaufen. Bringt keine Rendite, sondern ändert eine Regel. */
    fun buyUpgrade(state: GameState, upgradeId: String): Outcome {
        val upgrade = state.upgrades.find { it.id == upgradeId }
            ?: return Outcome.Rejected("Das gibt es nicht.")
        if (upgrade.owned) return Outcome.Rejected("Das hast du schon.")
        if (state.cash < upgrade.price) return Outcome.Rejected("Dafür hast du nicht genug Geld.")

        val bought = state.copy(
            cash = state.cash - upgrade.price,
            upgrades = state.upgrades.map { if (it.id == upgradeId) it.copy(owned = true) else it },
            log = state.log + LogEntry(
                state.day,
                "${upgrade.name} gekauft. ${upgrade.description}",
                LogEntry.Tone.GOOD,
            ),
        )
        // Die Assistenz soll noch am Tag des Kaufs helfen.
        return Outcome.Ok(
            if (upgradeId == "assistenz") bought.copy(actionPoints = bought.actionPoints + 1) else bought
        )
    }

    /**
     * Notnagel: zählt als Einnahme für heute, wenn das Tagesziel sonst nicht zu
     * schaffen ist. Wirkt nur einen Tag — als Dauerlösung taugt er nicht.
     */
    fun sideGig(state: GameState, rng: Random): Outcome {
        if (state.actionPoints < 1) return Outcome.Rejected("Heute kannst du nichts mehr machen.")

        val base = (state.goal * 0.25).toLong().coerceAtLeast(5_000L)
        val payout = base + (base * rng.nextDouble()).toLong()
        return Outcome.Ok(
            state.copy(
                bonusIncome = state.bonusIncome + payout,
                actionPoints = state.actionPoints - 1,
                log = state.log + LogEntry(
                    state.day,
                    "Du warst arbeiten. Das bringt heute ${payout.asEuro()}.",
                    LogEntry.Tone.GOOD,
                ),
            )
        )
    }

    // ------------------------------------------------------------ Tagesabschluss

    /**
     * Rechnet den Tag ab: Einnahmen minus Steuern und Fixkosten. Bleibt zu wenig
     * übrig, um das Tagesziel zu decken — oder rutscht das Konto ins Minus —,
     * ist das Spiel vorbei.
     */
    fun endDay(state: GameState, rng: Random): Pair<GameState, DayReport> {
        val gross = grossWithRisk(state, rng)
        val taxes = state.drains.filter { it.isTax }.sumOf { it.costFor(gross) }
        val fixed = state.fixedCost
        val net = gross - taxes - fixed
        val cashAfter = state.cash + net
        val worthAfter = cashAfter + state.investedValue + state.upgradeValue
        val broke = cashAfter < 0
        val survived = worthAfter >= state.goal && !broke

        val report = DayReport(
            day = state.day,
            gross = gross,
            taxes = taxes,
            fixed = fixed,
            net = net,
            cashAfter = cashAfter,
            worthAfter = worthAfter,
            goal = state.goal,
            survived = survived,
            broke = broke,
        )

        if (!survived) {
            val reason = if (broke) {
                "Dein Konto ist leer. Du kannst nichts mehr bezahlen."
            } else {
                "Zu wenig Geld: ${worthAfter.asEuro()} statt ${state.goal.asEuro()}."
            }
            val lost = state.copy(
                cash = cashAfter,
                status = GameStatus.LOST,
                log = state.log + LogEntry(state.day, reason, LogEntry.Tone.BAD),
            )
            return lost to report
        }

        var next = state.copy(
            day = state.day + 1,
            cash = cashAfter,
            goal = Balancing.goalForNextDay(state.goal, state.day),
            actionPoints = Balancing.ACTION_POINTS_PER_DAY +
                if (state.hasUpgrade("assistenz")) 1 else 0,
            bonusIncome = 0,
            drains = state.drains.map { it.advanceDay(state.hasUpgrade("steuerberater")) },
            bestDay = maxOf(state.bestDay, state.day + 1),
            log = state.log + LogEntry(
                state.day,
                "Tag ${state.day} geschafft. ${net.asEuro()} übrig geblieben.",
                LogEntry.Tone.GOOD,
            ),
        )

        Events.rollFor(next, rng)?.let { (afterEvent, entry) ->
            next = afterEvent.copy(log = afterEvent.log + entry)
        }

        return next to report
    }

    /**
     * Bruttoeinnahmen inklusive Risiko: riskante Anlagen können an einem Tag
     * einen Teil ihres Ertrags verlieren.
     */
    private fun grossWithRisk(state: GameState, rng: Random): Cents {
        var total = state.passiveIncome + state.bonusIncome
        for (asset in state.assets) {
            if (asset.owned == 0) continue
            val income = (asset.dailyIncome() * state.yieldMultiplier).toLong()
            val cushion = if (state.hasUpgrade("notgroschen")) 0.5 else 1.0
            total += if (rng.nextDouble() < asset.risk) {
                -(income * cushion *
                    (Balancing.RISK_LOSS_MIN + rng.nextDouble() * Balancing.RISK_LOSS_SPAN)).toLong()
            } else {
                income
            }
        }
        return total
    }

    private fun Drain.bumpNegotiations() = copy(negotiations = negotiations + 1)

    /**
     * Tageswechsel für einen Kostenpunkt: Steuern klettern ein Stück, und der
     * Verhandlungswiderstand kühlt wieder ab — sonst wäre nach drei Versuchen
     * für immer Schluss.
     */
    private fun Drain.advanceDay(taxAdvisor: Boolean): Drain = copy(
        incomeRate = if (isTax) {
            val creep = if (taxAdvisor) Balancing.TAX_CREEP_PER_DAY / 2 else Balancing.TAX_CREEP_PER_DAY
            (incomeRate + creep).coerceAtMost(Balancing.MAX_TAX_RATE)
        } else {
            incomeRate
        },
        negotiations = (negotiations - 1).coerceAtLeast(0),
    )
}
