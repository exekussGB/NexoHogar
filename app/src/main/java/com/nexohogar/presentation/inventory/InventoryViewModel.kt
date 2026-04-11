package com.nexohogar.presentation.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.getOrThrow
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.domain.model.InventoryCategory
import com.nexohogar.domain.model.InventoryMovement
import com.nexohogar.domain.model.Product
import com.nexohogar.domain.model.PurchaseSuggestion
import com.nexohogar.domain.repository.InventoryRepository
import com.nexohogar.domain.repository.WishlistRepository
import com.nexohogar.domain.repository.FuturePurchasesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

// ─── Estadística por categoría ─────────────────────────────────────────────────
data class CategoryStat(
    val category: String,
    val totalSpent: Double,
    val productCount: Int,
    val lowStockCount: Int = 0,
    val avgConsumption: Double = 0.0
)

// ─── Estado de la pantalla principal ───────────────────────────────────────────
data class InventoryUiState(
    val products: List<Product> = emptyList(),
    val suggestions: List<PurchaseSuggestion> = emptyList(),
    val categoryStats: List<CategoryStat> = emptyList(),
    val categories: List<InventoryCategory> = emptyList(),
    val selectedCategory: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val suggestionThreshold: Double = 0.5,
    val shoppingListProducts: List<Product> = emptyList(),
    val futurePurchasesItems: List<com.nexohogar.domain.model.FuturePurchase> = emptyList()
) {
    val filteredProducts: List<Product>
        get() = if (selectedCategory == null) products
        else products.filter { it.category == selectedCategory }

    val availableCategories: List<String>
        get() = categories.map { it.name }
}

