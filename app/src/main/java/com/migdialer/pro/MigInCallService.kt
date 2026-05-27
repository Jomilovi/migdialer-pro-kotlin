package com.migdialer.pro

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.InCallService
import com.migdialer.pro.ui.dialer.InCallActivity
import com.migdialer.pro.ui.dialer.IncomingCallActivity

class MigInCallService : InCallService() {

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        currentCall = call
        call.registerCallback(callStateCallback)

        val rawNumber = call.details?.handle?.schemeSpecificPart ?: ""
        val contact   = lookupContact(rawNumber)

        currentPhotoUri = contact.photoUri

        val displayName = when {
            contact.name.isNotBlank() -> contact.name
            rawNumber.isNotBlank()    -> rawNumber
            else                      -> ""
        }

        val targetActivity = when (call.state) {
            Call.STATE_RINGING -> IncomingCallActivity::class.java
            else               -> InCallActivity::class.java
        }

        startActivity(Intent(this, targetActivity).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_DISPLAY_NAME, displayName)
            putExtra(EXTRA_PHOTO_URI, contact.photoUri)
            putExtra(EXTRA_NUMBER, rawNumber)
        })
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callStateCallback)
        if (currentCall == call) {
            currentCall    = null
            currentPhotoUri = null
        }
    }

    private val callStateCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            stateListener?.invoke(state)
        }
    }

    /**
     * Busca el contacto en la agenda por número de teléfono.
     * Devuelve nombre y URI de foto si existe.
     */
    private fun lookupContact(number: String): ContactInfo {
        if (number.isBlank()) return ContactInfo("", null)
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            val cursor = contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup.CONTACT_ID,
                    ContactsContract.PhoneLookup.PHOTO_ID
                ),
                null, null, null
            )
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val name    = c.getString(0) ?: ""
                    val id      = c.getLong(1)
                    val photoId = c.getLong(2)
                    val photoUri: String? = if (photoId > 0L) {
                        val base = ContentUris.withAppendedId(
                            ContactsContract.Contacts.CONTENT_URI, id
                        )
                        Uri.withAppendedPath(
                            base,
                            ContactsContract.Contacts.Photo.CONTENT_DIRECTORY
                        ).toString()
                    } else null
                    ContactInfo(name, photoUri)
                } else ContactInfo("", null)
            } ?: ContactInfo("", null)
        } catch (e: Exception) {
            ContactInfo("", null)
        }
    }

    data class ContactInfo(val name: String, val photoUri: String?)

    companion object {
        const val EXTRA_DISPLAY_NAME = "display_name"
        const val EXTRA_PHOTO_URI    = "photo_uri"
        const val EXTRA_NUMBER       = "number"

        @Volatile var currentCall:     Call?   = null
        @Volatile var currentPhotoUri: String? = null
        var stateListener: ((Int) -> Unit)?    = null
    }
}
