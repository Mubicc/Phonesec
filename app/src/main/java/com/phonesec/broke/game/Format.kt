package com.phonesec.broke.game

import kotlin.math.abs

/**
 * "1.234,56 €" — deutsche Schreibweise, ohne Abhängigkeit von der Geräte-Locale.
 *
 * Das Spiel zeigt bewusst nur Eurobeträge und keine Prozentsätze: Ein Kind kann
 * "bringt 37,50 € am Tag" sofort einordnen, "7,5 % Rendite" nicht.
 */
fun Cents.asEuro(): String {
    val negative = this < 0
    val whole = abs(this) / 100
    val fraction = abs(this) % 100
    val grouped = whole.toString().reversed().chunked(3).joinToString(".").reversed()
    val sign = if (negative) "-" else ""
    return "$sign$grouped,${fraction.toString().padStart(2, '0')} €"
}
