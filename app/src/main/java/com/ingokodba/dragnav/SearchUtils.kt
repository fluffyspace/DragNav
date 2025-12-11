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
     * Scores how well a label matches a query using fuzzy word-initial matching.
     *
     * Algorithm:
     * - Splits label into words (at uppercase letters)
     * - Tries matching query starting from each word
     * - Matches query letters in order from the start of each word
     * - Scores based on word position and case matching
     * - Returns the best score from all starting positions
     *
     * @param label The text to match against
     * @param query Search query string
     * @return Score if all query letters matched, null otherwise
     */
    fun scoreMatch(label: String, query: String): Int? {
        if (query.isEmpty()) return 0

        val queryLower = query.map { it.lowercaseChar() }
        val words = label.split(Regex("(?=[A-Z])")).filter { it.isNotEmpty() }

        var bestScore: Int? = null

        // Try starting the match from each word
        for (startWord in words.indices) {
            var count = 0
            var score = 0
            var pos = 0

            for ((i, word) in words.withIndex()) {
                if (i < startWord) continue
                if (pos >= query.length) break

                for (letter in word) {
                    if (letter.lowercaseChar() == queryLower[pos]) {
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

            if (count == query.length) {
                if (bestScore == null || score > bestScore) {
                    bestScore = score
                }
            }
        }

        return bestScore
    }

    /**
     * Fuzzy search that scores apps based on query matching.
     *
     * @param apps List of apps to search
     * @param query Search query string
     * @return List of (score, app) pairs sorted by score descending
     */
    fun getAppsByQuery(apps: List<AppInfo>, query: String): MutableList<Pair<Int, AppInfo>> {
        return apps.mapNotNull { app ->
            scoreMatch(app.label, query)?.let { score ->
                Log.d("SearchUtils", "${app.label} $score")
                Pair(score, app)
            }
        }.toMutableList()
    }
}
