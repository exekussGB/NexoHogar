package com.nexohogar.core.tutorial

/**
 * Módulos de la app que tienen tutorial.
 */
enum class TutorialModule(
    val key: String,
    val displayName: String,
    val description: String
) {
    DASHBOARD(
        key = "tutorial_dashboard",
        displayName = "Dashboard",
        description = "Resumen general del hogar"
    ),
    ACCOUNTS(
        key = "tutorial_accounts",
        displayName = "Cuentas",
        description = "Gestión de cuentas financieras"
    ),
    TRANSACTIONS(
        key = "tutorial_transactions",
        displayName = "Transacciones",
        description = "Registro de ingresos y gastos"
    ),
    BUDGETS(
        key = "tutorial_budgets",
        displayName = "Presupuestos",
        description = "Control de presupuestos por categoría"
    ),
    INVENTORY(
        key = "tutorial_inventory",
        displayName = "Inventario",
        description = "Gestión de productos del hogar"
    ),
    RECURRING_BILLS(
        key = "tutorial_recurring_bills",
        displayName = "Gastos Recurrentes",
        description = "Pagos y suscripciones periódicas"
    ),
    HOUSEHOLD(
        key = "tutorial_household",
        displayName = "Hogar",
        description = "Configuración y miembros del hogar"
    ),
    INVITE_MEMBER(
        key = "tutorial_invite_member",
        displayName = "Invitar Miembro",
        description = "Cómo invitar personas a tu hogar"
    ),
    WISHLIST(
        key = "tutorial_wishlist",
        displayName = "Lista de Deseos",
        description = "Organiza compras pendientes del hogar"
    )
}
