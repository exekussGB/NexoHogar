package com.nexohogar.data.network.service

import android.graphics.Bitmap
import android.util.Base64
import com.nexohogar.core.network.SupabaseConfig
import com.nexohogar.core.util.AppLogger
import com.nexohogar.domain.model.ScannedReceiptItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Resultado del parseo con IA.
 */
data class AiParsedReceipt(
    val store: String?,
    val date: String?,
    val items: List<ScannedReceiptItem>,
    val total: Double?
)

/**
 * Servicio que envía la foto de la boleta a una Edge Function de Supabase
 * que usa Gemini Vision para extraer productos, precios y categorías.
 *
 * USA SU PROPIO OkHttpClient con:
 *  - Timeout de 60s (la IA puede tardar 15-30s)
 *  - Sin AuthInterceptor (usa API_KEY directamente)
 *  - Sin CertificatePinner (evita problemas de rotación de certs)
 */
class AiReceiptParserService(
    @Suppress("UNUSED_PARAMETER") unusedClient: OkHttpClient? = null
) {

    companion object {
        private const val TAG = "AiReceiptParser"
        private const val FUNCTION_NAME = "parse-receipt-ai"
        private const val MAX_IMAGE_WIDTH = 1024
        private const val JPEG_QUALITY = 85
        private const val TIMEOUT_SECONDS = 60L
    }

    // Cliente propio — sin interceptors que interfieran, con timeout largo
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Envía la imagen a la Edge Function y retorna los items parseados.
     * @return AiParsedReceipt con productos, o null si falla.
     */
    suspend fun parseReceipt(bitmap: Bitmap): AiParsedReceipt? = withContext(Dispatchers.IO) {
        try {
            // 1. Redimensionar si es muy grande
            val scaled = scaleBitmap(bitmap)

            // 2. Convertir a base64
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos)
            val base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

            AppLogger.d(TAG, "Imagen: ${scaled.width}x${scaled.height}, base64: ${base64Image.length} chars")

            // 3. Construir request
            val json = JSONObject().apply {
                put("image_base64", base64Image)
            }

            val url = "${SupabaseConfig.BASE_URL}/functions/v1/$FUNCTION_NAME"
            val body = json.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.API_KEY}")
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Content-Type", "application/json")
                .build()

            AppLogger.d(TAG, "Enviando a: $url (timeout: ${TIMEOUT_SECONDS}s)")

            // 4. Ejecutar request con cliente propio
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "sin body"
                AppLogger.e(TAG, "Error HTTP ${response.code}: $errorBody")
                return@withContext null
            }

            val responseBody = response.body?.string() ?: return@withContext null
            AppLogger.d(TAG, "Respuesta IA recibida: ${responseBody.take(300)}...")

            // 5. Parsear respuesta JSON
            return@withContext parseResponse(responseBody)

        } catch (e: Exception) {
            AppLogger.e(TAG, "Error al parsear con IA: ${e.javaClass.simpleName}: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Parsea la respuesta JSON de la Edge Function.
     */
    private fun parseResponse(jsonString: String): AiParsedReceipt? {
        try {
            val json = JSONObject(jsonString)

            // Verificar si hubo error
            if (json.has("error")) {
                AppLogger.e(TAG, "Edge Function error: ${json.getString("error")}")
                return null
            }

            val store = json.optString("store", "").ifBlank { null }
            val date = json.optString("date", "").ifBlank { null }
            val total = if (json.has("total") && !json.isNull("total")) json.optDouble("total") else null

            val itemsArray = json.optJSONArray("items") ?: return null
            val items = mutableListOf<ScannedReceiptItem>()

            for (i in 0 until itemsArray.length()) {
                val itemObj = itemsArray.getJSONObject(i)
                val name = itemObj.optString("name", "").trim()
                if (name.isBlank()) continue

                val quantity = itemObj.optDouble("quantity", 1.0)
                val pricePerUnit = if (itemObj.has("price_per_unit") && !itemObj.isNull("price_per_unit"))
                    itemObj.optDouble("price_per_unit") else null
                val priceTotal = if (itemObj.has("price_total") && !itemObj.isNull("price_total"))
                    itemObj.optDouble("price_total") else null
                val category = itemObj.optString("category", "").ifBlank { null }
                val unit = itemObj.optString("unit", "unidad").ifBlank { "unidad" }

                items.add(
                    ScannedReceiptItem(
                        name = name,
                        quantity = quantity,
                        pricePerUnit = pricePerUnit,
                        priceTotal = priceTotal,
                        category = category,
                        unit = unit,
                        isSelected = true
                    )
                )
            }

            if (items.isEmpty()) {
                AppLogger.d(TAG, "IA no encontró productos en la respuesta")
                return null
            }

            AppLogger.d(TAG, "Parseados ${items.size} productos con IA")
            return AiParsedReceipt(
                store = store,
                date = date,
                items = items,
                total = total
            )

        } catch (e: Exception) {
            AppLogger.e(TAG, "Error parseando respuesta JSON", e)
            return null
        }
    }

    /**
     * Escala el bitmap para no exceder MAX_IMAGE_WIDTH manteniendo proporciones.
     */
    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= MAX_IMAGE_WIDTH) return bitmap

        val ratio = MAX_IMAGE_WIDTH.toFloat() / bitmap.width
        val newHeight = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, MAX_IMAGE_WIDTH, newHeight, true)
    }
}
