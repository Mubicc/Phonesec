package com.phonesec.broke.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phonesec.broke.game.GameStatus
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class GameScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun launch(): GameViewModel {
        val viewModel = GameViewModel()
        compose.setContent { BrokeTheme { GameScreen(viewModel) } }
        return viewModel
    }

    @Test
    fun zeigt_den_ersten_tag_mit_vermoegen_und_kosten() {
        launch()

        compose.onNodeWithText("Tag 1").assertIsDisplayed()
        compose.onNodeWithText("Vermögen").assertIsDisplayed()

        compose.onNodeWithTag("drain-list").performScrollToNode(hasText("WG-Zimmer"))
        compose.onNodeWithText("WG-Zimmer").assertIsDisplayed()

        compose.onNodeWithTag("drain-list").performScrollToNode(hasText("Einkommensteuer"))
        compose.onNodeWithText("Einkommensteuer").assertIsDisplayed()
    }

    @Test
    fun kuendigen_entfernt_das_abo_aus_der_liste() {
        launch()

        compose.onNodeWithTag("drain-list").performScrollToNode(hasText("Nutflix Premium"))
        compose.onAllNodesWithText("Nutflix Premium").assertCountEquals(1)

        compose.onNodeWithTag("cancel-streaming").performClick()

        compose.onAllNodesWithText("Nutflix Premium").assertCountEquals(0)
    }

    @Test
    fun tag_abschliessen_zeigt_die_abrechnung_und_zaehlt_weiter() {
        launch()

        compose.onNodeWithTag("end-day").performClick()

        compose.onNodeWithText("Tag 1 abgerechnet").assertIsDisplayed()
        compose.onNodeWithText("Weiter").performClick()
        compose.onNodeWithText("Tag 2").assertIsDisplayed()
    }

    @Test
    fun anlagen_tab_zeigt_die_kaufbaren_posten() {
        launch()

        compose.onNodeWithTag("tab-anlagen").performClick()

        compose.onNodeWithTag("asset-list").performScrollToNode(hasText("Tagesgeldkonto"))
        compose.onNodeWithText("Tagesgeldkonto").assertIsDisplayed()

        compose.onNodeWithTag("asset-list").performScrollToNode(hasText("Krypto-Bag"))
        compose.onNodeWithText("Krypto-Bag").assertIsDisplayed()
    }

    @Test
    fun wer_nur_tage_abschliesst_landet_im_broke_screen() {
        val viewModel = launch()

        // Passives Durchklicken verliert nach wenigen Tagen — genau das prüfen wir.
        repeat(40) {
            if (viewModel.state.status != GameStatus.RUNNING) return@repeat
            compose.onNodeWithTag("end-day").performClick()
            val weiter = compose.onAllNodesWithText("Weiter")
            if (weiter.fetchSemanticsNodes().isNotEmpty()) weiter[0].performClick()
        }

        assertTrue(
            "Nichtstun muss innerhalb von 40 Tagen scheitern",
            viewModel.state.status != GameStatus.RUNNING,
        )
        compose.onNodeWithText("BROKE").assertIsDisplayed()
        compose.onNodeWithText("Nochmal").performClick()
        compose.onNodeWithText("Tag 1").assertIsDisplayed()
    }
}
