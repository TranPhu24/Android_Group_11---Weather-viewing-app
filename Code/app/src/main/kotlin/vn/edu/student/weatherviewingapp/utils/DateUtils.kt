package vn.edu.student.weatherviewingapp.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatCacheAge(ageMillis: Long): String {
    val minutes = ageMillis / 60_000
    return when {
        minutes < 1 -> "vừa xong"
        minutes < 60 -> "$minutes phút"
        else -> "${minutes / 60} giờ ${minutes % 60} phút"
    }
}

fun getDayNameVi(timestamp: Long): String {
    val date = Date(timestamp * 1000)
    val localeVi = Locale.forLanguageTag("vi-VN")
    val sdf = SimpleDateFormat("EEEE", localeVi)
    return sdf.format(date).replaceFirstChar { it.uppercase() }
}

fun formatDateShort(timestamp: Long): String {
    val date = Date(timestamp * 1000)
    val localeVi = Locale.forLanguageTag("vi-VN")
    val sdf = SimpleDateFormat("dd/MM", localeVi)
    return sdf.format(date)
}