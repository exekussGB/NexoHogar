package com.nexohogar.data.mapper

import com.nexohogar.data.remote.dto.CategoryResponse
import org.junit.Assert.*
import org.junit.Test

class CategoryMappersTest {

    @Test
    fun `CategoryResponse toDomain maps all fields`() {
        val response = CategoryResponse(
            id = "cat-1",
            name = "Alimentación",
            type = "expense",
            householdId = "hh-1"
        )
        val category = response.toDomain()

        assertEquals("cat-1", category.id)
        assertEquals("Alimentación", category.name)
        assertEquals("expense", category.type)
    }

    @Test
    fun `List CategoryResponse toDomain maps all items`() {
        val responses = listOf(
            CategoryResponse(id = "1", name = "Comida", type = "expense", householdId = "hh-1"),
            CategoryResponse(id = "2", name = "Sueldo", type = "income", householdId = "hh-1"),
            CategoryResponse(id = "3", name = "Transporte", type = "expense", householdId = "hh-1")
        )
        val categories = responses.toDomain()

        assertEquals(3, categories.size)
        assertEquals("Comida", categories[0].name)
        assertEquals("income", categories[1].type)
    }

    @Test
    fun `empty list toDomain returns empty list`() {
        val categories = emptyList<CategoryResponse>().toDomain()
        assertTrue(categories.isEmpty())
    }
}
