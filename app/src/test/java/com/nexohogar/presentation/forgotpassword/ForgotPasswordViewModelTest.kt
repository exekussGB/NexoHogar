package com.nexohogar.presentation.forgotpassword

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.repository.AuthRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests para ForgotPasswordViewModel.
 *
 * Nota: El test de email inválido requiere Robolectric porque el ViewModel
 * usa android.util.Patterns.EMAIL_ADDRESS. Se incluye con @Test pero puede
 * necesitar @Config(sdk = [33]) con Robolectric.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: AuthRepository
    private lateinit var viewModel: ForgotPasswordViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = ForgotPasswordViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() {
        assertEquals(ForgotPasswordState.Idle, viewModel.state.value)
    }

    @Test
    fun `blank email emits Error`() {
        viewModel.sendRecoveryEmail("")
        val state = viewModel.state.value
        assertTrue(state is ForgotPasswordState.Error)
        assertEquals("Ingresa un correo válido", (state as ForgotPasswordState.Error).message)
    }

    @Test
    fun `whitespace-only email emits Error`() {
        viewModel.sendRecoveryEmail("   ")
        assertTrue(viewModel.state.value is ForgotPasswordState.Error)
    }

    @Test
    fun `successful recovery emits Success`() = runTest {
        coEvery { repository.forgotPassword("test@nexo.cl") } returns AppResult.Success(Unit)

        viewModel.sendRecoveryEmail("test@nexo.cl")

        assertEquals(ForgotPasswordState.Success, viewModel.state.value)
    }

    @Test
    fun `failed recovery emits Error with message`() = runTest {
        coEvery { repository.forgotPassword(any()) } returns AppResult.Error("No se pudo enviar")

        viewModel.sendRecoveryEmail("test@nexo.cl")

        val state = viewModel.state.value
        assertTrue(state is ForgotPasswordState.Error)
        assertEquals("No se pudo enviar", (state as ForgotPasswordState.Error).message)
    }

    @Test
    fun `email is trimmed before sending`() = runTest {
        coEvery { repository.forgotPassword("test@nexo.cl") } returns AppResult.Success(Unit)

        viewModel.sendRecoveryEmail("  test@nexo.cl  ")

        coVerify { repository.forgotPassword("test@nexo.cl") }
    }
}
