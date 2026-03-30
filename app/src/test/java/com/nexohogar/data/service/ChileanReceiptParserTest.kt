package com.nexohogar.data.service

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ChileanReceiptParserTest {

    private lateinit var parser: ChileanReceiptParser

    @Before
    fun setUp() {
        parser = ChileanReceiptParser()
    }

    // ── Store detection ─────────────────────────────────────────────────────

    @Test
    fun `detectStore identifies LIDER`() {
        val receipt = """
            WALMART CHILE COMERCIAL LTDA
            LIDER EXPRESS
            RUT: 76.933.310-4
            LECHE ENTERA   $1.290
        """.trimIndent()
        val result = parser.parse(receipt)
        assertEquals("LIDER", result.store)
    }

    @Test
    fun `detectStore identifies JUMBO`() {
        val receipt = """
            CENCOSUD RETAIL S.A.
            JUMBO COSTANERA CENTER
            PAN MOLDE   $2.390
        """.trimIndent()
        val result = parser.parse(receipt)
        assertEquals("JUMBO", result.store)
    }

    @Test
    fun `detectStore identifies SANTA ISABEL`() {
        val receipt = """
            SANTA ISABEL
            BOLETA ELECTRONICA
            ARROZ TUCAPEL   $1.890
        """.trimIndent()
        val result = parser.parse(receipt)
        assertEquals("SANTA ISABEL", result.store)
    }

    @Test
    fun `detectStore identifies UNIMARC`() {
        val receipt = """
            SMU S.A.
            UNIMARC - SUC 42
            CAFE INSTANTANEO   $3.490
        """.trimIndent()
        val result = parser.parse(receipt)
        assertEquals("UNIMARC", result.store)
    }

    @Test
    fun `detectStore returns null for unknown store`() {
        val receipt = """
            FERRETERIA DON PEPE
            TORNILLOS 10MM   $500
        """.trimIndent()
        val result = parser.parse(receipt)
        assertNull(result.store)
    }

    // ── Date detection ──────────────────────────────────────────────────────

    @Test
    fun `detectDate with slash format dd-MM-yyyy`() {
        val receipt = """
            LIDER EXPRESS
            FECHA: 15/03/2026
            PRODUCTO X   $1.000
        """.trimIndent()
        val result = parser.parse(receipt)
        assertEquals("2026-03-15", result.date)
    }

    @Test
    fun `detectDate with dash format`() {
        val receipt = """
            JUMBO
            15-03-2026 14:30
            PRODUCTO X   $1.000
        """.trimIndent()
        val result = parser.parse(receipt)
        assertEquals("2026-03-15", result.date)
    }

    @Test
    fun `detectDate with 2-digit year`() {
        val receipt = """
            UNIMARC
            FECHA: 05/01/26
            PRODUCTO X   $1.000
        """.trimIndent()
        val result = parser.parse(receipt)
        assertEquals("2026-01-05", result.date)
    }

    @Test
    fun `detectDate returns null when no date present`() {
        val receipt = """
            LIDER
            LECHE   $1.290
            TOTAL   $1.290
        """.trimIndent()
        val result = parser.parse(receipt)
        assertNull(result.date)
    }

    // ── Total detection ─────────────────────────────────────────────────────

    @Test
    fun `detectTotal finds total line`() {
        val receipt = """
            LIDER
            LECHE   $1.290
            PAN   $990
            TOTAL   $2.280
        """.trimIndent()
        val result = parser.parse(receipt)
        assertEquals(2280.0, result.total!!, 0.01)
    }

    @Test
    fun `detectTotal with large amount`() {
        val receipt = """
            JUMBO
            TOTAL $125.430
        """.trimIndent()
        val result = parser.parse(receipt)
        assertEquals(125430.0, result.total!!, 0.01)
    }

    @Test
    fun `detectTotal returns null when no total`() {
        val receipt = """
            LIDER
            LECHE   $1.290
        """.trimIndent()
        val result = parser.parse(receipt)
        assertNull(result.total)
    }

    // ── Item parsing ────────────────────────────────────────────────────────

    @Test
    fun `parse simple product lines`() {
        val receipt = """
            ALGUNA TIENDA
            LECHE ENTERA   $1.290
            PAN INTEGRAL   $2.490
            TOTAL   $3.780
        """.trimIndent()
        val result = parser.parse(receipt)

        assertEquals(2, result.items.size)
        assertEquals("LECHE ENTERA", result.items[0].name)
        assertEquals(1290.0, result.items[0].priceTotal!!, 0.01)
        assertEquals(1.0, result.items[0].quantity, 0.01)
        assertEquals("PAN INTEGRAL", result.items[1].name)
        assertEquals(2490.0, result.items[1].priceTotal!!, 0.01)
    }

    @Test
    fun `parse product with quantity prefix`() {
        val receipt = """
            TIENDA
            2 UN YOGHURT NATURAL   $1.980
            TOTAL   $1.980
        """.trimIndent()
        val result = parser.parse(receipt)

        assertTrue(result.items.isNotEmpty())
        val item = result.items[0]
        assertEquals(2.0, item.quantity, 0.01)
        assertEquals(1980.0, item.priceTotal!!, 0.01)
    }

    @Test
    fun `parse empty receipt returns no items`() {
        val result = parser.parse("")
        assertTrue(result.items.isEmpty())
        assertNull(result.store)
        assertNull(result.date)
        assertNull(result.total)
    }

    @Test
    fun `skip non-product lines`() {
        val receipt = """
            LIDER
            RUT: 76.933.310-4
            BOLETA ELECTRONICA
            FECHA: 15/03/2026
            LECHE   $1.290
            SUBTOTAL   $1.290
            IVA   $245
            TOTAL   $1.290
            EFECTIVO   $2.000
            VUELTO   $710
            GRACIAS POR SU COMPRA
        """.trimIndent()
        val result = parser.parse(receipt)

        // Should only have LECHE as product
        assertEquals(1, result.items.size)
        assertEquals("LECHE", result.items[0].name)
    }

    // ── Chilean price parsing ───────────────────────────────────────────────

    @Test
    fun `parse price with dots as thousands separator`() {
        val receipt = """
            TIENDA GENERICA
            PRODUCTO CARO   $12.990
            TOTAL   $12.990
        """.trimIndent()
        val result = parser.parse(receipt)
        assertEquals(12990.0, result.items[0].priceTotal!!, 0.01)
    }

    @Test
    fun `parse price without dots`() {
        val receipt = """
            TIENDA GENERICA
            PRODUCTO BARATO   $990
            TOTAL   $990
        """.trimIndent()
        val result = parser.parse(receipt)
        assertEquals(990.0, result.items[0].priceTotal!!, 0.01)
    }
}
