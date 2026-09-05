package com.phonesec.broke.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.phonesec.broke.game.DayReport
import com.phonesec.broke.game.GameEngine
import com.phonesec.broke.game.GameState
import kotlin.random.Random

class GameViewModel(
    initialBestDay: Int = 1,
    private val onBestDay: (Int) -> Unit = {},
) : ViewModel() {

    private val rng = Random(System.currentTimeMillis())

    var state by mutableStateOf(GameEngine.newGame(initialBestDay))
        private set

    /** Bericht des zuletzt abgerechneten Tages; die UI zeigt ihn als Dialog. */
    var lastReport by mutableStateOf<DayReport?>(null)
        private set

    /** Kurze Rückmeldung, wenn eine Aktion nicht möglich war. */
    var message by mutableStateOf<String?>(null)
        private set

    fun cancelDrain(id: String) = apply(GameEngine.cancel(state, id))

    fun negotiate(id: String) = apply(GameEngine.negotiate(state, id, rng))

    fun buyAsset(id: String) = apply(GameEngine.buyAsset(state, id))

    fun sideGig() = apply(GameEngine.sideGig(state, rng))

    fun endDay() {
        val (next, report) = GameEngine.endDay(state, rng)
        state = next
        lastReport = report
        if (next.bestDay > 1) onBestDay(next.bestDay)
    }

    fun dismissReport() {
        lastReport = null
    }

    fun dismissMessage() {
        message = null
    }

    fun restart() {
        state = GameEngine.newGame(state.bestDay)
        lastReport = null
        message = null
    }

    private fun apply(outcome: GameEngine.Outcome) {
        when (outcome) {
            is GameEngine.Outcome.Ok -> state = outcome.state
            is GameEngine.Outcome.Rejected -> message = outcome.reason
        }
    }
}
