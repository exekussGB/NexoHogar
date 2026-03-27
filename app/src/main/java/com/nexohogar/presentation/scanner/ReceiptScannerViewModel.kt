package com.nexohogar.presentation.scanner

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.nexohogar.data.service.ChileanReceiptParser
import com.nexohogar.domain.model.ScannedReceiptItem
import com.nexohogar.domain.repository.InventoryRepository
import com.nexohogar.data.local.TenantContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

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
    val importResult: Map<String, Any>? = null
)

class ReceiptScannerViewModel(
    private val inventoryRepository: InventoryRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptScannerUiState())
    val uiState: StateFlow<ReceiptScannerUiState> = _uiState.asStateFlow()

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val receiptParser = ChileanReceiptParser()

    fun processImage(bitmap: Bitmap) {
        _uiState.update { it.copy(step = ScannerStep.PROCESSING, isProcessing = true, error = null) }

        viewModelScope.launch {
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val visionText = textRecognizer.process(inputImage).await()
                val ocrText = visionText.text

                val parsed = receiptParser.parse(ocrText)

                _uiState.update {
                    it.copy(
                        step = ScannerStep.REVIEW,
                        ocrText = ocrText,
                        store = parsed.store ?: "",
                        receiptDate = parsed.date ?: LocalDate.now().toString(),
                        items = parsed.items,
                        detectedTotal = parsed.total,
                        isProcessing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        step = ScannerStep.ERROR,
                        error = "Error al procesar la imagen: ${e.message}",
                        isProcessing = false
                    )
                }
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

        val householdId = tenantContext.householdId ?: run {
            _uiState.update { it.copy(error = "No se encontró el hogar activo") }
            return
        }

        val userId = tenantContext.userId ?: run {
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
                )

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
                error = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        textRecognizer.close()
    }
}