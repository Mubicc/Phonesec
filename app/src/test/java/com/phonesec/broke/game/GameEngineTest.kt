package com.phonesec.broke.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private fun ok(outcome: GameEngine.Outcome): GameState {
        assertTrue("erwartet Ok, war $outcome", outcome is GameEngine.Outcome.Ok)
        return (outcome as GameEngine.Outcome.Ok).state
    }

    private fun rejected(outcome: GameEngine.Outcome): String {
        assertTrue("erwartet Rejected, war $outcome", outcome is GameEngine.Outcome.Rejected)
        return (outcome as GameEngine.Outcome.Rejected).reason
    }

    @Test
    fun `neues spiel startet knapp ueber dem tagesziel`() {
        val state = GameEngine.newGame()
        assertEquals(1, state.day)
        assertEquals(Balancing.ACTION_POINTS_PER_DAY, state.actionPoints)
        assertTrue("Tag 1 muss ohne Aktion schaffbar sein", state.onTrack)
    }

    @Test
    fun `kuendigen entfernt den posten und zieht die gebuehr ab`() {
        val start = GameEngine.newGame()
        val handy = start.drains.first { it.id == "handy" }

        val after = ok(GameEngine.cancel(start, "handy"))

        assertNull(after.drains.find { it.id == "handy" })
        assertEquals(start.cash - handy.cancelFee, after.cash)
        assertEquals(start.actionPoints - 1, after.actionPoints)
    }

    @Test
    fun `kuendigen waehrend der mindestlaufzeit wird abgelehnt`() {
        val start = GameEngine.newGame()
        val reason = rejected(GameEngine.cancel(start, "gym"))
        assertTrue(reason.contains("Mindestlaufzeit"))
    }

    @Test
    fun `nicht kuendbare posten bleiben bestehen`() {
        val start = GameEngine.newGame()
        rejected(GameEngine.cancel(start, "miete"))
        rejected(GameEngine.cancel(start, "steuer"))
    }

    @Test
    fun `ohne aktionspunkte geht nichts mehr`() {
        val start = GameEngine.newGame().copy(actionPoints = 0)
        assertTrue(rejected(GameEngine.cancel(start, "handy")).contains("Aktionen"))
        assertTrue(rejected(GameEngine.sideGig(start, Random(1))).contains("Aktionen"))
    }

    @Test
    fun `verhandeln senkt entweder die kosten oder verbraucht nur den versuch`() {
        val start = GameEngine.newGame()
        val before = start.drains.first { it.id == "streaming" }

        val after = ok(GameEngine.negotiate(start, "streaming", Random(42)))
        val now = after.drains.first { it.id == "streaming" }

        assertEquals(start.actionPoints - 1, after.actionPoints)
        assertEquals(before.negotiations + 1, now.negotiations)
        assertTrue(now.dailyCost <= before.dailyCost)
    }

    @Test
    fun `wiederholtes verhandeln wird unwahrscheinlicher`() {
        val fresh = Drain("x", "X", DrainType.ABO, dailyCost = 100)
        val once = fresh.copy(negotiations = 1)
        val twice = fresh.copy(negotiations = 2)

        assertTrue(once.negotiationChance < fresh.negotiationChance)
        assertTrue(twice.negotiationChance < once.negotiationChance)
        assertTrue(fresh.copy(negotiations = 10).negotiationChance >= 0.1)
    }

    @Test
    fun `anlage kaufen kostet geld und erhoeht die einnahmen`() {
        val start = GameEngine.newGame()
        val price = start.assets.first { it.id == "tagesgeld" }.nextPrice

        val after = ok(GameEngine.buyAsset(start, "tagesgeld"))

        assertEquals(start.cash - price, after.cash)
        assertEquals(1, after.assets.first { it.id == "tagesgeld" }.owned)
        assertTrue(after.assetIncome > start.assetIncome)
        assertEquals("Kaufen kostet keine Aktion", start.actionPoints, after.actionPoints)
    }

    @Test
    fun `anlagenpreis steigt mit jedem kauf`() {
        val start = GameEngine.newGame()
        val first = start.assets.first { it.id == "tagesgeld" }.nextPrice
        val after = ok(GameEngine.buyAsset(start, "tagesgeld"))
        val second = after.assets.first { it.id == "tagesgeld" }.nextPrice

        assertTrue(second > first)
    }

    @Test
    fun `wohnung bringt die grundsteuer mit`() {
        val start = GameEngine.newGame().copy(cash = 5_000_000L)
        val after = ok(GameEngine.buyAsset(start, "immo"))

        assertNotNull(after.drains.find { it.id == "grundsteuer" })

        // Beim zweiten Kauf darf sie nicht erneut auftauchen.
        val again = ok(GameEngine.buyAsset(after, "immo"))
        assertEquals(1, again.drains.count { it.id == "grundsteuer" })
    }

    @Test
    fun `zu teure anlage wird abgelehnt`() {
        val broke = GameEngine.newGame().copy(cash = 100L)
        assertTrue(rejected(GameEngine.buyAsset(broke, "immo")).contains("fehlt"))
    }

    @Test
    fun `tagesziel verfehlt beendet das spiel`() {
        val doomed = GameEngine.newGame().copy(goal = 99_999_999L)
        val (after, report) = GameEngine.endDay(doomed, Random(7))

        assertEquals(GameStatus.LOST, after.status)
        assertTrue(!report.survived)
        assertEquals(doomed.day, after.day)
    }

    @Test
    fun `ueberstandener tag erhoeht tag und ziel`() {
        val start = GameEngine.newGame()
        val (after, report) = GameEngine.endDay(start, Random(3))

        assertTrue(report.survived)
        assertEquals(GameStatus.RUNNING, after.status)
        assertEquals(start.day + 1, after.day)
        assertTrue("Das Ziel muss anziehen", after.goal > start.goal)
        assertEquals(Balancing.ACTION_POINTS_PER_DAY, after.actionPoints)
    }

    @Test
    fun `abrechnung entspricht einnahmen minus steuern und fixkosten`() {
        // Ohne Anlagen ist das Ergebnis deterministisch: nur Zinsen, kein Risiko.
        val start = GameEngine.newGame()
        val (_, report) = GameEngine.endDay(start, Random(11))

        assertEquals(start.passiveIncome, report.gross)
        assertEquals(report.gross - report.taxes - report.fixed, report.net)
        assertEquals(start.cash + report.net, report.cashAfter)
    }

    @Test
    fun `steuern wachsen mit den einnahmen mit`() {
        val steuer = Drain("s", "Steuer", DrainType.STEUER, incomeRate = 0.25)
        assertEquals(25_000L, steuer.costFor(100_000L))
        assertEquals(250_000L, steuer.costFor(1_000_000L))
    }

    @Test
    fun `nebenjob hebt das heutige einkommen und kostet eine aktion`() {
        val start = GameEngine.newGame()
        val after = ok(GameEngine.sideGig(start, Random(5)))

        assertTrue(after.grossIncome > start.grossIncome)
        assertEquals(start.actionPoints - 1, after.actionPoints)
    }

    @Test
    fun `der nebenjob wirkt nur einen tag`() {
        val start = GameEngine.newGame()
        val boosted = ok(GameEngine.sideGig(start, Random(5)))
        val (nextDay, _) = GameEngine.endDay(boosted, Random(5))

        assertEquals(0L, nextDay.bonusIncome)
    }

    @Test
    fun `ein ausufernder kostenposten beendet das spiel`() {
        val start = GameEngine.newGame().copy(
            drains = listOf(Drain("x", "Riesenkredit", DrainType.KREDIT, dailyCost = 500_000L)),
        )
        val (after, report) = GameEngine.endDay(start, Random(2))

        assertTrue(!report.survived)
        assertTrue(report.cashAfter < report.goal)
        assertEquals(GameStatus.LOST, after.status)
    }

    @Test
    fun `rekordtag bleibt ueber einen neustart erhalten`() {
        var state = GameEngine.newGame()
        repeat(3) { state = GameEngine.endDay(state, Random(it.toLong())).first }
        val record = state.bestDay
        assertTrue(record > 1)

        val restarted = GameEngine.newGame(record)
        assertEquals(record, restarted.bestDay)
        assertEquals(1, restarted.day)
    }

    @Test
    fun `das spiel bleibt ohne eingriff nicht ewig gewinnbar`() {
        // Wer nur Tage abschließt und nichts verändert, muss irgendwann scheitern —
        // sonst wäre die Kernmechanik wirkungslos.
        var state = GameEngine.newGame()
        var days = 0
        while (state.status == GameStatus.RUNNING && days < 100) {
            state = GameEngine.endDay(state, Random(1234)).first
            days++
        }
        assertEquals(GameStatus.LOST, state.status)
    }
}
