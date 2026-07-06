package com.khushi.assistant

import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.*

object CommandProcessor {

    fun process(context: Context, spokenText: String): String {
        val text = spokenText.lowercase(Locale.ROOT)

        return when {
            "time" in text -> {
                val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                "Abhi samay hai $time"
            }

            "date" in text || "today" in text -> {
                val date = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(Date())
                "Aaj ki date hai $date"
            }

            "open camera" in text -> {
                launch(context, Intent("android.media.action.IMAGE_CAPTURE"))
                "Camera khol rahi hoon"
            }

            "open whatsapp" in text -> {
                openApp(context, "com.whatsapp") ?: "Mujhe WhatsApp nahi mila"
            }

            "call" in text -> {
                "Abhi calling feature demo mein nahi hai, aap ise CommandProcessor mein add kar sakte hain"
            }

            "search" in text -> {
                val query = text.substringAfter("search").trim()
                val intent = Intent(Intent.ACTION_WEB_SEARCH)
                intent.putExtra("query", query)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launch(context, intent)
                "Main $query search kar rahi hoon"
            }

            "thank" in text -> "Aapka swagat hai!"

            "who are you" in text || "kaun ho" in text ->
                "Main Khushi hoon, aapki apni voice assistant, bilkul Jarvis jaisi!"

            else -> "Maaf kijiye, mujhe samajh nahi aaya. Kripya dobara boliye."
        }
    }

    private fun launch(context: Context, intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
        }
    }

    private fun openApp(context: Context, packageName: String): String? {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            "Khol rahi hoon"
        } else null
    }
}
