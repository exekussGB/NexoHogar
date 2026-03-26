package com.nexohogar.presentation.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.domain.model.InventoryCategory
import com.nexohogar.domain.model.InventoryMovement
import com.nexohogar.domain.model.Product
import com.nexohogar.domain.model.PurchaseSuggestion
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

// ─── Estadística por categoría ─────────────────────────────────────────────────
data class CategoryStat(
    val category: String,
    val totalSpent: Double,
    val productCount: Int
)

// ─── Estado de la pantalla principal ───────────────────────────────────────────
data class InventoryUiState(
    val products: List<Product> = emptyList(),
    val suggestions: List<PurchaseSuggestion> = emptyList(),
    val categoryStats: List<CategoryStat> = emptyList(),
    val categories: List<InventoryCategory> = emptyList(),
    val selectedCategory: String? = null,      // null = "Todos"
    val isLoading: Boolean = false,
    val error: String? = null
) {
    // Productos filtrados por categoría seleccionada
    val filteredProducts: List<Product>
        get() = if (selectedCategory == null) products
                else products.filter { it.category == selectedCategory }

    // Categorías disponibles: se derivan de las categorías cargadas del backend
    val availableCategories: List<String>
        get() = categories.map { it.name }
}

// ─── Estado del formulario de producto ─────────────────────────────────────────
// v8: registerAsPurchase permite crear producto con stock pero sin registrar gasto
data class ProductFormState(
    val name: String = "",
    val unit: String = "kg",
    val brand: String = "",
    val category: String = "",
    val initialQuantity: String = "",   // stock inicial opcional
    val registerAsPurchase: Boolean = false, // si true, los precios cuentan como gasto
    val store: String = "",             // tienda de la compra inicial
    val pricePerUnit: String = "",      // precio por unidad de la compra inicial
    val priceTotal: String = "",        // precio total de la compra inicial
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

// ─── Estado del formulario de categoría ──────────────────────────────────────
data class CategoryFormState(
    val name: String = "",
    val icon: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

// ─── Estado del formulario de movimiento ───────────────────────────────────────
data class MovementFormState(
    val selectedProduct: Product? = null,
    val movementType: String = "in",   // "in" o "out"
    val quantity: String = "",
    val pricePerUnit: String = "",
    val priceTotal: String = "",
    val brand: String = "",
    val store: String = "",
    val movementDate: String = LocalDate.now().toString(),
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

// ─── Estado del historial de movimientos ───────────────────────────────────────
data class MovementsUiState(
    val movements: List<InventoryMovement> = emptyList(),
    val product: Product? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class InventoryViewModel(
    private val repository: InventoryRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val householdId: String get() = tenantContext.getCurrentHouseholdId() ?: ""

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private val _productForm = MutableStateFlow(ProductFormState())
    val productForm: StateFlow<ProductFormState> = _productForm.asStateFlow()

    private val _movementForm = MutableStateFlow(MovementFormState())
    val movementForm: StateFlow<MovementFormState> = _movementForm.asStateFlow()

    private val _movementsState = MutableStateFlow(MovementsUiState())
    val movementsState: StateFlow<MovementsUiState> = _movementsState.asStateFlow()

    private val _categoryForm = MutableStateFlow(CategoryFormState())
    val categoryForm: StateFlow<CategoryFormState> = _categoryForm.asStateFlow()

    // ─── Tracking de última edición de precio (para auto-cálculo) ──────────────
    private var _productPriceEditSource: String? = null
    private var _movementPriceEditSource: String? = null

    init { loadData() }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val products = repository.getProducts(householdId)
                val movements = try { repository.getMovements(householdId) } catch (e: Exception) { emptyList() }
                val categories = try { repository.getCategories(householdId) } catch (e: Exception) { emptyList() }
                val suggestions = calcSuggestions(products, movements)
                val stats = calcCategoryStats(products, movements)
                _uiState.value = _uiState.value.copy(
                    products = products,
                    suggestions = suggestions,
                    categoryStats = stats,
                    categories = categories,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cargar datos"
                )
            }
        }
    }

    private fun calcCategoryStats(products: List<Product>, movements: List<InventoryMovement>): List<CategoryStat> {
        val productCategory = products.associate { it.id to (it.category ?: "Sin categoría") }
        val purchasesByCategory = movements
            .filter { it.movementType == "in" }
            .groupBy { productCategory[it.itemId] ?: "Sin categoría" }
        return purchasesByCategory.map { (cat, movs) ->
            val total = movs.sumOf { m ->
                when {
                    m.priceTotal != null -> m.priceTotal
                    m.pricePerUnit != null -> m.pricePerUnit * m.quantity
                    else -> 0.0
                }
            }
            val productCount = movs.map { it.itemId }.distinct().size
            CategoryStat(category = cat, totalSpent = total, productCount = productCount)
        }.sortedByDescending { it.totalSpent }
    }

    private fun calcSuggestions(products: List<Product>, movements: List<InventoryMovement>): List<PurchaseSuggestion> {
        val cutoff = LocalDate.now().minusMonths(1).toString()
        return products.mapNotNull { product ->
            val recentOut = movements.filter { it.itemId == product.id && it.movementType == "out" && it.movementDate >= cutoff }
            val monthlyConsumption = recentOut.sumOf { it.quantity }
            if (monthlyConsumption > 0 && product.currentStock < monthlyConsumption * 0.5) {
                val suggested = monthlyConsumption - product.currentStock
                val avgPrice = movements
                    .filter { it.itemId == product.id && it.movementType == "in" && it.pricePerUnit != null }
                    .map { it.pricePerUnit!! }
                    .average().takeIf { !it.isNaN() }
                PurchaseSuggestion(
                    product = product,
                    suggestedQuantity = suggested,
                    estimatedCost = avgPrice?.let { it * suggested },
                    reason = "Consumiste ${String.format("%.1f", monthlyConsumption)} ${product.unit} el último mes"
                )
            } else null
        }
    }

    fun selectCategory(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    // ─── Product form setters ───────────────────────────────────────────────────
    fun onProductNameChange(v: String)     { _productForm.value = _productForm.value.copy(name = v) }
    fun onProductUnitChange(v: String)     { _productForm.value = _productForm.value.copy(unit = v) }
    fun onProductBrandChange(v: String)    { _productForm.value = _productForm.value.copy(brand = v) }
    fun onProductCategoryChange(v: String) { _productForm.value = _productForm.value.copy(category = v) }
    fun onProductStoreChange(v: String)    { _productForm.value = _productForm.value.copy(store = v) }

    fun onProductRegisterAsPurchaseChange(v: Boolean) {
        _productForm.value = _productForm.value.copy(registerAsPurchase = v)
    }

    // ─── Product form: auto-cálculo bidireccional de precios ────────────────────
    fun onProductPricePerUnitChange(v: String) {
        _productPriceEditSource = "perUnit"
        val form = _productForm.value
        val perUnit = v.toDoubleOrNull()
        val qty = form.initialQuantity.toDoubleOrNull()
        val newTotal = if (perUnit != null && qty != null && qty > 0) {
            String.format("%.0f", perUnit * qty)
        } else ""
        _productForm.value = form.copy(pricePerUnit = v, priceTotal = newTotal)
    }

    fun onProductPriceTotalChange(v: String) {
        _productPriceEditSource = "total"
        val form = _productForm.value
        val total = v.toDoubleOrNull()
        val qty = form.initialQuantity.toDoubleOrNull()
        val newPerUnit = if (total != null && qty != null && qty > 0) {
            String.format("%.0f", total / qty)
        } else ""
        _productForm.value = form.copy(priceTotal = v, pricePerUnit = newPerUnit)
    }

    fun onProductInitialQuantityChange(v: String) {
        val form = _productForm.value
        val qty = v.toDoubleOrNull()
        val updatedForm = form.copy(initialQuantity = v)

        if (qty != null && qty > 0) {
            when (_productPriceEditSource) {
                "perUnit" -> {
                    val perUnit = form.pricePerUnit.toDoubleOrNull()
                    if (perUnit != null) {
                        _productForm.value = updatedForm.copy(priceTotal = String.format("%.0f", perUnit * qty))
                        return
                    }
                }
                "total" -> {
                    val total = form.priceTotal.toDoubleOrNull()
                    if (total != null) {
                        _productForm.value = updatedForm.copy(pricePerUnit = String.format("%.0f", total / qty))
                        return
                    }
                }
            }
        }
        _productForm.value = updatedForm
    }

    fun submitProduct() {
        val form = _productForm.value
        if (form.name.isBlank()) {
            _productForm.value = form.copy(error = "El nombre es obligatorio")
            return
        }
        val initialQty = form.initialQuantity.toDoubleOrNull()
        if (form.initialQuantity.isNotBlank() && initialQty == null) {
            _productForm.value = form.copy(error = "La cantidad inicial debe ser un número válido")
            return
        }
        viewModelScope.launch {
            _productForm.value = form.copy(isSubmitting = true, error = null)
            try {
                val product = repository.createProduct(
                    householdId = householdId,
                    name        = form.name.trim(),
                    unit        = form.unit,
                    brand       = form.brand.takeIf { it.isNotBlank() },
                    category    = form.category.takeIf { it.isNotBlank() }
                )
                // Si hay cantidad inicial, registrar movimiento de entrada
                if (initialQty != null && initialQty > 0) {
                    repository.addPurchase(
                        householdId  = householdId,
                        itemId       = product.id,
                        quantity     = initialQty,
                        movementDate = LocalDate.now().toString(),
                        pricePerUnit = if (form.registerAsPurchase) form.pricePerUnit.toDoubleOrNull() else null,
                        priceTotal   = if (form.registerAsPurchase) form.priceTotal.toDoubleOrNull() else null,
                        brand        = form.brand.takeIf { it.isNotBlank() },
                        store        = if (form.registerAsPurchase) form.store.takeIf { it.isNotBlank() } else null
                    )
                }
                _productForm.value = ProductFormState(success = true)
                loadData()
            } catch (e: Exception) {
                _productForm.value = _productForm.value.copy(
                    isSubmitting = false,
                    error = e.message ?: "Error al crear producto"
                )
            }
        }
    }

    fun resetProductForm() { _productForm.value = ProductFormState() }

    // ─── Category form setters ──────────────────────────────────────────────────
    fun onCategoryNameChange(v: String) { _categoryForm.value = _categoryForm.value.copy(name = v) }
    fun onCategoryIconChange(v: String) { _categoryForm.value = _categoryForm.value.copy(icon = v) }

    fun submitCategory() {
        val form = _categoryForm.value
        if (form.name.isBlank()) {
            _categoryForm.value = form.copy(error = "El nombre es obligatorio")
            return
        }
        viewModelScope.launch {
            _categoryForm.value = form.copy(isSubmitting = true, error = null)
            try {
                repository.createCategory(
                    householdId = householdId,
                    name = form.name.trim(),
                    icon = form.icon.takeIf { it.isNotBlank() }
                )
                _categoryForm.value = CategoryFormState(success = true)
                loadData()
            } catch (e: Exception) {
                _categoryForm.value = _categoryForm.value.copy(
                    isSubmitting = false,
                    error = e.message ?: "Error al crear categoría"
                )
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            try {
                repository.deleteCategory(categoryId)
                loadData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al eliminar categoría: ${e.message}"
                )
            }
        }
    }

    fun resetCategoryForm() { _categoryForm.value = CategoryFormState() }

    // ─── Movement form setters ──────────────────────────────────────────────────
    fun onMovementProductSelect(p: Product) { _movementForm.value = _movementForm.value.copy(selectedProduct = p) }
    fun onMovementTypeChange(t: String)     { _movementForm.value = _movementForm.value.copy(movementType = t) }
    fun onMovementBrandChange(v: String)    { _movementForm.value = _movementForm.value.copy(brand = v) }
    fun onMovementStoreChange(v: String)    { _movementForm.value = _movementForm.value.copy(store = v) }
    fun onMovementDateChange(v: String)     { _movementForm.value = _movementForm.value.copy(movementDate = v) }

    // ─── Movement form: auto-cálculo bidireccional de precios ───────────────────
    fun onMovementPricePerUnitChange(v: String) {
        _movementPriceEditSource = "perUnit"
        val form = _movementForm.value
        val perUnit = v.toDoubleOrNull()
        val qty = form.quantity.toDoubleOrNull()
        val newTotal = if (perUnit != null && qty != null && qty > 0) {
            String.format("%.0f", perUnit * qty)
        } else ""
        _movementForm.value = form.copy(pricePerUnit = v, priceTotal = newTotal)
    }

    fun onMovementPriceTotalChange(v: String) {
        _movementPriceEditSource = "total"
        val form = _movementForm.value
        val total = v.toDoubleOrNull()
        val qty = form.quantity.toDoubleOrNull()
        val newPerUnit = if (total != null && qty != null && qty > 0) {
            String.format("%.0f", total / qty)
        } else ""
        _movementForm.value = form.copy(priceTotal = v, pricePerUnit = newPerUnit)
    }

    fun onMovementQuantityChange(v: String) {
        val form = _movementForm.value
        val qty = v.toDoubleOrNull()
        val updatedForm = form.copy(quantity = v)

        if (qty != null && qty > 0) {
            when (_movementPriceEditSource) {
                "perUnit" -> {
                    val perUnit = form.pricePerUnit.toDoubleOrNull()
                    if (perUnit != null) {
                        _movementForm.value = updatedForm.copy(priceTotal = String.format("%.0f", perUnit * qty))
                        return
                    }
                }
                "total" -> {
                    val total = form.priceTotal.toDoubleOrNull()
                    if (total != null) {
                        _movementForm.value = updatedForm.copy(pricePerUnit = String.format("%.0f", total / qty))
                        return
                    }
                }
            }
        }
        _movementForm.value = updatedForm
    }

    fun submitMovement() {
        val form = _movementForm.value
        val product = form.selectedProduct
        if (product == null) {
            _movementForm.value = form.copy(error = "Selecciona un producto")
            return
        }
        val qty = form.quantity.toDoubleOrNull()
        if (qty == null || qty <= 0) {
            _movementForm.value = form.copy(error = "Cantidad inválida")
            return
        }

        viewModelScope.launch {
            _movementForm.value = form.copy(isSubmitting = true, error = null)
            try {
                if (form.movementType == "in") {
                    repository.addPurchase(
                        householdId  = householdId,
                        itemId       = product.id,
                        quantity     = qty,
                        movementDate = form.movementDate,
                        pricePerUnit = form.pricePerUnit.toDoubleOrNull(),
                        priceTotal   = form.priceTotal.toDoubleOrNull(),
                        brand        = form.brand.takeIf { it.isNotBlank() },
                        store        = form.store.takeIf { it.isNotBlank() }
                    )
                } else {
                    repository.addConsumption(
                        householdId  = householdId,
                        itemId       = product.id,
                        quantity     = qty,
                        movementDate = form.movementDate
                    )
                }
                _movementForm.value = MovementFormState(success = true)
                loadData()
            } catch (e: Exception) {
                _movementForm.value = _movementForm.value.copy(
                    isSubmitting = false,
                    error = e.message ?: "Error al registrar movimiento"
                )
            }
        }
    }

    fun resetMovementForm() { _movementForm.value = MovementFormState() }

    // ─── Consumo rápido desde la tarjeta de producto ────────────────────────────
    fun quickConsume(itemId: String, quantity: Double) {
        viewModelScope.launch {
            try {
                repository.addConsumption(
                    householdId  = householdId,
                    itemId       = itemId,
                    quantity     = quantity,
                    movementDate = LocalDate.now().toString()
                )
                loadData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al registrar consumo: ${e.message}"
                )
            }
        }
    }

    // ─── Historial de movimientos de un producto ────────────────────────────────
    fun loadMovementsForProduct(product: Product) {
        viewModelScope.launch {
            _movementsState.value = MovementsUiState(isLoading = true, product = product)
            try {
                val movements = repository.getMovements(householdId, product.id)
                _movementsState.value = MovementsUiState(
                    movements = movements,
                    product   = product,
                    isLoading = false
                )
            } catch (e: Exception) {
                _movementsState.value = _movementsState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cargar movimientos"
                )
            }
        }
    }
}
