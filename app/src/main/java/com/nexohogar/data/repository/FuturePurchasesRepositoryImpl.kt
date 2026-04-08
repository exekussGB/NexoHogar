package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.network.FuturePurchasesApi
import com.nexohogar.data.remote.dto.FuturePurchaseDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class FuturePurchasesRepositoryImpl(
    private val api: FuturePurchasesApi
) {

    suspend fun getFuturePurchases(householdId: String): AppResult<List<FuturePurchaseDto>> {
        return try {
            val response = api.getFuturePurchases(householdIdFilter = "eq.$householdId")
            if (response.isSuccessful) {
                AppResult.Success(response.body() ?: emptyList())
            } else {
                AppResult.Error("Error al obtener compras futuras: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    suspend fun createFuturePurchase(
        householdId: String,
        name: String,
        description: String?,
        category: String?,
        priority: String = "medium",
        estimatedPrice: Double?,
        createdBy: String
    ): AppResult<FuturePurchaseDto> {
        return try {
            val item = FuturePurchaseDto(
                id = UUID.randomUUID().toString(),
                householdId = householdId,
                name = name,
                description = description,
                category = category,
                priority = priority,
                estimatedPrice = estimatedPrice,
                isPurchased = false,
                createdBy = createdBy,
                createdAt = nowIso(),
                updatedAt = nowIso()
            )
            val response = api.createFuturePurchase(item = item)
            if (response.isSuccessful) {
                val created = response.body()?.firstOrNull()
                    ?: return AppResult.Error("No se recibió respuesta del servidor")
                AppResult.Success(created)
            } else {
                AppResult.Error("Error al crear compra futura: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    suspend fun updateFuturePurchase(
        itemId: String,
        name: String?,
        description: String?,
        category: String?,
        priority: String?,
        estimatedPrice: Double?,
        isPurchased: Boolean?
    ): AppResult<FuturePurchaseDto> {
        return try {
            val item = FuturePurchaseDto(
                id = itemId,
                householdId = "",
                name = name ?: "",
                description = description,
                category = category,
                priority = priority,
                estimatedPrice = estimatedPrice,
                isPurchased = isPurchased ?: false,
                createdBy = "",
                updatedAt = nowIso()
            )
            val response = api.updateFuturePurchase(
                idFilter = "eq.$itemId",
                item = item
            )
            if (response.isSuccessful) {
                val updated = response.body()?.firstOrNull()
                    ?: return AppResult.Error("No se recibió respuesta del servidor")
                AppResult.Success(updated)
            } else {
                AppResult.Error("Error al actualizar compra futura: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    suspend fun markAsPurchased(itemId: String): AppResult<FuturePurchaseDto> {
        return try {
            val item = FuturePurchaseDto(
                id = itemId,
                householdId = "",
                name = "",
                isPurchased = true,
                purchasedAt = nowIso(),
                createdBy = "",
                updatedAt = nowIso()
            )
            val response = api.updateFuturePurchase(
                idFilter = "eq.$itemId",
                item = item
            )
            if (response.isSuccessful) {
                val updated = response.body()?.firstOrNull()
                    ?: return AppResult.Error("No se recibió respuesta del servidor")
                AppResult.Success(updated)
            } else {
                AppResult.Error("Error al marcar como comprado: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    suspend fun deleteFuturePurchase(itemId: String): AppResult<Unit> {
        return try {
            val response = api.deleteFuturePurchase(idFilter = "eq.$itemId")
            if (response.isSuccessful) {
                AppResult.Success(Unit)
            } else {
                AppResult.Error("Error al eliminar compra futura: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private fun nowIso(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
}
