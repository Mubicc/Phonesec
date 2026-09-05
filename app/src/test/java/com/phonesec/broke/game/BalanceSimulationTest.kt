package com.phonesec.broke.game

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Balancing-Absicherung: Das Spiel muss in beide Richtungen funktionieren —
 * schlechtes Spiel scheitert früh, gutes Spiel kommt deutlich weiter. Ohne diese
 * Spanne wäre die Kernmechanik entweder sinnlos oder frustrierend.
 */
class BalanceSimulationTest {

    /** Erwarteter Ertrag pro investiertem Euro, inklusive der schlechten Tage. */
    private fun Asset.expectedYield(): Double {
        val badDayLoss = Balancing.RISK_LOSS_MIN + Balancing.RISK_LOSS_SPAN / 2
        return dailyYield * ((1 - risk) - risk * badDayLoss)
    }

    /** Spielt einen Tag so, wie es eine aufmerksame Person tun würde. */
    private fun playSmartDay(start: GameState, rng: Random): GameState {
        var state = start

        // 1. Teuerste kündbare Posten loswerden.
        while (state.actionPoints > 1) {
            val target = state.drains
                .filter { !it.isTax && it.cancellable && !it.isLocked(state.day) }
                .filter { it.cancelFee <= state.cash / 4 }
                .maxByOrNull { it.dailyCost }
                ?: break
            val outcome = GameEngine.cancel(state, target.id)
            if (outcome is GameEngine.Outcome.Ok) state = outcome.state else break
        }

        // 2. Rest gegen die Steuer einsetzen — sie wächst als einzige mit.
        while (state.actionPoints > 0) {
            val tax = state.drains.filter { it.isTax }.maxByOrNull { it.incomeRate } ?: break
            val outcome = GameEngine.negotiate(state, tax.id, rng)
            if (outcome is GameEngine.Outcome.Ok) state = outcome.state else break
        }

        // 3. Investieren, aber genug Liquidität für rote Tage und Rechnungen behalten.
        while (true) {
            val buffer = state.fixedCost * 4 + state.assetIncome * 2
            val candidate = state.assets
                .filter { state.cash - it.nextPrice >= buffer }
                .maxByOrNull { it.expectedYield() } ?: break
            val outcome = GameEngine.buyAsset(state, candidate.id)
            if (outcome !is GameEngine.Outcome.Ok) break
            state = outcome.state
        }

        return GameEngine.endDay(state, rng).first
    }

    private fun survivedDays(seed: Long, play: (GameState, Random) -> GameState): Int {
        val rng = Random(seed)
        var state = GameEngine.newGame()
        var days = 0
        while (state.status == GameStatus.RUNNING && days < 200) {
            state = play(state, rng)
            days++
        }
        return days
    }

    @Test
    fun `gutes spiel kommt deutlich weiter als passives spiel`() {
        val seeds = listOf(1L, 7L, 42L, 99L, 2024L)

        val passive = seeds.map { seed ->
            survivedDays(seed) { state, rng -> GameEngine.endDay(state, rng).first }
        }
        val smart = seeds.map { seed -> survivedDays(seed) { state, rng -> playSmartDay(state, rng) } }

        println("passiv: $passive")
        println("aktiv:  $smart")

        val passiveAvg = passive.average()
        val smartAvg = smart.average()

        assertTrue("Nichtstun darf nicht weit tragen (war $passiveAvg)", passiveAvg < 18)
        assertTrue("Gutes Spiel muss belohnt werden (war $smartAvg)", smartAvg > passiveAvg * 1.5)
    }

    @Test
    fun `auch perfektes spiel endet irgendwann`() {
        // Das beschleunigende Tagesziel muss jede Strategie einholen — sonst
        // gäbe es Läufe, die schlicht nie aufhören.
        val results = listOf(3L, 5L, 42L, 77L).map { seed ->
            survivedDays(seed) { state, rng -> playSmartDay(state, rng) }
        }
        println("perfekt: $results")
        assertTrue("Kein Lauf darf endlos sein (war $results)", results.all { it < 200 })
    }

