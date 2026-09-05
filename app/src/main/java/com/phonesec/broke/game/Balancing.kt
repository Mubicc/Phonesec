package com.phonesec.broke.game

object Balancing {
    const val START_CASH: Cents = 500_000L

    /**
     * Das Vermögen, das Tag 1 verlangt. Bewusst unter dem Startkapital: Der
     * Abstand ist dein Spielraum — und den frisst das wachsende Ziel Tag für Tag auf.
     */
    const val START_GOAL: Cents = 350_000L
    const val START_INTEREST = 0.02

    const val ACTION_POINTS_PER_DAY = 3

    /**
     * Das Tagesziel zieht exponentiell an — und die Rate selbst wächst mit. Ohne
     * diese Beschleunigung könnte perfektes Spiel die Kurve dauerhaft überholen
     * und das Spiel würde nie enden.
     */
    const val GOAL_GROWTH = 1.055
    const val GOAL_ACCEL = 0.0006
    const val GOAL_FLAT: Cents = 500L

    const val MIN_INTEREST = 0.005

    /**
     * Der Steuersatz kriecht jeden Tag ein Stück nach oben. Fixkosten verlieren mit
     * wachsendem Vermögen an Gewicht — die Steuer nicht, sie ist der Gegenspieler,
     * gegen den du dauerhaft anverhandeln musst.
     */
    const val TAX_CREEP_PER_DAY = 0.004
    const val MAX_TAX_RATE = 0.75

    /** Anteil des Tagesertrags, den eine riskante Anlage an einem schlechten Tag frisst. */
    const val RISK_LOSS_MIN = 0.3
    const val RISK_LOSS_SPAN = 0.6

    fun goalForNextDay(currentGoal: Cents, day: Int): Cents =
        (currentGoal * (GOAL_GROWTH + day * GOAL_ACCEL)).toLong() + GOAL_FLAT

    fun startingDrains(): List<Drain> = listOf(
        Drain(
            id = "miete",
            name = "Miete für dein Zimmer",
            type = DrainType.FIXKOSTEN,
            dailyCost = 4_500L,
            cancellable = false,
        ),
        Drain(
            id = "steuer",
            name = "Steuern",
            type = DrainType.STEUER,
            incomeRate = 0.25,
            cancellable = false,
        ),
        Drain(
            id = "streaming",
            name = "Nutflix Premium",
            type = DrainType.ABO,
            dailyCost = 60L,
        ),
        Drain(
            id = "gym",
            name = "Fitnessstudio",
            type = DrainType.ABO,
            dailyCost = 130L,
            cancelFee = 2_500L,
            lockedUntilDay = 4,
        ),
        Drain(
            id = "handy",
            name = "Handyvertrag",
            type = DrainType.FIXKOSTEN,
            dailyCost = 120L,
            cancelFee = 1_000L,
        ),
    )

    /**
     * Rendite, Risiko und Liquidität ziehen bewusst gegeneinander: Was am besten
     * verzinst, lässt sich am schlechtesten wieder zu Geld machen. Es gibt daher
     * keine Anlage, die immer richtig ist.
     */
    fun startingAssets(): List<Asset> = listOf(
        Asset(
            id = "tagesgeld",
            name = "Tagesgeldkonto",
            description = "Wie ein Sparschwein bei der Bank.",
            basePrice = 50_000L,
            dailyYield = 0.065,
            risk = 0.0,
            sellRate = 1.0,
        ),
        Asset(
            id = "etf",
            name = "ETF-Sparplan",
            description = "Du kaufst kleine Anteile an vielen Firmen.",
            basePrice = 150_000L,
            dailyYield = 0.12,
            risk = 0.15,
            sellRate = 0.9,
        ),
        Asset(
            id = "krypto",
            name = "Krypto-Bag",
            description = "Digitales Geld. Mal geht es hoch, mal runter.",
            basePrice = 90_000L,
            dailyYield = 0.22,
            risk = 0.35,
            sellRate = 0.7,
        ),
        Asset(
            id = "immo",
            name = "Eigentumswohnung",
            description = "Eine Wohnung, die du vermietest.",
            basePrice = 900_000L,
            dailyYield = 0.135,
            risk = 0.05,
            sellRate = 0.55,
        ),
    )

    fun startingUpgrades(): List<Upgrade> = listOf(
        Upgrade(
            id = "steuerberater",
            name = "Steuerberater",
            description = "Die Steuern steigen nur noch halb so schnell.",
            price = 400_000L,
        ),
        Upgrade(
            id = "assistenz",
            name = "Assistenz",
            description = "Du darfst jeden Tag eine Sache mehr machen.",
            price = 900_000L,
        ),
        Upgrade(
            id = "notgroschen",
            name = "Risikopuffer",
            description = "Schlechte Tage tun nur noch halb so weh.",
            price = 650_000L,
        ),
        Upgrade(
            id = "netzwerk",
            name = "Netzwerk",
            description = "\"Billiger machen\" klappt viel öfter.",
            price = 300_000L,
        ),
        Upgrade(
            id = "depot",
            name = "Depot-Optimierung",
            description = "Alles, was du angelegt hast, bringt mehr Geld.",
            price = 1_500_000L,
        ),
    )

    fun newGame(): GameState = GameState(
        day = 1,
        cash = START_CASH,
        goal = START_GOAL,
        actionPoints = ACTION_POINTS_PER_DAY,
        baseInterest = START_INTEREST,
        drains = startingDrains(),
        assets = startingAssets(),
        upgrades = startingUpgrades(),
        log = listOf(
            LogEntry(1, "Tag 1. Los geht's!", LogEntry.Tone.NEUTRAL),
        ),
    )
}
