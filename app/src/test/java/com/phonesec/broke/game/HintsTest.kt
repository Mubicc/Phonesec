package com.phonesec.broke.game

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Tipp ist die eigentliche Einstiegshilfe: Er muss in jeder Lage sagen, was
 * jetzt dran ist — und vor allem die dringenden Fälle zuerst.
 */
class HintsTest {

    @Test
    fun `ein leerlaufendes konto ist der dringendste hinweis`() {
        val squeezed = GameEngine.newGame().copy(
            cash = 1_000L,
            drains = listOf(Drain("x", "Riesenkredit", DrainType.KREDIT, dailyCost = 900_000L)),
        )
        assertTrue(squeezed.projectedCash < 0)
        assertTrue(Hints.forState(squeezed).contains("Konto wird heute leer"))
    }

    @Test
    fun `bei zu wenig geld nennt der tipp den fehlbetrag`() {
        val behind = GameEngine.newGame().copy(goal = 99_999_999L)
        val hint = Hints.forState(behind)

        assertTrue(hint.contains("fehlen"))
        assertTrue("Der Fehlbetrag gehört in den Satz", hint.contains(behind.shortfall.asEuro()))
    }

    @Test
    fun `viel geld auf dem konto fuehrt zum anlegen`() {
        val flush = GameEngine.newGame().copy(cash = 5_000_000L)
        assertTrue(Hints.forState(flush).contains("Anlegen"))
    }

    @Test
    fun `sind aktionen offen weist der tipp darauf hin`() {
        // Wenig Bargeld, aber Ziel erreicht: dann sind die offenen Aktionen dran.
        val tight = GameEngine.newGame().copy(cash = 20_000L, goal = 1L)
        val hint = Hints.forState(tight)

        assertTrue(tight.onTrack)
        assertTrue(hint.contains("Sachen frei"))
    }

    @Test
    fun `ohne offene aktionen schickt der tipp zum tagesende`() {
        val done = GameEngine.newGame().copy(cash = 20_000L, goal = 1L, actionPoints = 0)
        assertTrue(Hints.forState(done).contains("grünen Knopf"))
    }

    @Test
    fun `die worte fuer chance risiko und verkauf sind abgestuft`() {
        assertTrue(Hints.chanceWord(0.85).contains("meistens"))
        assertTrue(Hints.chanceWord(0.5).contains("oft"))
        assertTrue(Hints.chanceWord(0.1).contains("selten"))

        assertTrue(Hints.riskWord(0.0).contains("sicher"))
        assertTrue(Hints.riskWord(0.4).contains("oft"))

        assertTrue(Hints.sellWord(1.0).contains("alles zurück"))
        assertTrue(Hints.sellWord(0.55).contains("sehr viel"))
    }

    @Test
    fun `kein tipp enthaelt ein prozentzeichen oder fachwoerter`() {
        // Die App soll ohne Finanzvokabular auskommen.
        val jargon = listOf("%", "Rendite", "Liquidität", "netto", "Netto", "Vermögen")
        val states = listOf(
            GameEngine.newGame(),
            GameEngine.newGame().copy(cash = 5_000_000L),
            GameEngine.newGame().copy(goal = 99_999_999L),
            GameEngine.newGame().copy(actionPoints = 0, cash = 20_000L, goal = 1L),
        )
        for (state in states) {
            val hint = Hints.forState(state)
            for (word in jargon) {
                assertTrue("\"$word\" gehört nicht in \"$hint\"", !hint.contains(word))
            }
        }
    }
}
