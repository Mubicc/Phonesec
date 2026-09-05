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
 */
data class Asset(
    val id: String,
    val name: String,
    val description: String,
    val basePrice: Cents,
    val dailyYield: Double,
    val risk: Double,
    val owned: Int = 0,
) {
    val nextPrice: Cents get() = (basePrice * Math.pow(PRICE_GROWTH, owned.toDouble())).toLong()

    /** Summe, die insgesamt in dieser Anlage steckt — zählt zu deinem Vermögen. */
    fun investedValue(): Cents {
        var total = 0L
        for (i in 0 until owned) {
            total += (basePrice * Math.pow(PRICE_GROWTH, i.toDouble())).toLong()
        }
        return total
    }

    fun dailyIncome(): Cents = (investedValue() * dailyYield).toLong()

    companion object {
        const val PRICE_GROWTH = 1.35
    }
}

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
    val bonusIncome: Cents = 0,
    val log: List<LogEntry> = emptyList(),
    val status: GameStatus = GameStatus.RUNNING,
    val bestDay: Int = 1,
) {
    val grossIncome: Cents get() = passiveIncome + assetIncome + bonusIncome

    val passiveIncome: Cents get() = (cash * baseInterest).toLong().coerceAtLeast(0)

    val assetIncome: Cents get() = assets.sumOf { it.dailyIncome() }

    val taxCost: Cents get() = drains.filter { it.isTax }.sumOf { it.costFor(grossIncome) }

    val fixedCost: Cents get() = drains.filterNot { it.isTax }.sumOf { it.dailyCost }

    val totalDrain: Cents get() = taxCost + fixedCost

    val netIncome: Cents get() = grossIncome - totalDrain

    /** Was in Anlagen steckt. Zählt zum Vermögen, ist aber nicht liquide. */
    val investedValue: Cents get() = assets.sumOf { it.investedValue() }

    val netWorth: Cents get() = cash + investedValue

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
