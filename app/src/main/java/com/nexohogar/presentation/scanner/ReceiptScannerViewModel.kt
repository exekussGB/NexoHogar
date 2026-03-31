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
    val parsedWithAi: Boolean = false
)

class ReceiptScannerViewModel(
    private val inventoryRepository: InventoryRepository,
    private val accountsRepository: com.nexohogar.domain.repository.AccountsRepository,
    private val tenantContext: com.nexohogar.core.tenant.TenantContext,
    private val aiReceiptParserService: AiReceiptParserService? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptScannerUiState())
    val uiState: StateFlow<ReceiptScannerUiState> = _uiState.asStateFlow()

    private val _accounts = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val accounts: StateFlow<List<Pair<String, String>>> = _accounts.asStateFlow()

    private val _categories = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val categories: StateFlow<List<Pair<String, String>>> = _categories.asStateFlow()

    init {
        loadAccountsAndCategories()
    }

    private fun loadAccountsAndCategories() {
        viewModelScope.launch {
            try {
                val householdId = tenantContext.getCurrentHouseholdId() ?: return@launch
                val result = accountsRepository.getAccountBalances(householdId)
                if (result is AppResult.Success) {
                    _accounts.value = result.data.map { it.accountId to it.accountName }
                }

                val catsResult = inventoryRepository.getCategories(householdId)
                if (catsResult is AppResult.Success) {
                    _categories.value = catsResult.data.map { it.id to it.name }
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

    fun processImage(bitmap: Bitmap) {
        _uiState.update { it.copy(step = ScannerStep.PROCESSING, isProcessing = true) }

        viewModelScope.launch {
            // ── Paso 1: Intentar con IA si está disponible ──────────────
            if (aiReceiptParserService != null) {
                try {
                    AppLogger.d("ReceiptScanner", "Intentando parseo con IA...")
                    val aiResult = aiReceiptParserService.parseReceipt(bitmap)

                    if (aiResult != null && aiResult.items.isNotEmpty()) {
                        AppLogger.d("ReceiptScanner", "IA parseó ${aiResult.items.size} productos")
                        _uiState.update {
                            it.copy(
                                step = ScannerStep.REVIEW,
                                ocrText = "(Procesado con IA)",
                                store = aiResult.store ?: "",
                                items = aiResult.items,
                                detectedTotal = aiResult.total,
                                isProcessing = false,
                                parsedWithAi = true,
                                receiptDate = aiResult.date ?: LocalDate.now().toString()
                            )
                        }
                        return@launch
                    }
                    AppLogger.d("ReceiptScanner", "IA no retornó resultados, cayendo a OCR local")
                } catch (e: Exception) {
                    AppLogger.e("ReceiptScanner", "Error con IA, cayendo a OCR local", e)
                }
            }

            // ── Paso 2: Fallback → ML Kit OCR + ChileanReceiptParser ────
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
                        parsedWithAi = false,
                        receiptDate = parseResult.date ?: LocalDate.now().toString()
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
    }
}
