package com.example.unipazar

import java.text.NumberFormat
import java.util.Locale

object PriceFormatter {
    fun format(priceStr: String): String {
        val clean = priceStr.replace("[^\\d]".toRegex(), "")
        if (clean.isEmpty()) return priceStr
        return try {
            val num = clean.toLong()
            val format = NumberFormat.getNumberInstance(Locale("tr", "TR"))
            format.format(num)
        } catch (e: Exception) {
            priceStr
        }
    }
}
