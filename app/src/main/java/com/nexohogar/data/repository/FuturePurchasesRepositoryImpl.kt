package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.network.FuturePurchasesApi
import com.nexohogar.data.remote.dto.CreateFuturePurchaseRequest
import com.nexohogar.data.remote.dto.UpdateFuturePurchaseRequest
import com.nexohogar.domain.model.FuturePurchase
import com.nexohogar.domain.repository.FuturePurchasesRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class FuturePurchasesRepositoryImpl(
    private val api: FuturePurchasesApi
) : FuturePurchasesRepository {

    override suspend fun getFuturePurchases(householdId: String): AppResult<List<FuturePurchase>> {
        return try {
            val response = api.getFuturePurchases(householdIdFilter = "eq.$householdId")
            if (response.isSuccessful) {
                AppResult.Success(response.body()?.map { it.toDomain() } ?: emptyList())
            } else {
                AppResult.Error("Error al obtener sugerencias: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun createFuturePurchase(
        householdId: String,
        name: String,
        description: String?,
        category: String?,
        estimatedPrice: Double?,
        priority: String,
        createdBy: String
    ): AppResult<FuturePurchase> {
        return try {
            val request = CreateFuturePurchaseRequest(
                householdId     = householdId,
                name            = name,
                description     = description,
                category        = category,
                estimatedPrice  = estimatedPrice,
                priority        = priority,
                createdBy       = createdBy
            )
            val response = api.createFuturePurchase(request = request)
            if (response.isSuccessful) {
                val created = response.body()?.firstOrNull()
                    ?: return AppResult.Error("No se recibió respuesta del servidor")
                AppResult.Success(created.toDomain())
            } else {
                AppResult.Error("Error al crear sugerencia: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun updateFuturePurchase(
        itemId: String,
        name: String?,
        description: String?,
        category: String?,
        estimatedPrice: Double?,
        priority: String?
    ): AppResult<FuturePurchase> {
        return try {
            val request = UpdateFuturePurchaseRequest(
                name            = name,
                description     = description,
                category        = category,
                estimatedPrice  = estimatedPrice,
                priority        = priority,
                updatedAt       = nowIso()
            )
            val response = api.updateFuturePurchase(
                idFilter = "eq.$itemId",
                request  = request
            )
            if (response.isSuccessful) {
                val updated = response.body()?.firstOrNull()
                    ?: return AppResult.Error("No se recibió respuesta del servidor")
                AppResult.Success(updated.toDomain())
            } else {
                AppResult.Error("Error al actualizar sugerencia: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun markAsPurchased(itemId: String): AppResult<FuturePurchase> {
        return try {
            val request = UpdateFuturePurchaseRequest(
                isPurchased = true,
                purchasedAt = nowIso(),
                updatedAt   = nowIso()
            )
            val response = api.updateFuturePurchase(
                idFilter = "eq.$itemId",
                request  = request
            )
            if (response.isSuccessful) {
                val updated = response.body()?.firstOrNull()
                    ?: return AppResult.Error("No se recibió respuesta del servidor")
                AppResult.Success(updated.toDomain())
            } else {
                AppResult.Error("Error al marcar como comprado: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun deleteFuturePurchase(itemId: String): AppResult<Unit> {
        return try {
            val response = api.deleteFuturePurchase(idFilter = "eq.$itemId")
            if (response.isSuccessful) {
                AppResult.Success(Unit)
            } else {
                AppResult.Error("Error al eliminar: ${response.code()}")
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
