package com.phonesec.broke.game

import kotlin.math.abs

/** "1.234,56 €" — deutsche Schreibweise, ohne Abhängigkeit von der Geräte-Locale. */
fun Cents.asEuro(): String {
    val negative = this < 0
    val whole = abs(this) / 100
    val fraction = abs(this) % 100
    val grouped = whole.toString().reversed().chunked(3).joinToString(".").reversed()
    val sign = if (negative) "-" else ""
    return "$sign$grouped,${fraction.toString().padStart(2, '0')} €"
}

/** Kompakt für enge Stellen: "12,3k €" statt "12.340,00 €". */
fun Cents.asEuroShort(): String {
    val euros = this / 100.0
    val sign = if (euros < 0) "-" else ""
    val value = abs(euros)
    return when {
        value >= 1_000_000 -> "$sign${(value / 1_000_000).format1()}M €"
        value >= 1_000 -> "$sign${(value / 1_000).format1()}k €"
        else -> "$sign${value.toLong()} €"
    }
}

fun Double.format1(): String {
    val rounded = Math.round(this * 10) / 10.0
    val whole = rounded.toLong()
    val decimal = Math.round(abs(rounded - whole) * 10)
    return "$whole,$decimal"
}

fun Double.asPercent(): String = "${(this * 100).format1()} %"
