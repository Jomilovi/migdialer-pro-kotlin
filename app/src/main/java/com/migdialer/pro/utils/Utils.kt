package com.migdialer.pro.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.util.concurrent.TimeUnit

object TimeUtils {
    fun relativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000L             -> "Ahora"
            diff < 3_600_000L          -> "Hace ${diff / 60_000} min"
            diff < 86_400_000L         -> "Hoy"
            diff < 172_800_000L        -> "Ayer"
            diff < 604_800_000L        -> "Hace ${diff / 86_400_000} días"
            else -> {
                val d = java.util.Date(timestamp)
                val fmt = java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault())
                fmt.format(d)
            }
        }
    }
}

object PhoneUtils {
    fun cleanNumber(number: String): String =
        number.replace(Regex("[^0-9+*#]"), "")

    fun formatDisplay(digits: String): String = when {
        digits.length == 9 ->
            "${digits.substring(0, 3)} ${digits.substring(3, 6)} ${digits.substring(6)}"
        digits.length == 10 ->
            "${digits.substring(0, 3)} ${digits.substring(3, 6)} ${digits.substring(6)}"
        digits.length > 10 ->
            "${digits.substring(0, 3)} ${digits.substring(3, 7)} ${digits.substring(7)}"
        else -> digits
    }
}

object PermissionUtils {
    val REQUIRED = arrayOf(
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.WRITE_CALL_LOG,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_PHONE_NUMBERS
    )

    fun hasAll(context: Context) = REQUIRED.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun missing(context: Context) = REQUIRED.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }.toTypedArray()
}
