package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.network.AuthApi
import com.nexohogar.data.remote.dto.LoginRequest
import com.nexohogar.data.remote.dto.LoginResponse
import com.nexohogar.data.remote.dto.UserResponse
import com.nexohogar.data.remote.dto.VerifyOtpResponse
import com.nexohogar.domain.model.UserSession
import io.mockk.*
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class AuthRepositoryImplTest {

    private lateinit var authApi: AuthApi
    private lateinit var sessionManager: SessionManager
    private lateinit var repository: AuthRepositoryImpl

    private val fakeLoginResponse = LoginResponse(
        accessToken = "access-123",
        refreshToken = "refresh-456",
        expiresIn = 3600L,
        tokenType = "bearer",
        user = UserResponse(id = "user-1", email = "test@nexo.cl")
    )

    @Before
    fun setUp() {
        authApi = mockk()
        sessionManager = mockk(relaxed = true)
        repository = AuthRepositoryImpl(authApi, sessionManager)
    }

    // ── login() ─────────────────────────────────────────────────────────────

    @Test
    fun `login success returns UserSession and saves to session`() = runTest {
        coEvery { authApi.login(any()) } returns Response.success(fakeLoginResponse)

        val result = repository.login("test@nexo.cl", "pass123")

        assertTrue(result is AppResult.Success)
        val session = (result as AppResult.Success).data
        assertEquals("access-123", session.accessToken)
        assertEquals("user-1", session.userId)
        coVerify { sessionManager.saveSession(any()) }
    }

    @Test
    fun `login with invalid credentials returns Error`() = runTest {
        coEvery { authApi.login(any()) } returns Response.error(
            401, "Unauthorized".toResponseBody(null)
        )

        val result = repository.login("test@nexo.cl", "wrong")

        assertTrue(result is AppResult.Error)
        assertEquals("Credenciales inválidas", (result as AppResult.Error).message)
    }

    @Test
    fun `login with null body returns Error`() = runTest {
        coEvery { authApi.login(any()) } returns Response.success(null)

        val result = repository.login("test@nexo.cl", "pass123")

        assertTrue(result is AppResult.Error)
    }

    @Test
    fun `login with incomplete response returns Error`() = runTest {
        val incompleteResponse = LoginResponse(
            accessToken = "token",
            refreshToken = null, // missing
            expiresIn = 3600L,
            tokenType = "bearer",
            user = UserResponse(id = "u1", email = "e@e.cl")
        )
        coEvery { authApi.login(any()) } returns Response.success(incompleteResponse)

        val result = repository.login("test@nexo.cl", "pass123")

        assertTrue(result is AppResult.Error)
        assertEquals("Respuesta de sesión inválida", (result as AppResult.Error).message)
    }

    @Test
    fun `login network error returns Error`() = runTest {
        coEvery { authApi.login(any()) } throws java.io.IOException("Timeout")

        val result = repository.login("test@nexo.cl", "pass123")

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("Error de conexión"))
    }

    // ── register() ──────────────────────────────────────────────────────────

    @Test
    fun `register success returns Success Unit`() = runTest {
        coEvery { authApi.register(any()) } returns Response.success(fakeLoginResponse)

        val result = repository.register("test@nexo.cl", "Pass123", "Juan")

        assertTrue(result is AppResult.Success)
        coVerify { sessionManager.saveSession(any()) }
    }

    @Test
    fun `register 422 returns duplicate email error`() = runTest {
        coEvery { authApi.register(any()) } returns Response.error(
            422, "already registered".toResponseBody(null)
        )

        val result = repository.register("test@nexo.cl", "Pass123", "Juan")

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("ya está registrado"))
    }

    @Test
    fun `register 400 returns invalid data error`() = runTest {
        coEvery { authApi.register(any()) } returns Response.error(
            400, "bad request".toResponseBody(null)
        )

        val result = repository.register("test@nexo.cl", "Pass123", "Juan")

        assertTrue(result is AppResult.Error)
        assertEquals("Datos inválidos", (result as AppResult.Error).message)
    }

    @Test
    fun `register network error returns Error`() = runTest {
        coEvery { authApi.register(any()) } throws java.io.IOException("No internet")

        val result = repository.register("test@nexo.cl", "Pass123", "Juan")

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("Error de conexión"))
    }

    // ── logout() ────────────────────────────────────────────────────────────

    @Test
    fun `logout clears session`() = runTest {
        repository.logout()
        coVerify { sessionManager.clearSession() }
    }

    // ── forgotPassword() ────────────────────────────────────────────────────

    @Test
    fun `forgotPassword success returns Success`() = runTest {
        coEvery { authApi.forgotPassword(any()) } returns Response.success(Unit)

        val result = repository.forgotPassword("test@nexo.cl")

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `forgotPassword failure returns Error`() = runTest {
        coEvery { authApi.forgotPassword(any()) } returns Response.error(
            500, "server error".toResponseBody(null)
        )

        val result = repository.forgotPassword("test@nexo.cl")

        assertTrue(result is AppResult.Error)
    }

    // ── verifyOtp() ─────────────────────────────────────────────────────────

    @Test
    fun `verifyOtp success returns access token`() = runTest {
        coEvery { authApi.verifyOtp(any()) } returns Response.success(
            VerifyOtpResponse(
                accessToken = "otp-token-123",
                tokenType = "bearer",
                expiresIn = 3600,
                refreshToken = "refresh-otp"
            )
        )

        val result = repository.verifyOtp("test@nexo.cl", "123456")

        assertTrue(result is AppResult.Success)
        assertEquals("otp-token-123", (result as AppResult.Success).data)
    }

    @Test
    fun `verifyOtp with null body returns Error`() = runTest {
        coEvery { authApi.verifyOtp(any()) } returns Response.success(null)

        val result = repository.verifyOtp("test@nexo.cl", "123456")

        assertTrue(result is AppResult.Error)
        assertEquals("No se recibió el token de acceso", (result as AppResult.Error).message)
    }

    @Test
    fun `verifyOtp invalid code returns Error`() = runTest {
        coEvery { authApi.verifyOtp(any()) } returns Response.error(
            422, "invalid".toResponseBody(null)
        )

        val result = repository.verifyOtp("test@nexo.cl", "000000")

        assertTrue(result is AppResult.Error)
        assertEquals("Código inválido o expirado", (result as AppResult.Error).message)
    }

    // ── updatePassword() ────────────────────────────────────────────────────

    @Test
    fun `updatePassword success returns Success`() = runTest {
        coEvery { authApi.updatePassword(any(), any()) } returns Response.success(Unit)

        val result = repository.updatePassword("token-123", "NewPass1!")

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `updatePassword failure returns Error`() = runTest {
        coEvery { authApi.updatePassword(any(), any()) } returns Response.error(
            401, "unauthorized".toResponseBody(null)
        )

        val result = repository.updatePassword("bad-token", "NewPass1!")

        assertTrue(result is AppResult.Error)
    }

    // ── getCurrentSession() ─────────────────────────────────────────────────

    @Test
    fun `getCurrentSession returns session from manager`() {
        val session = UserSession("t", "r", "u", "e@e.cl", 999L)
        every { sessionManager.fetchSession() } returns session

        assertEquals(session, repository.getCurrentSession())
    }

    @Test
    fun `getCurrentSession returns null when no session`() {
        every { sessionManager.fetchSession() } returns null

        assertNull(repository.getCurrentSession())
    }
}
