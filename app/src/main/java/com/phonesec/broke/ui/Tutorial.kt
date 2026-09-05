package com.phonesec.broke.ui

/**
 * Ein geführter Durchlauf für den ersten Start. Jeder Schritt schaltet auf den
 * Tab, über den er spricht — erklärter Text ohne den passenden Bildschirm
 * darunter bleibt sonst abstrakt.
 *
 * @param tab Tab, der während dieses Schritts sichtbar sein soll (0 Ausgaben,
 *   1 Anlegen, 2 Extras, 3 Verlauf).
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
            title = "Hallo!",
            body = "In diesem Spiel passt du auf dein Geld auf. " +
                "Jeden Tag kommt Geld dazu — und jeden Tag geht welches weg. " +
                "Mal sehen, wie lange du durchhältst.",
        ),
        TutorialStep(
            tab = 0,
            title = "Dein Geld",
            body = "Ganz oben siehst du, wie viel Geld du hast. " +
                "Darunter steht, wie viel du heute brauchst. " +
                "Der Balken zeigt dir, ob es reicht: grün ist gut, rot ist schlecht.",
        ),
        TutorialStep(
            tab = 0,
            title = "Jeden Tag mehr",
            body = "Morgen brauchst du schon ein bisschen mehr als heute. " +
                "Und übermorgen noch mehr. Dein Geld muss also wachsen.",
        ),
        TutorialStep(
            tab = 0,
            title = "Zwei Sachen können schiefgehen",
            body = "Erstens: Du hast am Abend zu wenig Geld. " +
                "Zweitens: Dein Konto ist leer. Rechnungen zahlst du nämlich " +
                "vom Konto — nicht von dem, was du angelegt hast.",
        ),
        TutorialStep(
            tab = 0,
            title = "Was dir Geld wegnimmt",
            body = "Diese Liste kostet dich jeden Tag Geld. " +
                "\"Weg damit\" wird es für immer los. " +
                "\"Billiger machen\" macht es nur günstiger, klappt aber nicht immer.",
        ),
        TutorialStep(
            tab = 0,
            title = "Drei Sachen pro Tag",
            body = "Oben rechts siehst du grüne Punkte. So viele Sachen darfst du " +
                "heute noch machen. Sind sie weg, musst du den Tag beenden.",
        ),
        TutorialStep(
            tab = 0,
            title = "Der Tipp hilft dir",
            body = "Oben in jeder Liste steht ein Tipp. Der sagt dir immer, " +
                "was gerade am wichtigsten ist. Wenn du nicht weiterweißt: lies den Tipp.",
        ),
        TutorialStep(
            tab = 1,
            title = "Geld anlegen",
            body = "Geld auf dem Konto bringt fast nichts. Hier kannst du es anlegen, " +
                "dann verdient es jeden Tag etwas für dich. Das kostet keinen Punkt, nur Geld.",
        ),
        TutorialStep(
            tab = 1,
            title = "Sicher oder mutig?",
            body = "Manche Anlagen bringen viel, machen aber auch mal Verluste — " +
                "und beim Verkaufen bekommst du weniger zurück. " +
                "Das Tagesgeldkonto bringt wenig, ist dafür immer sicher.",
        ),
        TutorialStep(
            tab = 2,
            title = "Extras",
            body = "Extras bringen dir kein Geld. Sie machen das Spiel leichter, " +
                "zum Beispiel eine Sache mehr pro Tag. Dein Geld bleibt dabei erhalten.",
        ),
        TutorialStep(
            tab = 0,
            title = "Fertig!",
            body = "Wenn du alles gemacht hast, tippst du unten auf den großen Knopf. " +
                "Das Fragezeichen oben zeigt dir diese Erklärung jederzeit wieder.",
        ),
    )
}
