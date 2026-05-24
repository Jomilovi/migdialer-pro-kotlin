package com.migdialer.pro.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.migdialer.pro.data.model.Contact
import com.migdialer.pro.data.model.PhoneNumber
import com.migdialer.pro.utils.T9
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactRepository(private val context: Context) {

    suspend fun getContacts(query: String = ""): List<Contact> = withContext(Dispatchers.IO) {
        val all = loadAllContacts()
        if (query.isBlank()) return@withContext all
        filterContacts(all, query)
    }

    private fun loadAllContacts(): List<Contact> {
        val contacts = mutableMapOf<Long, MutableContact>()

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL,
                // Use HAS_PHONE_NUMBER proxy — we check photo separately
                ContactsContract.CommonDataKinds.Phone.PHOTO_ID
            ),
            null, null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        ) ?: return emptyList()

        cursor.use {
            while (it.moveToNext()) {
                val id      = it.getLong(0)
                val name    = it.getString(1)?.trim() ?: continue
                if (name.isEmpty()) continue
                val number  = it.getString(2)?.replace(Regex("[\\s\\-()]"), "") ?: continue
                val type    = it.getInt(3)
                val label   = it.getString(4) ?: ""
                val photoId = it.getLong(5)

                // Build photo URI ONLY using the contact's OWN contact_id + photo_id
                // This prevents photo bleeding between contacts
                val photoUri: String? = if (photoId > 0L) {
                    // Use the content URI that is strictly tied to this contact_id
                    // Format: content://com.android.contacts/contacts/{id}/photo
                    val base = ContentUris.withAppendedId(
                        ContactsContract.Contacts.CONTENT_URI, id
                    )
                    Uri.withAppendedPath(
                        base,
                        ContactsContract.Contacts.Photo.CONTENT_DIRECTORY
                    ).toString()
                } else null

                val phone = PhoneNumber(number, type, label)
                val existing = contacts[id]
                if (existing != null) {
                    existing.phones.add(phone)
                } else {
                    contacts[id] = MutableContact(id, name, mutableListOf(phone), photoUri)
                }
            }
        }

        return contacts.values.map { mc ->
            Contact(
                id           = mc.id,
                name         = mc.name,
                phoneNumbers = mc.phones,
                photoUri     = mc.photoUri
            )
        }
    }

    private fun filterContacts(all: List<Contact>, query: String): List<Contact> {
        val q = query.trim()
        return if (q.all { it.isDigit() }) {
            all.filter { c ->
                T9.nameMatchesT9(c.name, q) ||
                c.phoneNumbers.any { T9.numberMatchesDigits(it.number, q) }
            }
        } else {
            val qLower = q.lowercase()
            all.filter { c ->
                c.name.lowercase().contains(qLower) ||
                c.phoneNumbers.any { it.number.contains(q) }
            }
        }
    }

    private data class MutableContact(
        val id: Long,
        val name: String,
        val phones: MutableList<PhoneNumber>,
        val photoUri: String?
    )
}
