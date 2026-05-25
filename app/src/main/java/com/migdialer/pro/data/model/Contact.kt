package com.migdialer.pro.data.model

data class Contact(
    val id: Long,
    val name: String,
    val phoneNumbers: List<PhoneNumber>,
    val photoUri: String? = null,
    val initial: String = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
)

data class PhoneNumber(
    val number: String,
    val type: Int,    // ContactsContract.CommonDataKinds.Phone.TYPE_*
    val label: String
)
