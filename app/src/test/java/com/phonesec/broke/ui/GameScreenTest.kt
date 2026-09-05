package com.phonesec.broke.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phonesec.broke.game.GameStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Das Fenster ist bewusst sehr hoch, damit die Listen vollständig rendern:
 * performScrollToNode kommt unter Robolectric nicht zur Ruhe und blockiert.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h2400dp")
class GameScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun launch(tutorialSeen: Boolean = true): GameViewModel {
        val viewModel = GameViewModel(tutorialSeen = tutorialSeen)
        compose.setContent { BrokeTheme { GameScreen(viewModel) } }
        return viewModel
    }

    @Test
    fun zeigt_den_ersten_tag_mit_vermoegen_und_kosten() {
        launch()

        compose.onNodeWithText("Tag 1").assertIsDisplayed()
        compose.onNodeWithText("Dein Geld").assertIsDisplayed()
        compose.onNodeWithText("Miete für dein Zimmer").assertIsDisplayed()
        compose.onNodeWithText("Steuern").assertIsDisplayed()
    }

    @Test
    fun kuendigen_entfernt_das_abo_aus_der_liste() {
        launch()

        compose.onAllNodesWithText("Nutflix Premium").assertCountEquals(1)

        compose.onNodeWithTag("cancel-streaming").performClick()

        compose.onAllNodesWithText("Nutflix Premium").assertCountEquals(0)
    }

    @Test
    fun tag_abschliessen_zeigt_die_abrechnung_und_zaehlt_weiter() {
        launch()

        compose.onNodeWithTag("end-day").performClick()

        compose.onNodeWithText("Tag 1 geschafft!").assertIsDisplayed()
        compose.onNodeWithText("Weiter").performClick()
        compose.onNodeWithText("Tag 2").assertIsDisplayed()
    }

    @Test
    fun anlagen_tab_zeigt_die_kaufbaren_posten() {
        launch()

        compose.onNodeWithTag("tab-anlagen").performClick()

        compose.onNodeWithText("Tagesgeldkonto").assertIsDisplayed()
        compose.onNodeWithText("Krypto-Bag").assertIsDisplayed()
    }

    @Test
    fun wer_nur_tage_abschliesst_landet_im_broke_screen() {
        val viewModel = launch()

        repeat(25) {
            if (viewModel.state.status != GameStatus.RUNNING) return@repeat
            compose.onNodeWithTag("end-day").performClick()
            val weiter = compose.onAllNodesWithText("Weiter")
            if (weiter.fetchSemanticsNodes().isNotEmpty()) weiter[0].performClick()
        }

        assertTrue(
            "Nichtstun muss innerhalb von 25 Tagen scheitern",
            viewModel.state.status != GameStatus.RUNNING,
        )
        compose.onNodeWithText("BROKE").assertIsDisplayed()
        compose.onNodeWithText("Nochmal").performClick()
        compose.onNodeWithText("Tag 1").assertIsDisplayed()
    }

    @Test
    fun tutorial_startet_beim_ersten_mal_und_fuehrt_durch() {
        val viewModel = launch(tutorialSeen = false)

        compose.onNodeWithTag("tutorial-card").assertIsDisplayed()
        compose.onNodeWithText("Hallo!").assertIsDisplayed()

        compose.onNodeWithTag("tutorial-next").performClick()
        // Über den Fließtext prüfen: Die Überschrift "Dein Geld" steht auch auf der Geldkarte.
        assertEquals(1, viewModel.tutorialStep)
        compose.onNodeWithText(Tutorial.steps[1].body).assertIsDisplayed()

        // Ab dem Anlagen-Schritt muss auch der passende Tab zu sehen sein.
        while (viewModel.tutorialStep != null &&
            Tutorial.steps[viewModel.tutorialStep!!].title != "Geld anlegen"
        ) {
            compose.onNodeWithTag("tutorial-next").performClick()
        }
        compose.onNodeWithText("Tagesgeldkonto").assertIsDisplayed()
    }

    @Test
    fun tutorial_laesst_sich_ueberspringen_und_wieder_oeffnen() {
        val viewModel = launch(tutorialSeen = false)

        compose.onNodeWithTag("tutorial-skip").performClick()
        assertNull(viewModel.tutorialStep)
        compose.onAllNodesWithText("Hallo!").assertCountEquals(0)

        compose.onNodeWithTag("help").performClick()
        assertEquals(0, viewModel.tutorialStep)
        compose.onNodeWithText("Hallo!").assertIsDisplayed()
    }

    @Test
    fun anlage_kaufen_und_wieder_verkaufen() {
        val viewModel = launch()
        compose.onNodeWithTag("tab-anlagen").performClick()

        val cashBefore = viewModel.state.cash
        compose.onNodeWithTag("buy-tagesgeld").performClick()
        assertTrue(viewModel.state.cash < cashBefore)
        assertEquals(1, viewModel.state.assets.first { it.id == "tagesgeld" }.owned)

        compose.onNodeWithTag("sell-tagesgeld").performClick()
        assertEquals(0, viewModel.state.assets.first { it.id == "tagesgeld" }.owned)
        assertEquals("Tagesgeld ist voll liquide", cashBefore, viewModel.state.cash)
    }

    @Test
    fun ausbau_tab_zeigt_und_kauft_ausbauten() {
        val viewModel = launch()
        compose.onNodeWithTag("tab-ausbau").performClick()

        compose.onNodeWithText("Netzwerk").assertIsDisplayed()
        compose.onNodeWithTag("upgrade-netzwerk").performClick()

        assertTrue(viewModel.state.hasUpgrade("netzwerk"))
        compose.onNodeWithText("hast du").assertIsDisplayed()
    }
}
