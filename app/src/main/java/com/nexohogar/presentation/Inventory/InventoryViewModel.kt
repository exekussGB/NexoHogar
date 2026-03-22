package com.nexohogar.presentation.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

// ─── Estado de la pantalla principal ───────────────────────────────────────────
data class InventoryUiState(
    val products: List<Product> = emptyList(),
    val suggestions: List<PurchaseSuggestion> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// ─── Estado del formulario de producto ─────────────────────────────────────────
data class ProductFormState(
    val name: String = "",
    val unit: String = "kg",
    val brand: String = "",
    val initialQuantity: String = "",   // stock inicial opcional
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

    private val householdId: String
        get() = tenantContext.getCurrentHouseholdId() ?: ""


    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private val _productForm = MutableStateFlow(ProductFormState())
    val productForm: StateFlow<ProductFormState> = _productForm.asStateFlow()

    private val _movementForm = MutableStateFlow(MovementFormState())
    val movementForm: StateFlow<MovementFormState> = _movementForm.asStateFlow()

    private val _movementsState = MutableStateFlow(MovementsUiState())
    val movementsState: StateFlow<MovementsUiState> = _movementsState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val products = repository.getProducts(householdId)
                val suggestions = repository.getSuggestions(householdId)
                _uiState.value = InventoryUiState(
                    products = products,
                    suggestions = suggestions,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cargar inventario"
                )
            }
        }
    }

    // ─── Product form ──────────────────────────────────────────────────────────
    fun onProductNameChange(v: String) { _productForm.value = _productForm.value.copy(name = v) }
    fun onProductUnitChange(v: String) { _productForm.value = _productForm.value.copy(unit = v) }
    fun onProductBrandChange(v: String) { _productForm.value = _productForm.value.copy(brand = v) }
    fun onProductInitialQuantityChange(v: String) { _productForm.value = _productForm.value.copy(initialQuantity = v) }

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
                    name = form.name.trim(),
                    unit = form.unit,
                    brand = form.brand.takeIf { it.isNotBlank() }
                )
                // Si ingresaron cantidad inicial, crear movimiento "in"
                if (initialQty != null && initialQty > 0) {
                    repository.addPurchase(
                        householdId = householdId,
                        itemId = product.id,
                        quantity = initialQty,
                        movementDate = java.time.LocalDate.now().toString(),
                        pricePerUnit = null,
                        priceTotal = null,
                        brand = null,
                        store = null
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

    // ─── Movement form ─────────────────────────────────────────────────────────
    fun onMovementProductSelect(p: Product) { _movementForm.value = _movementForm.value.copy(selectedProduct = p) }
    fun onMovementTypeChange(t: String) { _movementForm.value = _movementForm.value.copy(movementType = t) }
    fun onMovementQuantityChange(v: String) { _movementForm.value = _movementForm.value.copy(quantity = v) }
    fun onMovementPricePerUnitChange(v: String) { _movementForm.value = _movementForm.value.copy(pricePerUnit = v) }
    fun onMovementPriceTotalChange(v: String) { _movementForm.value = _movementForm.value.copy(priceTotal = v) }
    fun onMovementBrandChange(v: String) { _movementForm.value = _movementForm.value.copy(brand = v) }
    fun onMovementStoreChange(v: String) { _movementForm.value = _movementForm.value.copy(store = v) }
    fun onMovementDateChange(v: String) { _movementForm.value = _movementForm.value.copy(movementDate = v) }

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
                        householdId = householdId,
                        itemId = product.id,
                        quantity = qty,
                        movementDate = form.movementDate,
                        pricePerUnit = form.pricePerUnit.toDoubleOrNull(),
                        priceTotal = form.priceTotal.toDoubleOrNull(),
                        brand = form.brand.takeIf { it.isNotBlank() },
                        store = form.store.takeIf { it.isNotBlank() }
                    )
                } else {
                    repository.addConsumption(
                        householdId = householdId,
                        itemId = product.id,
                        quantity = qty,
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

    // ─── Historial de movimientos de un producto ────────────────────────────────
    fun loadMovementsForProduct(product: Product) {
        viewModelScope.launch {
            _movementsState.value = MovementsUiState(isLoading = true, product = product)
            try {
                val movements = repository.getMovements(householdId, product.id)
                _movementsState.value = MovementsUiState(
                    movements = movements,
                    product = product,
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
