package com.example.unipazar

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {
    fun getTimeAgo(time: Long): String {
        if (time < 1000000000000L) {
            return getTimeAgo(time * 1000)
        }
        val now = System.currentTimeMillis()
        if (time > now || time <= 0) return "Az önce"
        val diff = now - time
        val minute = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hour = TimeUnit.MILLISECONDS.toHours(diff)
        val day = TimeUnit.MILLISECONDS.toDays(diff)
        return when {
            minute < 1 -> "Az önce"
            minute < 60 -> "$minute dakika önce"
            hour < 24 -> "$hour saat önce"
            day == 1L -> "Dün"
            day < 7 -> "$day gün önce"
            day < 30 -> "${day / 7} hafta önce"
            day < 365 -> "${day / 30} ay önce"
            else -> "${day / 365} yıl önce"
        }
    }

    // Short format for conversation list (12:45, Dün, Pzt, 22 Eki)
    fun getShortTimeAgo(time: Long): String {
        if (time <= 0L) return ""
        val now = System.currentTimeMillis()
        val diff = now - time
        val day = TimeUnit.MILLISECONDS.toDays(diff)
        val hour = TimeUnit.MILLISECONDS.toHours(diff)

        return when {
            hour < 24 -> SimpleDateFormat("HH:mm", Locale("tr")).format(Date(time))
            day == 1L -> "Dün"
            day < 7 -> SimpleDateFormat("EEE", Locale("tr")).format(Date(time))
            else -> SimpleDateFormat("d MMM", Locale("tr")).format(Date(time))
        }
    }
}
