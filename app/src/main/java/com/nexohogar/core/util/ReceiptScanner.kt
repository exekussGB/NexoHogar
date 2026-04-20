package com.nexohogar.core.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class ReceiptScanner(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Procesa una imagen desde una URI y devuelve el monto detectado más probable.
     * Retorna null si no se encuentra ningún patrón de monto.
     */
    suspend fun scanReceipt(uri: Uri): Long? {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(image).await()
            extractAmount(result.text)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Lógica para extraer el monto:
     * 1. Busca líneas que contengan "total", "monto", "pagar", "$".
     * 2. Limpia caracteres no numéricos excepto el punto de miles.
     * 3. Devuelve el número más alto que parezca un monto razonable.
     */
    private fun extractAmount(text: String): Long? {
        val lines = text.lines()
        val candidates = mutableListOf<Long>()

        // Regex para capturar números con separadores de miles opcionales (ej: 15.000, 15000)
        // Buscamos patrones que tengan al menos 3 dígitos para evitar capturar fechas o IDs cortos.
        val amountRegex = Regex("""(\d{1,3}(?:\.\d{3})+)|\d{4,7}""")

        for (line in lines) {
            val lowerLine = line.lowercase()
            // Si la línea parece ser un total
            if (lowerLine.contains("total") || lowerLine.contains("monto") || 
                lowerLine.contains("pagar") || lowerLine.contains("$") ||
                lowerLine.contains("clp")) {
                
                amountRegex.findAll(line).forEach { match ->
                    val cleaned = match.value.replace(".", "")
                    cleaned.toLongOrNull()?.let { candidates.add(it) }
                }
            }
        }

        // Si no encontramos nada con palabras clave, buscamos cualquier número grande en el texto
        if (candidates.isEmpty()) {
            amountRegex.findAll(text).forEach { match ->
                val cleaned = match.value.replace(".", "")
                cleaned.toLongOrNull()?.let { candidates.add(it) }
            }
        }

        // Retornamos el valor más alto (generalmente el TOTAL está al final o es el número más grande)
        // Filtramos números sospechosamente grandes para CLP (ej: > 2.000.000) si es una cuenta normal.
        return candidates.filter { it in 100..2000000 }.maxOrNull()
    }
}
