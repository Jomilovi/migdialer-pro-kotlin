package com.migdialer.pro.utils

/**
 * T9 keyboard mapping — pressing a digit searches contacts
 * by the letters on that key, exactly like a physical phone keypad.
 *
 * 2 → ABC    3 → DEF    4 → GHI
 * 5 → JKL    6 → MNO    7 → PQRS
 * 8 → TUV    9 → WXYZ   0,1,*,# → digit itself
 */
object T9 {

    private val MAP = mapOf(
        '2' to "abc",
        '3' to "def",
        '4' to "ghi",
        '5' to "jkl",
        '6' to "mno",
        '7' to "pqrs",
        '8' to "tuv",
        '9' to "wxyz",
    )

    /**
     * Returns all possible letter combinations for a digit string.
     * E.g. "26" → combinations of [a,b,c] × [m,n,o] = "am","an","ao","bm",...
     * Used to match contact names.
     */
    fun lettersForDigit(digit: Char): String = MAP[digit] ?: digit.toString()

    /**
     * Check if a contact name matches a T9 digit sequence.
     * E.g. digits="262" matches "Ana", "Bob", "Cob", etc.
     */
    fun nameMatchesT9(name: String, digits: String): Boolean {
        if (digits.isEmpty()) return true
        val nameLower = name.lowercase().filter { it.isLetter() || it.isWhitespace() }
        val words = nameLower.split(" ").filter { it.isNotEmpty() }

        // Try matching from start of any word
        for (word in words) {
            if (wordMatchesT9(word, digits)) return true
        }
        // Also try matching the full name without spaces
        if (wordMatchesT9(nameLower.replace(" ", ""), digits)) return true
        return false
    }

    private fun wordMatchesT9(word: String, digits: String): Boolean {
        if (digits.length > word.length) return false
        for (i in digits.indices) {
            val digit = digits[i]
            val letter = word.getOrNull(i) ?: return false
            val validLetters = MAP[digit]
            if (validLetters != null) {
                // Letter must be in the set for this digit
                if (letter !in validLetters) return false
            } else {
                // No mapping (1, 0, *, #) — match digit directly
                if (letter.toString() != digit.toString()) return false
            }
        }
        return true
    }

    /**
     * Check if a phone number contains the digit sequence directly.
     */
    fun numberMatchesDigits(number: String, digits: String): Boolean {
        val clean = number.replace(Regex("[^0-9+*#]"), "")
        return clean.contains(digits)
    }
}
