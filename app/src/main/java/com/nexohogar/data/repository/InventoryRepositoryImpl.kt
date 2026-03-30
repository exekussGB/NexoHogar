package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.core.result.getOrThrow
import com.nexohogar.data.network.InventoryApi
import com.nexohogar.data.remote.dto.CreateInventoryCategoryRequest
import com.nexohogar.data.remote.dto.CreateInventoryMovementRequest
import com.nexohogar.data.remote.dto.CreateProductRequest
import com.nexohogar.data.remote.dto.ImportReceiptItemDto
import com.nexohogar.data.remote.dto.ImportReceiptRequest
import com.nexohogar.domain.model.InventoryCategory
import com.nexohogar.domain.model.InventoryMovement
import com.nexohogar.domain.model.Product
import com.nexohogar.domain.model.PurchaseSuggestion
import com.nexohogar.domain.model.ScannedReceiptItem
import com.nexohogar.domain.repository.InventoryRepository
import java.time.LocalDate

/**
 * COH-03: Migrado a AppResult para consistencia con los demás repositorios.
 */
class InventoryRepositoryImpl(
    private val api: InventoryApi
) : InventoryRepository {

    override suspend fun getProducts(householdId: String): AppResult<List<Product>> {
        return try {
            val response = api.getProducts("eq.$householdId")
            if (!response.isSuccessful) {
                return AppResult.Error("Error al obtener productos: ${response.code()} ${response.errorBody()?.string()}")
            }
            val dtos = response.body() ?: emptyList()

            // Calcular stock actual para cada producto
            val movements = when (val movResult = getMovements(householdId)) {
                is AppResult.Success -> movResult.data
                else -> emptyList()
            }

            val products = dtos.map { dto ->
                val productMovements = movements.filter { it.itemId == dto.id }
                val stock = productMovements.sumOf { m ->
                    if (m.movementType == "in") m.quantity else -m.quantity
                }
                dto.toDomain(currentStock = maxOf(0.0, stock))
            }
            AppResult.Success(products)
        } catch (e: Exception) {
            AppResult.Error("Fallo de red: ${e.message}", e)
        }
    }

    override suspend fun createProduct(
        householdId: String,
        name: String,
        unit: String,
        brand: String?,
        category: String?
    ): AppResult<Product> {
        return try {
            val response = api.createProduct(
                CreateProductRequest(
                    householdId = householdId,
                    name = name,
                    unit = unit,
                    brand = brand,
                    category = category
                )
            )
            if (!response.isSuccessful) {
                return AppResult.Error("Error al crear producto: ${response.code()} ${response.errorBody()?.string()}")
            }
            val product = response.body()?.firstOrNull()?.toDomain()
                ?: return AppResult.Error("Respuesta vacía al crear producto")
            AppResult.Success(product)
        } catch (e: Exception) {
            AppResult.Error("Fallo de red: ${e.message}", e)
        }
    }

    override suspend fun getMovements(householdId: String, itemId: String?): AppResult<List<InventoryMovement>> {
        return try {
            val response = api.getMovements(
                householdId = "eq.$householdId",
                itemId = itemId?.let { "eq.$it" }
            )
            if (!response.isSuccessful) {
                return AppResult.Error("Error al obtener movimientos: ${response.code()} ${response.errorBody()?.string()}")
            }
            AppResult.Success(response.body()?.map { it.toDomain() } ?: emptyList())
        } catch (e: Exception) {
            AppResult.Error("Fallo de red: ${e.message}", e)
        }
    }

    override suspend fun addPurchase(
        householdId: String,
        itemId: String,
        quantity: Double,
        movementDate: String,
        pricePerUnit: Double?,
        priceTotal: Double?,
        brand: String?,
        store: String?
    ): AppResult<InventoryMovement> {
        return try {
            val response = api.addMovement(
                CreateInventoryMovementRequest(
                    householdId = householdId,
                    itemId = itemId,
                    movementType = "in",
                    quantity = quantity,
                    movementDate = movementDate,
                    pricePerUnit = pricePerUnit,
                    priceTotal = priceTotal,
                    brand = brand,
                    store = store
                )
            )
            if (!response.isSuccessful) {
                return AppResult.Error("Error al registrar compra: ${response.code()} ${response.errorBody()?.string()}")
            }
            val movement = response.body()?.firstOrNull()?.toDomain()
                ?: return AppResult.Error("Respuesta vacía al registrar compra")
            AppResult.Success(movement)
        } catch (e: Exception) {
            AppResult.Error("Fallo de red: ${e.message}", e)
        }
    }

    override suspend fun addConsumption(
        householdId: String,
        itemId: String,
        quantity: Double,
        movementDate: String
    ): AppResult<InventoryMovement> {
        return try {
            val response = api.addMovement(
                CreateInventoryMovementRequest(
                    householdId = householdId,
                    itemId = itemId,
                    movementType = "out",
                    quantity = quantity,
                    movementDate = movementDate
                )
            )
            if (!response.isSuccessful) {
                return AppResult.Error("Error al registrar consumo: ${response.code()} ${response.errorBody()?.string()}")
            }
            val movement = response.body()?.firstOrNull()?.toDomain()
                ?: return AppResult.Error("Respuesta vacía al registrar consumo")
            AppResult.Success(movement)
        } catch (e: Exception) {
            AppResult.Error("Fallo de red: ${e.message}", e)
        }
    }

    override suspend fun getSuggestions(householdId: String): AppResult<List<PurchaseSuggestion>> {
        return try {
            val products = getProducts(householdId).getOrThrow()
            val movements = getMovements(householdId).getOrThrow()
            val suggestions = mutableListOf<PurchaseSuggestion>()

            for (product in products) {
                val productMovements = movements.filter { it.itemId == product.id }
                if (productMovements.isEmpty()) continue

                val oneMonthAgo = LocalDate.now().minusMonths(1).toString()
                val recentConsumptions = productMovements.filter {
                    it.movementType == "out" && it.movementDate >= oneMonthAgo
                }
                if (recentConsumptions.isEmpty()) continue

                val monthlyConsumption = recentConsumptions.sumOf { it.quantity }

                if (product.currentStock < monthlyConsumption * 0.5) {
                    val needed = monthlyConsumption - product.currentStock
                    val lastPrice = productMovements
                        .filter { it.movementType == "in" && it.pricePerUnit != null }
                        .maxByOrNull { it.movementDate }
                        ?.pricePerUnit
                    val estimatedCost = lastPrice?.let { it * needed }

                    suggestions.add(
                        PurchaseSuggestion(
                            product = product,
                            suggestedQuantity = needed,
                            estimatedCost = estimatedCost,
                            reason = "Consumo mensual: ${String.format("%.1f", monthlyConsumption)} ${product.unit}, stock actual: ${String.format("%.1f", product.currentStock)} ${product.unit}"
                        )
                    )
                }
            }
            AppResult.Success(suggestions.sortedByDescending { it.estimatedCost ?: 0.0 })
        } catch (e: Exception) {
            AppResult.Error("Error al calcular sugerencias: ${e.message}", e)
        }
    }

    // ─── Categorías ──────────────────────────────────────────────────────────────

    override suspend fun getCategories(householdId: String): AppResult<List<InventoryCategory>> {
        return try {
            val response = api.getCategories("eq.$householdId")
            if (!response.isSuccessful) {
                return AppResult.Error("Error al obtener categorías: ${response.code()} ${response.errorBody()?.string()}")
            }
            AppResult.Success(response.body()?.map { it.toDomain() } ?: emptyList())
        } catch (e: Exception) {
            AppResult.Error("Fallo de red: ${e.message}", e)
        }
    }

    override suspend fun createCategory(householdId: String, name: String, icon: String?): AppResult<InventoryCategory> {
        return try {
            val response = api.createCategory(
                CreateInventoryCategoryRequest(
                    householdId = householdId,
                    name = name,
                    icon = icon
                )
            )
            if (!response.isSuccessful) {
                return AppResult.Error("Error al crear categoría: ${response.code()} ${response.errorBody()?.string()}")
            }
            val category = response.body()?.firstOrNull()?.toDomain()
                ?: return AppResult.Error("Respuesta vacía al crear categoría")
            AppResult.Success(category)
        } catch (e: Exception) {
            AppResult.Error("Fallo de red: ${e.message}", e)
        }
    }

    override suspend fun deleteCategory(categoryId: String): AppResult<Unit> {
        return try {
            val response = api.deleteCategory("eq.$categoryId")
            if (!response.isSuccessful) {
                return AppResult.Error("Error al eliminar categoría: ${response.code()} ${response.errorBody()?.string()}")
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Fallo de red: ${e.message}", e)
        }
    }

    override suspend fun importReceipt(
        householdId: String,
        userId: String,
        accountId: String,
        categoryId: String?,
        store: String?,
        receiptDate: String,
        items: List<ScannedReceiptItem>
    ): AppResult<Map<String, Any>> {
        return try {
            val itemDtos = items.map { item ->
                ImportReceiptItemDto(
                    name = item.name,
                    quantity = item.quantity,
                    pricePerUnit = item.pricePerUnit,
                    priceTotal = item.priceTotal,
                    brand = item.brand,
                    unit = item.unit,
                    category = item.category
                )
            }
            val request = ImportReceiptRequest(
                householdId = householdId,
                userId = userId,
                accountId = accountId,
                categoryId = categoryId,
                store = store,
                receiptDate = receiptDate,
                items = itemDtos
            )
            val response = api.importReceipt(request)
            if (response.isSuccessful) {
                AppResult.Success(response.body() ?: emptyMap())
            } else {
                val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                AppResult.Error("Error al importar boleta: $errorBody")
            }
        } catch (e: Exception) {
            AppResult.Error("Fallo de red: ${e.message}", e)
        }
    }
}
