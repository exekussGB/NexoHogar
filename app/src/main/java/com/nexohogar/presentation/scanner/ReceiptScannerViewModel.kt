package com.nexohogar.presentation.scanner

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.nexohogar.data.service.ChileanReceiptParser
import com.nexohogar.data.network.service.AiReceiptParserService
import com.nexohogar.domain.model.ScannedReceiptItem
import com.nexohogar.domain.repository.InventoryRepository
import com.nexohogar.core.tenant.TenantContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.result.getOrThrow
import com.nexohogar.core.util.AppLogger

enum class ScannerStep { CAMERA, PROCESSING, REVIEW, IMPORTING, DONE, ERROR }

data class ReceiptScannerUiState(
    val step: ScannerStep = ScannerStep.CAMERA,
    val ocrText: String = "",
    val store: String = "",
    val receiptDate: String = LocalDate.now().toString(),
    val items: List<ScannedReceiptItem> = emptyList(),
    val detectedTotal: Double? = null,
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val isProcessing: Boolean = false,
    val error: String? = null,
    val importResult: Map<String, Any>? = null,
    val parsedWithAi: Boolean = false  // Indica si se usó IA o fallback OCR
)

class ReceiptScannerViewModel(
    private val inventoryRepository: InventoryRepository,
    private val accountsRepository: com.nexohogar.domain.repository.AccountsRepository,
    private val tenantContext: com.nexohogar.core.tenant.TenantContext,
    private val aiReceiptParserService: AiReceiptParserService? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptScannerUiState())
    val uiState: StateFlow<ReceiptScannerUiState> = _uiState.asStateFlow()

    // Cuentas y categorías para los dropdowns
    private val _accounts = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val accounts: StateFlow<List<Pair<String, String>>> = _accounts.asStateFlow()

    private val _categories = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val categories: StateFlow<List<Pair<String, String>>> = _categories.asStateFlow()

    // Nombres para contexto de la IA
    private var existingProductNames: List<String> = emptyList()
    private var existingCategoryNames: List<String> = emptyList()

    init {
        loadAccountsAndCategories()
    }

    private fun loadAccountsAndCategories() {
        viewModelScope.launch {
            try {
                val householdId = tenantContext.getCurrentHouseholdId() ?: return@launch

                // Cargar cuentas
                val result = accountsRepository.getAccountBalances(householdId)
                if (result is com.nexohogar.core.result.AppResult.Success) {
                    _accounts.value = result.data.map { it.accountId to it.accountName }
                }

                // Cargar categorías
                val catsResult = inventoryRepository.getCategories(householdId)
                if (catsResult is AppResult.Success) {
                    _categories.value = catsResult.data.map { it.id to it.name }
                    existingCategoryNames = catsResult.data.map { it.name }
                }

                // Cargar nombres de productos existentes (para matching de IA)
                val prodsResult = inventoryRepository.getProducts(householdId)
                if (prodsResult is AppResult.Success) {
                    existingProductNames = prodsResult.data.map { it.name }
                }
            } catch (e: Exception) {
                AppLogger.e("ReceiptScanner", "Error cargando cuentas/categorías", e)
            }
        }
    }

    fun toggleItem(index: Int) {
        _uiState.update { state ->
            val updatedItems = state.items.toMutableList()
            if (index in updatedItems.indices) {
                val item = updatedItems[index]
                updatedItems[index] = item.copy(isSelected = !item.isSelected)
            }
            state.copy(items = updatedItems)
        }
    }

    fun updateItem(index: Int, item: ScannedReceiptItem) {
        _uiState.update { state ->
            val updatedItems = state.items.toMutableList()
            if (index in updatedItems.indices) {
                updatedItems[index] = item
            }
            state.copy(items = updatedItems)
        }
    }

    fun removeItem(index: Int) {
        _uiState.update { state ->
            val updatedItems = state.items.toMutableList()
            if (index in updatedItems.indices) {
                updatedItems.removeAt(index)
            }
            state.copy(items = updatedItems)
        }
    }

    fun addItem() {
        _uiState.update { state ->
            val updatedItems = state.items.toMutableList()
            updatedItems.add(
                ScannedReceiptItem(
                    name = "",
                    quantity = 1.0,
                    pricePerUnit = null,
                    priceTotal = null,
                    isSelected = true
                )
            )
            state.copy(items = updatedItems)
        }
    }

    fun updateStore(store: String) {
        _uiState.update { it.copy(store = store) }
    }

    fun updateDate(date: String) {
        _uiState.update { it.copy(receiptDate = date) }
    }

    fun setAccount(accountId: String) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }

    fun setCategory(categoryId: String?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun confirmImport() {
        val state = _uiState.value
        val selectedItems = state.items.filter { it.isSelected }

        if (selectedItems.isEmpty()) {
            _uiState.update { it.copy(error = "No hay productos seleccionados") }
            return
        }

        val accountId = state.selectedAccountId
        if (accountId == null) {
            _uiState.update { it.copy(error = "Debe seleccionar una cuenta") }
            return
        }

        val householdId = tenantContext.getCurrentHouseholdId() ?: run {
            _uiState.update { it.copy(error = "No se encontró el hogar activo") }
            return
        }

        val userId = tenantContext.getCurrentUserId() ?: run {
            _uiState.update { it.copy(error = "No se encontró el usuario activo") }
            return
        }

        _uiState.update {
            it.copy(step = ScannerStep.IMPORTING, isProcessing = true, error = null)
        }

        viewModelScope.launch {
            try {
                val result = inventoryRepository.importReceipt(
                    householdId = householdId,
                    userId = userId,
                    accountId = accountId,
                    categoryId = state.selectedCategoryId,
                    store = state.store.ifBlank { null },
                    receiptDate = state.receiptDate,
                    items = selectedItems
                ).getOrThrow()

                _uiState.update {
                    it.copy(
                        step = ScannerStep.DONE,
                        importResult = result,
                        isProcessing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        step = ScannerStep.ERROR,
                        error = "Error al importar: ${e.message}",
                        isProcessing = false
                    )
                }
            }
        }
    }

    fun reset() {
        _uiState.value = ReceiptScannerUiState()
    }

    fun retakePhoto() {
        _uiState.update {
            it.copy(
                step = ScannerStep.CAMERA,
                ocrText = "",
                items = emptyList(),
                error = null,
                parsedWithAi = false
            )
        }
    }

    /**
     * Procesa la imagen de la boleta.
     *
     * Flujo:
     *   1. Intenta parseo con IA (Edge Function + Gemini Vision) → más preciso
     *   2. Si falla (sin internet, error de servidor), usa fallback local:
     *      ML Kit OCR + ChileanReceiptParser
     */
    fun processImage(bitmap: android.graphics.Bitmap) {
        _uiState.update { it.copy(step = ScannerStep.PROCESSING, isProcessing = true) }

        viewModelScope.launch {
            // ── Intento 1: IA (Edge Function) ────────────────────────────────
            if (aiReceiptParserService != null) {
                val aiResult = aiReceiptParserService.parseReceipt(
                    bitmap = bitmap,
                    existingProducts = existingProductNames,
                    existingCategories = existingCategoryNames
                )

                if (aiResult is AppResult.Success) {
                    val parsed = aiResult.data
                    _uiState.update {
                        it.copy(
                            step = ScannerStep.REVIEW,
                            ocrText = "[Procesado con IA - ${parsed.items.size} productos detectados]",
                            store = parsed.store ?: "",
                            items = aiReceiptParserService.toScannedItems(parsed),
                            detectedTotal = parsed.total,
                            isProcessing = false,
                            receiptDate = parsed.date ?: LocalDate.now().toString(),
                            parsedWithAi = true
                        )
                    }
                    return@launch
                }

                // IA falló, continuar con fallback
                AppLogger.w("ReceiptScanner", "IA no disponible, usando OCR local como fallback")
            }

            // ── Intento 2: Fallback local (ML Kit + ChileanReceiptParser) ────
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())
                val result = recognizer.process(image).await()
                val ocrText = result.text
                recognizer.close()

                val parser = ChileanReceiptParser()
                val parseResult = parser.parse(ocrText)

                _uiState.update {
                    it.copy(
                        step = ScannerStep.REVIEW,
                        ocrText = ocrText,
                        store = parseResult.store ?: "",
                        items = parseResult.items,
                        detectedTotal = parseResult.total,
                        isProcessing = false,
                        receiptDate = parseResult.date ?: LocalDate.now().toString(),
                        parsedWithAi = false
                    )
                }
            } catch (e: Exception) {
                AppLogger.e("ReceiptScanner", "Error procesando imagen", e)
                _uiState.update {
                    it.copy(
                        step = ScannerStep.ERROR,
                        error = "Error al procesar imagen: ${e.message}",
                        isProcessing = false
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // TextRecognizer se crea y cierra dentro de processImage
    }
}
