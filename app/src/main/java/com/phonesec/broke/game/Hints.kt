package com.phonesec.broke.game

/**
 * Übersetzt den Spielstand in einen einzigen Satz, der sagt, was jetzt dran ist.
 * Damit muss niemand die Zahlen selbst deuten, um sinnvoll zu spielen.
 */
object Hints {

    fun forState(state: GameState): String {
        val cheapest = state.assets.minByOrNull { it.nextPrice }
        return when {
            state.projectedCash < 0 ->
                "Achtung: Dein Konto wird heute leer! Geh auf \"Anlegen\" und verkaufe etwas."

            !state.onTrack ->
                "Dir fehlen noch ${state.shortfall.asEuro()}. " +
                    "Wirf bei \"Ausgaben\" etwas raus oder mach einen Nebenjob."

            cheapest != null && state.cash >= cheapest.nextPrice * 3 ->
                "Du hast Geld übrig. Kauf etwas bei \"Anlegen\" — dann arbeitet es für dich."

            state.actionPoints > 0 ->
                "Du hast noch ${state.actionPoints} Sachen frei. " +
                    "Mach bei \"Ausgaben\" etwas billiger oder weg."

            else ->
                "Alles erledigt. Tipp unten auf den grünen Knopf."
        }
    }

    /** Wie oft eine Verhandlung klappt — in Worten statt in Prozent. */
    fun chanceWord(chance: Double): String = when {
        chance >= 0.7 -> "klappt meistens"
        chance >= 0.4 -> "klappt oft"
        else -> "klappt selten"
    }

    /** Wie riskant eine Anlage ist — in Worten statt in Prozent. */
    fun riskWord(risk: Double): String = when {
        risk <= 0.0 -> "immer sicher"
        risk < 0.2 -> "selten mal Verlust"
        risk < 0.35 -> "manchmal Verlust"
        else -> "oft Verlust"
    }

    /** Wie gut man wieder rauskommt — in Worten statt in Prozent. */
    fun sellWord(sellRate: Double): String = when {
        sellRate >= 1.0 -> "Du bekommst alles zurück"
        sellRate >= 0.85 -> "Beim Verkaufen verlierst du etwas"
        sellRate >= 0.65 -> "Beim Verkaufen verlierst du viel"
        else -> "Beim Verkaufen verlierst du sehr viel"
    }
}
