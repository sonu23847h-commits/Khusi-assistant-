package com.khushi.assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

object CommandProcessor {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val timeWords = listOf("time", "samay", "baje")
    private val dateWords = listOf("date", "today", "aaj", "tareek")
    private val weatherWords = listOf("weather", "mausam")
    private val newsWords = listOf("news", "headline", "khabar", "samachar")
    private val callWords = listOf("call", "phone karo", "call karo")
    private val messageWords = listOf("message", "sms", "text", "sandesh")

    fun process(context: Context, spokenText: String, callback: (String) -> Unit) {
        val text = spokenText.lowercase(Locale.ROOT)

        when {
            timeWords.any { it in text } -> {
                val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                callback("Abhi samay hai $time")
            }

            dateWords.any { it in text } -> {
                val date = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(Date())
                callback("Aaj ki date hai $date")
            }

            weatherWords.any { it in text } -> {
                val city = extractCityFromWeatherQuery(text)
                thread {
                    val report = WeatherHelper.getWeatherReport(city)
                    mainHandler.post { callback(report) }
                }
            }

            newsWords.any { it in text } -> {
                thread {
                    val headlines = NewsHelper.getTopHeadlines()
                    mainHandler.post { callback(headlines) }
                }
            }

            callWords.any { it in text } -> {
                val name = extractNameForCall(text)
                callback(handleCall(context, name))
            }

            messageWords.any { it in text } -> {
                callback(handleMessage(context, text))
            }

            "open camera" in text -> {
                launch(context, Intent("android.media.action.IMAGE_CAPTURE"))
                callback("Camera khol rahi hoon")
            }

            "open whatsapp" in text -> {
                callback(openApp(context, "com.whatsapp") ?: "Mujhe WhatsApp nahi mila")
            }

            "search" in text -> {
                val query = text.substringAfter("search").trim()
                val intent = Intent(Intent.ACTION_WEB_SEARCH)
                intent.putExtra("query", query)
                launch(context, intent)
                callback("Main $query search kar rahi hoon")
            }

            "thank" in text -> callback("Aapka swagat hai!")

            "who are you" in text || "kaun ho" in text ->
                callback("Main Khushi hoon, aapki apni voice assistant, bilkul Jarvis jaisi!")

            else -> callback("Maaf kijiye, mujhe samajh nahi aaya. Kripya dobara boliye.")
        }
    }

    // ---- Calling ----

    private fun extractNameForCall(text: String): String {
        // Handles "call Sonu", "Sonu ko call karo", "Sonu ko call"
        return when {
            "call" in text && "ko" in text -> text.substringBefore("ko").trim()
            "call" in text -> text.substringAfter("call").replace("karo", "").trim()
            else -> text.substringBefore("ko").trim()
        }
    }

    private fun handleCall(context: Context, spokenName: String): String {
        if (spokenName.isBlank()) return "Kisko call karna hai, naam boliye."

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return "Call karne ke liye permission nahi mili. App mein jaakar permission allow kijiye."
        }

        val number = ContactsHelper.findPhoneNumber(context, spokenName)
            ?: return "Mujhe $spokenName naam ka contact nahi mila."

        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            "$spokenName ko call kar rahi hoon"
        } catch (e: Exception) {
            "Call karne mein error aa gaya."
        }
    }

    // ---- SMS ----
    // Expected phrasing: "message <name> that <message body>"

    private fun handleMessage(context: Context, text: String): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return "Message bhejne ke liye permission nahi mili."
        }

        var afterTrigger = text
        for (word in messageWords) {
            if (word in afterTrigger) {
                afterTrigger = afterTrigger.substringAfter(word)
                break
            }
        }
        afterTrigger = afterTrigger.trim()

        if (afterTrigger.isBlank()) return "Kisko aur kya message bhejna hai, bataiye."

        val name: String
        val body: String
        if ("that" in afterTrigger) {
            name = afterTrigger.substringBefore("that").trim()
            body = afterTrigger.substringAfter("that").trim()
        } else if ("ki" in afterTrigger) {
            name = afterTrigger.substringBefore("ki").trim()
            body = afterTrigger.substringAfter("ki").trim()
        } else {
            val parts = afterTrigger.split(" ", limit = 2)
            name = parts.getOrElse(0) { "" }
            body = parts.getOrElse(1) { "" }
        }

        if (name.isBlank() || body.isBlank()) {
            return "Mujhe naam aur message dono chahiye, jaise 'message Sonu that main aa rahi hoon'."
        }

        val number = ContactsHelper.findPhoneNumber(context, name)
            ?: return "Mujhe $name naam ka contact nahi mila."

        return try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(number, null, body, null, null)
            "$name ko message bhej diya"
        } catch (e: Exception) {
            "Message bhejne mein error aa gaya."
        }
    }

    // ---- Helpers ----

    private fun extractCityFromWeatherQuery(text: String): String? {
        if (" in " !in text) return null
        return text.substringAfter(" in ").trim().takeIf { it.isNotBlank() }
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
