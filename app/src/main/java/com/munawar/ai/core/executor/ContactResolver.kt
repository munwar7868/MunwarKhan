package com.munawar.ai.core.executor

import android.content.Context
import android.provider.ContactsContract

data class ResolvedContact(val name: String, val number: String)

class ContactResolver(private val context: Context) {

    // Simple Urdu/relationship aliases the user can extend later.
    private val aliases = mapOf(
        "mom" to "Ammi", "ammi" to "Ammi", "amma" to "Ammi",
        "dad" to "Abbu", "abbu" to "Abbu", "abu" to "Abbu",
        "brother" to "Bhai", "bhai" to "Bhai",
        "sister" to "Behen", "behen" to "Behen"
    )

    fun findContacts(rawName: String): List<ResolvedContact> {
        val query = aliases[rawName.trim().lowercase()] ?: rawName.trim()
        val results = mutableListOf<ResolvedContact>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, null
        )
        cursor?.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val name = it.getString(nameIdx) ?: continue
                val number = it.getString(numIdx) ?: continue
                if (name.contains(query, ignoreCase = true)) {
                    results.add(ResolvedContact(name, number))
                }
            }
        }
        return results.distinctBy { it.number }
    }
}
