package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.network.WishlistApi
import com.nexohogar.data.remote.dto.CreateWishlistItemRequest
import com.nexohogar.data.remote.dto.UpdateWishlistItemRequest
import com.nexohogar.domain.model.WishlistItem
import com.nexohogar.domain.repository.WishlistRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class WishlistRepositoryImpl(
    private val api: WishlistApi
) : WishlistRepository {

    override suspend fun getWishlistItems(householdId: String): AppResult<List<WishlistItem>> {
        return try {
            val response = api.getWishlistItems(householdIdFilter = "eq.$householdId")
            if (response.isSuccessful) {
                AppResult.Success(response.body()?.map { it.toDomain() } ?: emptyList())
            } else {
                AppResult.Error("Error al obtener lista de deseos: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun createWishlistItem(
        householdId: String,
        name: String,
        description: String?,
        price: Double?,
        priority: String,
        createdBy: String
    ): AppResult<WishlistItem> {
        return try {
            val request = CreateWishlistItemRequest(
                householdId = householdId,
                name        = name,
                description = description,
                price       = price,
                priority    = priority,
                createdBy   = createdBy
            )
            val response = api.createWishlistItem(request = request)
            if (response.isSuccessful) {
                val created = response.body()?.firstOrNull()
                    ?: return AppResult.Error("No se recibió respuesta del servidor")
                AppResult.Success(created.toDomain())
            } else {
                AppResult.Error("Error al crear deseo: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun updateWishlistItem(
        itemId: String,
        name: String,
        description: String?,
        price: Double?,
        priority: String
    ): AppResult<WishlistItem> {
        return try {
            val request = UpdateWishlistItemRequest(
                name        = name,
                description = description,
                price       = price,
                priority    = priority,
                updatedAt   = nowIso()
            )
            val response = api.updateWishlistItem(
                idFilter = "eq.$itemId",
                request  = request
            )
            if (response.isSuccessful) {
                val updated = response.body()?.firstOrNull()
                    ?: return AppResult.Error("No se recibió respuesta del servidor")
                AppResult.Success(updated.toDomain())
            } else {
                AppResult.Error("Error al actualizar deseo: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun markAsPurchased(itemId: String): AppResult<WishlistItem> {
        return try {
            val request = UpdateWishlistItemRequest(
                isPurchased = true,
                purchasedAt = nowIso(),
                updatedAt   = nowIso()
            )
            val response = api.updateWishlistItem(
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

    override suspend fun deleteWishlistItem(itemId: String): AppResult<Unit> {
        return try {
            val response = api.deleteWishlistItem(idFilter = "eq.$itemId")
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
