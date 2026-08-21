package com.archieapps.calendar.core.net

object AccessCode {
    const val HEADER = "code"

    const val LENGTH = 8

    @Volatile
    private var code: String? = null

    val present: Boolean
        get() = code != null

    fun header(): String? = code

    fun remember(value: String) {
        code = value.takeIf { it.length == LENGTH }
    }

    fun forget() {
        code = null
    }

    fun pairs(): List<Pair<Int, Int>> {
        val digits = (0..9).shuffled()

        return (0 until 10 step 2).map { digits[it] to digits[it + 1] }
    }
}
