package com.phonesec.broke.ui

/**
 * Ein geführter Durchlauf für den ersten Start. Jeder Schritt schaltet auf den
 * Tab, über den er spricht — erklärter Text ohne den passenden Bildschirm
 * darunter bleibt sonst abstrakt.
 *
 * @param tab Tab, der während dieses Schritts sichtbar sein soll (0 Kosten,
 *   1 Anlagen, 2 Ausbau, 3 Verlauf).
 */
data class TutorialStep(
    val title: String,
    val body: String,
    val tab: Int,
)

object Tutorial {

    val steps: List<TutorialStep> = listOf(
        TutorialStep(
            tab = 0,
            title = "Worum es geht",
            body = "Dein Geld verdient Geld. Gleichzeitig ziehen Abos, Fixkosten " +
                "und Steuern jeden Tag daran. Überlebst du das lange genug?",
        ),
        TutorialStep(
            tab = 0,
            title = "Das Tagesziel",
            body = "Oben siehst du dein Vermögen und darunter das Tagesziel. Am " +
                "Abend muss dein Vermögen darüber liegen — sonst ist Schluss. " +
                "Und das Ziel steigt jeden Tag, immer schneller.",
        ),
        TutorialStep(
            tab = 0,
            title = "Zwei Wege zu verlieren",
            body = "Das Ziel verfehlen ist der eine. Der andere: ein leeres Konto. " +
                "Rechnungen werden aus dem liquiden Geld bezahlt, nicht aus " +
                "Anlagen. Leg also nie alles an.",
        ),
        TutorialStep(
            tab = 0,
            title = "Deine Kostenliste",
            body = "Das hier zieht dir täglich Geld ab. Kündigen wird sie los, " +
                "kostet aber manchmal eine Gebühr. Verhandeln senkt nur die " +
                "Kosten, klappt dafür nicht immer.",
        ),
        TutorialStep(
            tab = 0,
            title = "Drei Aktionen pro Tag",
            body = "Die Punkte oben rechts sind dein Budget an Handlungen. " +
                "Kündigen, verhandeln und Nebenjob kosten je eine. Überleg dir " +
                "gut, worauf du sie setzt.",
        ),
        TutorialStep(
            tab = 0,
            title = "Die Steuer wächst mit",
            body = "Fixkosten werden irgendwann klein gegen dein Vermögen — die " +
                "Steuer nicht, sie nimmt einen Anteil deiner Einnahmen und " +
                "klettert täglich. Gegen sie musst du dauerhaft anverhandeln.",
        ),
        TutorialStep(
            tab = 1,
            title = "Anlagen",
            body = "Hier arbeitet dein Geld. Bargeld bringt fast nichts, also muss " +
                "der Überschuss hier rein. Käufe kosten keine Aktion, nur Geld.",
        ),
        TutorialStep(
            tab = 1,
            title = "Rendite gegen Liquidität",
            body = "Es gibt keine beste Anlage. Was am meisten abwirft, ist riskant " +
                "oder lässt sich nur mit Verlust wieder verkaufen. Tagesgeld " +
                "zahlt mager, dafür jederzeit voll aus.",
        ),
        TutorialStep(
            tab = 2,
            title = "Ausbauten",
            body = "Diese kosten viel und bringen keine Rendite — sie ändern " +
                "Spielregeln. Die Frage ist immer: lieber jetzt Ertrag, oder " +
                "später bessere Bedingungen?",
        ),
        TutorialStep(
            tab = 0,
            title = "Los geht's",
            body = "Wenn du fertig bist, schließt du den Tag unten ab. Das Tutorial " +
                "erreichst du jederzeit wieder über das Fragezeichen oben.",
        ),
    )
}
