package com.nexohogar.presentation.register

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.local.SessionManager
import com.nexohogar.domain.repository.AuthRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: AuthRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        sessionManager = mockk(relaxed = true)
        viewModel = RegisterViewModel(repository, sessionManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has no loading or error`() {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertNull(state.errorMessage)
    }

    @Test
    fun `register with blank name emits error`() {
        viewModel.register("", "test@nexo.cl", "Password1")
        assertEquals("Todos los campos son obligatorios", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `register with blank email emits error`() {
        viewModel.register("Juan", "", "Password1")
        assertEquals("Todos los campos son obligatorios", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `register with blank password emits error`() {
        viewModel.register("Juan", "test@nexo.cl", "")
        assertEquals("Todos los campos son obligatorios", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `register with weak password emits security error`() {
        viewModel.register("Juan", "test@nexo.cl", "abc")
        assertTrue(
            viewModel.uiState.value.errorMessage!!.contains("requisitos mínimos")
        )
    }

    @Test
    fun `register with password without uppercase emits security error`() {
        viewModel.register("Juan", "test@nexo.cl", "abcdefg1")
        assertTrue(
            viewModel.uiState.value.errorMessage!!.contains("requisitos mínimos")
        )
    }

    @Test
    fun `successful register emits success`() = runTest {
        coEvery { repository.register(any(), any(), any()) } returns AppResult.Success(Unit)

        viewModel.register("Juan Pérez", "test@nexo.cl", "Password1")

        assertTrue(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `failed register emits error message`() = runTest {
        coEvery { repository.register(any(), any(), any()) } returns AppResult.Error("El correo ya está registrado o es inválido")

        viewModel.register("Juan", "existing@nexo.cl", "Password1")

        assertFalse(viewModel.uiState.value.isSuccess)
        assertEquals("El correo ya está registrado o es inválido", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `clearError resets error message`() {
        viewModel.register("", "", "")
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `register trims email and name`() = runTest {
        coEvery { repository.register(any(), any(), any()) } returns AppResult.Success(Unit)

        viewModel.register("  Juan  ", "  test@nexo.cl  ", "Password1")

        coVerify { repository.register("test@nexo.cl", "Password1", "Juan") }
    }
}
