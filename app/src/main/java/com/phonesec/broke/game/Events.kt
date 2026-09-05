package com.phonesec.broke.game

import kotlin.random.Random

/** Ein Ereignis am Tagesanfang: verändert den Zustand und erklärt sich im Log. */
class GameEvent(
    val name: String,
    val weight: Int,
    val minDay: Int = 1,
    val apply: (GameState, Random) -> Pair<GameState, LogEntry>,
)

object Events {

    private val abofallen = listOf(
        Triple("Fitness-App Pro", 90L, DrainType.ABO),
        Triple("Cloud-Speicher XXL", 70L, DrainType.ABO),
        Triple("Zeitungs-Abo", 110L, DrainType.ABO),
        Triple("Dating-App Gold", 150L, DrainType.ABO),
        Triple("Versicherungspaket", 200L, DrainType.FIXKOSTEN),
        Triple("Gaming-Pass", 80L, DrainType.ABO),
    )

    val all: List<GameEvent> = listOf(
        GameEvent("Abofalle", weight = 30) { state, rng ->
            val (name, cost, type) = abofallen.random(rng)
            val scaled = cost + (state.day * 15L)
            val drain = Drain(
                id = "auto-${state.day}-${rng.nextInt(10_000)}",
                name = name,
                type = type,
                dailyCost = scaled,
                cancelFee = scaled * 4,
            )
            state.copy(drains = state.drains + drain) to
                LogEntry(state.day, "Abofalle: \"$name\" läuft jetzt für ${scaled.asEuro()}/Tag.", LogEntry.Tone.BAD)
        },

        GameEvent("Steuererhöhung", weight = 18, minDay = 3) { state, _ ->
            val updated = state.drains.map {
                if (it.isTax) it.copy(incomeRate = (it.incomeRate + 0.04).coerceAtMost(0.75)) else it
            }
            state.copy(drains = updated) to
                LogEntry(state.day, "Steuerreform: dein Steuersatz steigt um 4 Prozentpunkte.", LogEntry.Tone.BAD)
        },

        GameEvent("Inflation", weight = 16, minDay = 4) { state, _ ->
            val updated = state.drains.map {
                if (it.isTax) it else it.copy(dailyCost = (it.dailyCost * 1.12).toLong())
            }
            state.copy(drains = updated) to
                LogEntry(state.day, "Inflation: alle Fixkosten werden 12 % teurer.", LogEntry.Tone.BAD)
        },

        GameEvent("Zinssenkung", weight = 14, minDay = 3) { state, _ ->
            val next = (state.baseInterest - 0.004).coerceAtLeast(Balancing.MIN_INTEREST)
            state.copy(baseInterest = next) to
                LogEntry(state.day, "Die Bank senkt die Zinsen auf ${(next * 100).format1()} %.", LogEntry.Tone.BAD)
        },

        GameEvent("Nachzahlung", weight = 12, minDay = 5) { state, rng ->
            val amount = (state.cash * (0.03 + rng.nextDouble() * 0.05)).toLong()
            state.copy(cash = state.cash - amount) to
                LogEntry(state.day, "Steuernachzahlung: ${amount.asEuro()} weg.", LogEntry.Tone.BAD)
        },

        GameEvent("Steuerrückzahlung", weight = 12) { state, rng ->
            val amount = (state.cash * (0.02 + rng.nextDouble() * 0.04)).toLong()
            state.copy(cash = state.cash + amount) to
                LogEntry(state.day, "Steuerrückzahlung: ${amount.asEuro()} zurück.", LogEntry.Tone.GOOD)
        },

        GameEvent("Kulanz", weight = 10, minDay = 4) { state, rng ->
            val cancellable = state.drains.filter { it.cancellable && !it.isTax }
            if (cancellable.isEmpty()) {
                state to LogEntry(state.day, "Ruhiger Tag. Nichts passiert.", LogEntry.Tone.NEUTRAL)
            } else {
                val victim = cancellable.random(rng)
                state.copy(drains = state.drains - victim) to
                    LogEntry(state.day, "\"${victim.name}\" wurde vom Anbieter eingestellt. Glück gehabt.", LogEntry.Tone.GOOD)
            }
        },

        GameEvent("Zinsbonus", weight = 10) { state, _ ->
            val next = state.baseInterest + 0.005
            state.copy(baseInterest = next) to
                LogEntry(state.day, "Bank-Aktion: Zinsen steigen auf ${(next * 100).format1()} %.", LogEntry.Tone.GOOD)
        },

        GameEvent("Extra-Tag", weight = 8, minDay = 3) { state, _ ->
            state.copy(actionPoints = state.actionPoints + 2) to
                LogEntry(state.day, "Du hast dir freigenommen: +2 Aktionen heute.", LogEntry.Tone.GOOD)
        },
    )

    /**
     * Zieht ein Ereignis für den Tag. Je später im Spiel, desto wahrscheinlicher
     * passiert überhaupt etwas.
     */
    fun rollFor(state: GameState, rng: Random): Pair<GameState, LogEntry>? {
        val chance = (0.35 + state.day * 0.04).coerceAtMost(0.9)
        if (rng.nextDouble() > chance) return null

        val pool = all.filter { state.day >= it.minDay }
        val totalWeight = pool.sumOf { it.weight }
        var pick = rng.nextInt(totalWeight)
        for (event in pool) {
            pick -= event.weight
            if (pick < 0) return event.apply(state, rng)
        }
        return null
    }
}
