package no.carlorrall.fjordvaer.data

import no.carlorrall.fjordvaer.data.grib.GribParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class GribParserTest {

    @Test
    fun parsesMetsTimeUnits() {
        val (unit, reference) = GribParser.parseTimeUnits("Hour since 2026-09-29T06:00:00Z")!!
        assertEquals(3_600_000L, unit)
        assertEquals(Instant.parse("2026-09-29T06:00:00Z").toEpochMilli(), reference)
    }

    @Test
    fun acceptsPluralUnitsAndSpaceSeparatedDates() {
        val (unit, reference) = GribParser.parseTimeUnits("minutes since 2026-09-29 06:00:00")!!
        assertEquals(60_000L, unit)
        assertEquals(Instant.parse("2026-09-29T06:00:00Z").toEpochMilli(), reference)
    }

    @Test
    fun unknownUnitsReturnNull() {
        assertNull(GribParser.parseTimeUnits("fortnights since 2026-09-29T06:00:00Z"))
        assertNull(GribParser.parseTimeUnits(null))
    }
}
