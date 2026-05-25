package com.migdialer.pro.data.repository

import android.content.Context
import android.database.Cursor
import android.provider.CallLog
import android.telephony.SubscriptionManager
import com.migdialer.pro.data.model.CallEntry
import com.migdialer.pro.data.model.CallType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CallLogRepository(private val context: Context) {

    /**
     * Builds a map of subscriptionId → Pair(slotNumber, simDisplayName).
     * Uses SubscriptionManager to get real SIM info from the device.
     * Falls back to defaults if permission denied.
     */
    private fun buildSimMap(): Map<Int, Pair<Int, String>> {
        val result = mutableMapOf<Int, Pair<Int, String>>()
        try {
            val sm = context.getSystemService(SubscriptionManager::class.java)
            val subs = sm?.activeSubscriptionInfoList ?: return result
            for (info in subs) {
                val slot = info.simSlotIndex + 1  // 1-based
                // Best display name: user label > carrier name > "SIM N"
                val name = when {
                    !info.displayName.isNullOrBlank()  -> info.displayName.toString().trim()
                    !info.carrierName.isNullOrBlank()  -> info.carrierName.toString().trim()
                    else                                -> "SIM $slot"
                }
                result[info.subscriptionId] = Pair(slot, name)
            }
        } catch (e: Exception) { /* no permission */ }
        return result
    }

    suspend fun getCallLog(limit: Int = 200): List<CallEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<CallEntry>()

        // Build SIM map once per load
        val simMap = buildSimMap()

        // If we have exactly 2 SIMs, note which subId belongs to which slot
        // This helps detect when subscription_id is not set correctly in the log
        val slot1SubId = simMap.entries.firstOrNull { it.value.first == 1 }?.key
        val slot2SubId = simMap.entries.firstOrNull { it.value.first == 2 }?.key

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls._ID,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.DATE,
                    "subscription_id",
                    CallLog.Calls.CACHED_PHOTO_URI,
                    "phone_account_id"   // extra field some devices use for SIM
                ),
                null, null,
                "${CallLog.Calls.DATE} DESC"
            ) ?: return@withContext emptyList()

            var count = 0
            while (cursor.moveToNext() && count < limit) {
                try {
                    val id       = cursor.getLong(0)
                    val rawName  = cursor.getString(1)
                    val number   = cursor.getString(2) ?: continue
                    val typeInt  = cursor.getInt(3)
                    val duration = cursor.getLong(4)
                    val date     = cursor.getLong(5)
                    val subId    = cursor.getInt(6)
                    val photo    = cursor.getString(7)
                    val acctId   = cursor.getString(8) ?: ""

                    val name = rawName?.takeIf { it.isNotBlank() } ?: number

                    val callType = when (typeInt) {
                        CallLog.Calls.INCOMING_TYPE  -> CallType.INCOMING
                        CallLog.Calls.OUTGOING_TYPE  -> CallType.OUTGOING
                        CallLog.Calls.MISSED_TYPE    -> CallType.MISSED
                        CallLog.Calls.REJECTED_TYPE  -> CallType.REJECTED
                        else                          -> CallType.MISSED
                    }

                    // Resolve SIM: try subscriptionId first, then phone_account_id heuristic
                    val simPair = resolveSimPair(subId, acctId, simMap, slot1SubId, slot2SubId)

                    entries.add(
                        CallEntry(
                            id       = id,
                            name     = name,
                            number   = number,
                            type     = callType,
                            duration = duration,
                            date     = date,
                            simSlot  = simPair.first,
                            simName  = simPair.second,
                            photoUri = photo?.takeIf { it.isNotBlank() }
                        )
                    )
                    count++
                } catch (e: Exception) { continue }
            }
        } catch (e: SecurityException) {
            // READ_CALL_LOG not granted
        } catch (e: Exception) {
            // Some columns may not exist on this device — retry without phone_account_id
            cursor?.close()
            return@withContext getCallLogFallback(simMap)
        } finally {
            cursor?.close()
        }

        entries
    }

    private fun resolveSimPair(
        subId: Int,
        acctId: String,
        simMap: Map<Int, Pair<Int, String>>,
        slot1SubId: Int?,
        slot2SubId: Int?
    ): Pair<Int, String> {
        // 1. Direct match in simMap
        simMap[subId]?.let { return it }

        // 2. Try phone_account_id — some devices store "1" or "2" or carrier name
        if (acctId.isNotBlank()) {
            // Check if account id contains the subId
            if (slot2SubId != null && acctId.contains(slot2SubId.toString())) return simMap[slot2SubId] ?: Pair(2, "SIM 2")
            if (slot1SubId != null && acctId.contains(slot1SubId.toString())) return simMap[slot1SubId] ?: Pair(1, "SIM 1")
            // Some devices use slot index directly in account id
            if (acctId.endsWith("1") || acctId == "1") {
                val name = simMap[slot2SubId]?.second ?: "SIM 2"
                return Pair(2, name)
            }
        }

        // 3. Fallback: SIM 1
        val fallbackName = simMap[slot1SubId]?.second ?: "SIM 1"
        return Pair(1, fallbackName)
    }

    /** Fallback query without phone_account_id column */
    private suspend fun getCallLogFallback(
        simMap: Map<Int, Pair<Int, String>>
    ): List<CallEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<CallEntry>()
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls._ID,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.DATE,
                    "subscription_id",
                    CallLog.Calls.CACHED_PHOTO_URI
                ),
                null, null,
                "${CallLog.Calls.DATE} DESC"
            ) ?: return@withContext emptyList()

            val slot1SubId = simMap.entries.firstOrNull { it.value.first == 1 }?.key
            var count = 0
            while (cursor.moveToNext() && count < 200) {
                try {
                    val id       = cursor.getLong(0)
                    val rawName  = cursor.getString(1)
                    val number   = cursor.getString(2) ?: continue
                    val typeInt  = cursor.getInt(3)
                    val duration = cursor.getLong(4)
                    val date     = cursor.getLong(5)
                    val subId    = cursor.getInt(6)
                    val photo    = cursor.getString(7)
                    val name = rawName?.takeIf { it.isNotBlank() } ?: number
                    val callType = when (typeInt) {
                        CallLog.Calls.INCOMING_TYPE  -> CallType.INCOMING
                        CallLog.Calls.OUTGOING_TYPE  -> CallType.OUTGOING
                        CallLog.Calls.MISSED_TYPE    -> CallType.MISSED
                        else                          -> CallType.MISSED
                    }
                    val simPair = simMap[subId] ?: Pair(1, simMap[slot1SubId]?.second ?: "SIM 1")
                    entries.add(CallEntry(id, name, number, callType, duration, date, simPair.first, simPair.second, photo?.takeIf { it.isNotBlank() }))
                    count++
                } catch (e: Exception) { continue }
            }
        } finally { cursor?.close() }
        entries
    }
}
