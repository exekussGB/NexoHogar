package com.nexohogar.data.network.service

import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.nexohogar.core.network.SupabaseConfig
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.util.AppLogger
import com.nexohogar.domain.model.ScannedReceiptItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

/**
 * Servicio que envía la imagen de una boleta a la Edge Function parse-receipt-ai
 * para obtener los productos parseados mediante IA (Gemini Vision).
 *
 * Usa el OkHttpClient compartido para aprovechar AuthInterceptor (apikey + Bearer token)
 * y certificate pinning.
 */
class AiReceiptParserService(
    private val okHttpClient: OkHttpClient
) {
    private val gson = Gson()

    // ── DTOs de respuesta ────────────────────────────────────────────────────

    data class AiParsedReceipt(
        val store: String?,
        val date: String?,
        val total: Double?,
        val items: List<AiParsedItem>
    )

    data class AiParsedItem(
        val name: String,
        val quantity: Double?,
        @SerializedName("pricePerUnit") val pricePerUnit: Double?,
        @SerializedName("priceTotal") val priceTotal: Double?,
        val brand: String?,
        val unit: String?,
        val category: String?
    )

    // ── DTO de request ───────────────────────────────────────────────────────

    private data class AiParseRequest(
        @SerializedName("image_base64") val imageBase64: String,
        @SerializedName("existing_products") val existingProducts: List<String>?,
        @SerializedName("existing_categories") val existingCategories: List<String>?
    )

    // ── API pública ──────────────────────────────────────────────────────────

    /**
     * Envía la imagen de la boleta a la Edge Function y devuelve los productos parseados.
     *
     * @param bitmap           Imagen capturada de la boleta
     * @param existingProducts Nombres de productos existentes en inventario (para matching)
     * @param existingCategories Nombres de categorías del hogar (para clasificación)
     * @return AppResult con el receipt parseado o error
     */
    suspend fun parseReceipt(
        bitmap: Bitmap,
        existingProducts: List<String> = emptyList(),
        existingCategories: List<String> = emptyList()
    ): AppResult<AiParsedReceipt> = withContext(Dispatchers.IO) {
        try {
            // 1. Comprimir y codificar la imagen
            val base64Image = bitmapToBase64(bitmap)

            // 2. Construir el request
            val requestBody = AiParseRequest(
                imageBase64 = base64Image,
                existingProducts = existingProducts.takeIf { it.isNotEmpty() },
                existingCategories = existingCategories.takeIf { it.isNotEmpty() }
            )
            val jsonBody = gson.toJson(requestBody)

            // 3. Llamar a la Edge Function
            // AuthInterceptor inyecta apikey y Bearer token automáticamente
            val url = "${SupabaseConfig.BASE_URL}functions/v1/parse-receipt-ai"

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Error desconocido"
                AppLogger.e("AiReceiptParser", "Error ${response.code}: $errorBody")
                return@withContext AppResult.Error(
                    "Error al procesar boleta con IA (${response.code})"
                )
            }

            val responseBody = response.body?.string()
                ?: return@withContext AppResult.Error("Respuesta vacía del servidor")

            val parsed = gson.fromJson(responseBody, AiParsedReceipt::class.java)
            AppLogger.d("AiReceiptParser", "Parseados ${parsed.items.size} productos con IA")
            AppResult.Success(parsed)

        } catch (e: Exception) {
            AppLogger.e("AiReceiptParser", "Error procesando boleta con IA", e)
            AppResult.Error("Error de conexión: ${e.message}", e)
        }
    }

    /**
     * Convierte [AiParsedReceipt] a lista de [ScannedReceiptItem] para la UI.
     */
    fun toScannedItems(parsed: AiParsedReceipt): List<ScannedReceiptItem> {
        return parsed.items.map { item ->
            ScannedReceiptItem(
                name = item.name,
                quantity = item.quantity ?: 1.0,
                pricePerUnit = item.pricePerUnit,
                priceTotal = item.priceTotal,
                brand = item.brand,
                unit = item.unit ?: "unidad",
                category = item.category,
                isSelected = true
            )
        }
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    /**
     * Comprime un Bitmap a JPEG base64.
     * Redimensiona si excede 1920px para reducir payload (~200-400 KB típico).
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val maxDimension = 1920
        val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val scale = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val bytes = outputStream.toByteArray()

        if (scaledBitmap !== bitmap) {
            scaledBitmap.recycle()
        }

        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
