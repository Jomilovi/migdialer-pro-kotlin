package com.migdialer.pro.data.model

data class CallEntry(
    val id: Long,
    val name: String,
    val number: String,
    val type: CallType,
    val duration: Long,       // seconds
    val date: Long,           // timestamp millis
    val simSlot: Int = 1,
    val simName: String = "SIM $simSlot",   // real SIM display name
    val photoUri: String? = null,
    val initial: String = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
) {
    val durationFormatted: String get() {
        if (duration <= 0L) return ""
        val h = duration / 3600
        val m = (duration % 3600) / 60
        val s = duration % 60
        return when {
            h > 0  -> "${h}h ${m}min"
            m > 0  -> "${m}min ${s}seg"
            else   -> "${s}seg"
        }
    }
}

enum class CallType {
    INCOMING, OUTGOING, MISSED, REJECTED
}
