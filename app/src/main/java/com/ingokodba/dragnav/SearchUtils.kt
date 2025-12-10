package com.ingokodba.dragnav

import android.util.Log
import com.ingokodba.dragnav.modeli.AppInfo

/**
 * SearchUtils - Utility functions for app search functionality
 *
 * Extracted from SearchFragment to be reusable across the codebase.
 */
object SearchUtils {

    /**
     * Fuzzy search algorithm that scores apps based on query matching
     *
     * Algorithm:
     * - Splits app label into words (at uppercase letters)
     * - Matches query letters in order
     * - Scores based on word position and case matching
     * - Returns list of apps that match all query letters
     *
     * @param apps List of apps to search
     * @param query Search query string
     * @return List of (score, app) pairs sorted by score descending
     */
    fun getAppsByQuery(apps: List<AppInfo>, query: String): MutableList<Pair<Int, AppInfo>> {
        val search_lista_aplikacija: MutableList<Pair<Int, AppInfo>> = mutableListOf()
        val slova_search_lowercase = query.map { it.lowercaseChar() }

        for (app in apps) {
            // Split app name into words (at uppercase letters)
            var words = app.label.split(Regex("(?=[A-Z])"), 0)
            words = words.filter { it != "" }

            var count = 0
            var score = 0
            var pos = 0

            // Try to match query letters in order
            for ((i, word) in words.withIndex()) {
                if (pos >= query.length) break

                // Check each letter in the word
                for (letter in word) {
                    if (letter.lowercaseChar() == slova_search_lowercase[pos]) {
                        count++
                        score += 10 - i  // Earlier words score higher
                        if (letter.isUpperCase()) score += 10  // Uppercase matches score higher
                        pos++
                        if (pos >= query.length) break
                    } else {
                        break
                    }
                }
            }

            // Add to results if all query letters matched
            if (count == query.length) {
                search_lista_aplikacija.add(Pair(score, app))
                Log.d("SearchUtils", "${app.label} $count $score $words")
            }
        }

        return search_lista_aplikacija
    }
}
