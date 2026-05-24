package com.migdialer.pro.utils

import android.content.Context
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager

/**
 * Reads real SIM names from the device's SubscriptionManager.
 * Falls back to user-configured names if system names are unavailable.
 * Works on all Android 12+ devices regardless of brand.
 */
object SimUtils {

    // Fallback names if SIM names can't be read from system
    private const val FALLBACK_SIM1 = "Migue"
    private const val FALLBACK_SIM2 = "Miguel"

    data class SimInfo(
        val subscriptionId: Int,
        val slotIndex: Int,       // 0-based
        val displayName: String,  // Carrier or user-set name
        val slotNumber: Int = slotIndex + 1  // 1-based for display
    )

    /**
     * Get all active SIM cards with their real names.
     * Returns empty list if permission denied or single-SIM device.
     */
    fun getActiveSimCards(context: Context): List<SimInfo> {
        return try {
            val sm = context.getSystemService(SubscriptionManager::class.java)
            val subs: List<SubscriptionInfo> = sm?.activeSubscriptionInfoList
                ?: return emptyList()

            subs.sortedBy { it.simSlotIndex }.map { info ->
                SimInfo(
                    subscriptionId = info.subscriptionId,
                    slotIndex      = info.simSlotIndex,
                    displayName    = resolveSimName(info)
                )
            }
        } catch (e: SecurityException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Build a map of subscriptionId → SimInfo for fast lookup.
     */
    fun getSimMap(context: Context): Map<Int, SimInfo> {
        return getActiveSimCards(context).associateBy { it.subscriptionId }
    }

    /**
     * Get display name for a SIM by its slot number (1 or 2).
     * Uses real system name if available, falls back to user defaults.
     */
    fun getSimDisplayName(context: Context, slotNumber: Int): String {
        val sims = getActiveSimCards(context)
        val sim = sims.firstOrNull { it.slotNumber == slotNumber }
        return when {
            sim != null && sim.displayName.isNotBlank() -> sim.displayName
            slotNumber == 2 -> FALLBACK_SIM2
            else -> FALLBACK_SIM1
        }
    }

    /**
     * Resolve the best display name for a SIM subscription.
     * Priority: User-set display name > Carrier name > "SIM N"
     */
    private fun resolveSimName(info: SubscriptionInfo): String {
        val displayName = info.displayName?.toString()?.trim()
        if (!displayName.isNullOrBlank() && displayName != info.carrierName?.toString()) {
            return displayName
        }
        val carrierName = info.carrierName?.toString()?.trim()
        if (!carrierName.isNullOrBlank()) {
            return carrierName
        }
        return "SIM ${info.simSlotIndex + 1}"
    }

    /**
     * Resolve SIM slot from subscriptionId using the active subscription map.
     */
    fun resolveSlot(simMap: Map<Int, SimInfo>, subscriptionId: Int): Int {
        return simMap[subscriptionId]?.slotNumber ?: 1
    }
}
