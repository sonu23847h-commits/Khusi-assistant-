package com.khushi.assistant

import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL

/**
 * Pulls the top headlines from Google News RSS (India edition, English).
 * No API key needed.
 */
object NewsHelper {

    fun getTopHeadlines(maxItems: Int = 3): String {
        return try {
            val url = URL("https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            val parser = XmlPullParser::class.java
            val factory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
            val xpp = factory.newPullParser()
            xpp.setInput(connection.inputStream, "UTF-8")

            val headlines = mutableListOf<String>()
            var insideItem = false
            var currentTag: String? = null

            var eventType = xpp.eventType
            while (eventType != XmlPullParser.END_DOCUMENT && headlines.size < maxItems) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = xpp.name
                        if (currentTag == "item") insideItem = true
                    }
                    XmlPullParser.TEXT -> {
                        if (insideItem && currentTag == "title") {
                            val title = xpp.text?.trim()
                            if (!title.isNullOrBlank()) headlines.add(title)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (xpp.name == "item") insideItem = false
                    }
                }
                eventType = xpp.next()
            }

            if (headlines.isEmpty()) {
                "Abhi news fetch nahi ho payi."
            } else {
                "Aaj ki top headlines hain: " + headlines.joinToString(". ")
            }
        } catch (e: Exception) {
            "Maaf kijiye, news fetch karne mein problem aa gayi."
        }
    }
}