// ─── Estado del formulario de producto ─────────────────────────────────────────
data class ProductFormState(
    val name: String = "",
    val unit: String = "kg",
    val brand: String = "",
    val category: String = "",
    val minStock: String = "",
    val initialQuantity: String = "",
    val registerAsPurchase: Boolean = false,
    val store: String = "",
    val pricePerUnit: String = "",
    val priceTotal: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

// ─── Estado del formulario de edición de producto ──────────────────────────────
data class EditProductFormState(
    val productId: String = "",
    val name: String = "",
    val unit: String = "kg",
    val brand: String = "",
    val category: String = "",
    val minStock: String = "",
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
    val movementType: String = "in",
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
// ─── Validación de inputs ───────────────────────────────────────────────────
private object InputValidator {
    private val SAFE_TEXT = Regex("^[\\p{L}\\p{N}\\s\\-_.,()áéíóúÁÉÍÓÚñÑüÜ]+$")

    fun sanitizeText(input: String): String = input.trim()

    fun validateName(name: String): String? {
        if (name.isBlank()) return "El nombre es obligatorio"
        if (name.length > 100) return "El nombre no puede superar 100 caracteres"
        if (!SAFE_TEXT.matches(name)) return "El nombre contiene caracteres no permitidos"
        return null
    }

    fun validateOptionalText(value: String, fieldName: String, maxLength: Int = 100): String? {
        if (value.isBlank()) return null
        if (value.length > maxLength) return "$fieldName no puede superar $maxLength caracteres"
        if (!SAFE_TEXT.matches(value)) return "$fieldName contiene caracteres no permitidos"
        return null
    }

    fun validateQuantity(qty: Double): String? {
        if (qty <= 0) return "La cantidad debe ser mayor a 0"
        if (qty > 99999) return "La cantidad es demasiado grande"
        return null
    }

    fun validatePrice(price: Double): String? {
        if (price < 0) return "El precio no puede ser negativo"
        if (price > 999_999_999) return "El precio es demasiado grande"
        return null
    }
}
class InventoryViewModel(
    private val repository: InventoryRepository,
    private val tenantContext: TenantContext,
    private val wishlistRepository: WishlistRepository? = null,
    private val futurePurchasesRepository: FuturePurchasesRepository? = null
) : ViewModel() {

    private val householdId: String get() = tenantContext.getCurrentHouseholdId() ?: ""

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private val _productForm = MutableStateFlow(ProductFormState())
    val productForm: StateFlow<ProductFormState> = _productForm.asStateFlow()

    private val _editProductForm = MutableStateFlow(EditProductFormState())
    val editProductForm: StateFlow<EditProductFormState> = _editProductForm.asStateFlow()

    private val _movementForm = MutableStateFlow(MovementFormState())
    val movementForm: StateFlow<MovementFormState> = _movementForm.asStateFlow()

    private val _movementsState = MutableStateFlow(MovementsUiState())
    val movementsState: StateFlow<MovementsUiState> = _movementsState.asStateFlow()

    private val _categoryForm = MutableStateFlow(CategoryFormState())
    val categoryForm: StateFlow<CategoryFormState> = _categoryForm.asStateFlow()

    private var _productPriceEditSource: String? = null
    private var _movementPriceEditSource: String? = null

    init { loadData() }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val products = repository.getProducts(householdId).getOrThrow()
                val movements = try {
                    repository.getMovements(householdId).getOrThrow()
                } catch (e: Exception) { emptyList() }
                val categories = try {
                    repository.getCategories(householdId).getOrThrow()
                } catch (e: Exception) { emptyList() }
                val futurePurchases = try {
                    futurePurchasesRepository?.getFuturePurchases(householdId)?.getOrThrow() ?: emptyList()
                } catch (e: Exception) { emptyList() }

                val threshold = _uiState.value.suggestionThreshold
                val suggestions = withContext(Dispatchers.Default) {
                    calcSuggestions(products, movements, threshold)
                }
                val stats = withContext(Dispatchers.Default) {
                    calcCategoryStats(products, movements)
                }
                val shoppingList = products.filter { p ->
                    suggestions.any { it.productId == p.id }
                }

                _uiState.update {
                    it.copy(
                        products = products,
                        suggestions = suggestions,
                        categoryStats = stats,
                        categories = categories,
                        shoppingListProducts = shoppingList,
                        futurePurchasesItems = futurePurchases,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Error al cargar datos")
                }
            }
        }
    }

    private fun calcCategoryStats(
        products: List<Product>,
        movements: List<InventoryMovement>
    ): List<CategoryStat> {
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
            val lowStockCount = products.count { p ->
                p.category == cat && p.currentStock < 1.0
            }
            CategoryStat(
                category = cat,
                totalSpent = total,
                productCount = productCount,
                lowStockCount = lowStockCount
            )
        }.sortedByDescending { it.totalSpent }
    }

    private fun calcSuggestions(
        products: List<Product>,
        movements: List<InventoryMovement>,
        threshold: Double = 0.5
    ): List<PurchaseSuggestion> {
        val cutoff = LocalDate.now().minusMonths(1).toString()
        return products.mapNotNull { product ->
            val recentOut = movements.filter {
                it.itemId == product.id && it.movementType == "out" && it.movementDate >= cutoff
            }
            val monthlyConsumption = recentOut.sumOf { it.quantity }
            val minStockThreshold = product.minStock?.toDouble() ?: (monthlyConsumption * threshold)
            if (monthlyConsumption > 0 && product.currentStock < minStockThreshold) {
                val suggested = monthlyConsumption - product.currentStock
                val avgPrice = movements
                    .filter { it.itemId == product.id && it.movementType == "in" && it.pricePerUnit != null }
                    .map { it.pricePerUnit!! }
                    .average().takeIf { !it.isNaN() }
                PurchaseSuggestion(
                    productId = product.id,
                    productName = product.name,
                    unit = product.unit,
                    category = product.category,
                    currentStock = product.currentStock,
                    suggestedQuantity = suggested,
                    estimatedCost = avgPrice?.let { it * suggested },
                    reason = "Consumiste ${String.format("%.1f", monthlyConsumption)} ${product.unit} el último mes"
                )
            } else null
        }
    }

    // ─── Generar texto lista de compras ─────────────────────────────────────────
    fun generateShoppingListText(): String {
        val products = _uiState.value.shoppingListProducts
        if (products.isEmpty()) return ""
        val sb = StringBuilder("🛒 Lista de compras NexoHogar\n\n")
        products.forEach { p ->
            val suggestion = _uiState.value.suggestions.find { it.productId == p.id }
            val qty = suggestion?.suggestedQuantity?.let {
                " - ${String.format("%.1f", it)} ${p.unit}"
            } ?: ""
            sb.appendLine("• ${p.name}$qty")
        }
        return sb.toString().trim()
    }

    // ─── Añadir sugerencia a future_purchases ────────────────────────────────────
    fun addSuggestionToWishlist(suggestion: PurchaseSuggestion) {
        android.util.Log.d("InventoryVM", "=== 📝 addSuggestionToWishlist START ===")
        android.util.Log.d("InventoryVM", "Product: ${suggestion.productName}")
        android.util.Log.d("InventoryVM", "futurePurchasesRepository != null: ${futurePurchasesRepository != null}")

        val repo = futurePurchasesRepository ?: run {
            android.util.Log.e("InventoryVM", "❌ FuturePurchasesRepository es null!")
            _uiState.update { it.copy(error = "Error: FuturePurchasesRepository no inicializado") }
            return
        }

        viewModelScope.launch {
            android.util.Log.d("InventoryVM", "📡 Iniciando viewModelScope...")

            if (futurePurchasesRepository != null) {
                android.util.Log.d("InventoryVM", "📨 Llamando a createFuturePurchase()")
                val hId = householdId
                val uId = tenantContext.getCurrentUserId() ?: ""
                android.util.Log.d("InventoryVM", "   householdId: '$hId' (isEmpty: ${hId.isEmpty()})")
                android.util.Log.d("InventoryVM", "   createdBy: '$uId' (isEmpty: ${uId.isEmpty()})")
                android.util.Log.d("InventoryVM", "   name: ${suggestion.productName}")

                if (hId.isEmpty() || uId.isEmpty()) {
                    android.util.Log.e("InventoryVM", "⚠️ householdId o userId están vacíos!")
                    android.util.Log.e("InventoryVM", "   tenantContext.getCurrentHouseholdId(): ${tenantContext.getCurrentHouseholdId()}")
                    android.util.Log.e("InventoryVM", "   tenantContext.getCurrentUserId(): ${tenantContext.getCurrentUserId()}")
                }

                val result = futurePurchasesRepository.createFuturePurchase(
                    householdId = hId,
                    name = suggestion.productName,
                    description = suggestion.unit?.let { "Cant. sugerida: ${suggestion.suggestedQuantity} $it" },
                    category = suggestion.category,
                    estimatedPrice = suggestion.estimatedCost,
                    priority = "MEDIUM",
                    createdBy = uId
                )

                android.util.Log.d("InventoryVM", "✅ createFuturePurchase() retornó")
                android.util.Log.d("InventoryVM", "   Result type: ${result.javaClass.simpleName}")

                when (result) {
                    is com.nexohogar.core.result.AppResult.Success -> {
                        android.util.Log.d("InventoryVM", "✅✅✅ SUCCESS!")
                        android.util.Log.d("InventoryVM", "   ID: ${result.data.id}")
                        android.util.Log.d("InventoryVM", "   Name: ${result.data.name}")
                        android.util.Log.d("InventoryVM", "   HouseholdId: ${result.data.householdId}")
                        _uiState.update { it.copy(successMessage = "${suggestion.productName} agregado a la lista de compras") }
                        // 🔄 RECARGAR la lista del carrito
                        loadData()
                    }
                    is com.nexohogar.core.result.AppResult.Error -> {
                        android.util.Log.e("InventoryVM", "❌❌❌ ERROR!")
                        android.util.Log.e("InventoryVM", "   Message: ${result.message}")
                        _uiState.update { it.copy(error = "Error al agregar: ${result.message}") }
                    }
                    is com.nexohogar.core.result.AppResult.Loading -> {
                        android.util.Log.d("InventoryVM", "⏳ Loading state")
                    }
                }
            } else {
                android.util.Log.d("InventoryVM", "⚠️ Usando WishlistRepository como fallback")
                try {
                    wishlistRepository?.createWishlistItem(
                        householdId = householdId,
                        name = suggestion.productName,
                        description = suggestion.unit?.let { "Cant. sugerida: ${suggestion.suggestedQuantity} $it" },
                        price = suggestion.estimatedCost,
                        priority = "MEDIUM",
                        createdBy = tenantContext.getCurrentUserId() ?: ""
                    )
                    _uiState.update { it.copy(successMessage = "${suggestion.productName} agregado a la lista de deseos") }
                } catch (e: Exception) {
                    android.util.Log.e("InventoryVM", "❌ Error con fallback: ${e.message}", e)
                    _uiState.update { it.copy(error = "Error al agregar: ${e.message}") }
                }
            }
            android.util.Log.d("InventoryVM", "=== 📝 addSuggestionToWishlist END ===")
        }
    }

    // ─── Eliminar item de future_purchases ────────────────────────────────────
    fun deleteFuturePurchase(itemId: String) {
        viewModelScope.launch {
            try {
                futurePurchasesRepository?.deleteFuturePurchase(itemId)?.getOrThrow()
                _uiState.update { it.copy(successMessage = "Item eliminado de la lista de compras") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al eliminar: ${e.message}") }
            }
        }
    }

    fun selectCategory(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    // ─── Editar producto ────────────────────────────────────────────────────────
    fun startEditProduct(product: Product) {
        _editProductForm.value = EditProductFormState(
            productId = product.id,
            name = product.name,
            unit = product.unit,
            brand = product.brand ?: "",
            category = product.category ?: "",
            minStock = product.minStock?.toString() ?: ""
        )
    }

    fun onEditNameChange(v: String) { _editProductForm.update { it.copy(name = v) } }
    fun onEditUnitChange(v: String) { _editProductForm.update { it.copy(unit = v) } }
    fun onEditBrandChange(v: String) { _editProductForm.update { it.copy(brand = v) } }
    fun onEditCategoryChange(v: String) { _editProductForm.update { it.copy(category = v) } }
    fun onEditMinStockChange(v: String) { _editProductForm.update { it.copy(minStock = v) } }

    fun submitEditProduct() {
        val form = _editProductForm.value
        val productId = _currentEditingProductId

        // Validar inputs
        val nameError = InputValidator.validateName(form.name)
        if (productId == null || nameError != null) {
            _editProductForm.value = form.copy(error = nameError ?: "Error interno")
            return
        }
        val brandError = InputValidator.validateOptionalText(form.brand, "La marca")
        if (brandError != null) {
            _editProductForm.value = form.copy(error = brandError)
            return
        }

        viewModelScope.launch {
            _editProductForm.value = form.copy(isSubmitting = true, error = null)
            try {
                repository.updateProduct(
                    productId = productId,
                    name      = InputValidator.sanitizeText(form.name),
                    unit      = form.unit,
                    brand     = form.brand.takeIf { it.isNotBlank() }
                        ?.let { InputValidator.sanitizeText(it) },
                    category  = form.category.takeIf { it.isNotBlank() },
                    minStock  = form.minStock.toDoubleOrNull()?.toInt()
                ).getOrThrow()

                _editProductForm.value = EditProductFormState()
                _currentEditingProductId = null
                loadData()
            } catch (e: Exception) {
                _editProductForm.value = _editProductForm.value.copy(
                    isSubmitting = false,
                    error = e.message ?: "Error al actualizar producto"
                )
            }
        }
    }

    fun resetEditProductForm() { _editProductForm.value = EditProductFormState() }

    // ─── Product form setters ───────────────────────────────────────────────────
    fun onProductNameChange(v: String) { _productForm.update { it.copy(name = v) } }
    fun onProductUnitChange(v: String) { _productForm.update { it.copy(unit = v) } }
    fun onProductBrandChange(v: String) { _productForm.update { it.copy(brand = v) } }
    fun onProductCategoryChange(v: String) { _productForm.update { it.copy(category = v) } }
    fun onProductStoreChange(v: String) { _productForm.update { it.copy(store = v) } }
    fun onProductMinStockChange(v: String) { _productForm.update { it.copy(minStock = v) } }
    fun onProductRegisterAsPurchaseChange(v: Boolean) { _productForm.update { it.copy(registerAsPurchase = v) } }

    fun onProductPricePerUnitChange(v: String) {
        _productPriceEditSource = "perUnit"
        val form = _productForm.value
        val perUnit = v.toDoubleOrNull()
        val qty = form.initialQuantity.toDoubleOrNull()
        val newTotal = if (perUnit != null && qty != null && qty > 0) String.format("%.0f", perUnit * qty) else ""
        _productForm.update { it.copy(pricePerUnit = v, priceTotal = newTotal) }
    }

    fun onProductPriceTotalChange(v: String) {
        _productPriceEditSource = "total"
        val form = _productForm.value
        val total = v.toDoubleOrNull()
        val qty = form.initialQuantity.toDoubleOrNull()
        val newPerUnit = if (total != null && qty != null && qty > 0) String.format("%.0f", total / qty) else ""
        _productForm.update { it.copy(priceTotal = v, pricePerUnit = newPerUnit) }
    }

    fun onProductInitialQuantityChange(v: String) {
        val form = _productForm.value
        val qty = v.toDoubleOrNull()
        val updatedForm = form.copy(initialQuantity = v)
        if (qty != null && qty > 0) {
            when (_productPriceEditSource) {
                "perUnit" -> {
                    val perUnit = form.pricePerUnit.toDoubleOrNull()
                    if (perUnit != null) { _productForm.value = updatedForm.copy(priceTotal = String.format("%.0f", perUnit * qty)); return }
                }
                "total" -> {
                    val total = form.priceTotal.toDoubleOrNull()
                    if (total != null) { _productForm.value = updatedForm.copy(pricePerUnit = String.format("%.0f", total / qty)); return }
                }
            }
        }
        _productForm.value = updatedForm
    }

    fun submitProduct() {
        val form = _productForm.value

        // Validar y sanitizar inputs antes de enviar al API
        val nameError = InputValidator.validateName(form.name)
        if (nameError != null) {
            _productForm.value = form.copy(error = nameError)
            return
        }
        val brandError = InputValidator.validateOptionalText(form.brand, "La marca")
        if (brandError != null) {
            _productForm.value = form.copy(error = brandError)
            return
        }
        val storeError = InputValidator.validateOptionalText(form.store, "La tienda")
        if (storeError != null) {
            _productForm.value = form.copy(error = storeError)
            return
        }
        val initialQty = form.initialQuantity.toDoubleOrNull()
        if (form.initialQuantity.isNotBlank()) {
            if (initialQty == null) {
                _productForm.value = form.copy(error = "La cantidad inicial debe ser un número válido")
                return
            }
            val qtyError = InputValidator.validateQuantity(initialQty)
            if (qtyError != null) {
                _productForm.value = form.copy(error = qtyError)
                return
            }
        }
        val pricePerUnit = form.pricePerUnit.toDoubleOrNull()
        if (pricePerUnit != null) {
            val priceError = InputValidator.validatePrice(pricePerUnit)
            if (priceError != null) {
                _productForm.value = form.copy(error = priceError)
                return
            }
        }

        viewModelScope.launch {
            _productForm.value = form.copy(isSubmitting = true, error = null)
            try {
                val product = repository.createProduct(
                    householdId = householdId,
                    name        = InputValidator.sanitizeText(form.name),
                    unit        = form.unit,
                    brand       = form.brand.takeIf { it.isNotBlank() }
                        ?.let { InputValidator.sanitizeText(it) },
                    category    = form.category.takeIf { it.isNotBlank() }
                ).getOrThrow()

                if (initialQty != null && initialQty > 0) {
                    repository.addPurchase(
                        householdId  = householdId,
                        itemId       = product.id,
                        quantity     = initialQty,
                        movementDate = LocalDate.now().toString(),
                        pricePerUnit = if (form.registerAsPurchase) pricePerUnit else null,
                        priceTotal   = if (form.registerAsPurchase) form.priceTotal.toDoubleOrNull() else null,
                        brand        = form.brand.takeIf { it.isNotBlank() }
                            ?.let { InputValidator.sanitizeText(it) },
                        store        = if (form.registerAsPurchase)
                            form.store.takeIf { it.isNotBlank() }
                                ?.let { InputValidator.sanitizeText(it) }
                        else null
                    ).getOrThrow()
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

    // ─── Category form ──────────────────────────────────────────────────────────
    fun onCategoryNameChange(v: String) { _categoryForm.update { it.copy(name = v) } }
    fun onCategoryIconChange(v: String) { _categoryForm.update { it.copy(icon = v) } }

    fun submitCategory() {
        val form = _categoryForm.value
        if (form.name.isBlank()) { _categoryForm.update { it.copy(error = "El nombre es obligatorio") }; return }
        viewModelScope.launch {
            _categoryForm.update { it.copy(isSubmitting = true, error = null) }
            try {
                repository.createCategory(householdId = householdId, name = form.name.trim(), icon = form.icon.takeIf { it.isNotBlank() }).getOrThrow()
                _categoryForm.value = CategoryFormState(success = true)
                loadData()
            } catch (e: Exception) {
                _categoryForm.update { it.copy(isSubmitting = false, error = e.message ?: "Error al crear categoría") }
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            try {
                repository.deleteCategory(categoryId).getOrThrow()
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al eliminar categoría: ${e.message}") }
            }
        }
    }

    fun resetCategoryForm() { _categoryForm.value = CategoryFormState() }

    // ─── Movement form ──────────────────────────────────────────────────────────
    fun onMovementProductSelect(p: Product) { _movementForm.update { it.copy(selectedProduct = p) } }
    fun onMovementTypeChange(t: String) { _movementForm.update { it.copy(movementType = t) } }
    fun onMovementBrandChange(v: String) { _movementForm.update { it.copy(brand = v) } }
    fun onMovementStoreChange(v: String) { _movementForm.update { it.copy(store = v) } }
    fun onMovementDateChange(v: String) { _movementForm.update { it.copy(movementDate = v) } }

    fun onMovementPricePerUnitChange(v: String) {
        _movementPriceEditSource = "perUnit"
        val form = _movementForm.value
        val perUnit = v.toDoubleOrNull()
        val qty = form.quantity.toDoubleOrNull()
        val newTotal = if (perUnit != null && qty != null && qty > 0) String.format("%.0f", perUnit * qty) else ""
        _movementForm.update { it.copy(pricePerUnit = v, priceTotal = newTotal) }
    }

    fun onMovementPriceTotalChange(v: String) {
        _movementPriceEditSource = "total"
        val form = _movementForm.value
        val total = v.toDoubleOrNull()
        val qty = form.quantity.toDoubleOrNull()
        val newPerUnit = if (total != null && qty != null && qty > 0) String.format("%.0f", total / qty) else ""
        _movementForm.update { it.copy(priceTotal = v, pricePerUnit = newPerUnit) }
    }

    fun onMovementQuantityChange(v: String) {
        val form = _movementForm.value
        val qty = v.toDoubleOrNull()
        val updatedForm = form.copy(quantity = v)
        if (qty != null && qty > 0) {
            when (_movementPriceEditSource) {
                "perUnit" -> {
                    val perUnit = form.pricePerUnit.toDoubleOrNull()
                    if (perUnit != null) { _movementForm.value = updatedForm.copy(priceTotal = String.format("%.0f", perUnit * qty)); return }
                }
                "total" -> {
                    val total = form.priceTotal.toDoubleOrNull()
                    if (total != null) { _movementForm.value = updatedForm.copy(pricePerUnit = String.format("%.0f", total / qty)); return }
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
        // Validar cantidad y precios
        InputValidator.validateQuantity(qty)?.let {
            _movementForm.value = form.copy(error = it)
            return
        }
        val pricePerUnit = form.pricePerUnit.toDoubleOrNull()
        pricePerUnit?.let {
            InputValidator.validatePrice(it)?.let { err ->
                _movementForm.value = form.copy(error = err)
                return
            }
        }
        val brandError = InputValidator.validateOptionalText(form.brand, "La marca")
        if (brandError != null) {
            _movementForm.value = form.copy(error = brandError)
            return
        }
        val storeError = InputValidator.validateOptionalText(form.store, "La tienda")
        if (storeError != null) {
            _movementForm.value = form.copy(error = storeError)
            return
        }
    }

    fun resetMovementForm() { _movementForm.value = MovementFormState() }

    // ─── Consumo rápido ─────────────────────────────────────────────────────────
    fun quickConsume(itemId: String, quantity: Double) {
        viewModelScope.launch {
            try {
                repository.addConsumption(
                    householdId = householdId, itemId = itemId,
                    quantity = quantity, movementDate = LocalDate.now().toString()
                ).getOrThrow()
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al registrar consumo: ${e.message}") }
            }
        }
    }

    // ─── Historial de movimientos ────────────────────────────────────────────────
    fun loadMovementsForProduct(product: Product) {
        viewModelScope.launch {
            _movementsState.value = MovementsUiState(isLoading = true, product = product)
            try {
                val movements = repository.getMovements(householdId, product.id).getOrThrow()
                _movementsState.value = MovementsUiState(movements = movements, product = product, isLoading = false)
            } catch (e: Exception) {
                _movementsState.update { it.copy(isLoading = false, error = e.message ?: "Error al cargar movimientos") }
            }
        }
    }
}