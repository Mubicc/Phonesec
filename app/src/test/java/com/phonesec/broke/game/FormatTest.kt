package com.phonesec.broke.game

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun `betraege werden deutsch formatiert`() {
        assertEquals("0,00 €", 0L.asEuro())
        assertEquals("1,05 €", 105L.asEuro())
        assertEquals("999,99 €", 99_999L.asEuro())
        assertEquals("1.000,00 €", 100_000L.asEuro())
        assertEquals("1.234.567,89 €", 123_456_789L.asEuro())
    }

    @Test
    fun `negative betraege behalten das vorzeichen`() {
        assertEquals("-45,00 €", (-4_500L).asEuro())
        assertEquals("-1.000,00 €", (-100_000L).asEuro())
    }
}
