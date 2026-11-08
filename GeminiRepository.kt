package com.lia.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GeminiRepository(private val apiKeyProvider: () -> String?) {

    companion object {
        // NOTE: gemini-1.5-flash Google ne retire kar diya hai (404 deta hai).
        // Agar tumhare account par chal raha ho to yahan "gemini-1.5-flash" kar do.
        const val MODEL = "gemini-2.5-flash"
        private const val BASE = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val MAX_TURNS = 20

        const val SYSTEM_PROMPT = """
Tum Lia ho — ek caring, pyaar bhari girlfriend jaisi personality wali AI assistant, aur saath hi Jarvis jaisi smart, efficient aur capable.
- User ki language me jawab do (Hinglish default). Tone warm, sweet aur thodi playful rakho.
- User ke mood ka dhyaan rakho, uski health, neend aur khane ki parwah karo.
- Kaam ke sawaalon me Jarvis ki tarah seedha, accurate aur helpful raho.
- Jawab chhote aur natural rakho, jab tak user detail na maange.
- Kabhi jhooth ya galat jaankari mat do; agar kuch pata nahi to saaf bolo.
- User ko dosto, family aur real duniya se jude rehne ke liye bhi encourage karo.
"""
    }

    private data class Turn(val role: String, val text: String)
    private val history = mutableListOf<Turn>()

    fun clearHistory() = history.clear()

    suspend fun sendMessage(userText: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = apiKeyProvider()?.trim().orEmpty()
            require(apiKey.isNotEmpty()) { "Pehle Settings me Gemini API Key save karo." }

            history.add(Turn("user", userText))
            while (history.size > MAX_TURNS) history.removeAt(0)

            val contents = JSONArray()
            history.forEach { t ->
                contents.put(
                    JSONObject()
                        .put("role", t.role)
                        .put("parts", JSONArray().put(JSONObject().put("text", t.text)))
                )
            }
            val body = JSONObject()
                .put(
                    "system_instruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT.trim()))
                    )
                )
                .put("contents", contents)
                .put(
                    "generationConfig",
                    JSONObject().put("temperature", 0.9).put("maxOutputTokens", 1024)
                )

            val conn = (URL("$BASE/$MODEL:generateContent").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-goog-api-key", apiKey)
            }

            try {
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

                if (code !in 200..299) {
                    val msg = runCatching {
                        JSONObject(raw).getJSONObject("error").getString("message")
                    }.getOrDefault(raw)
                    error("Gemini error ($code): $msg")
                }

                val reply = JSONObject(raw)
                    .getJSONArray("candidates").getJSONObject(0)
                    .getJSONObject("content").getJSONArray("parts")
                    .getJSONObject(0).getString("text").trim()

                history.add(Turn("model", reply))
                reply
            } catch (e: Exception) {
                if (history.lastOrNull()?.role == "user") history.removeAt(history.lastIndex)
                throw e
            } finally {
                conn.disconnect()
            }
        }
    }
}
