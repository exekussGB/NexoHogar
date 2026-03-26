package com.nexohogar.data.repository

import com.nexohogar.data.network.InventoryApi
import com.nexohogar.data.remote.dto.CreateInventoryCategoryRequest
import com.nexohogar.data.remote.dto.CreateInventoryMovementRequest
import com.nexohogar.data.remote.dto.CreateProductRequest
import com.nexohogar.domain.model.InventoryCategory
import com.nexohogar.domain.model.InventoryMovement
import com.nexohogar.domain.model.Product
import com.nexohogar.domain.model.PurchaseSuggestion
import com.nexohogar.domain.repository.InventoryRepository
import java.time.LocalDate

class InventoryRepositoryImpl(
    private val api: InventoryApi
) : InventoryRepository {

    override suspend fun getProducts(householdId: String): List<Product> {
        val response = api.getProducts("eq.$householdId")
        if (!response.isSuccessful) {
            throw Exception("Error al obtener productos: ${response.code()} ${response.errorBody()?.string()}")
        }
        val dtos = response.body() ?: emptyList()

        // Calcular stock actual para cada producto
        val movements = try {
            getMovements(householdId)
        } catch (e: Exception) {
            emptyList()
        }

        return dtos.map { dto ->
            val productMovements = movements.filter { it.itemId == dto.id }
            val stock = productMovements.sumOf { m ->
                if (m.movementType == "in") m.quantity else -m.quantity
            }
            dto.toDomain(currentStock = maxOf(0.0, stock))
        }
    }

    override suspend fun createProduct(
        householdId: String,
        name: String,
        unit: String,
        brand: String?,
        category: String?
    ): Product {
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
            throw Exception("Error al crear producto: ${response.code()} ${response.errorBody()?.string()}")
        }
        return response.body()?.firstOrNull()?.toDomain()
            ?: throw Exception("Respuesta vacía al crear producto")
    }

    override suspend fun getMovements(householdId: String, itemId: String?): List<InventoryMovement> {
        val response = api.getMovements(
            householdId = "eq.$householdId",
            itemId = itemId?.let { "eq.$it" }
        )
        if (!response.isSuccessful) {
            throw Exception("Error al obtener movimientos: ${response.code()} ${response.errorBody()?.string()}")
        }
        return response.body()?.map { it.toDomain() } ?: emptyList()
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
    ): InventoryMovement {
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
            throw Exception("Error al registrar compra: ${response.code()} ${response.errorBody()?.string()}")
        }
        return response.body()?.firstOrNull()?.toDomain()
            ?: throw Exception("Respuesta vacía al registrar compra")
    }

    override suspend fun addConsumption(
        householdId: String,
        itemId: String,
        quantity: Double,
        movementDate: String
    ): InventoryMovement {
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
            throw Exception("Error al registrar consumo: ${response.code()} ${response.errorBody()?.string()}")
        }
        return response.body()?.firstOrNull()?.toDomain()
            ?: throw Exception("Respuesta vacía al registrar consumo")
    }

    override suspend fun getSuggestions(householdId: String): List<PurchaseSuggestion> {
        val products = getProducts(householdId)
        val movements = getMovements(householdId)
        val suggestions = mutableListOf<PurchaseSuggestion>()

        for (product in products) {
            val productMovements = movements.filter { it.itemId == product.id }
            if (productMovements.isEmpty()) continue

            // Consumos del último mes
            val oneMonthAgo = LocalDate.now().minusMonths(1).toString()
            val recentConsumptions = productMovements.filter {
                it.movementType == "out" && it.movementDate >= oneMonthAgo
            }
            if (recentConsumptions.isEmpty()) continue

            val monthlyConsumption = recentConsumptions.sumOf { it.quantity }

            // Solo sugerir si el stock es menor al 50% del consumo mensual
            if (product.currentStock < monthlyConsumption * 0.5) {
                val needed = monthlyConsumption - product.currentStock

                // Precio estimado: último precio registrado
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

        return suggestions.sortedByDescending { it.estimatedCost ?: 0.0 }
    }

    // ─── Categorías ──────────────────────────────────────────────────────────────

    override suspend fun getCategories(householdId: String): List<InventoryCategory> {
        val response = api.getCategories("eq.$householdId")
        if (!response.isSuccessful) {
            throw Exception("Error al obtener categorías: ${response.code()} ${response.errorBody()?.string()}")
        }
        return response.body()?.map { it.toDomain() } ?: emptyList()
    }

    override suspend fun createCategory(householdId: String, name: String, icon: String?): InventoryCategory {
        val response = api.createCategory(
            CreateInventoryCategoryRequest(
                householdId = householdId,
                name = name,
                icon = icon
            )
        )
        if (!response.isSuccessful) {
            throw Exception("Error al crear categoría: ${response.code()} ${response.errorBody()?.string()}")
        }
        return response.body()?.firstOrNull()?.toDomain()
            ?: throw Exception("Respuesta vacía al crear categoría")
    }

    override suspend fun deleteCategory(categoryId: String) {
        val response = api.deleteCategory("eq.$categoryId")
        if (!response.isSuccessful) {
            throw Exception("Error al eliminar categoría: ${response.code()} ${response.errorBody()?.string()}")
        }
    }
}
