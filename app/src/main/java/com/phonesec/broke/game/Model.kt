package com.phonesec.broke.game

/** Alle Geldbeträge sind Cent-Werte, damit keine Rundungsfehler auflaufen. */
typealias Cents = Long

enum class DrainType(val label: String) {
    ABO("Abo"),
    FIXKOSTEN("Fixkosten"),
    KREDIT("Kredit"),
    STEUER("Steuer"),
}

/**
 * Etwas, das dir täglich Geld aus der Tasche zieht.
 *
 * Fixe Posten kosten [dailyCost] pro Tag. Steuern kosten stattdessen [incomeRate]
 * als Anteil deiner Tageseinnahmen — sie wachsen also mit, je erfolgreicher du wirst.
 */
data class Drain(
    val id: String,
    val name: String,
    val type: DrainType,
    val dailyCost: Cents = 0,
    val incomeRate: Double = 0.0,
    val cancelFee: Cents = 0,
    val cancellable: Boolean = true,
    val lockedUntilDay: Int = 0,
    val negotiations: Int = 0,
) {
    val isTax: Boolean get() = type == DrainType.STEUER

    fun isLocked(day: Int): Boolean = day < lockedUntilDay

    /** Jede weitere Verhandlung am selben Posten wird unwahrscheinlicher. */
    val negotiationChance: Double get() = (0.85 - 0.25 * negotiations).coerceAtLeast(0.1)

    fun costFor(grossIncome: Cents): Cents =
        if (isTax) (grossIncome * incomeRate).toLong().coerceAtLeast(0) else dailyCost
}

/**
 * Eine Anlage, die für dich arbeitet. Der Preis steigt mit jedem Kauf, damit
 * sich eine einzelne Anlage nicht endlos hochstapeln lässt.
 *
 * @param sellRate Anteil des Kaufpreises, den ein Verkauf zurückbringt. Genau
 *   hier liegt die Abwägung: Die Anlagen mit der besten Rendite sind die, aus
 *   denen du am teuersten wieder rauskommst.
 */
data class Asset(
    val id: String,
    val name: String,
    val description: String,
    val basePrice: Cents,
    val dailyYield: Double,
    val risk: Double,
    val sellRate: Double,
    val owned: Int = 0,
) {
    val nextPrice: Cents get() = priceOfUnit(owned)

    /** Was der zuletzt gekaufte Anteil beim Verkauf einbringt. */
    val sellValue: Cents
        get() = if (owned == 0) 0 else (priceOfUnit(owned - 1) * sellRate).toLong()

    /** Summe, die insgesamt in dieser Anlage steckt — zählt zu deinem Vermögen. */
    fun investedValue(): Cents {
        var total = 0L
        for (i in 0 until owned) {
            total += priceOfUnit(i)
        }
        return total
    }

    fun dailyIncome(): Cents = (investedValue() * dailyYield).toLong()

    private fun priceOfUnit(index: Int): Cents =
        (basePrice * Math.pow(PRICE_GROWTH, index.toDouble())).toLong()

    companion object {
        const val PRICE_GROWTH = 1.35
    }
}

/**
 * Ein dauerhafter Ausbau. Er behält seinen Wert und zählt weiter zum Vermögen —
 * sein Preis ist also nicht das Geld selbst, sondern die Rendite, die dasselbe
 * Geld als Anlage gebracht hätte. Genau das ist die Abwägung.
 */
data class Upgrade(
    val id: String,
    val name: String,
    val description: String,
    val price: Cents,
    val owned: Boolean = false,
)

enum class GameStatus { RUNNING, LOST }

data class LogEntry(val day: Int, val text: String, val tone: Tone) {
    enum class Tone { GOOD, BAD, NEUTRAL }
}

/**
 * Zwei Dinge können dich rauswerfen: Dein Vermögen bleibt unter dem Tagesziel,
 * oder dein Konto läuft leer. Das erste zwingt dich zum Investieren, das zweite
 * verbietet dir, alles zu investieren.
 *
 * @param goal Vermögen (Konto + Anlagen), das der Tag überstehen muss.
 * @param bonusIncome Einnahme aus einem Nebenjob, zählt nur für heute.
 */
data class GameState(
    val day: Int = 1,
    val cash: Cents,
    val goal: Cents,
    val actionPoints: Int,
    val baseInterest: Double,
    val drains: List<Drain>,
    val assets: List<Asset>,
    val upgrades: List<Upgrade> = emptyList(),
    val bonusIncome: Cents = 0,
    val log: List<LogEntry> = emptyList(),
    val status: GameStatus = GameStatus.RUNNING,
    val bestDay: Int = 1,
) {
    val grossIncome: Cents get() = passiveIncome + assetIncome + bonusIncome

    val passiveIncome: Cents get() = (cash * baseInterest).toLong().coerceAtLeast(0)

    /** Die Depot-Optimierung hebt den Ertrag aller Anlagen gleichermaßen. */
    val yieldMultiplier: Double get() = if (hasUpgrade("depot")) 1.15 else 1.0

    val assetIncome: Cents get() = (assets.sumOf { it.dailyIncome() } * yieldMultiplier).toLong()

    val taxCost: Cents get() = drains.filter { it.isTax }.sumOf { it.costFor(grossIncome) }

    val fixedCost: Cents get() = drains.filterNot { it.isTax }.sumOf { it.dailyCost }

    val totalDrain: Cents get() = taxCost + fixedCost

    val netIncome: Cents get() = grossIncome - totalDrain

    fun hasUpgrade(id: String): Boolean = upgrades.any { it.id == id && it.owned }

    /** Was in Anlagen steckt. Zählt zum Vermögen, ist aber nicht liquide. */
    val investedValue: Cents get() = assets.sumOf { it.investedValue() }

    /** Gekaufte Ausbauten behalten ihren Wert, werfen aber nichts ab. */
    val upgradeValue: Cents get() = upgrades.filter { it.owned }.sumOf { it.price }

    val netWorth: Cents get() = cash + investedValue + upgradeValue

    /** Kontostand am Abend, wenn du ab jetzt nichts mehr tust. */
    val projectedCash: Cents get() = cash + netIncome

    /** Vermögen am Abend, wenn du ab jetzt nichts mehr tust. */
    val projectedWorth: Cents get() = netWorth + netIncome

    val onTrack: Boolean get() = projectedWorth >= goal && projectedCash >= 0

    /** Wie viel dir zum Tagesziel noch fehlt (0 wenn du es schaffst). */
    val shortfall: Cents get() = (goal - projectedWorth).coerceAtLeast(0)
}

/** Aufschlüsselung eines abgeschlossenen Tages, damit die UI zeigen kann, was passiert ist. */
data class DayReport(
    val day: Int,
    val gross: Cents,
    val taxes: Cents,
    val fixed: Cents,
    val net: Cents,
    val cashAfter: Cents,
    val worthAfter: Cents,
    val goal: Cents,
    val survived: Boolean,
    val broke: Boolean = false,
)
