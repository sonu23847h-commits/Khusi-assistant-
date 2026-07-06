package com.khushi.assistant

import android.content.Context
import android.provider.ContactsContract

/**
 * Looks up a phone number by a spoken contact name, using a simple
 * "does the contact name contain this word" match.
 */
object ContactsHelper {

    fun findPhoneNumber(context: Context, spokenName: String): String? {
        val name = spokenName.trim().lowercase()
        if (name.isBlank()) return null

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val contactName = cursor.getString(nameIndex)?.lowercase() ?: continue
                if (contactName.contains(name)) {
                    return cursor.getString(numberIndex)
                }
            }
        }
        return null
    }
}
