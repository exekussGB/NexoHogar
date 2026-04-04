package com.nexohogar.data.mapper

import com.nexohogar.data.remote.dto.CategoryResponse
import com.nexohogar.domain.model.Category

/**
 * Mappers para convertir DTOs de categorías a modelos de dominio.
 */

fun CategoryResponse.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        type = type,
        icon = icon
    )
}

fun List<CategoryResponse>.toDomain(): List<Category> {
    return map { it.toDomain() }
}
