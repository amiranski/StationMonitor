package com.amiran.stationmonitor.ui.stations

import com.amiran.stationmonitor.domain.model.Station
import com.amiran.stationmonitor.testutil.FIXED_NOW
import com.amiran.stationmonitor.ui.common.ListUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class StationFilterTest {

    private val chodov = Station("1", "Praha – Chodov", "Roztylská 2321/19, Praha 11", "CZ", 3)
    private val smichov = Station("2", "Praha – Smíchov", "Strakonická 1199/2a, Praha 5", "CZ", 6)
    private val zlin = Station("3", "Zlín – Nová stanice", "třída Tomáše Bati 5, Zlín", "CZ", 0)
    private val petrzalka = Station("4", "Bratislava – Petržalka", "Panónska cesta 2", "SK", 4)
    private val lodz = Station("5", "Łódź – Centrum", "ul. Piotrkowska 100", "PL", 2)
    private val all = listOf(chodov, smichov, zlin, petrzalka, lodz)

    private fun idsMatching(query: String = "", countries: Set<String> = emptySet()) =
        all.matching(StationFilter(query, countries)).map { it.id }

    @Test
    fun `no filter keeps every station in the original order`() {
        assertSame(all, all.matching(StationFilter()))
        assertFalse(StationFilter(query = "   ").isActive)
    }

    @Test
    fun `query ignores case and diacritics`() {
        assertEquals(listOf("3"), idsMatching("ZLIN"))
        assertEquals(listOf("2"), idsMatching("smichov"))
        assertEquals(listOf("5"), idsMatching("lodz"))
    }

    @Test
    fun `query also searches the address`() {
        assertEquals(listOf("2"), idsMatching("strakonicka"))
    }

    @Test
    fun `every word of the query has to match`() {
        assertEquals(listOf("1", "2"), idsMatching("praha"))
        assertEquals(listOf("2"), idsMatching("  praha   smichov "))
        assertEquals(emptyList<String>(), idsMatching("praha brno"))
    }

    @Test
    fun `country filter combines with the query`() {
        assertEquals(listOf("4"), idsMatching(countries = setOf("SK")))
        assertEquals(listOf("4", "5"), idsMatching(countries = setOf("SK", "PL")))
        assertEquals(emptyList<String>(), idsMatching(query = "praha", countries = setOf("SK")))
    }

    @Test
    fun `country options are sorted by size and keep counts of the whole cache`() {
        val options = all.countryOptions(selected = setOf("SK"))

        assertEquals(
            listOf(
                CountryOption("CZ", 3, isSelected = false),
                CountryOption("PL", 1, isSelected = false),
                CountryOption("SK", 1, isSelected = true),
            ),
            options,
        )
    }

    @Test
    fun `only the Content state is filtered`() {
        val content = ListUiState.Content(all, FIXED_NOW, isRefreshing = false, isStale = false)
        val filter = StationFilter(query = "praha")

        assertEquals(listOf(chodov, smichov), (content.filteredBy(filter) as ListUiState.Content).items)
        assertSame(ListUiState.Loading, ListUiState.Loading.filteredBy(filter))
    }
}
