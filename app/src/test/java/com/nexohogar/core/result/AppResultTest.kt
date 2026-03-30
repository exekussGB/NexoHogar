package com.nexohogar.core.result

import org.junit.Assert.*
import org.junit.Test

class AppResultTest {

    @Test
    fun `Success contains correct data`() {
        val result = AppResult.Success("hello")
        assertEquals("hello", result.data)
    }

    @Test
    fun `Error contains message`() {
        val result = AppResult.Error("algo falló")
        assertEquals("algo falló", result.message)
        assertNull(result.exception)
    }

    @Test
    fun `Error contains exception`() {
        val ex = RuntimeException("boom")
        val result = AppResult.Error("algo falló", ex)
        assertEquals("algo falló", result.message)
        assertEquals(ex, result.exception)
    }

    @Test
    fun `Loading is singleton`() {
        assertSame(AppResult.Loading, AppResult.Loading)
    }

    // ── getOrThrow() ────────────────────────────────────────────────────────

    @Test
    fun `getOrThrow returns data on Success`() {
        val result: AppResult<Int> = AppResult.Success(42)
        assertEquals(42, result.getOrThrow())
    }

    @Test(expected = Exception::class)
    fun `getOrThrow throws on Error`() {
        val result: AppResult<Int> = AppResult.Error("fail")
        result.getOrThrow()
    }

    @Test
    fun `getOrThrow Error includes message`() {
        val result: AppResult<Int> = AppResult.Error("detalle del error")
        try {
            result.getOrThrow()
            fail("Should have thrown")
        } catch (e: Exception) {
            assertEquals("detalle del error", e.message)
        }
    }

    @Test
    fun `getOrThrow Error chains original exception`() {
        val original = RuntimeException("original")
        val result: AppResult<Int> = AppResult.Error("wrapper", original)
        try {
            result.getOrThrow()
            fail("Should have thrown")
        } catch (e: Exception) {
            assertEquals(original, e.cause)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `getOrThrow throws IllegalStateException on Loading`() {
        val result: AppResult<Int> = AppResult.Loading
        result.getOrThrow()
    }

    // ── Pattern matching ────────────────────────────────────────────────────

    @Test
    fun `when expression matches Success`() {
        val result: AppResult<String> = AppResult.Success("ok")
        val value = when (result) {
            is AppResult.Success -> result.data
            is AppResult.Error -> "error"
            is AppResult.Loading -> "loading"
        }
        assertEquals("ok", value)
    }

    @Test
    fun `when expression matches Error`() {
        val result: AppResult<String> = AppResult.Error("bad")
        val value = when (result) {
            is AppResult.Success -> "ok"
            is AppResult.Error -> result.message
            is AppResult.Loading -> "loading"
        }
        assertEquals("bad", value)
    }
}
