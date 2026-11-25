package com.amiran.stationmonitor.ui.stations

import com.amiran.stationmonitor.domain.model.Station
import com.amiran.stationmonitor.ui.common.ListUiState
import java.text.Normalizer
import java.util.Locale

/**
 * What the user narrowed the station list down to.
 *
 * @param query free text, matched against name and address (see [matching]).
 * @param countries selected country codes; empty means "all countries".
 */
data class StationFilter(
    val query: String = "",
    val countries: Set<String> = emptySet(),
) {
    val isActive: Boolean get() = query.isNotBlank() || countries.isNotEmpty()
}

/** One country chip: the code, how many cached stations it has, and whether it is selected. */
data class CountryOption(
    val code: String,
    val stationCount: Int,
    val isSelected: Boolean,
)

/*
 * Filtering runs in memory, in the ViewModel, not as a Room query:
 * - 147 stations are filtered in well under a millisecond,
 * - SQLite LIKE cannot ignore diacritics ("zlin" would not find "Zlín"),
 * - the repository and the database stay unchanged.
 * With thousands of stations the next step would be Room FTS or a server-side search.
 */

/**
 * Stations that pass [stationFilter]. Every word of the query has to appear in the name or the
 * address, ignoring case and diacritics: "praha smichov" finds "Praha – Smíchov".
 */
fun List<Station>.matching(stationFilter: StationFilter): List<Station> {
    if (!stationFilter.isActive) return this
    val words = stationFilter.query.normalizedForSearch()
        .split(WHITESPACE)
        .filter { it.isNotEmpty() }
    return this.filter { station ->
        val inSelectedCountry = stationFilter.countries.isEmpty() || station.country in stationFilter.countries
        inSelectedCountry && words.all { word -> station.searchableText().contains(word) }
    }
}

/** Applies the filter to the visible items; loading, empty and error states stay as they are. */
fun ListUiState<Station>.filteredBy(stationFilter: StationFilter): ListUiState<Station> =
    if (this is ListUiState.Content) copy(items = items.matching(stationFilter)) else this

/**
 * One chip per country in the cache, biggest first. Counts are taken from the whole cache, not
 * from the filtered list, so the chips do not change while the user types.
 */
fun List<Station>.countryOptions(selected: Set<String>): List<CountryOption> =
    groupingBy { it.country }
        .eachCount()
        .map { (code, count) -> CountryOption(code, count, isSelected = code in selected) }
        .sortedWith(compareByDescending<CountryOption> { it.stationCount }.thenBy { it.code })

private fun Station.searchableText(): String = "$name $address".normalizedForSearch()

/**
 * Lowercase text without diacritics: "Zlín – Nová" -> "zlin – nova".
 * NFD splits "í" into "i" + a combining accent, which the regex then removes. A few letters
 * have no such split form and are replaced by hand (Polish "ł", German "ß").
 */
internal fun String.normalizedForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .lowercase(Locale.ROOT)
        .replace("ł", "l")
        .replace("ß", "ss")
        .trim()

private val COMBINING_MARKS = Regex("\\p{Mn}+")
private val WHITESPACE = Regex("\\s+")
