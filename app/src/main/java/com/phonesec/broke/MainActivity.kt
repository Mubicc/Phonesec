package com.phonesec.broke

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.phonesec.broke.ui.BrokeTheme
import com.phonesec.broke.ui.GameScreen
import com.phonesec.broke.ui.GameViewModel

private const val PREFS = "broke_prefs"
private const val KEY_BEST_DAY = "best_day"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        setContent {
            BrokeTheme {
                val gameViewModel: GameViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            GameViewModel(
                                initialBestDay = prefs.getInt(KEY_BEST_DAY, 1),
                                onBestDay = { day ->
                                    if (day > prefs.getInt(KEY_BEST_DAY, 1)) {
                                        prefs.edit().putInt(KEY_BEST_DAY, day).apply()
                                    }
                                },
                            )
                        }
                    }
                )
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    GameScreen(gameViewModel)
                }
            }
        }
    }
}
