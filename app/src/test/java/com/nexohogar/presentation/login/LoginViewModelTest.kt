package com.nexohogar.presentation.login

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.local.SessionManager
import com.nexohogar.domain.model.UserSession
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
class LoginViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: AuthRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: LoginViewModel

    private val fakeSession = UserSession(
        accessToken = "token",
        refreshToken = "refresh",
        userId = "user-1",
        email = "test@nexo.cl",
        expiresAt = System.currentTimeMillis() + 3600000
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        sessionManager = mockk(relaxed = true)
        viewModel = LoginViewModel(repository, sessionManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() {
        assertEquals(LoginState.Idle, viewModel.loginState.value)
    }

    @Test
    fun `login with blank email emits Error`() {
        viewModel.login("", "password123")
        val state = viewModel.loginState.value
        assertTrue(state is LoginState.Error)
        assertEquals("Campos obligatorios", (state as LoginState.Error).message)
    }

    @Test
    fun `login with blank password emits Error`() {
        viewModel.login("test@nexo.cl", "")
        val state = viewModel.loginState.value
        assertTrue(state is LoginState.Error)
    }

    @Test
    fun `login with both blank emits Error`() {
        viewModel.login("", "")
        assertTrue(viewModel.loginState.value is LoginState.Error)
    }

    @Test
    fun `successful login emits Success and saves session`() = runTest {
        coEvery { repository.login("test@nexo.cl", "pass123") } returns AppResult.Success(fakeSession)

        viewModel.login("test@nexo.cl", "pass123")

        assertEquals(LoginState.Success, viewModel.loginState.value)
        coVerify { sessionManager.saveSession(fakeSession) }
    }

    @Test
    fun `failed login emits Error with message`() = runTest {
        coEvery { repository.login(any(), any()) } returns AppResult.Error("Credenciales inválidas")

        viewModel.login("test@nexo.cl", "wrong")

        val state = viewModel.loginState.value
        assertTrue(state is LoginState.Error)
        assertEquals("Credenciales inválidas", (state as LoginState.Error).message)
    }

    @Test
    fun `session is not saved on login failure`() = runTest {
        coEvery { repository.login(any(), any()) } returns AppResult.Error("fail")

        viewModel.login("test@nexo.cl", "wrong")

        coVerify(exactly = 0) { sessionManager.saveSession(any()) }
    }
}