    /**
     * Wie [playSmartDay], nutzt aber zusätzlich Ausbauten und verkauft Anlagen,
     * wenn die Liquidität knapp wird. Wenn diese Mechaniken echte Tiefe haben,
     * muss diese Spielweise messbar weiter tragen.
     */
    private fun playStrategicDay(start: GameState, rng: Random): GameState {
        var state = start

        while (state.actionPoints > 1) {
            val target = state.drains
                .filter { !it.isTax && it.cancellable && !it.isLocked(state.day) }
                .filter { it.cancelFee <= state.cash / 4 }
                .maxByOrNull { it.dailyCost }
                ?: break
            val outcome = GameEngine.cancel(state, target.id)
            if (outcome is GameEngine.Outcome.Ok) state = outcome.state else break
        }

        while (state.actionPoints > 0) {
            val tax = state.drains.filter { it.isTax }.maxByOrNull { it.incomeRate } ?: break
            val outcome = GameEngine.negotiate(state, tax.id, rng)
            if (outcome is GameEngine.Outcome.Ok) state = outcome.state else break
        }

        // Ausbauten wirken dauerhaft, kosten aber Rendite. Erst kaufen, wenn sie
        // gemessen am Vermögen klein sind — früh ist jeder Euro im Zinseszins mehr wert.
        val buffer = { s: GameState -> s.fixedCost * 4 + s.assetIncome * 2 }
        for (id in listOf("netzwerk", "steuerberater", "notgroschen", "depot", "assistenz")) {
            val upgrade = state.upgrades.first { it.id == id }
            if (upgrade.owned) continue
            if (upgrade.price > state.netWorth / 5) continue
            if (state.cash - upgrade.price < buffer(state)) continue
            val outcome = GameEngine.buyUpgrade(state, id)
            if (outcome is GameEngine.Outcome.Ok) state = outcome.state
        }

        while (true) {
            val candidate = state.assets
                .filter { state.cash - it.nextPrice >= buffer(state) }
                .maxByOrNull { it.expectedYield() } ?: break
            val outcome = GameEngine.buyAsset(state, candidate.id)
            if (outcome !is GameEngine.Outcome.Ok) break
            state = outcome.state
        }

        // Droht das Konto leerzulaufen, das Liquideste zu Geld machen.
        while (state.projectedCash < 0) {
            val liquid = state.assets
                .filter { it.owned > 0 }
                .maxByOrNull { it.sellRate } ?: break
            val outcome = GameEngine.sellAsset(state, liquid.id)
            if (outcome !is GameEngine.Outcome.Ok) break
            state = outcome.state
        }

        return GameEngine.endDay(state, rng).first
    }

    @Test
    fun `ausbauten und verkaufen geben dem spiel zusaetzliche tiefe`() {
        val seeds = listOf(1L, 7L, 42L, 99L, 2024L, 5L, 77L, 300L)

        val ohne = seeds.map { survivedDays(it) { s, r -> playSmartDay(s, r) } }
        val mit = seeds.map { survivedDays(it) { s, r -> playStrategicDay(s, r) } }

        println("ohne Ausbau: $ohne  (Schnitt ${ohne.average()})")
        println("mit Ausbau:  $mit  (Schnitt ${mit.average()})")

        // Bewusst nur ein Zehntel Vorsprung als Schwelle: Der gemessene Effekt liegt
        // höher, aber eine knapp gesetzte Grenze würde bei jeder Nachjustierung kippen.
        assertTrue(
            "Die Strategie-Ebene muss sich lohnen (${ohne.average()} vs ${mit.average()})",
            mit.average() > ohne.average() * 1.10,
        )
        assertTrue("Auch damit muss das Spiel enden", mit.all { it < 200 })
    }
}
