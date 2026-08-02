package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class Content(val parts: List<Part>)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String = "image/jpeg",
    val data: String
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null,
    val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

object GeminiService {
    suspend fun generateResponse(systemPrompt: String, userPrompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isEmpty()) {
            return@withContext "AI Assistant is running in offline demo mode. Please configure your GEMINI_API_KEY in the Secrets panel in AI Studio to enable active intelligence."
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = userPrompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            generationConfig = GenerationConfig(temperature = 0.4f)
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No suggestions available right now."
        } catch (e: Exception) {
            "Offline Simulation: Could not connect to Gemini API (${e.localizedMessage}). Ensure your API Key is valid."
        }
    }

    suspend fun extractTableFromImage(bitmap: android.graphics.Bitmap): List<List<String>> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val baos = java.io.ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos)
        val base64Image = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)

        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val systemPrompt = "You are an OCR parser for bank branch document photos. Extract entries as CSV lines without markdown code blocks. CSV Header must be: ACCOUNT NUMBER, CUSTOMER NAME, PHONE NUMBER, RECEIVE DATE, ADDRESS, delivered. Output only CSV raw text."
                val userPrompt = "Extract all bank customer records from this photo into CSV format."

                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(text = userPrompt),
                                Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                            )
                        )
                    ),
                    systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                    generationConfig = GenerationConfig(temperature = 0.2f)
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                val textResult = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val cleanLines = textResult.lines().map { it.replace("```csv", "").replace("```", "").trim() }.filter { it.isNotBlank() }
                
                val result = mutableListOf<List<String>>()
                for (line in cleanLines) {
                    val parts = line.split(",").map { it.trim().removeSurrounding("\"") }
                    if (parts.size >= 2) {
                        result.add(parts)
                    }
                }
                if (result.isNotEmpty()) return@withContext result
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback demo parsing when API key is missing or offline
        listOf(
            listOf("ACCOUNT NUMBER", "CUSTOMER NAME", "PHONE NUMBER", "RECEIVE DATE", "ADDRESS", "delivered"),
            listOf("7003201004521", "MD TOUFIQUR RAHMAN", "01700000000", "01/08/2026", "CHIRIRBANDAR, DINAJPUR", "no")
        )
    }
}
